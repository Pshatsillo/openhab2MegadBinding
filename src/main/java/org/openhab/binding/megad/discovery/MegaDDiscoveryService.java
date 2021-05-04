/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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

import static java.lang.Thread.sleep;

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
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.megad.MegaDBindingConstants;
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
    private final Runnable scanner;
    private @Nullable ScheduledFuture<?> backgroundFuture;

    public MegaDDiscoveryService() {
        super(Collections.singleton(MegaDBindingConstants.THING_TYPE_DEVICE_BRIDGE), 30, true);
        try {
            socket = new DatagramSocket(42000);
        } catch (SocketException e) {
            logger.debug("{}", e.getMessage());
        }
        scanner = createScanner();
    }

    @Override
    protected void startScan() {
        logger.debug("StartScan");
    }

    @Override
    protected void startBackgroundDiscovery() {
        logger.debug("startBackgroundDiscovery");
        server.start();
        backgroundFuture = scheduler.scheduleWithFixedDelay(scanner, 0, 60, TimeUnit.SECONDS);
    }

    @Override
    protected void stopBackgroundDiscovery() {
        logger.debug("stopBackgroundDiscovery");
        socket.close();
        server.interrupt();
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
                byte[] buf = { (byte) 170, 0, 12 };
                for (InetAddress broadcastAddress : getBroadcastAddresses()) {
                    logger.trace("Broadcast address is {}", broadcastAddress.toString());
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
        ThingUID thingUID = new ThingUID(MegaDBindingConstants.THING_TYPE_DEVICE_BRIDGE, ips.replace('.', '_'));
        DiscoveryResult resultS = DiscoveryResultBuilder.create(thingUID).withProperty("hostname", ips)
                .withRepresentationProperty("hostname").withLabel("megad " + ips).build();

        thingDiscovered(resultS);

        logger.trace("Found MegaD at: {}", ips);
    }

    Thread server = new Thread(new Runnable() {
        final byte[] buffer = new byte[5];

        public void run() {
            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                try {
                    socket.receive(packet);
                } catch (IOException ignored) {
                }
                byte[] received = packet.getData();

                try {
                    sleep(100);
                } catch (InterruptedException e) {
                    break;
                }
                receivePacketAndDiscover(received);
            }
            socket.close();
        }
    });

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
}
