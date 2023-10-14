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
import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.megad.MegaDBindingConstants;
import org.openhab.binding.megad.handler.MegaDDeviceHandler;
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
    public static @Nullable List<MegaDDeviceHandler> megaDDeviceHandlerList = new ArrayList<>();
    private final Logger logger = LoggerFactory.getLogger(MegaDDiscoveryService.class);
    @Nullable
    DatagramSocket socket;
    private @Nullable ScheduledFuture<?> backgroundFuture;

    public MegaDDiscoveryService() {
        super(Collections.singleton(MegaDBindingConstants.THING_TYPE_DEVICE), 30, true);
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
        @Nullable Runnable scanner1 = createScanner();
        final Runnable scanner = scanner1;
        if (scanner != null) {
            scanner.run();
            logger.error("StartScan");
        }
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

            public void run() {
                while (true) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                    try {
                        loSock.receive(packet);
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
        return server;
    }

    @Override
    protected void startBackgroundDiscovery() {
        logger.error("startBackgroundDiscovery");
        backgroundFuture = scheduler.scheduleWithFixedDelay(this::scan, 0, 30, TimeUnit.SECONDS);
    }

    @Override
    protected void stopBackgroundDiscovery() {
        logger.error("stopBackgroundDiscovery");
        ScheduledFuture<?> scan = backgroundFuture;
        if (scan != null) {
            scan.cancel(true);
            backgroundFuture = null;
        }
        super.stopBackgroundDiscovery();
    }

    private Runnable createScanner() {
        return () -> {
            long timestampOfLastScan = getTimestampOfLastScan();
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
            removeOlderResults(timestampOfLastScan);
        };
    }

    private void receivePacketAndDiscover(byte[] result) {
        String ips = String.format("%d.%d.%d.%d", result[1] & 0xFF, result[2] & 0xFF, result[3] & 0xFF,
                result[4] & 0xFF);
        ThingUID thingUID = new ThingUID(MegaDBindingConstants.THING_TYPE_DEVICE, ips.replace('.', '_'));
        DiscoveryResult resultS = DiscoveryResultBuilder.create(thingUID).withProperty("hostname", ips)
                .withRepresentationProperty("hostname").withLabel("megad " + ips).build();
        thingDiscovered(resultS);

        logger.error("Found MegaD at: {}", ips);
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

    private synchronized void scan() {
        logger.info("Scanning...");

        // ThingUID thingUID = new ThingUID(MegaDBindingConstants.THING_TYPE_PORT,
        // megaDDeviceHandlerList.get(0).getThing().getUID(), ips.replace('.', '_'));
        // DiscoveryResult resultS = DiscoveryResultBuilder.create(thingUID).withProperty("hostname", ips)
        // .withRepresentationProperty("hostname")
        // .withLabel("megad " + ips + " at " + incoming.getThing().getLabel())
        // .withBridge(incoming.getThing().getUID()).build();
        // thingDiscovered(resultS);
    }
}
