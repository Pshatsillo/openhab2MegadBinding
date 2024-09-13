/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
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

import static org.openhab.binding.megad.enums.MegaDModesEnum.PWM;

import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.megad.MegaDBindingConstants;
import org.openhab.binding.megad.MegaDConfiguration;
import org.openhab.binding.megad.discovery.MegaDDiscoveryService;
import org.openhab.binding.megad.dto.MegaDHardware;
import org.openhab.binding.megad.internal.MegaDHTTPResponse;
import org.openhab.binding.megad.internal.MegaDHttpHelpers;
import org.openhab.core.library.types.HSBType;
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
 * The {@link MegaDRGBHandler} is responsible for RGB LED strips,
 * connected to Megad PWM ports features of megsd
 *
 * @author Petr Shatsillo - Initial contribution
 */
@NonNullByDefault
public class MegaDRGBHandler extends BaseThingHandler {
    @Nullable
    public MegaDDeviceHandler bridgeDeviceHandler;
    MegaDConfiguration configuration = getConfigAs(MegaDConfiguration.class);
    private Logger logger = LoggerFactory.getLogger(MegaDRGBHandler.class);
    private @Nullable ScheduledFuture<?> refreshPollingJob;
    int colorRed = 0;
    int colorGreen = 0;
    int colorBlue = 0;

    // MegaDHardware.Port port = new MegaDHardware.Port();
    public MegaDRGBHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        configuration = getConfigAs(MegaDConfiguration.class);
        bridgeDeviceHandler = getBridgeHandler();
        final MegaDDeviceHandler bridgeDeviceHandler = this.bridgeDeviceHandler;
        if (bridgeDeviceHandler != null) {
            int reconnect = 0;
            while (!bridgeDeviceHandler.getThing().getStatus().equals(ThingStatus.ONLINE)) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {
                }
                if (reconnect == 10) {
                    logger.error("Bridge is offline during 10 seconds");
                    updateStatus(ThingStatus.UNKNOWN, ThingStatusDetail.BRIDGE_UNINITIALIZED,
                            "Bridge is offline during 10 seconds");
                    break;
                }
                reconnect++;
            }
            if (bridgeDeviceHandler.getThing().getStatus().equals(ThingStatus.ONLINE)) {
                boolean isR = false;
                boolean isG = false;
                boolean isB = false;
                MegaDHardware.Port r = bridgeDeviceHandler.megaDHardware.getPortStatus(configuration.red);
                MegaDHardware.Port g = bridgeDeviceHandler.megaDHardware.getPortStatus(configuration.green);
                MegaDHardware.Port b = bridgeDeviceHandler.megaDHardware.getPortStatus(configuration.blue);
                if (r != null) {
                    if (r.getM().equals(PWM)) {
                        isR = true;
                    }
                } else {
                    logger.error("Port {} is not PWM", configuration.red);
                }
                if (g != null) {
                    if (g.getM().equals(PWM)) {
                        isG = true;
                    }
                } else {
                    logger.error("Port {} is not PWM", configuration.green);
                }
                if (b != null) {
                    if (b.getM().equals(PWM)) {
                        isB = true;
                    }
                } else {
                    logger.error("Port {} is not PWM", configuration.blue);
                }
                if (isR && isG && isB) {
                    Map<Integer, Boolean> ep = MegaDDiscoveryService.excludePortList;
                    if (ep != null) {
                        ep.put(configuration.red, true);
                        ep.put(configuration.green, true);
                        ep.put(configuration.blue, true);

                    }
                    ScheduledFuture<?> refreshPollingJob = this.refreshPollingJob;
                    if (configuration.refresh != 0) {
                        logger.debug("Thing {}, refresh interval is {} sec", getThing().getUID(),
                                configuration.refresh);
                        if (refreshPollingJob == null || refreshPollingJob.isCancelled()) {
                            refreshPollingJob = scheduler.scheduleWithFixedDelay(this::refresh, 10,
                                    configuration.refresh, TimeUnit.SECONDS);
                            this.refreshPollingJob = refreshPollingJob;
                        }
                    }
                    updateStatus(ThingStatus.ONLINE);
                } else {
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Cannot define RGB ports");
                }
            }
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        MegaDHttpHelpers httpRequest = new MegaDHttpHelpers();
        if (command instanceof HSBType color) {
            String colorRed = color.format("%rgb%").split(",")[0];
            String colorGreen = color.format("%rgb%").split(",")[1];
            String colorBlue = color.format("%rgb%").split(",")[2];

            MegaDDeviceHandler bridgeDeviceHandler = this.bridgeDeviceHandler;
            if (bridgeDeviceHandler != null) {
                httpRequest.request(
                        "http://" + bridgeDeviceHandler.getThing().getConfiguration().get("hostname").toString() + "/"
                                + bridgeDeviceHandler.getThing().getConfiguration().get("password").toString()
                                + "/?cmd=" + configuration.red + ":" + colorRed);
                httpRequest.request(
                        "http://" + bridgeDeviceHandler.getThing().getConfiguration().get("hostname").toString() + "/"
                                + bridgeDeviceHandler.getThing().getConfiguration().get("password").toString()
                                + "/?cmd=" + configuration.green + ":" + colorGreen);
                httpRequest.request(
                        "http://" + bridgeDeviceHandler.getThing().getConfiguration().get("hostname").toString() + "/"
                                + bridgeDeviceHandler.getThing().getConfiguration().get("password").toString()
                                + "/?cmd=" + configuration.blue + ":" + colorBlue);
                this.colorRed = Integer.parseInt(colorRed);
                this.colorGreen = Integer.parseInt(colorGreen);
                this.colorBlue = Integer.parseInt(colorBlue);

            }
        } else if (command instanceof OnOffType colorSwitch) {
            MegaDDeviceHandler bridgeDeviceHandler = this.bridgeDeviceHandler;
            if (bridgeDeviceHandler != null) {
                if (colorSwitch == OnOffType.OFF) {
                    httpRequest.request(
                            "http://" + bridgeDeviceHandler.getThing().getConfiguration().get("hostname").toString()
                                    + "/" + bridgeDeviceHandler.getThing().getConfiguration().get("password").toString()
                                    + "/?cmd=" + configuration.red + ":" + 0);
                    httpRequest.request(
                            "http://" + bridgeDeviceHandler.getThing().getConfiguration().get("hostname").toString()
                                    + "/" + bridgeDeviceHandler.getThing().getConfiguration().get("password").toString()
                                    + "/?cmd=" + configuration.green + ":" + 0);
                    httpRequest.request(
                            "http://" + bridgeDeviceHandler.getThing().getConfiguration().get("hostname").toString()
                                    + "/" + bridgeDeviceHandler.getThing().getConfiguration().get("password").toString()
                                    + "/?cmd=" + configuration.blue + ":" + 0);
                } else if (colorSwitch == OnOffType.ON) {
                    httpRequest.request(
                            "http://" + bridgeDeviceHandler.getThing().getConfiguration().get("hostname").toString()
                                    + "/" + bridgeDeviceHandler.getThing().getConfiguration().get("password").toString()
                                    + "/?cmd=" + configuration.red + ":" + this.colorRed);
                    httpRequest.request(
                            "http://" + bridgeDeviceHandler.getThing().getConfiguration().get("hostname").toString()
                                    + "/" + bridgeDeviceHandler.getThing().getConfiguration().get("password").toString()
                                    + "/?cmd=" + configuration.green + ":" + this.colorGreen);
                    httpRequest.request(
                            "http://" + bridgeDeviceHandler.getThing().getConfiguration().get("hostname").toString()
                                    + "/" + bridgeDeviceHandler.getThing().getConfiguration().get("password").toString()
                                    + "/?cmd=" + configuration.blue + ":" + this.colorBlue);
                }

            }
        }
        logger.debug("get Command");
    }

    public void refresh() {
        MegaDHttpHelpers httpRequest = new MegaDHttpHelpers();
        MegaDDeviceHandler bridgeDeviceHandler = this.bridgeDeviceHandler;
        if ((bridgeDeviceHandler != null) && (bridgeDeviceHandler.getThing().getStatus().equals(ThingStatus.ONLINE))) {
            logger.debug("Refresh port {} at {}", configuration.port, thing.getLabel());
            for (Channel channel : getThing().getChannels()) {
                if (isLinked(channel.getUID().getId())) {
                    if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_RGB)) {
                        int red_color = 0;
                        int green_color = 0;
                        int blue_color = 0;
                        MegaDHTTPResponse red = httpRequest.request("http://"
                                + bridgeDeviceHandler.getThing().getConfiguration().get("hostname").toString() + "/"
                                + bridgeDeviceHandler.getThing().getConfiguration().get("password").toString() + "/?pt="
                                + configuration.red + "?cmd=get");
                        if (red.getResponseCode() == 200) {
                            red_color = Integer.parseInt(red.getResponseResult());
                        } else {
                            logger.error("Cannot get red channel value");
                        }
                        MegaDHTTPResponse green = httpRequest.request("http://"
                                + bridgeDeviceHandler.getThing().getConfiguration().get("hostname").toString() + "/"
                                + bridgeDeviceHandler.getThing().getConfiguration().get("password").toString() + "/?pt="
                                + configuration.green + "?cmd=get");
                        if (green.getResponseCode() == 200) {
                            green_color = Integer.parseInt(green.getResponseResult());
                        } else {
                            logger.error("Cannot get green channel value");
                        }
                        MegaDHTTPResponse blue = httpRequest.request("http://"
                                + bridgeDeviceHandler.getThing().getConfiguration().get("hostname").toString() + "/"
                                + bridgeDeviceHandler.getThing().getConfiguration().get("password").toString() + "/?pt="
                                + configuration.blue + "?cmd=get");
                        if (blue.getResponseCode() == 200) {
                            blue_color = Integer.parseInt(blue.getResponseResult());
                        } else {
                            logger.error("Cannot get blue channel value");
                        }
                        updateState(channel.getUID().getId(), HSBType.fromRGB(red_color, green_color, blue_color));
                    }
                }
            }
        }
    }

    private synchronized @Nullable MegaDDeviceHandler getBridgeHandler() {
        Bridge bridge = getBridge();
        if (bridge == null) {
            logger.error("Required bridge not defined for device.");
            return null;
        } else {
            return getBridgeHandler(bridge);
        }
    }

    private synchronized @Nullable MegaDDeviceHandler getBridgeHandler(Bridge bridge) {
        ThingHandler handler = bridge.getHandler();
        if (handler instanceof MegaDDeviceHandler) {
            return (MegaDDeviceHandler) handler;
        } else {
            logger.debug("No available bridge handler found yet. Bridge: {} .", bridge.getUID());
            return null;
        }
    }
}
