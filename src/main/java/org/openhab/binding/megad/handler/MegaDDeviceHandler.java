/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
package org.openhab.binding.megad.handler;

import static org.openhab.binding.megad.discovery.MegaDDiscoveryService.megaDDeviceHandlerList;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.megad.MegaDBindingConstants;
import org.openhab.binding.megad.MegaDConfiguration;
import org.openhab.binding.megad.dto.MegaDHardware;
import org.openhab.binding.megad.enums.MegaDTypesEnum;
import org.openhab.binding.megad.internal.MegaDHTTPResponse;
import org.openhab.binding.megad.internal.MegaDHttpHelpers;
import org.openhab.binding.megad.internal.MegaDService;
import org.openhab.core.OpenHAB;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseBridgeHandler;
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.openhab.core.thing.binding.builder.ThingBuilder;
import org.openhab.core.thing.type.ChannelKind;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link MegaDDeviceHandler} is responsible for creating things and thing
 * handlers.
 *
 * @author Petr Shatsillo - Initial contribution
 */
@NonNullByDefault
public class MegaDDeviceHandler extends BaseBridgeHandler {
    private Logger logger = LoggerFactory.getLogger(MegaDDeviceHandler.class);
    private final MegaDHttpHelpers httpHelper = new MegaDHttpHelpers();
    private final ArrayList<MegaDRs485Handler> megaDRs485HandlerMap = new ArrayList<>();
    private @Nullable ScheduledFuture<?> refreshPollingJob;
    // protected long lastRefresh = 0;
    public MegaDHardware megaDHardware = new MegaDHardware();
    public MegaDConfiguration config = getConfigAs(MegaDConfiguration.class);
    private boolean firmwareUpdate = false;
    @Nullable
    DatagramSocket socket = null;
    String checkData = "";
    @Nullable
    Channel statusChannel;
    boolean beta = false;
    @Nullable
    InetAddress broadcastAddress;

    public MegaDDeviceHandler(Bridge bridge) {
        super(bridge);
    }

    @Override
    public void initialize() {
        config = getConfigAs(MegaDConfiguration.class);
        megaDHardware = new MegaDHardware(Objects.requireNonNull(config).hostname, config.password);
        MegaDHTTPResponse response = httpHelper.request("http://" + config.hostname + "/" + config.password);
        if (response.getResponseCode() >= 400) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Wrong password");
        } else if (response.getResponseCode() == 200) {
            updateStatus(ThingStatus.UNKNOWN, ThingStatusDetail.CONFIGURATION_PENDING);
            Map<String, String> properties = new HashMap<>();
            properties.put("Type:", megaDHardware.getType());
            properties.put("Firmware:", megaDHardware.getFirmware());
            properties.put("Actual Firmware:", megaDHardware.getActualFirmware());
            updateProperties(properties);
            String ip = config.hostname.substring(0, config.hostname.lastIndexOf("."));
            for (InetAddress address : MegaDService.interfacesAddresses) {
                if (address.getHostAddress().startsWith(ip)) {
                    if (MegaDService.interfacesAddresses.stream().findFirst().isPresent()) {
                        if ((!megaDHardware.getSip()
                                .equals(MegaDService.interfacesAddresses.stream().findFirst().get().getHostAddress()
                                        + ":" + MegaDService.port))
                                || (!megaDHardware.getSct().equals("megad"))) {
                            httpHelper.request("http://" + config.hostname + "/" + config.password + "/?cf=1&sip="
                                    + MegaDService.interfacesAddresses.stream().findFirst().get().getHostAddress()
                                    + "%3A" + MegaDService.port + "&sct=megad&srvt=0");
                        }
                    }
                }
            }
            List<Channel> channelList = new ArrayList<>();
            List<Channel> existingChannelList = new LinkedList<>(thing.getChannels());

            Configuration channelConfiguration = new Configuration();
            channelConfiguration.put("beta", false);
            ChannelUID startUID = new ChannelUID(thing.getUID(), MegaDBindingConstants.CHANNEL_ST);
            Channel start = ChannelBuilder.create(startUID)
                    .withType(new ChannelTypeUID(MegaDBindingConstants.BINDING_ID, MegaDBindingConstants.CHANNEL_ST))
                    .withKind(ChannelKind.TRIGGER).withAcceptedItemType("String").build();
            ChannelUID flashUID = new ChannelUID(thing.getUID(), "flash");
            Channel flash = ChannelBuilder.create(flashUID)
                    .withType(new ChannelTypeUID(MegaDBindingConstants.BINDING_ID, MegaDBindingConstants.CHANNEL_FLASH))
                    .withAcceptedItemType("Switch").withConfiguration(channelConfiguration).build();
            if (existingChannelList.stream().anyMatch(cn -> cn.getUID().equals(flash.getUID()))) {
                Channel foundedChannel = existingChannelList.stream().filter(cn -> cn.getUID().equals(flash.getUID()))
                        .findFirst().get();
                channelList.add(foundedChannel);
                existingChannelList.remove(foundedChannel);
            } else {
                channelList.add(flash);
            }
            if (existingChannelList.stream().anyMatch(cn -> cn.getUID().equals(start.getUID()))) {
                Channel foundedChannel = existingChannelList.stream().filter(cn -> cn.getUID().equals(start.getUID()))
                        .findFirst().get();
                channelList.add(foundedChannel);
                existingChannelList.remove(foundedChannel);
            } else {
                channelList.add(start);
            }
            ChannelUID progressUID = new ChannelUID(thing.getUID(), MegaDBindingConstants.CHANNEL_PROGRESS);
            Channel progress = ChannelBuilder.create(progressUID).withType(
                    new ChannelTypeUID(MegaDBindingConstants.BINDING_ID, MegaDBindingConstants.CHANNEL_PROGRESS))
                    .withAcceptedItemType("String").build();
            channelList.add(progress);

            ChannelUID statusUID = new ChannelUID(thing.getUID(), MegaDBindingConstants.CHANNEL_STATUS);
            Channel status = ChannelBuilder.create(statusUID)
                    .withType(
                            new ChannelTypeUID(MegaDBindingConstants.BINDING_ID, MegaDBindingConstants.CHANNEL_STATUS))
                    .withAcceptedItemType("String").build();
            channelList.add(status);
            ChannelUID writeConfUID = new ChannelUID(thing.getUID(), MegaDBindingConstants.CHANNEL_WRITE_CONF);
            Channel writeConf = ChannelBuilder.create(writeConfUID).withType(
                    new ChannelTypeUID(MegaDBindingConstants.BINDING_ID, MegaDBindingConstants.CHANNEL_WRITE_CONF))
                    .withAcceptedItemType("Switch").build();
            channelList.add(writeConf);
            ChannelUID readConfUID = new ChannelUID(thing.getUID(), MegaDBindingConstants.CHANNEL_READ_CONF);
            Channel readConf = ChannelBuilder.create(readConfUID).withType(
                    new ChannelTypeUID(MegaDBindingConstants.BINDING_ID, MegaDBindingConstants.CHANNEL_READ_CONF))
                    .withAcceptedItemType("Switch").build();
            channelList.add(readConf);
            ThingBuilder thingBuilder = editThing();
            thingBuilder.withChannels(channelList);
            updateThing(thingBuilder.build());
            updateStatus(ThingStatus.ONLINE);

            Objects.requireNonNull(megaDDeviceHandlerList).add(this);
            final ScheduledFuture<?> refreshPollingJob = this.refreshPollingJob;
            if (refreshPollingJob == null || refreshPollingJob.isCancelled()) {
                this.refreshPollingJob = scheduler.scheduleWithFixedDelay(this::refresh, 0, 1000,
                        TimeUnit.MILLISECONDS);
            } else {
                updateStatus(ThingStatus.OFFLINE);
            }
        } else {
            updateStatus(ThingStatus.OFFLINE);
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (channelUID.getId().equals(MegaDBindingConstants.CHANNEL_FLASH) && command.equals(OnOffType.ON)) {
            int bl = 0;
            readConf();
            logger.warn("Flashing mega!");
            firmwareUpdate = true;
            Channel flashChannel = thing.getChannel(channelUID);
            if (flashChannel != null) {
                if (flashChannel.getConfiguration().get("beta") != null) {
                    beta = Boolean.parseBoolean(flashChannel.getConfiguration().get("beta").toString());
                }
            }
            statusChannel = thing.getChannel(MegaDBindingConstants.CHANNEL_STATUS);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.FIRMWARE_UPDATING);
            final Channel statusChannel = this.statusChannel;
            try {
                broadcastAddress = InetAddress
                        .getByName(config.hostname.substring(0, config.hostname.lastIndexOf(".") + 1) + "255");
                socket = new DatagramSocket(42000);
                final DatagramSocket socket = this.socket;
                if (socket != null) {
                    socket.setSoTimeout(50000);
                    this.socket = socket;

                    MegaDHTTPResponse response = httpHelper
                            .request("http://" + config.hostname + "/" + config.password + "/?bl=1");
                    String broadcast_string = "";
                    if (response.getResponseResult().equals("1")) {
                        bl = 1;
                        // logger.warn("Flashing mega bl {}", bl);
                        checkData = "DACA";
                        Socket sck = new Socket(config.hostname, 80);
                        sck.close();
                        Thread.sleep(100);
                        MegaDHTTPResponse modeResponse = httpHelper
                                .request("http://" + config.hostname + "/" + config.password + "/?fwup=1");
                        Thread.sleep(100);
                        broadcast_string = "AA0000" + checkData;
                        byte[] buf = HexFormat.of().parseHex(broadcast_string);
                        byte[] receivedPacket = new byte[200];
                        for (int i = 0; i < 10; i++) {
                            byte[] rcvBuf = new byte[200];
                            DatagramPacket packet = new DatagramPacket(buf, buf.length, broadcastAddress, 52000);
                            socket.send(packet);
                            DatagramPacket rcvPacket = new DatagramPacket(rcvBuf, 200);
                            socket.receive(rcvPacket);
                            if (rcvPacket.getData() != null) {
                                receivedPacket = rcvBuf.clone();
                            }
                        }
                        flashFirmware(receivedPacket);
                        //eraseEEPROM();
                        broadcast_string = "AA0003" + checkData;
                        buf = HexFormat.of().parseHex(broadcast_string);
                        DatagramPacket packet = new DatagramPacket(buf, buf.length, broadcastAddress, 52000);
                        socket.send(packet);
                        socket.close();
                    }
                    updateState(channelUID, OnOffType.OFF);
                    if (statusChannel != null) {
                        updateState(statusChannel.getUID(), StringType.valueOf("DONE"));
                    }
                    Map<String, String> properties = new HashMap<>();
                    properties.put("Type:", megaDHardware.getType());
                    properties.put("Firmware:", megaDHardware.getFirmware());
                    properties.put("Actual Firmware:", megaDHardware.getActualFirmware());
                    updateProperties(properties);
                    firmwareUpdate = false;
                    updateStatus(ThingStatus.ONLINE);
                }
            } catch (IOException e) {
                logger.warn("Flashing mega error. Device is offline");
                if (statusChannel != null) {
                    updateState(statusChannel.getUID(), StringType
                            .valueOf("Flashing mega error. " + e.getMessage() + " press reset to recover firmware"));
                }
                try {
                    broadcastAddress = InetAddress
                            .getByName(config.hostname.substring(0, config.hostname.lastIndexOf(".") + 1) + "255");
                    String broadcast_string = "AA0000";
                    byte[] buf = HexFormat.of().parseHex(broadcast_string);
                    byte[] rcvBuf = new byte[200];
                    DatagramSocket socket = this.socket;
                    if (socket != null) {
                        DatagramPacket packet = new DatagramPacket(buf, buf.length, broadcastAddress, 52000);
                        socket.send(packet);
                        DatagramPacket rcvPacket = new DatagramPacket(rcvBuf, 200);
                        socket.receive(rcvPacket);
                        if (rcvPacket.getData() != null) {
                            flashFirmware(rcvBuf);
                        }
                    }
                } catch (IOException ignored) {
                }
            } catch (InterruptedException ignored) {
            }
        } else if (channelUID.getId().equals(MegaDBindingConstants.CHANNEL_WRITE_CONF)
                && command.equals(OnOffType.ON)) {
            writeConf();
            updateState(channelUID, OnOffType.OFF);
        } else if (channelUID.getId().equals(MegaDBindingConstants.CHANNEL_READ_CONF) && command.equals(OnOffType.ON)) {
            readConf();
            updateState(channelUID, OnOffType.OFF);
        }
    }

    private void flashFirmware(byte[] receivedPacket) {
        boolean blUpgrade = false;
        int chipType = 0;
        Channel progressChannel = thing.getChannel(MegaDBindingConstants.CHANNEL_PROGRESS);
        byte[] buf;
        String broadcast_string;
        final Channel statusChannel = this.statusChannel;
        try {
            if (((receivedPacket[0] & 0xFF) == 0xAA) && ((receivedPacket[1] & 0xFF) == 0x00)) {
                logger.warn("OK");
                if ((receivedPacket[2] & 0xFF) == 0x99 || (receivedPacket[2] & 0xFF) == 0x9A) {
                    if (!checkData.isBlank()) {
                        logger.warn("New bootloader");
                    }
                    if ((receivedPacket[2] & 0xFF) == 0x99) {
                        if (statusChannel != null) {
                            updateState(statusChannel.getUID(),
                                    StringType.valueOf("WARNING! Please upgrade bootloader!"));
                        }
                        logger.warn("WARNING! Please upgrade bootloader!");
                        blUpgrade = true;
                    }
                    chipType = 2561;
                } else {
                    logger.warn("(chip type: atmega328)");
                }
                String dl_fw_fname;
                URL url;
                if (chipType == 2561) {
                    if (beta) {
                        dl_fw_fname = "megad-2561-beta.hex";
                    } else {
                        dl_fw_fname = "megad-2561.hex";
                    }
                    url = new URL("https://ab-log.ru/files/File/megad-firmware-2561/latest/" + dl_fw_fname);
                    logger.warn("Beta {}, firmware filename is {}", beta, dl_fw_fname);
                } else {
                    if (beta) {
                        dl_fw_fname = "megad-328-beta.hex";
                    } else {
                        dl_fw_fname = "megad-328-beta.hex";
                    }
                    url = new URL("https://ab-log.ru/files/File/megad-firmware/latest/" + dl_fw_fname);
                }
                logger.warn("Downloading firmware... {}", dl_fw_fname);
                // updateState(channelUID, OnOffType.OFF);
                if (statusChannel != null) {
                    updateState(statusChannel.getUID(), StringType.valueOf("Downloading firmware... " + dl_fw_fname));
                }
                File file = new File(
                        OpenHAB.getUserDataFolder() + File.separator + "MegaD" + File.separator + dl_fw_fname);
                if (file.exists()) {
                    boolean isDel = file.delete();
                    logger.debug("file {} deleted {}", file.getName(), isDel);
                }
                try (InputStream in = url.openStream()) {
                    Files.copy(in, Paths.get(file.toURI()), StandardCopyOption.REPLACE_EXISTING);
                }
                List<String> lines = null;
                lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
                if (lines != null) {
                    StringBuilder firmware = new StringBuilder();
                    if (statusChannel != null) {
                        updateState(statusChannel.getUID(), StringType.valueOf("Preparing firmware ... "));
                    }
                    for (int i = 0; i < lines.size(); i++) {
                        float progress = (float) i / lines.size();
                        if (progressChannel != null) {
                            updateState(progressChannel.getUID(),
                                    StringType.valueOf(String.valueOf((int) (progress * 100))));
                        }
                        logger.debug("Preparing firmware ... {}%", (int) (progress * 100));
                        char addrEnd = lines.get(i).charAt(8);
                        if (!lines.get(i).isEmpty() && addrEnd == 48) {
                            String byteCount = String.valueOf(lines.get(i).charAt(1));
                            byteCount += String.valueOf(lines.get(i).charAt(2));
                            String convResult = new BigInteger(byteCount, 16).toString(10);
                            for (int i1 = 0; i1 < Integer.parseInt(convResult); i1++) {
                                int pos = i1 * 2 + 9;
                                firmware.append(lines.get(i).charAt(pos)).append(lines.get(i).charAt(pos + 1));
                            }
                        }
                    }
                    logger.warn("Download finish");
                    DatagramSocket socket = this.socket;
                    if (socket != null) {
                        if (statusChannel != null) {
                            updateState(statusChannel.getUID(), StringType.valueOf("Download finish"));
                        }
                        byte[] intByte = HexFormat.of().parseHex(firmware.toString());
                        if (intByte.length > 258046 && chipType == 2561) {
                            if (statusChannel != null) {
                                updateState(statusChannel.getUID(),
                                        StringType.valueOf("FAULT! Firmware is too large!"));
                            }
                            logger.warn("FAULT! Firmware is too large!");
                        } else if (intByte.length < 1000) {
                            if (statusChannel != null) {
                                updateState(statusChannel.getUID(),
                                        StringType.valueOf("FAULT! Firmware length is zero or file is corrupted!"));
                            }
                            logger.warn("FAULT! Firmware length is zero or file is corrupted!");
                        } else if (intByte.length > 32768 && chipType == 2561 && blUpgrade) {
                            if (statusChannel != null) {
                                updateState(statusChannel.getUID(),
                                        StringType.valueOf("FAULT! You have to upgrade bootloader!"));
                            }
                            logger.warn("FAULT! You have to upgrade bootloader!");
                        } else {
                            if (statusChannel != null) {
                                updateState(statusChannel.getUID(), StringType.valueOf("Erasing firmware..."));
                            }
                            logger.warn("Erasing firmware... ");
                            broadcast_string = "AA0002" + checkData;
                            buf = HexFormat.of().parseHex(broadcast_string);
                            byte[] eraseBuf = new byte[5];
                            DatagramPacket erasePacket = new DatagramPacket(buf, buf.length, broadcastAddress, 52000);
                            socket.send(erasePacket);
                            DatagramPacket rcvErasePacket = new DatagramPacket(eraseBuf, 5);
                            socket.receive(rcvErasePacket);
                            if (rcvErasePacket.getData() != null) {
                                if (((receivedPacket[0] & 0xFF) == 0xAA) && ((receivedPacket[1] & 0xFF) == 0x00)) {
                                    logger.warn("Writing firmware...");
                                    if (statusChannel != null) {
                                        updateState(statusChannel.getUID(), StringType.valueOf("Writing firmware..."));
                                    }
                                    int block;
                                    if (chipType == 2561) {
                                        block = 256;
                                    } else {
                                        block = 128;
                                    }
                                    int fullCells = intByte.length / block;
                                    int restBytes = intByte.length % block;
                                    // logger.debug("FullCells {} and rest is {}", fullCells, restBytes);
                                    byte[][] fwPacket = new byte[fullCells + 1][];
                                    int x = 0;
                                    for (int i = 0; i < fullCells; i++) {
                                        byte[] packet = new byte[block];
                                        for (int j = 0; j < block; j++) {
                                            packet[j] = intByte[x];
                                            x++;
                                        }
                                        fwPacket[i] = packet;
                                    }
                                    byte[] restPacket = new byte[restBytes];
                                    for (int i = 0; i < restBytes; i++) {
                                        restPacket[i] = intByte[x];
                                        x++;
                                    }
                                    fwPacket[fullCells] = restPacket;
                                    int msgID = 0;
                                    for (int i = 0; i < fullCells + 1; i++) {
                                        byte[] preamble = { HexFormat.of().parseHex("AA")[0], (byte) i, 1,
                                                HexFormat.of().parseHex(checkData)[0],
                                                HexFormat.of().parseHex(checkData)[1] };
                                        byte[] bufPack = new byte[preamble.length + fwPacket[i].length];
                                        int cell = 0;
                                        for (int j = 0; j < preamble.length; j++) {
                                            bufPack[cell] = preamble[j];
                                            cell++;
                                        }
                                        for (int j = 0; j < fwPacket[i].length; j++) {
                                            bufPack[cell] = fwPacket[i][j];
                                            cell++;
                                        }
                                        logger.trace("Sending packet {} is {}", i, bufPack);
                                        float progress = (float) i / fullCells;
                                        logger.debug("Writing firmware ... {}%", (int) (progress * 100));
                                        if (progressChannel != null) {
                                            updateState(progressChannel.getUID(),
                                                    StringType.valueOf(String.valueOf((int) (progress * 100))));
                                        }
                                        DatagramPacket sendPacket = new DatagramPacket(bufPack, bufPack.length,
                                                broadcastAddress, 52000);
                                        socket.send(sendPacket);
                                        Thread.sleep(4);
                                        byte[] rcvBuf = new byte[10];
                                        DatagramPacket rcvPacket = new DatagramPacket(rcvBuf, 10);
                                        socket.receive(rcvPacket);
                                        if (rcvPacket.getData() != null) {
                                            if ((rcvPacket.getData()[0] & 0xFF) == 0xAA
                                                    && (rcvPacket.getData()[1] & 0xFF) != msgID) {
                                                if (statusChannel != null) {
                                                    updateState(statusChannel.getUID(), StringType
                                                            .valueOf("FAULT! Please update firmware in recovery mode"));
                                                }
                                                logger.warn("FAULT! Please update firmware in recovery mode");
                                                break;
                                            }
                                        }
                                        msgID++;
                                        if (msgID == 256)
                                            msgID = 0;
                                    }
                                    // logger.warn("next: {}", rcvPacket.getData());
                                }
                            }
                        }
                    } else {
                        logger.error("Socket is null");
                    }
                }
            }
        } catch (IOException | InterruptedException ignore) {
            logger.warn("FAULT! firmware update error");
        }
    }

    private void eraseEEPROM() {
        logger.warn("Erasing EEPROM");
        byte[] buf;

        String broadcast_string = "AA0009" + checkData;
        try {
            buf = HexFormat.of().parseHex(broadcast_string);
            byte[] eraseBuf = new byte[5];
            DatagramPacket erasePacket = new DatagramPacket(buf, buf.length, broadcastAddress, 52000);
            socket.send(erasePacket);
            DatagramPacket rcvErasePacket = new DatagramPacket(eraseBuf, 5);
            socket.receive(rcvErasePacket);
            broadcast_string = "AA0109" + checkData;
            buf = HexFormat.of().parseHex(broadcast_string);
            eraseBuf = new byte[5];
            erasePacket = new DatagramPacket(buf, buf.length, broadcastAddress, 52000);
            socket.send(erasePacket);
            rcvErasePacket = new DatagramPacket(eraseBuf, 5);
            socket.receive(rcvErasePacket);
            if (rcvErasePacket.getData() != null) {
                if (((eraseBuf[0] & 0xFF) == 0xAA) && ((eraseBuf[1] & 0xFF) == 0x01)) {
                    logger.warn("EEPROM erased");
                }
            }

        } catch (IOException e) {
            logger.error("EEPROM deleting error {}", e.getMessage());
        }
    }

    private void refresh() {
        if (!firmwareUpdate) {
            if (config.ping) {
                MegaDHttpHelpers httpRequest = new MegaDHttpHelpers();
                int response = httpRequest.request("http://" + config.hostname + "/" + config.password)
                        .getResponseCode();
                if (response == 200) {
                    if (!thing.getStatus().equals(ThingStatus.ONLINE)) {
                        updateStatus(ThingStatus.ONLINE);
                    }
                } else {
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                            "Device not responding on ping");
                }
            }
            long now = System.currentTimeMillis();
            ArrayList<MegaDRs485Handler> megaDRs485HandlerMap = this.megaDRs485HandlerMap;
            if (!megaDRs485HandlerMap.isEmpty()) {
                try {
                    for (MegaDRs485Handler handler : megaDRs485HandlerMap) {
                        int interval = Integer
                                .parseInt(handler.getThing().getConfiguration().get("refresh").toString());
                        if (interval != 0) {
                            if (now >= (handler.getLastRefresh() + (interval * 1000L))) {
                                handler.updateData();
                                handler.lastrefreshAdd(now);
                                try {
                                    Thread.sleep(200);
                                } catch (InterruptedException ignored) {
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        }
    }

    // RS485
    public void registerMegaRs485Listener(MegaDRs485Handler megaDrs485Handler) {
        String rs485Address = megaDrs485Handler.getThing().getConfiguration().get("address").toString();
        ArrayList<MegaDRs485Handler> megaDRs485HandlerMap = this.megaDRs485HandlerMap;
        if (!megaDRs485HandlerMap.isEmpty()) {
            boolean isexist = false;
            for (MegaDRs485Handler handler : megaDRs485HandlerMap) {
                if (rs485Address.equals(handler.getThing().getConfiguration().get("address").toString())) {
                    isexist = true;
                }
            }
            if (!isexist) {
                this.megaDRs485HandlerMap.add(megaDrs485Handler);
            }
        } else {
            this.megaDRs485HandlerMap.add(megaDrs485Handler);
        }
    }

    public void unregisterMegadRs485Listener(MegaDRs485Handler megaDrs485Handler) {
        String rs485Address = megaDrs485Handler.getThing().getConfiguration().get("address").toString();
        megaDRs485HandlerMap.removeIf(
                handler -> rs485Address.equals(handler.getThing().getConfiguration().get("address").toString()));
    }

    @Override
    public void dispose() {
        ScheduledFuture<?> refreshPollingJob = this.refreshPollingJob;
        if (refreshPollingJob != null && !refreshPollingJob.isCancelled()) {
            refreshPollingJob.cancel(true);
            this.refreshPollingJob = null;
        }
        DatagramSocket socket = this.socket;
        if (socket != null) {
            socket.close();
            this.socket = socket;
        }
        super.dispose();
    }

    public void started() {
        triggerChannel(MegaDBindingConstants.CHANNEL_ST, "START");
        MegaDHttpHelpers http = new MegaDHttpHelpers();
        megaDHardware.getMegaPortsAndType(config.hostname, config.password, http);
        megaDHardware.getPortsStatus(config.hostname, config.password, http);
    }

    private void readConf() {
        Channel statusChannel = this.statusChannel;
        if (statusChannel != null) {
            updateState(statusChannel.getUID(), StringType.valueOf("Reading full configuration!"));
        }
        logger.warn("Reading full configuration!");
        MegaDHttpHelpers http = new MegaDHttpHelpers();
        megaDHardware.readConfigPage1(config.hostname, config.password, http);
        megaDHardware.readConfigPage2(config.hostname, config.password, http);
        megaDHardware.readScreens(config.hostname, config.password, http);
        megaDHardware.readElements(config.hostname, config.password, http);
        megaDHardware.readCron(config.hostname, config.password, http);
        megaDHardware.readKeys(config.hostname, config.password, http);
        megaDHardware.readProgram(config.hostname, config.password, http);
        megaDHardware.readPID(config.hostname, config.password, http);
        // megaDHardware.getMegaPortsAndType(config.hostname, config.password, http);
        megaDHardware.getPortsStatus(config.hostname, config.password, http);
        File file = new File(OpenHAB.getUserDataFolder() + File.separator + "MegaD" + File.separator + "cfg"
                + File.separator + config.hostname + ".cfg");
        if (file.exists()) {
            file.delete();
        }
        boolean createOk = file.getParentFile().mkdirs();
        if (createOk) {
            logger.debug("Folders {} created", file.getAbsolutePath());
        }
        try {
            StringBuilder cfgLine = new StringBuilder("cf=1&" + "eip=" + config.hostname + "&emsk="
                    + megaDHardware.getEmsk() + "&pwd=" + config.password + "&gw=" + megaDHardware.getGw() + "&sip="
                    + megaDHardware.getSip() + "&sct=" + megaDHardware.getSct() + "&pr=" + megaDHardware.getPr()
                    + "&lp=" + megaDHardware.getLp() + "&gsmf=" + megaDHardware.isGsmf() + "&srvt="
                    + megaDHardware.getSrvt() + "&gsm=" + megaDHardware.getGsm() + "&nr=1\n");
            Files.writeString(file.toPath(), cfgLine.toString(), StandardCharsets.UTF_8);
            cfgLine = new StringBuilder(
                    "cf=2" + "&mdid=" + megaDHardware.getMdid() + "&sl=" + megaDHardware.isSl() + "&nr=1\n");
            Files.writeString(file.toPath(), cfgLine.toString(), StandardCharsets.UTF_8, StandardOpenOption.APPEND);
            for (int i = 0; i < 5; i++) {
                MegaDHardware.Screen screen = megaDHardware.getScreenList().get(i);
                cfgLine = new StringBuilder("cf=6" + "&sc=" + i + "&scrnt="
                        + URLEncoder.encode(screen.getScrnt(), "windows-1251") + "&scrnc=" + screen.getScrnc());
                for (int j = 0; j < screen.getE().length; j++) {
                    if (screen.getE()[j]) {
                        cfgLine.append("&e").append(j).append("=").append("on");
                    } else {
                        cfgLine.append("&e").append(j).append("=");
                    }
                }
                cfgLine.append("&nr=1\n");
                Files.writeString(file.toPath(), cfgLine.toString(), StandardCharsets.UTF_8, StandardOpenOption.APPEND);
            }
            for (int i = 0; i < megaDHardware.getElementsList().size(); i++) {
                MegaDHardware.Elements elts = megaDHardware.getElementsList().get(i);
                cfgLine = new StringBuilder("cf=6" + "&sc=99");
                cfgLine.append("&el=").append(i);
                cfgLine.append("&elemt=").append(URLEncoder.encode(elts.getElemt(), "windows-1251"));
                String eli = URLEncoder.encode(elts.getElemi(), "windows-1251");
                cfgLine.append("&elemi=").append(eli.replace("&", "%26"));
                cfgLine.append("&elemp=").append(URLEncoder.encode(elts.getElemp(), "windows-1251"));
                cfgLine.append("&elemf=").append(URLEncoder.encode(elts.getElemf(), "windows-1251"));
                cfgLine.append("&elemc=").append(URLEncoder.encode(elts.getElemc(), "windows-1251"));
                cfgLine.append("&elemu=").append(URLEncoder.encode(elts.getElemu(), "windows-1251"));
                cfgLine.append("&elemz=").append(URLEncoder.encode(elts.getElemz(), "windows-1251"));
                cfgLine.append("&elemr=").append(URLEncoder.encode(elts.getElemr(), "windows-1251"));
                cfgLine.append("&elemy=").append(URLEncoder.encode(elts.getElemy(), "windows-1251"));
                cfgLine.append("&elema=").append(URLEncoder.encode(elts.getElema(), "windows-1251"));
                cfgLine.append("&nr=1\n");
                Files.writeString(file.toPath(), cfgLine.toString(), StandardCharsets.UTF_8, StandardOpenOption.APPEND);
            }
            cfgLine = new StringBuilder("cf=7");
            MegaDHardware.Cron cron = megaDHardware.getCron();
            cfgLine.append("&stime=").append(cron.getStime());
            cfgLine.append("&cscl=").append(cron.getCscl());
            cfgLine.append("&csda=").append(cron.getCsda());
            for (int i = 0; i < 5; i++) {
                cfgLine.append("&crnt").append(i).append("=").append(cron.getCrnt()[i]);
                cfgLine.append("&crna").append(i).append("=").append(cron.getCrna()[i]);
            }
            cfgLine.append("&nr=1\n");
            Files.writeString(file.toPath(), cfgLine.toString(), StandardCharsets.UTF_8, StandardOpenOption.APPEND);
            cfgLine = new StringBuilder("cf=8");
            for (int i = 0; i < 5; i++) {
                cfgLine.append("&key").append(i).append("=").append(megaDHardware.getKeys().getKey()[i]);
            }
            cfgLine.append("&nr=1\n");
            Files.writeString(file.toPath(), cfgLine.toString(), StandardCharsets.UTF_8, StandardOpenOption.APPEND);

            for (int i = 0; i < 10; i++) {
                cfgLine = new StringBuilder("cf=10");
                cfgLine.append("&prn").append("=").append(i);
                cfgLine.append("&prp").append("=").append(megaDHardware.getProgramList().get(i).getPrp());
                cfgLine.append("&prv").append("=").append(megaDHardware.getProgramList().get(i).getPrv());
                cfgLine.append("&prd").append("=").append(megaDHardware.getProgramList().get(i).getPrd());
                cfgLine.append("&prs").append("=").append(megaDHardware.getProgramList().get(i).isPrs());
                cfgLine.append("&prc").append("=").append(megaDHardware.getProgramList().get(i).getPrc());
                cfgLine.append("&nr=1\n");
                Files.writeString(file.toPath(), cfgLine.toString(), StandardCharsets.UTF_8, StandardOpenOption.APPEND);
            }

            for (int i = 0; i < 5; i++) {
                cfgLine = new StringBuilder("cf=11");
                cfgLine.append("&pid").append("=").append(i);
                cfgLine.append("&pide").append("=").append("1");
                cfgLine.append("&pidt").append("=").append(megaDHardware.getPidList().get(i).getPidt());
                cfgLine.append("&pidi").append("=").append(megaDHardware.getPidList().get(i).getPidi());
                cfgLine.append("&pido").append("=").append(megaDHardware.getPidList().get(i).getPido());
                cfgLine.append("&pidsp").append("=").append(megaDHardware.getPidList().get(i).getPidsp());
                cfgLine.append("&pidpf").append("=").append(megaDHardware.getPidList().get(i).getPidpf());
                cfgLine.append("&pidif").append("=").append(megaDHardware.getPidList().get(i).getPidif());
                cfgLine.append("&piddf").append("=").append(megaDHardware.getPidList().get(i).getPiddf());
                cfgLine.append("&pidc").append("=").append(megaDHardware.getPidList().get(i).getPidc());
                cfgLine.append("&pidm").append("=").append(megaDHardware.getPidList().get(i).getPidm());
                cfgLine.append("&nr=1\n");
                Files.writeString(file.toPath(), cfgLine.toString(), StandardCharsets.UTF_8, StandardOpenOption.APPEND);
            }

            for (int i = 0; i < megaDHardware.getPortsCount(); i++) {
                MegaDHardware.Port port = megaDHardware.getPortList().get(i);
                if (port != null) {
                    cfgLine = new StringBuilder("pn=").append(i);
                    cfgLine.append("&pty").append("=").append(port.getPty().getID());
                    if (port.getPty() == MegaDTypesEnum.IN) {
                        cfgLine.append("&ecmd").append("=").append(URLEncoder.encode(port.getEcmd(), "windows-1251"));
                        cfgLine.append("&af").append("=").append(port.isAf());
                        cfgLine.append("&eth").append("=").append(URLEncoder.encode(port.getEth(), "windows-1251"));
                        cfgLine.append("&naf").append("=").append(port.isNaf());
                        cfgLine.append("&m").append("=").append(port.getM().getID());
                        cfgLine.append("&misc").append("=").append(port.isMiscChecked());
                        cfgLine.append("&d").append("=").append(port.isdCheckbox());
                        cfgLine.append("&mt").append("=").append(port.isMt());
                    } else if (port.getPty() == MegaDTypesEnum.OUT) {
                        cfgLine.append("&d").append("=").append(port.getdSelect());
                        cfgLine.append("&m").append("=").append(port.getM().getID());
                        cfgLine.append("&grp").append("=").append(port.getGrp());
                    } else if (port.getPty() == MegaDTypesEnum.DSEN) {
                        cfgLine.append("&d").append("=").append(port.getdSelect());
                        cfgLine.append("&m").append("=").append(port.getM().getID());
                        cfgLine.append("&misc").append("=").append(port.getMisc());
                        cfgLine.append("&hst").append("=").append(port.getHst());
                        cfgLine.append("&ecmd").append("=").append(URLEncoder.encode(port.getEcmd(), "windows-1251"));
                        cfgLine.append("&eth").append("=").append(URLEncoder.encode(port.getEth(), "windows-1251"));
                        cfgLine.append("&af").append("=").append(port.isAf());
                        cfgLine.append("&naf").append("=").append(port.isNaf());
                        cfgLine.append("&m").append("=").append(port.getmAsString());
                    } else if (port.getPty() == MegaDTypesEnum.I2C) {
                        cfgLine.append("&m").append("=").append(port.getM().getID());
                        cfgLine.append("&misc").append("=").append(port.getMisc());
                        cfgLine.append("&gr").append("=").append(port.getGr());
                        cfgLine.append("&d").append("=").append(port.getdSelect());
                        cfgLine.append("&hst").append("=").append(port.getHst());
                        cfgLine.append("&inta").append("=").append(megaDHardware.getIntAsString(i));
                        cfgLine.append("&clock").append("=").append(port.getClock());
                    }
                    cfgLine.append("&emt").append("=").append(URLEncoder.encode(port.getEmt(), "windows-1251"));
                    cfgLine.append("&nr=1\n");
                    Files.writeString(file.toPath(), cfgLine.toString(), StandardCharsets.UTF_8,
                            StandardOpenOption.APPEND);
                    if (!port.getExtPorts().isEmpty()) {
                        for (int j = 0; j < port.getExtPorts().size(); j++) {
                            MegaDHardware.ExtPort extPort = port.getExtPorts().get(j);
                            if (extPort != null) {
                                cfgLine = new StringBuilder("pt=").append(i);
                                cfgLine.append("&ext=").append(j);
                                cfgLine.append("&ety").append("=").append(extPort.getEty().getID());
                                cfgLine.append("&ept").append("=")
                                        .append(URLEncoder.encode(extPort.getEpt(), "windows-1251"));
                                cfgLine.append("&eact").append("=")
                                        .append(URLEncoder.encode(extPort.getEact(), "windows-1251"));
                                cfgLine.append("&epf").append("=").append(extPort.isEpf());
                                cfgLine.append("&emode").append("=").append(extPort.getEmode());
                                cfgLine.append("&emin").append("=").append(extPort.getEmin());
                                cfgLine.append("&emax").append("=").append(extPort.getEmax());
                                cfgLine.append("&espd").append("=").append(extPort.getEspd());
                                cfgLine.append("&epwm").append("=").append(extPort.getEpwm());
                                cfgLine.append("&nr=1\n");
                                Files.writeString(file.toPath(), cfgLine.toString(), StandardCharsets.UTF_8,
                                        StandardOpenOption.APPEND);
                            }
                        }

                    }
                }
            }

        } catch (IOException e) {
            logger.error("Cannot write to file {}", file.getName());
        }
        logger.warn("Reading configuration done!");
    }

    private void writeConf() {
        try {
            final Channel statusChannel = this.statusChannel;
            logger.warn("Writing config...");
            // updateState(channelUID, OnOffType.OFF);
            if (statusChannel != null) {
                updateState(statusChannel.getUID(), StringType.valueOf("Writing config... "));
            }
            File file = new File(
                    OpenHAB.getUserDataFolder() + File.separator + "MegaD" + File.separator + "cfg" + File.separator);
            if (file.listFiles() != null) {
                for (File fileList : file.listFiles()) {
                    logger.debug("file {}", fileList.getName());
                    List<String> lines;
                    lines = Files.readAllLines(fileList.toPath(), StandardCharsets.UTF_8);
                    if (lines != null) {
                        if (lines.stream().anyMatch(ip -> ip.contains("eip=" + config.hostname))) {
                            for (String line : lines) {
                                MegaDHttpHelpers httpRequest = new MegaDHttpHelpers();
                                // logger.warn("line is {}", line);
                                int response = httpRequest.request(
                                        "http://" + config.hostname + "/" + config.password + "/?" + line.trim())
                                        .getResponseCode();
                                if (response == 200) {
                                    logger.warn("line written {}", line);
                                    Thread.sleep(100);
                                }
                            }
                        }
                    }
                }
            } else {
                boolean createOk = file.mkdirs();
                if (!createOk) {
                    logger.warn("Cannot create folder {}", file.getAbsolutePath());
                }
            }
        } catch (IOException err) {
            logger.warn("error write config...{}", err.getLocalizedMessage());
        } catch (InterruptedException ignored) {
        }
        MegaDHttpHelpers httpRequest = new MegaDHttpHelpers();
        httpRequest.request("http://" + config.hostname + "/" + config.password + "/?restart=1");
    }
}
