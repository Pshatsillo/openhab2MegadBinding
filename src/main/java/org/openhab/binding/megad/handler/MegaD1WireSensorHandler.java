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
import org.openhab.core.library.types.DecimalType;
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
 * The {@link MegaD1WireSensorHandler} class defines 1-wire bus feature.
 * You can read 1-wire sensors connected to one port of MegaD as bus
 *
 * @author Petr Shatsillo - Initial contribution
 */
@NonNullByDefault
public class MegaD1WireSensorHandler extends BaseThingHandler {

    private Logger logger = LoggerFactory.getLogger(MegaD1WireSensorHandler.class);

    @Nullable
    MegaDBridge1WireBusHandler bridge1WireBusHandler;

    public MegaD1WireSensorHandler(Thing thing) {
        super(thing);
    }

    @SuppressWarnings("null")
    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        int state = 0;
        String cmd = "";
        String addr = "";
        if (!command.toString().equals("REFRESH")) {
            if (command.toString().equals("ON")) {
                state = 1;
            } else if (command.toString().equals("OFF")) {
                state = 0;
            }
            if (channelUID.getId().equals(MegaDBindingConstants.CHANNEL_MEGAD2W_A)) {
                cmd = bridge1WireBusHandler.getThing().getConfiguration().get("port").toString() + "A:";
            } else if (channelUID.getId().equals(MegaDBindingConstants.CHANNEL_MEGAD2W_B)) {
                cmd = bridge1WireBusHandler.getThing().getConfiguration().get("port").toString() + "B:";
            }
            if (!getThing().getConfiguration().get("address").equals("0")) {
                addr = "&addr=" + getThing().getConfiguration().get("address").toString();
            }
            String result = "http://" + bridge1WireBusHandler.getHostPassword()[0] + "/"
                    + bridge1WireBusHandler.getHostPassword()[1] + "/?cmd=" + cmd + state + addr;
            logger.debug("Switch: {}", result);
            sendCommand(result);
        }
    }

    @SuppressWarnings("null")
    @Override
    public void dispose() {
        if (bridge1WireBusHandler != null) {
            bridge1WireBusHandler.unregisterMegad1WireListener(this);
        }
        super.dispose();
    }

    @SuppressWarnings("null")
    @Override
    public void initialize() {
        bridge1WireBusHandler = getBridgeHandler();
        if (bridge1WireBusHandler != null) {
            registerMegad1WireListener(bridge1WireBusHandler);
        } else {
            logger.debug("Can't register {} at bridge. BridgeHandler is null.", this.getThing().getUID());
        }
    }

    @SuppressWarnings("null")
    public void updateValues(String portStatus) {
        String[] ports = portStatus.split("[/]");
        for (Channel channel : getThing().getChannels()) {
            if (isLinked(channel.getUID().getId())) {
                if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_MEGAD2W_A)) {
                    try {
                        if (ports[0].equals("ON")) {
                            updateState(channel.getUID().getId(), OnOffType.ON);
                        } else if (ports[0].equals("OFF")) {
                            updateState(channel.getUID().getId(), OnOffType.OFF);
                        } else {
                            logger.debug("Status {} is udefined", ports[0]);
                        }
                    } catch (Exception e) {
                        logger.debug("Cannot find value for port A");
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_MEGAD2W_B)) {
                    try {
                        if (ports[1].equals("ON")) {
                            updateState(channel.getUID().getId(), OnOffType.ON);
                        } else if (ports[1].equals("OFF")) {
                            updateState(channel.getUID().getId(), OnOffType.OFF);
                        } else {
                            logger.debug("Status {} is udefined", ports[1]);
                        }
                    } catch (Exception e) {
                        logger.debug("Cannot find value for port B");
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_1WTEMP)) {
                    String address = getThing().getConfiguration().get("address").toString();
                    try {
                        updateState(channel.getUID().getId(),
                                DecimalType.valueOf(bridge1WireBusHandler.getOwvalues(address)));
                    } catch (Exception e) {
                        logger.debug("Can't update 1w-bus channel value bacause of {}", e.getMessage());
                    }
                }
            }
        }
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
        } catch (MalformedURLException | ProtocolException e) {
            logger.error("{}", e.getLocalizedMessage());
        } catch (IOException e) {
            logger.error("Connect to megadevice {} {} error: ", bridge1WireBusHandler.getHostPassword()[0],
                    e.getLocalizedMessage());
        }
    }

    private synchronized @Nullable MegaDBridge1WireBusHandler getBridgeHandler() {
        Bridge bridge = getBridge();
        if (bridge == null) {
            logger.error("Required bridge not defined for device.");
            // throw new NullPointerException("Required bridge not defined for device");
            return null;
        } else {
            return getBridgeHandler(bridge);
        }
    }

    private synchronized @Nullable MegaDBridge1WireBusHandler getBridgeHandler(Bridge bridge) {
        ThingHandler handler = bridge.getHandler();
        if (handler instanceof MegaDBridge1WireBusHandler) {
            return (MegaDBridge1WireBusHandler) handler;
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
    protected void updateStatus(ThingStatus status, ThingStatusDetail statusDetail, @Nullable String description) {
        super.updateStatus(status, statusDetail, description);
    }

    private void registerMegad1WireListener(@Nullable MegaDBridge1WireBusHandler bridgeHandler) {
        if (bridgeHandler != null) {
            bridgeHandler.registerMegad1WireListener(this);
        }
    }
}
