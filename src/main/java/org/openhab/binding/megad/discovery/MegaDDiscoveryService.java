/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.megad.MegaDBindingConstants;
import org.openhab.binding.megad.handler.MegaDBridge1WireBusHandler;
import org.openhab.binding.megad.handler.MegaDBridgeIToCHandler;
import org.openhab.binding.megad.handler.MegaDBridgeIncomingHandler;
import org.openhab.binding.megad.internal.MegaHttpHelpers;
import org.openhab.core.config.discovery.AbstractDiscoveryService;
import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.config.discovery.DiscoveryResultBuilder;
import org.openhab.core.config.discovery.DiscoveryService;
import org.openhab.core.net.NetUtil;
import org.openhab.core.thing.ThingUID;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Discovery service for Megad
 *
 * @author Petr Shatsillo - Initial contribution
 *
 */
@Component(service = DiscoveryService.class, configurationPid = "discovery.megad")
@NonNullByDefault
public class MegaDDiscoveryService extends AbstractDiscoveryService {
    private final Logger logger = LoggerFactory.getLogger(MegaDDiscoveryService.class);
    @Nullable
    DatagramSocket socket;
    @Nullable
    private Runnable scanner;
    private @Nullable ScheduledFuture<?> backgroundFuture;
    public static @Nullable List<MegaDBridge1WireBusHandler> oneWireBusList = new ArrayList<>();
    public static @Nullable List<MegaDBridgeIncomingHandler> incomingBusList = new ArrayList<>();
    public static @Nullable List<MegaDBridgeIToCHandler> i2cBusList = new ArrayList<>();

    public MegaDDiscoveryService() {
        super(Collections.singleton(MegaDBindingConstants.THING_TYPE_DEVICE_BRIDGE), 30, true);
    }

    @Override
    public synchronized void abortScan() {
        super.abortScan();
    }

    @Override
    protected synchronized void stopScan() {
        if (socket != null) {
            socket.close();
            socket = null;
        }

        ScheduledFuture<?> scan = backgroundFuture;
        if (scan != null) {
            scan.cancel(true);
            backgroundFuture = null;
        }
        super.stopScan();
    }

    @Override
    protected void startScan() {
        try {
            socket = new DatagramSocket(42000);
            socket.setSoTimeout(50000);
        } catch (SocketException e) {
            logger.debug("{}", e.getMessage());
        }
        Thread server = new Thread(new Runnable() {
            final byte[] buffer = new byte[5];

            public void run() {
                while (true) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    try {
                        if (socket != null) {
                            socket.receive(packet);
                        }
                    } catch (IOException e) {
                        logger.error("Scan socket closed: {}", e.getLocalizedMessage());
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

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            // e.printStackTrace();
        }
        scanner = createScanner();
        scanner.run();

        oneWireBusScan();
        iToCBusScan();
        logger.debug("StartScan");
        try {
            Thread.sleep(10000);
        } catch (InterruptedException ignored) {
        }
        server.interrupt();
    }

    @Override
    protected void startBackgroundDiscovery() {
        logger.debug("startBackgroundDiscovery");
    }

    @Override
    protected void stopBackgroundDiscovery() {
        logger.debug("stopBackgroundDiscovery");

        super.stopBackgroundDiscovery();
    }

    private Runnable createScanner() {
        return () -> {
            long timestampOfLastScan = getTimestampOfLastScan();
            try {
                DatagramSocket socket = new DatagramSocket();
                byte[] buf = { (byte) 170, 0, 12, (byte) 218, (byte) 202 };
                for (InetAddress broadcastAddress : getBroadcastAddresses()) {
                    logger.trace("Broadcast address is {}", broadcastAddress.toString());
                    DatagramPacket packet = new DatagramPacket(buf, buf.length, broadcastAddress, 52000);
                    socket.send(packet);
                }
                socket.close();
            } catch (IOException e) {
                logger.warn("{}", e.getMessage());
            }
            oneWireBusScan();
            iToCBusScan();
            removeOlderResults(timestampOfLastScan);
        };
    }

    private void receivePacketAndDiscover(byte[] result) {
        String ips = String.format("%d.%d.%d.%d", result[1] & 0xFF, result[2] & 0xFF, result[3] & 0xFF,
                result[4] & 0xFF);
        for (MegaDBridgeIncomingHandler incoming : incomingBusList) {
            ThingUID thingUID = new ThingUID(MegaDBindingConstants.THING_TYPE_DEVICE_BRIDGE,
                    incoming.getThing().getUID(), ips.replace('.', '_'));
            DiscoveryResult resultS = DiscoveryResultBuilder.create(thingUID).withProperty("hostname", ips)
                    .withRepresentationProperty("hostname")
                    .withLabel("megad " + ips + " at " + incoming.getThing().getLabel())
                    .withBridge(incoming.getThing().getUID()).build();
            thingDiscovered(resultS);
        }

        logger.trace("Found MegaD at: {}", ips);
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

    private void oneWireBusScan() {
        if (!oneWireBusList.isEmpty()) {
            logger.debug("scanning onewire bus");
            for (MegaDBridge1WireBusHandler onewireBus : oneWireBusList) {
                logger.debug("scanning {} port and {} host", onewireBus.getThing().getConfiguration().get("port"),
                        onewireBus.getHostPassword()[0]);
                String request = "http://" + onewireBus.getHostPassword()[0] + "/" + onewireBus.getHostPassword()[1]
                        + "/?pt=" + onewireBus.getThing().getConfiguration().get("port").toString() + "?cmd=list";
                String updateRequest = MegaHttpHelpers.sendRequest(request);
                String[] getAddress = updateRequest.split("[;]");
                logger.debug("scanner request: {}", request);
                for (String address : getAddress) {
                    String[] getValues = address.split("[:]");
                    logger.debug("{}", getValues[0]);
                    try {
                        ThingUID thingUID = new ThingUID(MegaDBindingConstants.THING_TYPE_1WIREADDRESS,
                                onewireBus.getThing().getUID(), "1wbusSensor_" + getValues[0]);
                        DiscoveryResult resultS = DiscoveryResultBuilder.create(thingUID)
                                .withProperty("address", getValues[0]).withRepresentationProperty("address")
                                .withLabel(
                                        "onwireSensor " + getValues[0] + " at bus " + onewireBus.getThing().getLabel())
                                .withBridge(onewireBus.getThing().getUID()).build();
                        thingDiscovered(resultS);
                    } catch (Exception e) {
                        logger.debug("Cannot create discover thing");
                    }
                }
            }
        }
    }

    private void iToCBusScan() {
        if (!i2cBusList.isEmpty()) {
            logger.debug("scanning i2c bus");
            for (MegaDBridgeIToCHandler i2cBridge : i2cBusList) {
                logger.debug("scanning {} port and {} host", i2cBridge.getThing().getConfiguration().get("port"),
                        i2cBridge.getHostPassword()[0]);
                String request = "http://" + i2cBridge.getHostPassword()[0] + "/" + i2cBridge.getHostPassword()[1]
                        + "/?pt=" + i2cBridge.getThing().getConfiguration().get("port").toString() + "&cmd=scan";
                String updateRequest = MegaHttpHelpers.sendRequest(request);
                String[] sensorsList = updateRequest.split("<br>");
                logger.debug("scanner request: {}", request);
                for (int i = 1; i < sensorsList.length; i++) {
                    try {
                        String[] sensorUrl = sensorsList[i].split("href=");
                        sensorUrl = sensorUrl[1].split("[>]");
                        String[] sensorType = sensorUrl[0].split("[=]");
                        logger.debug("sensor is {}", sensorType[3]);
                        ThingUID thingUID = new ThingUID(MegaDBindingConstants.THING_TYPE_I2CBUSSENSOR,
                                i2cBridge.getThing().getUID(), "I2CbusSensor_" + sensorType[3]);
                        DiscoveryResult resultS = DiscoveryResultBuilder.create(thingUID)
                                .withProperty("sensortype", sensorType[3]).withRepresentationProperty("sensortype")
                                .withLabel("I2CSensor " + sensorType[3] + " at bus " + i2cBridge.getThing().getLabel())
                                .withBridge(i2cBridge.getThing().getUID()).build();
                        thingDiscovered(resultS);
                    } catch (Exception e) {
                        logger.debug("SDA port is not defined");
                    }
                }
            }
        }
    }
}
