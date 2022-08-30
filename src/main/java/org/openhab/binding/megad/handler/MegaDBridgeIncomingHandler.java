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
package org.openhab.binding.megad.handler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.server.Server;
import org.openhab.binding.megad.MegaDConfiguration;
import org.openhab.binding.megad.discovery.MegaDDiscoveryService;
import org.openhab.binding.megad.internal.IncomingMessagesServlet;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseBridgeHandler;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link MegaDBridgeIncomingHandler} is responsible for creating things and thing
 * handlers.
 *
 * @author Petr Shatsillo - Initial contribution
 */
@NonNullByDefault
public class MegaDBridgeIncomingHandler extends BaseBridgeHandler {
    Logger logger = LoggerFactory.getLogger(MegaDBridgeIncomingHandler.class);
    private int port;
    @Nullable
    Server s;
    @Nullable
    MegaDBridgeDeviceHandler deviceHandler;
    private @Nullable Map<String, MegaDBridgeDeviceHandler> devicesHandlerMap = new HashMap<String, MegaDBridgeDeviceHandler>();
    private @Nullable ScheduledFuture<?> refreshPollingJob;

    public MegaDBridgeIncomingHandler(Bridge bridge) {
        super(bridge);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
    }

    @SuppressWarnings("null")
    @Override
    public void initialize() {
        logger.debug("Initializing Megad bridge handler {}", this.toString());
        MegaDDiscoveryService.incomingBusList.add(this);
        if (refreshPollingJob == null || refreshPollingJob.isCancelled()) {
            scheduler.execute(new Runnable() {
                @Override
                public void run() {
                    serverStart();
                }
            });
        }
    }

    @SuppressWarnings("null")
    private void serverStart() {
        MegaDConfiguration configuration = getConfigAs(MegaDConfiguration.class);
        port = configuration.port;
        try {
            s = new Server(port);
            s.setHandler(new IncomingMessagesServlet(this));
            s.start();
            updateStatus(ThingStatus.ONLINE);
            s.join();
        } catch (IOException e) {
            logger.error("ERROR! Cannot open port: {}", e.getMessage());
            updateStatus(ThingStatus.OFFLINE);
        } catch (Exception e) {
            logger.error("ERROR! {}", e.getMessage());
        }
    }

    // @SuppressWarnings("null")
    public void parseInput(@Nullable String s, @Nullable String remoteHost) {
        assert remoteHost != null;
        assert devicesHandlerMap != null;
        if ("[0:0:0:0:0:0:0:1]".equals(remoteHost)) {
            deviceHandler = devicesHandlerMap.get("localhost");
        } else {
            deviceHandler = devicesHandlerMap.get(remoteHost);
        }
        if (deviceHandler != null) {
            if (s != null) {
                deviceHandler.manageValues(s);
            }
        }

        logger.debug("incoming from Megad: {} {}", remoteHost, s);
    }

    public void registerMegaDeviceListener(MegaDBridgeDeviceHandler megaDBridgeDeviceHandler) {
        String ip = megaDBridgeDeviceHandler.getThing().getConfiguration().get("hostname").toString();
        logger.debug("Register Device with ip {}", ip);
        if (devicesHandlerMap.get(ip) != null) {
            updateThingHandlerStatus(megaDBridgeDeviceHandler, ThingStatus.OFFLINE,
                    ThingStatusDetail.CONFIGURATION_ERROR, "Device already exist");
        } else {
            devicesHandlerMap.put(ip, megaDBridgeDeviceHandler);
            updateThingHandlerStatus(megaDBridgeDeviceHandler, ThingStatus.ONLINE);
        }
    }

    public void unregisterMegaDeviceListener(MegaDBridgeDeviceHandler megaDBridgeDeviceHandler) {
        String ip = megaDBridgeDeviceHandler.getThing().getConfiguration().get("hostname").toString();
        if (devicesHandlerMap.get(ip) != null) {
            devicesHandlerMap.remove(ip);
            updateThingHandlerStatus(megaDBridgeDeviceHandler, ThingStatus.OFFLINE);
        }
    }

    private void updateThingHandlerStatus(MegaDBridgeDeviceHandler megaDBridgeDeviceHandler, ThingStatus status,
            ThingStatusDetail statusDetail, String decript) {
        megaDBridgeDeviceHandler.updateStatus(status, statusDetail, decript);
    }

    private void updateThingHandlerStatus(MegaDBridgeDeviceHandler thingHandler, ThingStatus status) {
        thingHandler.updateStatus(status);
    }

    @Override
    public void dispose() {
        if (s != null) {
            try {
                s.stop();
            } catch (Exception e) {
                logger.error("Dispose ERROR: {}", e.getLocalizedMessage());
            }
        }
        if (MegaDDiscoveryService.incomingBusList != null) {
            int index = MegaDDiscoveryService.incomingBusList.indexOf(this);
            MegaDDiscoveryService.incomingBusList.remove(index);
            logger.debug("{}", index);
            updateStatus(ThingStatus.OFFLINE); // Set all State to offline
        }
        super.dispose();
    }
}
