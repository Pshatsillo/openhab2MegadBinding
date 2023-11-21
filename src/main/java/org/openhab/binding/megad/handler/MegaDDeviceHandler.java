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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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
import org.openhab.binding.megad.internal.MegaDHTTPResponse;
import org.openhab.binding.megad.internal.MegaDHttpHelpers;
import org.openhab.binding.megad.internal.MegaDService;
import org.openhab.core.OpenHAB;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.library.types.OnOffType;
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

    public MegaDDeviceHandler(Bridge bridge) {
        super(bridge);
    }

    @Override
    public void initialize() {
        final Logger logger = LoggerFactory.getLogger(MegaDDeviceHandler.class);
        config = getConfigAs(MegaDConfiguration.class);
        megaDHardware = new MegaDHardware(Objects.requireNonNull(config).hostname, config.password);
        MegaDHTTPResponse response = httpHelper.request("http://" + config.hostname + "/" + config.password);
        if (response.getResponseCode() >= 400) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Wrong password");
        } else {
            if (response.getResponseResult().contains("[44,")) {
                logger.debug("Mega has 45 ports");
                megaDHardware.setPortsCount(45);
            } else {
                logger.debug("Mega has 37 ports");
                megaDHardware.setPortsCount(37);
            }
            megaDHardware.parse(response.getResponseResult());
            Map<String, String> properties = new HashMap<>();
            properties.put("Type:", megaDHardware.getType());
            properties.put("Firmware:", megaDHardware.getFirmware());
            properties.put("Actual Firmware:", megaDHardware.getActualFirmware());
            updateProperties(properties);
            String ip = config.hostname.substring(0, config.hostname.lastIndexOf("."));
            for (InetAddress address : MegaDService.interfacesAddresses) {
                if (address.getHostAddress().startsWith(ip)) {
                    if (MegaDService.interfacesAddresses.stream().findFirst().isPresent()) {
                        if ((!megaDHardware.getIp()
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
            ThingBuilder thingBuilder = editThing();
            thingBuilder.withChannels(channelList);
            updateThing(thingBuilder.build());
            updateStatus(ThingStatus.ONLINE);
        }
        Objects.requireNonNull(megaDDeviceHandlerList).add(this);
        final ScheduledFuture<?> refreshPollingJob = this.refreshPollingJob;
        if (refreshPollingJob == null || refreshPollingJob.isCancelled()) {
            this.refreshPollingJob = scheduler.scheduleWithFixedDelay(this::refresh, 0, 1000, TimeUnit.MILLISECONDS);
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE,
                    "Bridge for incoming connections not selected");
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (channelUID.getId().equals(MegaDBindingConstants.CHANNEL_FLASH) && command.equals(OnOffType.ON)) {
            int bl = 0;
            logger.warn("Flashing mega!");
            firmwareUpdate = true;
            int chipType = 0;
            boolean beta = false;
            boolean blUpgrade = false;
            Channel flashChannel = thing.getChannel(channelUID);
            if (flashChannel != null) {
                if (flashChannel.getConfiguration().get("beta") != null) {
                    beta = Boolean.parseBoolean(flashChannel.getConfiguration().get("beta").toString());
                }
            }
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.FIRMWARE_UPDATING);
            try {
                InetAddress broadcastAddress = InetAddress
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
                        String checkData = "DACA";
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
                        if (((receivedPacket[0] & 0xFF) == 0xAA) && ((receivedPacket[1] & 0xFF) == 0x00)) {
                            logger.warn("OK");
                            if ((receivedPacket[2] & 0xFF) == 0x99 || (receivedPacket[2] & 0xFF) == 0x9A) {
                                if (!checkData.isBlank()) {
                                    logger.warn("New bootloader");
                                }
                                if ((receivedPacket[2] & 0xFF) == 0x99) {
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
                            File file = new File(OpenHAB.getUserDataFolder() + File.separator + "MegaD" + File.separator
                                    + dl_fw_fname);
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
                                for (int i = 0; i < lines.size(); i++) {
                                    float progress = (float) i / lines.size();
                                    logger.debug("Preparing firmware ... {}%", (int) (progress * 100));
                                    char addrEnd = lines.get(i).charAt(8);
                                    if (!lines.get(i).isEmpty() && addrEnd == 48) {
                                        String byteCount = String.valueOf(lines.get(i).charAt(1));
                                        byteCount += String.valueOf(lines.get(i).charAt(2));
                                        String convResult = new BigInteger(byteCount, 16).toString(10);
                                        for (int i1 = 0; i1 < Integer.parseInt(convResult); i1++) {
                                            int pos = i1 * 2 + 9;
                                            firmware.append(lines.get(i).charAt(pos))
                                                    .append(String.valueOf(lines.get(i).charAt(pos + 1)));
                                        }
                                    }
                                }
                                logger.warn("Download finish");
                                byte[] intByte = HexFormat.of().parseHex(firmware.toString());
                                if (intByte.length > 258046 && chipType == 2561) {
                                    logger.warn("FAULT! Firmware is too large!");
                                } else if (intByte.length < 1000) {
                                    logger.warn("FAULT! Firmware length is zero or file is corrupted!");
                                } else if (intByte.length > 32768 && chipType == 2561 && blUpgrade) {
                                    logger.warn("FAULT! You have to upgrade bootloader!");
                                } else {
                                    logger.warn("Erasing firmware... ");
                                    broadcast_string = "AA0002" + checkData;
                                    buf = HexFormat.of().parseHex(broadcast_string);
                                    byte[] eraseBuf = new byte[5];
                                    DatagramPacket erasePacket = new DatagramPacket(buf, buf.length, broadcastAddress,
                                            52000);
                                    socket.send(erasePacket);
                                    DatagramPacket rcvErasePacket = new DatagramPacket(eraseBuf, 5);
                                    socket.receive(rcvErasePacket);
                                    if (rcvErasePacket.getData() != null) {
                                        if (((receivedPacket[0] & 0xFF) == 0xAA)
                                                && ((receivedPacket[1] & 0xFF) == 0x00)) {
                                            logger.warn("Writing firmware... ");
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
                                                        logger.warn("FAULT\nPlease update firmware in recovery mode");
                                                        break;
                                                    }
                                                }
                                                msgID++;
                                                if (msgID == 256)
                                                    msgID = 0;
                                                // logger.warn("next: {}", rcvPacket.getData());
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        broadcast_string = "AA0003" + checkData;
                        buf = HexFormat.of().parseHex(broadcast_string);
                        DatagramPacket packet = new DatagramPacket(buf, buf.length, broadcastAddress, 52000);
                        socket.send(packet);
                        socket.close();
                    }
                    updateState(channelUID, OnOffType.OFF);
                    Map<String, String> properties = new HashMap<>();
                    properties.put("Type:", megaDHardware.getType());
                    properties.put("Firmware:", megaDHardware.getFirmware());
                    properties.put("Actual Firmware:", megaDHardware.getActualFirmware());
                    updateProperties(properties);
                    updateStatus(ThingStatus.ONLINE);
                }
            } catch (IOException e) {
                logger.warn("Flashing mega error. Device is offline");
            } catch (InterruptedException ignored) {
            }
        }
    }

    private void refresh() {
        if (!firmwareUpdate) {
            try {
                Socket sck = new Socket(getThing().getConfiguration().get("hostname").toString(), 80);
                updateStatus(ThingStatus.ONLINE);
                sck.close();
            } catch (IOException e) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                        "Device not responding on ping");
                // logger.debug("proc error {}", e.getMessage());
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
    }
}
