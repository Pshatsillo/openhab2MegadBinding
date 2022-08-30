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
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.megad.MegaDBindingConstants;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link MegaDExtenderHandler} is responsible for creating MegaD extenders
 * based on MCP23008/MCP23017
 * this class represent bridge for port where extender is located
 *
 * @author Petr Shatsillo - Initial contribution
 */
@NonNullByDefault
public class MegaDExtenderHandler extends BaseThingHandler {
    @Nullable
    MegaDBridgeExtenderPortHandler extenderPortBridge;
    private Logger logger = LoggerFactory.getLogger(MegaDExtenderHandler.class);

    public MegaDExtenderHandler(Thing thing) {
        super(thing);
    }

    @SuppressWarnings("null")
    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (channelUID.getId().equals(MegaDBindingConstants.CHANNEL_EXTENDER_OUT)) {
            int state = 0;
            if (command.toString().equals("ON")) {
                state = 1;
            } else if (command.toString().equals("OFF")) {
                state = 0;
            }
            String result = "http://" + extenderPortBridge.getHostPassword()[0] + "/"
                    + extenderPortBridge.getHostPassword()[1] + "/?cmd="
                    + extenderPortBridge.getThing().getConfiguration().get("port").toString() + "e"
                    + getThing().getConfiguration().get("extport").toString() + ":" + state;
            logger.debug("Extender switch: {}", result);
            sendCommand(result);
        }
    }

    @SuppressWarnings("null")
    @Override
    public void initialize() {
        extenderPortBridge = getBridgeHandler();
        if (extenderPortBridge != null) {
            registerExtenderListener(extenderPortBridge);
        } else {
            logger.debug("Can't register {} at bridge. BridgeHandler is null.", this.getThing().getUID());
        }
        if (extenderPortBridge != null) {
            while (!extenderPortBridge.getStateStarted()) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    logger.error("{}", e.getMessage());
                }
            }
            update();
        }
    }

    private void registerExtenderListener(@Nullable MegaDBridgeExtenderPortHandler extenderPortBridge) {
        if (extenderPortBridge != null) {
            extenderPortBridge.registerExtenderListener(this);
        }
    }

    @SuppressWarnings("null")
    public void update() {
        try {
            String portValue = extenderPortBridge
                    .getPortsvalues(getThing().getConfiguration().get("extport").toString());
            // logger.debug("Extender port value is {}", extenderPortBridge.getPortsvalues(portValue));

            for (Channel channel : getThing().getChannels()) {
                if (isLinked(channel.getUID().getId())) {
                    if ((channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_EXTENDER_IN))
                            || (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_EXTENDER_OUT))) {
                        if (portValue.contains("ON")) {
                            updateState(channel.getUID().getId(), OnOffType.ON);
                        } else if (portValue.contains("OFF")) {
                            updateState(channel.getUID().getId(), OnOffType.OFF);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error updating port {}", e.getMessage());
        }
    }

    public void updateValues(String action) {
        for (Channel channel : getThing().getChannels()) {
            if (isLinked(channel.getUID().getId())) {
                if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_EXTENDER_IN)) {
                    if ("1".equals(action)) {
                        updateState(channel.getUID().getId(), OnOffType.ON);
                    } else if ("0".equals(action)) {
                        updateState(channel.getUID().getId(), OnOffType.OFF);
                    }
                }
            }
        }
    }

    @SuppressWarnings("null")
    @Override
    public void dispose() {
        /*
         * if (refreshPollingJob != null && !refreshPollingJob.isCancelled()) {
         * refreshPollingJob.cancel(true);
         * refreshPollingJob = null;
         * }
         */
        if (extenderPortBridge != null) {
            extenderPortBridge.unregisterExtenderListener(this);
        }
        super.dispose();
    }

    private synchronized @Nullable MegaDBridgeExtenderPortHandler getBridgeHandler() {
        Bridge bridge = getBridge();
        if (bridge == null) {
            logger.error("Required bridge not defined for device.");
            return null;
        } else {
            return getBridgeHandler(bridge);
        }
    }

    private synchronized @Nullable MegaDBridgeExtenderPortHandler getBridgeHandler(Bridge bridge) {
        ThingHandler handler = bridge.getHandler();
        if (handler instanceof MegaDBridgeExtenderPortHandler) {
            return (MegaDBridgeExtenderPortHandler) handler;
        } else {
            logger.debug("No available bridge handler found yet. Bridge: {} .", bridge.getUID());
            return null;
        }
    }

    @Override
    public void updateStatus(ThingStatus status) {
        super.updateStatus(status);
    }

    @Override
    public void updateStatus(ThingStatus status, ThingStatusDetail statusDetail, @Nullable String description) {
        super.updateStatus(status, statusDetail, description);
    }

    @SuppressWarnings("null")
    public void sendCommand(String Result) {
        HttpURLConnection con;

        URL megaURL;

        try {
            megaURL = new URL(Result);
            con = (HttpURLConnection) megaURL.openConnection();
            con.setReadTimeout(1000);
            con.setConnectTimeout(1000);
            con.setRequestMethod("GET");

            // add request header
            con.setRequestProperty("User-Agent", "Mozilla/5.0");
            if (con.getResponseCode() == 200) {
                logger.debug("OK");
            }
            con.disconnect();
        } catch (MalformedURLException e) {
            logger.error("{}", e.getLocalizedMessage());
        } catch (ProtocolException e) {
            logger.error("{}", e.getLocalizedMessage());
        } catch (IOException e) {
            logger.error("Connect to megadevice {} {} error: ", extenderPortBridge.getHostPassword()[0],
                    e.getLocalizedMessage());
        }
    }
}
