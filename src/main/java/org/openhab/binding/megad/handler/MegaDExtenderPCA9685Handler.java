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
import org.openhab.core.library.types.PercentType;
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
 * The {@link MegaDExtenderPCA9685Handler} is responsible for creating MegaD extenders
 * based on PCA9685
 * this class represent bridge for port where extender is located
 *
 * @author kosh_ - Initial contribution
 */
@NonNullByDefault
public class MegaDExtenderPCA9685Handler extends BaseThingHandler {
    @Nullable
    MegaDBridgeExtenderPCA9685Handler bridge;
    private int pwmMaxValue = 4095;
    protected int dimmervalue = 150;
    private Logger logger = LoggerFactory.getLogger(MegaDExtenderPCA9685Handler.class);

    public MegaDExtenderPCA9685Handler(Thing thing) {
        super(thing);
    }

    @SuppressWarnings("null")
    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        String strCommand = command.toString();
        if (!"REFRESH".equals(strCommand)) {
            String hostname = bridge.getHostPassword()[0];
            String password = bridge.getHostPassword()[1];
            String port = bridge.getThing().getConfiguration().get("port").toString();
            String extport = getThing().getConfiguration().get("extport").toString();
            String result = "http://" + hostname + "/" + password + "/?cmd=" + port + "e" + extport + ":";
            String idChannel = channelUID.getId();
            switch (idChannel) {
                case MegaDBindingConstants.CHANNEL_DIMMER:
                    switch (strCommand) {
                        case "OFF":
                            result += "0";
                            logger.info("Dimmer set to OFF");
                            sendCommand(result);
                            updateState(idChannel, PercentType.valueOf("0"));
                            break;
                        case "ON":
                            result += dimmervalue;
                            logger.info("Dimmer restored to previous value: {}", result);
                            sendCommand(result);
                            int percent = 0;
                            try {
                                percent = Math.round(dimmervalue * 100 / pwmMaxValue);
                            } catch (Exception e) {
                            }
                            updateState(idChannel, PercentType.valueOf(Integer.toString(percent)));
                            break;
                        default:
                            try {
                                int uivalue = Integer.parseInt(strCommand.split("[.]")[0]);
                                int resultInt = Math.round(uivalue * pwmMaxValue / 100);
                                if (uivalue == 1) {
                                    resultInt = uivalue;
                                } else if (resultInt != 0) {
                                    dimmervalue = resultInt;
                                }
                                result += resultInt;
                                logger.info("Dimmer: {}", result);
                                sendCommand(result);
                            } catch (Exception e) {
                                logger.warn("Illegal dimmer value: {}", result);
                            }
                            break;
                    }
                    break;
                case MegaDBindingConstants.CHANNEL_PWM:
                    int currentValue = 0;
                    try {
                        int uivalue = Integer.parseInt(strCommand.split("[.]")[0]);
                        if (uivalue != 0) {
                            currentValue = uivalue;
                        }
                        if (uivalue > pwmMaxValue) {
                            currentValue = pwmMaxValue;
                        }
                        result += currentValue;
                        logger.info("PWM: {}", result);
                        sendCommand(result);
                    } catch (Exception e) {
                        result += currentValue;
                        logger.info("PWM restored to previous value: {}", result);
                        sendCommand(result);
                        updateState(idChannel, DecimalType.valueOf(Integer.toString(currentValue)));
                    }
                    break;
                default:
                    logger.warn("Channel {} for ExtenderPCA9685(handleCommand) not found", idChannel);
                    break;
            }
        }
    }

    @SuppressWarnings("null")
    @Override
    public void initialize() {
        bridge = getBridgeHandler();
        registerListenerThing(bridge);
        if (bridge != null) {
            while (!bridge.getStateStarted()) {
                try {
                    logger.info("Waiting for a state started for Bridge");
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    logger.error("{}", e.getMessage());
                }
            }
            update();
        }
    }

    private void registerListenerThing(@Nullable MegaDBridgeExtenderPCA9685Handler bridge) {
        if (bridge != null) {
            bridge.registerListenerThing(this);
        } else {
            logger.debug("Can't register bridge for {}. BridgeHandler is null.", this.getThing().getUID());
        }
    }

    public void updateValues(String action) {
        for (Channel channel : getThing().getChannels()) {
            String idChannel = channel.getUID().getId();
            if (isLinked(idChannel)) {
                logger.debug("updateValues of thing {}: {}", getThing().getUID().toString(), action);
                switch (idChannel) {
                    case MegaDBindingConstants.CHANNEL_DIMMER:
                        try {
                            if ("0".equals(action)) {
                                logger.debug("dimmer value is 0, do not save dimmer value");
                            } else {
                                dimmervalue = Integer.parseInt(action);
                            }
                        } catch (Exception ignored) {
                        }
                        int percent = 0;
                        try {
                            percent = Math.round(dimmervalue * 100 / pwmMaxValue);
                        } catch (Exception e) {
                            logger.debug("Cannot convert to dimmer values string: '{}'", dimmervalue);
                        }
                        updateState(idChannel, PercentType.valueOf(Integer.toString(percent)));
                        break;
                    case MegaDBindingConstants.CHANNEL_PWM:
                        int currentValue = 0;
                        try {
                            if ("0".equals(action)) {
                                logger.debug("pwm value is 0, do not save pwm value");
                            } else {
                                currentValue = Integer.parseInt(action);
                            }
                        } catch (Exception ignored) {
                        }
                        try {
                            updateState(idChannel, DecimalType.valueOf(Integer.toString(currentValue)));
                        } catch (Exception ignored) {
                        }
                        break;
                    default:
                        logger.warn("Channel {} for ExtenderPCA9685(updateValues) not found", idChannel);
                        break;
                }
            }
        }
    }

    @SuppressWarnings("null")
    @Override
    public void dispose() {
        if (bridge != null) {
            bridge.unregisterListenerThing(this);
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

    private synchronized @Nullable MegaDBridgeExtenderPCA9685Handler getBridgeHandler(Bridge bridge) {
        ThingHandler handler = bridge.getHandler();
        if (handler instanceof MegaDBridgeExtenderPCA9685Handler) {
            return (MegaDBridgeExtenderPCA9685Handler) handler;
        } else {
            logger.debug("No available bridge handler found yet. Bridge: {} .", bridge.getUID());
            return null;
        }
    }

    @SuppressWarnings({ "null" })
    protected void update() {
        String extport = getThing().getConfiguration().get("extport").toString();
        String portValue = bridge.getPortsvalues(extport);
        for (Channel channel : getThing().getChannels()) {
            String idChannel = channel.getUID().getId();
            switch (idChannel) {
                case MegaDBindingConstants.CHANNEL_DIMMER:
                    if ("0".equals(portValue)) {
                        logger.debug("dimmer value is 0, do not save dimmer value");
                    } else {
                        dimmervalue = Integer.parseInt(portValue);
                    }
                    int percent = 0;
                    try {
                        percent = Math.round(Integer.parseInt(portValue) * 100 / pwmMaxValue);
                    } catch (Exception e) {
                        logger.debug("Cannot convert to dimmer values string: '{}'", portValue);
                    }
                    updateState(idChannel, PercentType.valueOf(Integer.toString(percent)));
                    break;
                case MegaDBindingConstants.CHANNEL_PWM:
                    int currentValue = 0;
                    try {
                        if ("0".equals(portValue)) {
                            logger.debug("pwm value is 0, do not save pwm value");
                        } else {
                            currentValue = Integer.parseInt(portValue);
                        }
                        if (currentValue > pwmMaxValue) {
                            currentValue = pwmMaxValue;
                        }
                        updateState(idChannel, DecimalType.valueOf(Integer.toString(currentValue)));
                    } catch (Exception e) {
                        logger.debug("Cannot update PWM value");
                    }
                    break;
                default:
                    logger.warn("Channel {} for ExtenderPCA9685(updateData) not found", idChannel);
                    break;
            }
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
    public void sendCommand(String result) {
        try {
            URL megaURL = new URL(result);
            HttpURLConnection con = (HttpURLConnection) megaURL.openConnection();
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
            logger.error("Connect to megadevice {} {} error: ", bridge.getHostPassword()[0], e.getLocalizedMessage());
        }
    }
}
