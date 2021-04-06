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
package org.openhab.binding.megad.handler;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.megad.MegaDBindingConstants;
import org.openhab.binding.megad.internal.MegaHttpHelpers;
import org.openhab.core.library.types.*;
import org.openhab.core.thing.*;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link MegaDExtenderPCA9685Handler} is responsible for creating MegaD extenders
 * based on PCA9685
 * this class represent bridge for port where extender is located
 *
 * @author kosh_ - Initial contribution
 */
@NonNullByDefault
public class MegaDExtenderPCA9685Handler extends BaseThingHandler {
    @Nullable
    MegaDBridgeExtenderPCA9685Handler extenderPCA9685Bridge;
    private double dimmerDivider = 40.95;
    protected int dimmervalue = 150;
    private Logger logger = LoggerFactory.getLogger(MegaDExtenderPCA9685Handler.class);

    public MegaDExtenderPCA9685Handler(Thing thing) {
        super(thing);
    }

    @SuppressWarnings("null")
    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        String result = "";
        String hostname = extenderPCA9685Bridge.getHostPassword()[0];
        String password = extenderPCA9685Bridge.getHostPassword()[1];
        String port = extenderPCA9685Bridge.getThing().getConfiguration().get("port").toString();
        String extport = getThing().getConfiguration().get("extport").toString();
        if (channelUID.getId().equals(MegaDBindingConstants.CHANNEL_EXTENDER_PCA9685_DIMMER)) {
            if (!command.toString().equals("REFRESH")) {
                try {
                    int uivalue = Integer.parseInt(command.toString().split("[.]")[0]);
                    int resultInt = (int) Math.round(uivalue * dimmerDivider);
                    if (uivalue == 1) {
                        resultInt = uivalue;
                    } else if (resultInt != 0) {
                        dimmervalue = resultInt;
                    }
                    result = "http://" + hostname + "/" + password + "/?cmd=" + port + "e" + extport + ":" + resultInt;
                    logger.info("Dimmer: {}", result);
                    sendCommand(result);
                } catch (Exception e) {
                    if (command.toString().equals("OFF")) {
                        result = "http://" + hostname + "/" + password + "/?cmd=" + port + "e" + extport + ":0";
                        logger.info("Dimmer set to OFF");
                        sendCommand(result);
                        updateState(channelUID.getId(), PercentType.valueOf("0"));
                    } else if (command.toString().equals("ON")) {
                        result = "http://" + hostname + "/" + password + "/?cmd=" + port + "e" + extport + ":"
                                + dimmervalue;
                        logger.info("Dimmer restored to previous value: {}", result);
                        sendCommand(result);
                        int percent = 0;
                        try {
                            percent = (int) Math.round(dimmervalue / dimmerDivider);
                        } catch (Exception ex) {
                        }
                        updateState(channelUID.getId(), PercentType.valueOf(Integer.toString(percent)));
                    } else {
                        logger.debug("Illegal dimmer value: {}", result);
                    }
                }
            }
        }
    }

    @SuppressWarnings("null")
    @Override
    public void initialize() {
        extenderPCA9685Bridge = getBridgeHandler();
        if (extenderPCA9685Bridge != null) {
            registerExtenderPCA9685Listener(extenderPCA9685Bridge);
        } else {
            logger.debug("Can't register {} at bridge. BridgeHandler is null.", this.getThing().getUID());
        }
        if (extenderPCA9685Bridge != null) {
            while (!extenderPCA9685Bridge.getStateStarted()) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    logger.error("{}", e.getMessage());
                }
            }
            updateData(null);
        }
    }

    private void registerExtenderPCA9685Listener(@Nullable MegaDBridgeExtenderPCA9685Handler extenderPCA9685Bridge) {
        if (extenderPCA9685Bridge != null) {
            extenderPCA9685Bridge.registerExtenderPCA9685Listener(this);
        }
    }

    public void updateValues(String action) {
        for (Channel channel : getThing().getChannels()) {
            String idChannel = channel.getUID().getId();
            if (isLinked(idChannel)) {
                logger.debug("updateValues of thing {}: {}", getThing().getUID().toString(), action);
                if (idChannel.equals(MegaDBindingConstants.CHANNEL_EXTENDER_PCA9685_DIMMER)) {
                    try {
                        if (action.equals("0")) {
                            logger.debug("dimmer value is 0, do not save dimmer value");
                        } else {
                            dimmervalue = Integer.parseInt(action);
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }

    @SuppressWarnings("null")
    @Override
    public void dispose() {
        if (extenderPCA9685Bridge != null) {
            extenderPCA9685Bridge.unregisterExtenderPCA9685Listener(this);
        }
        super.dispose();
    }

    private synchronized @Nullable MegaDBridgeExtenderPCA9685Handler getBridgeHandler() {
        Bridge bridge = getBridge();
        if (bridge == null) {
            logger.error("Required bridge not defined for device.");
            return null;
        } else {
            return getBridgeHandler(bridge);
        }
    }

    @SuppressWarnings({ "null" })
    protected void updateData(@Nullable String extport) {
        String hostname = extenderPCA9685Bridge.getHostPassword()[0];
        String password = extenderPCA9685Bridge.getHostPassword()[1];
        String port = extenderPCA9685Bridge.getThing().getConfiguration().get("port").toString();
        if (extport == null) {
            extport = getThing().getConfiguration().get("extport").toString();
        }
        logger.debug("Updating Megadevice thing {}...", getThing().getUID().toString());
        String result = "http://" + hostname + "/" + password + "/?pt=" + port + "ext=" + extport + "&cmd=get";
        String updateRequest = MegaHttpHelpers.sendRequest(result);
        for (Channel channel : getThing().getChannels()) {
            String idChannel = channel.getUID().getId();
            if (idChannel.equals(MegaDBindingConstants.CHANNEL_EXTENDER_PCA9685_DIMMER)) {
                if (updateRequest.equals("0")) {
                    logger.debug("dimmer value is 0, do not save dimmer value");
                } else {
                    dimmervalue = Integer.parseInt(updateRequest);
                }
                int percent = 0;
                try {
                    percent = (int) Math.round(Integer.parseInt(updateRequest) / dimmerDivider);
                } catch (Exception ex) {
                    logger.debug("Cannot convert to dimmer values string: '{}'", updateRequest);
                }
                updateState(idChannel, PercentType.valueOf(Integer.toString(percent)));
            }
        }
    }

    private synchronized @Nullable MegaDBridgeExtenderPCA9685Handler getBridgeHandler(Bridge bridge) {
        ThingHandler handler = bridge.getHandler();
        if (handler instanceof MegaDBridgeExtenderPCA9685Handler) {
            return (MegaDBridgeExtenderPCA9685Handler) handler;
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
            logger.error("Connect to megadevice {} {} error: ", extenderPCA9685Bridge.getHostPassword()[0],
                    e.getLocalizedMessage());
        }
    }
}
