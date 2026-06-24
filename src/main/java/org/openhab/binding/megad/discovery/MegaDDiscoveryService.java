/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.megad.discovery;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.openhab.binding.megad.MegaDBindingConstants;
import org.openhab.binding.megad.MegaDHTTPResponse;
import org.openhab.binding.megad.MegaDHttpHelpers;
import org.openhab.binding.megad.dto.MegaDHardware;
import org.openhab.binding.megad.dto.MegaDI2CSensors;
import org.openhab.binding.megad.enums.MegaDModesEnum;
import org.openhab.binding.megad.enums.MegaDTypesEnum;
import org.openhab.binding.megad.handler.MegaDDeviceHandler;
import org.openhab.core.OpenHAB;
import org.openhab.core.config.discovery.AbstractDiscoveryService;
import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.config.discovery.DiscoveryResultBuilder;
import org.openhab.core.config.discovery.DiscoveryService;
import org.openhab.core.io.net.http.HttpClientFactory;
import org.openhab.core.net.NetUtil;
import org.openhab.core.thing.ThingUID;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

/**
 * Discovery service for Megad
 *
 * @author Petr Shatsillo - Initial contribution
 *
 */
@Component(service = DiscoveryService.class, configurationPid = "discovery.megad")
@NonNullByDefault
public class MegaDDiscoveryService extends AbstractDiscoveryService {
    public static @Nullable List<MegaDDeviceHandler> megaDDeviceHandlerList = new ArrayList<>();
    public static @Nullable Map<String, MegaDI2CSensors> megaDI2CSensorsList = new HashMap<>();
    private final Logger logger = LoggerFactory.getLogger(MegaDDiscoveryService.class);
    @Nullable
    DatagramSocket socket;
    private @Nullable ScheduledFuture<?> backgroundDiscoveryFuture;
    private @Nullable ScheduledFuture<?> backgroundCheckFirmwareFuture;
    private @Nullable ScheduledFuture<?> backgroundSensorsFuture;

    static String urlString = "https://raw.githubusercontent.com/Pshatsillo/openhab2MegadBinding/refs/heads/jsons/sensors.json";
    public static String actualFirmware = "";
    private final HttpClient httpClient;

    @Activate
    public MegaDDiscoveryService(@Reference HttpClientFactory httpClientFactory) {
        super(Collections.singleton(MegaDBindingConstants.THING_TYPE_DEVICE), 30, true);
        httpClient = httpClientFactory.getCommonHttpClient();
    }

    @Override
    public synchronized void abortScan() {
        logger.info("abortScan");
        super.abortScan();
    }

    @Override
    protected synchronized void stopScan() {
        logger.info("stopScan");
        final DatagramSocket socket = this.socket;
        if (socket != null) {
            if (!socket.isClosed()) {
                socket.close();
            }
        }
        super.stopScan();
    }

    @Override
    protected void startScan() {
        logger.info("StartScan");
        removeOlderResults(getTimestampOfLastScan());
        discoverPortsOfKnownDevices();
        try {
            socket = new DatagramSocket(42000);
            final DatagramSocket socket = this.socket;
            if (socket != null) {
                socket.setSoTimeout(50000);
            }
        } catch (SocketException e) {
            logger.debug("{}", e.getMessage());
        }
        Thread server = getThread();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            // e.printStackTrace();
        }
        @Nullable
        Runnable scanner1 = createScanner();
        scanner1.run();
        logger.debug("StartScan");
        try {
            Thread.sleep(10000);
        } catch (InterruptedException ignored) {
        }
        server.interrupt();
    }

    private Thread getThread() {
        Thread server = new Thread(new Runnable() {
            final byte[] buffer = new byte[5];
            final DatagramSocket loSock = Objects.requireNonNull(socket);

            @Override
            public void run() {
                while (true) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                    try {
                        loSock.receive(packet);
                    } catch (IOException e) {
                        logger.debug("Scan socket closed: {}", e.getLocalizedMessage());
                        break;
                    }
                    byte[] received = packet.getData();

                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        logger.error("{}", e.getLocalizedMessage());
                    }
                    receivePacketAndDiscover(received);
                }
            }
        });

        server.start();
        return server;
    }

    @Override
    protected void startBackgroundDiscovery() {
        scheduler.submit(() -> {
            try {
                readSensorsFile(true);
            } catch (Exception e) {
                logger.warn("Error during initial MegaD sensors metadata load", e);
            }
        });
        // logger.error("startBackgroundDiscovery");
        backgroundDiscoveryFuture = scheduler.scheduleWithFixedDelay(this::discoverPortsOfKnownDevices, 10, 30,
                TimeUnit.SECONDS);
        backgroundCheckFirmwareFuture = scheduler.scheduleWithFixedDelay(this::checkFirmware, 1, 3, TimeUnit.HOURS);
        backgroundSensorsFuture = scheduler.scheduleWithFixedDelay(this::refreshSensorsDefinitions, 5, 12,
                TimeUnit.HOURS);
    }

    @Override
    protected void stopBackgroundDiscovery() {
        // logger.error("stopBackgroundDiscovery");
        ScheduledFuture<?> discovery = backgroundDiscoveryFuture;
        ScheduledFuture<?> firmware = backgroundCheckFirmwareFuture;
        ScheduledFuture<?> sensors = backgroundSensorsFuture;

        if (discovery != null) {
            discovery.cancel(true);
            backgroundDiscoveryFuture = null;
        }

        if (firmware != null) {
            firmware.cancel(true);
            backgroundCheckFirmwareFuture = null;
        }

        if (sensors != null) {
            sensors.cancel(true);
            backgroundSensorsFuture = null;
        }

        super.stopBackgroundDiscovery();
    }

    private Runnable createScanner() {
        return () -> {
            // long timestampOfLastScan = getTimestampOfLastScan();
            try {
                DatagramSocket socket = new DatagramSocket();
                byte[] buf = { (byte) 170, 0, 12, (byte) 218, (byte) 202 };
                for (InetAddress broadcastAddress : getBroadcastAddresses()) {
                    logger.debug("Broadcast address is {}", broadcastAddress.toString());
                    DatagramPacket packet = new DatagramPacket(buf, buf.length, broadcastAddress, 52000);
                    socket.send(packet);
                }
                socket.close();
            } catch (IOException e) {
                logger.warn("{}", e.getMessage());
            }
            // removeOlderResults(timestampOfLastScan);
        };
    }

    private void receivePacketAndDiscover(byte[] result) {
        String ips = String.format("%d.%d.%d.%d", result[1] & 0xFF, result[2] & 0xFF, result[3] & 0xFF,
                result[4] & 0xFF);
        ThingUID thingUID = new ThingUID(MegaDBindingConstants.THING_TYPE_DEVICE, ips.replace('.', '_'));
        DiscoveryResult resultS = DiscoveryResultBuilder.create(thingUID).withProperty("hostname", ips)
                .withRepresentationProperty("hostname").withLabel("megad " + ips).build();
        thingDiscovered(resultS);

        logger.debug("Found MegaD at: {}", ips);
    }

    private List<InetAddress> getBroadcastAddresses() {
        ArrayList<InetAddress> addresses = new ArrayList<>();

        for (String broadcastAddress : NetUtil.getAllBroadcastAddresses()) {
            try {
                addresses.add(InetAddress.getByName(broadcastAddress));
            } catch (UnknownHostException e) {
                logger.warn("Error broadcasting to {}: {}", broadcastAddress, e.getMessage());
            }
        }

        return addresses;
    }

    private synchronized void discoverPortsOfKnownDevices() {
        // logger.info("Scanning...");
        List<MegaDDeviceHandler> megaDDeviceHandlerList = MegaDDiscoveryService.megaDDeviceHandlerList;
        try {
            if (megaDDeviceHandlerList != null) {
                if (!megaDDeviceHandlerList.isEmpty()) {
                    for (MegaDDeviceHandler mega : megaDDeviceHandlerList) {
                        for (int i = 0; i <= mega.megaDHardware.getPortsCount(); i++) {
                            MegaDHardware.Port port = mega.megaDHardware.getPort(i);
                            if (port != null) {
                                if (!port.isExclude()) {
                                    // logger.debug("Discovering port {}", i);
                                    // port = mega.megaDHardware.getPortStatus(i);
                                    // if (port != null) {
                                    MegaDTypesEnum portType = port.getPty();
                                    if (portType != MegaDTypesEnum.NC) {
                                        if (port.getM() != MegaDModesEnum.SCL) {
                                            String label = "";
                                            if (!mega.megaDHardware.getMdid().isEmpty()) {
                                                label = mega.megaDHardware.getMdid();
                                            }
                                            addToDiscoverThing(mega, label, i);
                                        }
                                    }
                                    // }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Discovery service error {}", e.getLocalizedMessage());
        }
    }

    private void refreshSensorsDefinitions() {
        try {
            readSensorsFile(false);
        } catch (Exception e) {
            logger.warn("Error refreshing MegaD sensor definitions", e);
        }
    }

    static void createFile(File file) {
        Logger logger = LoggerFactory.getLogger("Discovery createFile");
        File parent = file.getParentFile();
        if (parent != null) {
            boolean createOk = parent.mkdirs();
            if (createOk) {
                logger.debug("Folders {} created", file.getAbsolutePath());
            }
            try {
                // TODO: Download file from ab-log.ru
                URL url = URI.create(urlString).toURL();
                URLConnection connection = url.openConnection();
                connection.setConnectTimeout(2000);
                connection.setReadTimeout(1000);

                try (InputStream in = connection.getInputStream()) {
                    Files.copy(in, Paths.get(file.toURI()), StandardCopyOption.REPLACE_EXISTING);
                    logger.debug("File downloaded successfully: {}", file.getName());
                } catch (Exception e) {
                    logger.error("Failed to download file: {}", e.getMessage());
                }
            } catch (IOException ignored) {
            }
        }
    }

    static boolean isMatchFile(File file) {
        Logger logger = LoggerFactory.getLogger(MegaDDiscoveryService.class);

        if (!file.exists() || !file.canRead()) {
            logger.warn("File does not exist or cannot be read: {}", file.getAbsolutePath());
            return false;
        }

        try {
            byte[] localData = Files.readAllBytes(file.toPath());
            Checksum localCrc = new CRC32();
            localCrc.update(localData);
            long localChecksum = localCrc.getValue();

            logger.debug("CRC32 Checksum of existing file: {}", localChecksum);

            URL url = URI.create(urlString).toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(2000);
            connection.setReadTimeout(1000);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "OpenHAB-MegaD-Binding");

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                logger.debug("Server responded with code: {}", responseCode);
                connection.disconnect();
                return false;
            }

            byte[] serverData;
            try (InputStream in = connection.getInputStream()) {
                serverData = in.readAllBytes();
            } finally {
                connection.disconnect();
            }

            Checksum serverCrc = new CRC32();
            serverCrc.update(serverData);
            long serverChecksum = serverCrc.getValue();

            logger.debug("CRC32 Checksum of server file: {}", serverChecksum);

            if (localChecksum != serverChecksum) {
                logger.debug("Checksums differ, updating local file");

                if (file.delete()) {
                    try (FileOutputStream outputStream = new FileOutputStream(file)) {
                        outputStream.write(serverData);
                        outputStream.flush();
                    }
                    logger.debug("File updated successfully");
                    return false;
                } else {
                    logger.warn("Cannot delete old file: {}", file.getAbsolutePath());
                    return false;
                }
            }

            return true;

        } catch (SocketTimeoutException e) {
            logger.error("Timeout while connecting to server: {}", e.getMessage());
            return true;
        } catch (IOException e) {
            logger.error("IO error while checking file: {}", e.getMessage());
            return true;
        } catch (Exception e) {
            logger.error("Unexpected error while checking file: {}", e.getMessage());
            return true;
        }
    }

    public static void readSensorsFile(boolean firstStart) {
        Logger logger = LoggerFactory.getLogger("Discovery readSensorsFile");

        File sensorsFile = getMainSensorsFile();
        File sensorsFolder = getCustomSensorsFolder();

        ensureCustomSensorsFolderExists(sensorsFolder, logger);
        loadCustomSensorsFromFolder(sensorsFolder, logger);

        if (!sensorsFile.exists()) {
            createFile(sensorsFile);
        }

        if (checkMainSensorsFile(sensorsFile, firstStart)) {
            loadMainSensorsFile(sensorsFile, logger);
        }
    }

    private static File getMainSensorsFile() {
        return new File(OpenHAB.getUserDataFolder() + File.separator + "MegaD" + File.separator + "sensors.json");
    }

    private static File getCustomSensorsFolder() {
        return new File(
                OpenHAB.getUserDataFolder() + File.separator + "MegaD" + File.separator + "sensors" + File.separator);
    }

    private static void ensureCustomSensorsFolderExists(File sensorsFolder, Logger logger) {
        if (!sensorsFolder.exists()) {
            boolean created = sensorsFolder.mkdirs();
            if (!created) {
                logger.warn("Cannot create folder {}", sensorsFolder.getAbsolutePath());
            }
        }
    }

    private static void loadCustomSensorsFromFolder(File sensorsFolder, Logger logger) {
        File[] listFiles = sensorsFolder.listFiles();
        if (listFiles == null || listFiles.length == 0) {
            return;
        }

        for (File file : listFiles) {
            if (!file.isFile()) {
                continue;
            }
            loadCustomSensorFile(file, logger);
        }
    }

    private static void loadCustomSensorFile(File file, Logger logger) {
        try (BufferedReader fileReader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8);
                JsonReader reader = new JsonReader(fileReader)) {

            Map<String, JsonElement> sensorMap = JsonParser.parseReader(reader).getAsJsonObject().asMap();

            sensorMap.forEach((k, v) -> {
                MegaDI2CSensors megaSensors = new MegaDI2CSensors(k, v);
                Objects.requireNonNull(megaDI2CSensorsList).put(k, megaSensors);
                logger.debug("Json sensor read {} with label {} with address {} from \"sensors\" folder added",
                        megaSensors.getSensorType(), megaSensors.getSensorLabel(), megaSensors.getSensorAddress());
            });
        } catch (Exception e) {
            logger.warn("Error reading custom sensor file {}: {}", file.getAbsolutePath(), e.getMessage());
        }
    }

    private static boolean checkMainSensorsFile(File sensorsFile, boolean firstStart) {
        return !isMatchFile(sensorsFile) || Objects.requireNonNull(megaDI2CSensorsList).isEmpty() || firstStart;
    }

    private static void loadMainSensorsFile(File file, Logger logger) {
        try (BufferedReader fileReader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8);
                JsonReader reader = new JsonReader(fileReader)) {
            Map<String, JsonElement> sensorsList = JsonParser.parseReader(reader).getAsJsonObject()
                    .getAsJsonObject("sensors").asMap();

            sensorsList.forEach((k, v) -> {
                MegaDI2CSensors megaSensors = new MegaDI2CSensors(k, v);
                logger.debug("Json sensor read {} with label {} with address {}", megaSensors.getSensorType(),
                        megaSensors.getSensorLabel(), megaSensors.getSensorAddress());
                Objects.requireNonNull(megaDI2CSensorsList).put(k, megaSensors);
            });
        } catch (Exception e) {
            logger.error("json parsing error {}", e.getLocalizedMessage());
        }
    }

    private void addToDiscoverThing(MegaDDeviceHandler mega, String label, int i) {
        if (label.isEmpty()) {
            label = "MD"
                    + mega.getThing().getConfiguration().get("hostname").toString().substring(
                            mega.getThing().getConfiguration().get("hostname").toString().lastIndexOf(".") + 1)
                    + "P" + i;
        } else {
            label = "id_" + label + "_P" + i;
        }
        ThingUID thingUID = new ThingUID(MegaDBindingConstants.THING_TYPE_PORT, mega.getThing().getUID(), label);
        DiscoveryResult resultS = DiscoveryResultBuilder.create(thingUID).withProperty("port", i).withLabel(label)
                .withBridge(mega.getThing().getUID()).build();
        thingDiscovered(resultS);
    }

    private void checkFirmware() {
        MegaDHttpHelpers http = new MegaDHttpHelpers();
        http.setHttpClient(httpClient);
        MegaDHTTPResponse megaDHTTPResponse;
        megaDHTTPResponse = http.request("https://www.ab-log.ru/smart-house/ethernet/megad-2561-firmware", 1000);
        if (megaDHTTPResponse.getResponseCode() == 200) {
            try {
                actualFirmware = megaDHTTPResponse.getResponseResult().substring(
                        megaDHTTPResponse.getResponseResult().indexOf("<ul><li>") + "<ul><li>".length(),
                        megaDHTTPResponse.getResponseResult().indexOf("</font><br>"));
                actualFirmware = actualFirmware.split("ver")[1].trim().strip();
            } catch (Exception e) {
                logger.error("Error getting actual firmware");
            }
        }
    }
}
