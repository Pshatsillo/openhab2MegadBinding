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

import static org.openhab.binding.megad.discovery.MegaDDiscoveryService.megaDI2CSensorsList;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.megad.MegaDBindingConstants;
import org.openhab.binding.megad.MegaDConfiguration;
import org.openhab.binding.megad.discovery.MegaDDiscoveryService;
import org.openhab.binding.megad.dto.MegaDI2CSensors;
import org.openhab.binding.megad.internal.MegaDDsenEnum;
import org.openhab.binding.megad.internal.MegaDExtendedType;
import org.openhab.binding.megad.internal.MegaDHTTPCallback;
import org.openhab.binding.megad.internal.MegaDHttpHelpers;
import org.openhab.binding.megad.internal.MegaDI2CSensorsEnum;
import org.openhab.binding.megad.internal.MegaDModesEnum;
import org.openhab.binding.megad.internal.MegaDTypesEnum;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.OpenClosedType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.openhab.core.thing.binding.builder.ThingBuilder;
import org.openhab.core.thing.type.ChannelKind;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link MegaDPortsHandler} is responsible for standart features of megsd
 *
 * @author Petr Shatsillo - Initial contribution
 */
@NonNullByDefault
public class MegaDPortsHandler extends BaseThingHandler {
    private int dimmervalue = 0;

    private Logger logger = LoggerFactory.getLogger(MegaDPortsHandler.class);

    private @Nullable ScheduledFuture<?> refreshPollingJob;
    @Nullable
    public MegaDDeviceHandler bridgeDeviceHandler;
    MegaDConfiguration configuration = getConfigAs(MegaDConfiguration.class);

    public MegaDPortsHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        int state = 0;
        String result = "";
        final MegaDDeviceHandler bridgeDeviceHandler = this.bridgeDeviceHandler;
        if (bridgeDeviceHandler != null) {
            if (channelUID.getId().equals(MegaDBindingConstants.CHANNEL_OUT)) {
                if (!command.toString().equals("REFRESH")) {
                    if (command.toString().equals("ON")) {
                        state = 1;
                    }
                    result = "http://" + bridgeDeviceHandler.getThing().getConfiguration().get("hostname").toString()
                            + "/" + bridgeDeviceHandler.getThing().getConfiguration().get("password").toString()
                            + "/?cmd=" + getThing().getConfiguration().get("port").toString() + ":" + state;
                    logger.debug("Switch: {}", result);
                    MegaDHttpHelpers httpRequest = new MegaDHttpHelpers();
                    int responseCode = httpRequest.request(result).getResponseCode();
                    if (responseCode != 200) {
                        logger.error("Send command at port {} error, check your mega {}", configuration.port,
                                bridgeDeviceHandler.config.hostname);
                    }
                }
            } else if (channelUID.getId().equals(MegaDBindingConstants.CHANNEL_DS2413)) {
                if (command.toString().equals("ON")) {
                    state = 1;
                }
                result = "http://" + bridgeDeviceHandler.getThing().getConfiguration().get("hostname").toString() + "/"
                        + bridgeDeviceHandler.getThing().getConfiguration().get("password").toString() + "/?cmd="
                        + getThing().getConfiguration().get("port").toString()
                        + getThing().getConfiguration().get("ds2413_ch") + ":" + state;
                logger.info("Switch: {}", result);
                MegaDHttpHelpers httpRequest = new MegaDHttpHelpers();
                int responseCode = httpRequest.request(result).getResponseCode();
                if (responseCode != 200) {
                    logger.error("Send command at port {} error, check your mega {}", configuration.port,
                            bridgeDeviceHandler.config.hostname);
                }
            } else if (channelUID.getId().equals(MegaDBindingConstants.CHANNEL_DIMMER)) {
                if (!command.toString().equals("REFRESH")) {
                    try {
                        int uivalue = Integer.parseInt(command.toString().split("[.]")[0]);
                        int resultInt = 0;
                        if (uivalue != 0) {
                            int minval = 0; // Integer.parseInt(getThing().getConfiguration().get("min_pwm").toString());
                            double getDiff = (255.0 - minval) / 100.0;
                            int corrVal = (int) Math.round(uivalue * getDiff);
                            resultInt = corrVal + minval;

                            if (uivalue == 1) {
                                if (minval != 0) {
                                    resultInt = minval;
                                } else {
                                    resultInt = uivalue;
                                }
                            } else if (resultInt != 0) {
                                dimmervalue = resultInt;
                            }
                        }
                        result = "http://"
                                + bridgeDeviceHandler.getThing().getConfiguration().get("hostname").toString() + "/"
                                + bridgeDeviceHandler.getThing().getConfiguration().get("password").toString()
                                + "/?cmd=" + getThing().getConfiguration().get("port").toString() + ":" + resultInt;
                        logger.info("Dimmer: {}", result);
                        MegaDHttpHelpers httpRequest = new MegaDHttpHelpers();
                        int responseCode = httpRequest.request(result).getResponseCode();
                        if (responseCode != 200) {
                            logger.error("Send command at port {} error, check your mega {}", configuration.port,
                                    bridgeDeviceHandler.config.hostname);
                        }
                    } catch (Exception e) {
                        if (command.toString().equals("OFF")) {
                            result = "http://"
                                    + bridgeDeviceHandler.getThing().getConfiguration().get("hostname").toString() + "/"
                                    + bridgeDeviceHandler.getThing().getConfiguration().get("password").toString()
                                    + "/?cmd=" + getThing().getConfiguration().get("port").toString() + ":0";
                            logger.info("Dimmer set to OFF");
                            MegaDHttpHelpers httpRequest = new MegaDHttpHelpers();
                            int responseCode = httpRequest.request(result).getResponseCode();
                            if (responseCode != 200) {
                                logger.error("Send command at port {} error, check your mega {}", configuration.port,
                                        bridgeDeviceHandler.config.hostname);
                            }
                            updateState(channelUID.getId(), PercentType.valueOf("0"));
                        } else if (command.toString().equals("ON")) {
                            result = "http://"
                                    + bridgeDeviceHandler.getThing().getConfiguration().get("hostname").toString() + "/"
                                    + bridgeDeviceHandler.getThing().getConfiguration().get("password").toString()
                                    + "/?cmd=" + getThing().getConfiguration().get("port").toString() + ":"
                                    + dimmervalue;
                            logger.info("Dimmer restored to previous value: {}", result);
                            MegaDHttpHelpers httpRequest = new MegaDHttpHelpers();
                            int responseCode = httpRequest.request(result).getResponseCode();
                            if (responseCode != 200) {
                                logger.error("Send command at port {} error, check your mega {}", configuration.port,
                                        bridgeDeviceHandler.config.hostname);
                            }
                            int percent = 0;
                            try {
                                percent = (int) Math.round(dimmervalue / 2.55);
                            } catch (Exception ignored) {
                            }
                            updateState(channelUID.getId(), PercentType.valueOf(Integer.toString(percent)));
                        } else {
                            logger.debug("Illegal dimmer value: {}", result);
                        }
                    }
                }
            } else if (channelUID.getId().equals(MegaDBindingConstants.CHANNEL_PWM)) {
                if (!command.toString().equals("REFRESH")) {
                    try {
                        int uivalue = Integer.parseInt(command.toString().split("[.]")[0]);
                        if (uivalue != 0) {
                            dimmervalue = uivalue;
                        }
                        result = "http://"
                                + bridgeDeviceHandler.getThing().getConfiguration().get("hostname").toString() + "/"
                                + bridgeDeviceHandler.getThing().getConfiguration().get("password").toString()
                                + "/?cmd=" + getThing().getConfiguration().get("port").toString() + ":" + uivalue;
                        logger.info("PWM: {}", result);
                        MegaDHttpHelpers httpRequest = new MegaDHttpHelpers();
                        int responseCode = httpRequest.request(result).getResponseCode();
                        if (responseCode != 200) {
                            logger.error("Send command at port {} error, check your mega {}", configuration.port,
                                    bridgeDeviceHandler.config.hostname);
                        }
                    } catch (Exception e) {
                        result = "http://"
                                + bridgeDeviceHandler.getThing().getConfiguration().get("hostname").toString() + "/"
                                + bridgeDeviceHandler.getThing().getConfiguration().get("password").toString()
                                + "/?cmd=" + getThing().getConfiguration().get("port").toString() + ":" + dimmervalue;
                        logger.info("PWM restored to previous value: {}", result);
                        MegaDHttpHelpers httpRequest = new MegaDHttpHelpers();
                        int responseCode = httpRequest.request(result).getResponseCode();
                        if (responseCode != 200) {
                            logger.error("Send command at port {} error, check your mega {}", configuration.port,
                                    bridgeDeviceHandler.config.hostname);
                        }
                        updateState(channelUID.getId(), DecimalType.valueOf(Integer.toString(dimmervalue)));
                    }
                }
            } else {
                MegaDTypesEnum portType = bridgeDeviceHandler.megaDHardware.getPortsType(configuration.port);
                if (portType != null) {
                    if (portType.equals(MegaDTypesEnum.I2C)) {
                        MegaDI2CSensorsEnum megaDI2CSensorsEnum = bridgeDeviceHandler.megaDHardware
                                .getDI2cType(configuration.port);
                        if (megaDI2CSensorsEnum != null) {
                            if (megaDI2CSensorsEnum.equals(MegaDI2CSensorsEnum.MCP230XX)) {
                                BigDecimal port = (BigDecimal) Objects.requireNonNull(thing.getChannel(channelUID))
                                        .getConfiguration().get("port");
                                BigDecimal thingPort = (BigDecimal) thing.getConfiguration().get("port");
                                String cmd = "";
                                if (command.equals(OnOffType.ON)) {
                                    cmd = "1";
                                } else if (command.equals(OnOffType.OFF)) {
                                    cmd = "0";
                                }
                                String request = "http://" + bridgeDeviceHandler.config.hostname + "/"
                                        + bridgeDeviceHandler.config.password + "/?cmd=" + thingPort + "e" + port + ":"
                                        + cmd;
                                MegaDHttpHelpers httpRequest = new MegaDHttpHelpers();
                                int responseCode = httpRequest.request(request).getResponseCode();
                                if (responseCode != 200) {
                                    logger.error("Send command at port {} error, check your mega {}",
                                            configuration.port, bridgeDeviceHandler.config.hostname);
                                }
                                logger.debug("MCP230XX request to mega: {}", request);
                            }
                            if (megaDI2CSensorsEnum.equals(MegaDI2CSensorsEnum.PCA9685)) {
                                BigDecimal port = (BigDecimal) Objects.requireNonNull(thing.getChannel(channelUID))
                                        .getConfiguration().get("port");
                                BigDecimal thingPort = (BigDecimal) thing.getConfiguration().get("port");
                                String cmd = "";
                                if (command.equals(OnOffType.ON)) {
                                    cmd = "1";
                                } else if (command.equals(OnOffType.OFF)) {
                                    cmd = "0";
                                } else {
                                    if (Objects.requireNonNull(thing.getChannel(channelUID)).getConfiguration()
                                            .get("type") != null) {
                                        String channelType = Objects.requireNonNull(thing.getChannel(channelUID))
                                                .getConfiguration().get("type").toString();
                                        if ("PWM".equals(channelType)) {
                                            cmd = command.toString();
                                        } else if ("DIMMER".equals(channelType)) {
                                            int value = Integer.parseInt(command.toString());
                                            cmd = String.valueOf(Math.round(value * 40.95));
                                        }
                                    } else {
                                        int value = Integer.parseInt(command.toString());
                                        cmd = String.valueOf(Math.round(value * 40.95));
                                    }
                                }
                                String request = "http://" + bridgeDeviceHandler.config.hostname + "/"
                                        + bridgeDeviceHandler.config.password + "/?cmd=" + thingPort + "e" + port + ":"
                                        + cmd;
                                MegaDHttpHelpers httpRequest = new MegaDHttpHelpers();
                                int responseCode = httpRequest.request(request).getResponseCode();
                                if (responseCode != 200) {
                                    logger.error("Send command at port {} error, check your mega {}",
                                            configuration.port, bridgeDeviceHandler.config.hostname);
                                }
                                logger.debug("PCA9685 request to mega: {}", request);
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    protected ThingBuilder editThing() {
        return super.editThing();
    }

    @Override
    public void thingUpdated(Thing thing) {
        super.thingUpdated(thing);
    }

    @Override
    public void initialize() {
        MegaDDiscoveryService.readSensorsFile();
        Map<String, String> properties = new HashMap<>();
        List<Channel> channelList = new ArrayList<>();
        configuration = getConfigAs(MegaDConfiguration.class);
        bridgeDeviceHandler = getBridgeHandler();
        if (bridgeDeviceHandler != null) {
            MegaDHTTPCallback.portListener.add(this);
            ScheduledFuture<?> refreshPollingJob = this.refreshPollingJob;
            if (configuration.refresh != 0) {
                logger.debug("Thing {}, refresh interval is {} sec", getThing().getUID(), configuration.refresh);
                if (refreshPollingJob == null || refreshPollingJob.isCancelled()) {
                    refreshPollingJob = scheduler.scheduleWithFixedDelay(this::refresh, 10, configuration.refresh,
                            TimeUnit.SECONDS);
                    this.refreshPollingJob = refreshPollingJob;
                }
            }
            final MegaDDeviceHandler bridgeDeviceHandler = this.bridgeDeviceHandler;
            if (bridgeDeviceHandler != null) {
                MegaDTypesEnum portType = MegaDTypesEnum.NC;
                MegaDTypesEnum discoverPort;
                String response = getType(bridgeDeviceHandler);
                String[] respSplit = response.split("<option");
                for (String type : respSplit) {
                    if (type.contains("selected")) {
                        if (!type.contains("value=255")) {
                            if (type.toUpperCase(Locale.ROOT).contains("IN")) {
                                bridgeDeviceHandler.megaDHardware.setPortType(configuration.port, MegaDTypesEnum.IN);
                                logger.info("port {} type is {}", configuration.port, MegaDTypesEnum.IN);
                            } else if (type.toUpperCase(Locale.ROOT).contains("OUT")) {
                                bridgeDeviceHandler.megaDHardware.setPortType(configuration.port, MegaDTypesEnum.OUT);
                                logger.info("port {} type is {}", configuration.port, MegaDTypesEnum.OUT);
                            } else if (type.toUpperCase(Locale.ROOT).contains("DSen".toUpperCase())) {
                                bridgeDeviceHandler.megaDHardware.setPortType(configuration.port, MegaDTypesEnum.DSEN);
                                logger.info("port {} type is {}", configuration.port, MegaDTypesEnum.DSEN);
                            } else if (type.toUpperCase(Locale.ROOT).contains("I2C")) {
                                bridgeDeviceHandler.megaDHardware.setPortType(configuration.port, MegaDTypesEnum.I2C);
                                logger.info("port {} type is {}", configuration.port, MegaDTypesEnum.I2C);
                            } else if (type.toUpperCase(Locale.ROOT).contains("ADC")) {
                                bridgeDeviceHandler.megaDHardware.setPortType(configuration.port, MegaDTypesEnum.ADC);
                                logger.info("port {} type is {}", configuration.port, MegaDTypesEnum.ADC);
                            }
                        } else {
                            bridgeDeviceHandler.megaDHardware.setPortType(configuration.port, MegaDTypesEnum.NC);
                        }
                    }
                }
                logger.debug("response port state {}", response);
                discoverPort = bridgeDeviceHandler.megaDHardware.getPortsType(configuration.port);
                if (discoverPort != null) {
                    portType = discoverPort;
                }

                switch (portType) {
                    case IN -> {
                        response = readLabel("http://" + bridgeDeviceHandler.config.hostname + "/"
                                + bridgeDeviceHandler.config.password + "/?pt=" + configuration.port);
                        String label = response.substring(
                                response.indexOf("name=emt size=25 value=") + "name=emt size=25 value=".length(),
                                response.indexOf("><br><input type=submit")).replace("\"", "");

                        int selectMbegin = response.indexOf("<select name=m>") + "<select name=m>".length();
                        int selectMend = response
                                .substring(response.indexOf("<select name=m>") + "<select name=m>".length())
                                .indexOf("</select>") + selectMbegin;
                        String modeResp = response.substring(selectMbegin, selectMend);
                        String[] modeList = modeResp.split("<option");
                        for (String mode : modeList) {
                            if (mode.contains("selected")) {
                                if (mode.toUpperCase(Locale.ROOT).contains(">C")) {
                                    ChannelUID clickUID = new ChannelUID(thing.getUID(),
                                            MegaDBindingConstants.CHANNEL_CLICK);
                                    Channel click = ChannelBuilder.create(clickUID)
                                            .withType(new ChannelTypeUID(MegaDBindingConstants.BINDING_ID,
                                                    MegaDBindingConstants.CHANNEL_CLICK))
                                            .withLabel(label + " Click trigger").withKind(ChannelKind.TRIGGER)
                                            .withAcceptedItemType("String").build();
                                    channelList.add(click);
                                    ChannelUID inCountUID = new ChannelUID(thing.getUID(),
                                            MegaDBindingConstants.CHANNEL_INCOUNT);
                                    Channel inCount = ChannelBuilder.create(inCountUID)
                                            .withType(new ChannelTypeUID(MegaDBindingConstants.BINDING_ID,
                                                    MegaDBindingConstants.CHANNEL_INCOUNT))
                                            .withLabel(label + " Count").withAcceptedItemType("Number").build();
                                    channelList.add(inCount);
                                    properties.put("Mode:", "CLICK");
                                } else {
                                    ChannelUID inUID = new ChannelUID(thing.getUID(), MegaDBindingConstants.CHANNEL_IN);
                                    Channel in = ChannelBuilder.create(inUID)
                                            .withType(new ChannelTypeUID(MegaDBindingConstants.BINDING_ID,
                                                    MegaDBindingConstants.CHANNEL_IN))
                                            .withLabel(label + " Input").withAcceptedItemType("Switch").build();
                                    channelList.add(in);
                                    ChannelUID lpUID = new ChannelUID(thing.getUID(),
                                            MegaDBindingConstants.CHANNEL_LONGPRESS);
                                    Channel lp = ChannelBuilder.create(lpUID)
                                            .withType(new ChannelTypeUID(MegaDBindingConstants.BINDING_ID,
                                                    MegaDBindingConstants.CHANNEL_LONGPRESS))
                                            .withLabel(label + " Long press Trigger").withKind(ChannelKind.TRIGGER)
                                            .withAcceptedItemType("String").build();
                                    channelList.add(lp);
                                    ChannelUID inCountUID = new ChannelUID(thing.getUID(),
                                            MegaDBindingConstants.CHANNEL_INCOUNT);
                                    Channel inCount = ChannelBuilder.create(inCountUID)
                                            .withType(new ChannelTypeUID(MegaDBindingConstants.BINDING_ID,
                                                    MegaDBindingConstants.CHANNEL_INCOUNT))
                                            .withLabel(label + " Counter").withAcceptedItemType("Number").build();
                                    channelList.add(inCount);
                                    ChannelUID inContactUID = new ChannelUID(thing.getUID(),
                                            MegaDBindingConstants.CHANNEL_CONTACT);
                                    Channel inContact = ChannelBuilder.create(inContactUID)
                                            .withType(new ChannelTypeUID(MegaDBindingConstants.BINDING_ID,
                                                    MegaDBindingConstants.CHANNEL_CONTACT))
                                            .withLabel(label + " Contact").withAcceptedItemType("Contact").build();
                                    channelList.add(inContact);
                                    properties.put("Mode:", "P, R, P&R (checkbox enabled)");
                                }
                            }
                        }

                        String misc = response.substring(
                                response.indexOf("type=checkbox name=misc value=1")
                                        + "type=checkbox name=misc value=1".length(),
                                response.indexOf("type=checkbox name=misc value=1")
                                        + "type=checkbox name=misc value=1".length() + 13);
                        if (!misc.contains("checked")) {
                            logger.debug("Set mode checkbox at port {}", configuration.port);
                            MegaDHttpHelpers httpRequest = new MegaDHttpHelpers();
                            int checkbox = httpRequest.request("http://" + bridgeDeviceHandler.config.hostname + "/"
                                    + bridgeDeviceHandler.config.password + "/?pt=" + configuration.port + "&misc=1")
                                    .getResponseCode();
                            if (checkbox != 200) {
                                logger.error("Set mode checkbox at port {} error, check your mega {}",
                                        configuration.port, bridgeDeviceHandler.config.hostname);
                            }
                        }
                    }
                    case OUT -> {
                        response = readLabel("http://" + bridgeDeviceHandler.config.hostname + "/"
                                + bridgeDeviceHandler.config.password + "/?pt=" + configuration.port);
                        int labelStartIndex = response.indexOf("name=emt size=25 value=")
                                + "name=emt size=25 value=".length();
                        int labelEndIndex = response.substring(labelStartIndex).indexOf("><br>") + labelStartIndex;
                        String label = response.substring(labelStartIndex, labelEndIndex).replace("\"", "");
                        int selectMbegin = response.indexOf("<select name=m>") + "<select name=m>".length();
                        int selectMend = response
                                .substring(response.indexOf("<select name=m>") + "<select name=m>".length())
                                .indexOf("</select>") + selectMbegin;
                        String modeResp = response.substring(selectMbegin, selectMend);
                        String[] modeList = modeResp.split("<option");
                        for (String mode : modeList) {
                            if (mode.contains("selected")) {
                                if (mode.toUpperCase(Locale.ROOT).contains("SW")) {
                                    ChannelUID outUID = new ChannelUID(thing.getUID(),
                                            MegaDBindingConstants.CHANNEL_OUT);
                                    Channel out = ChannelBuilder.create(outUID)
                                            .withType(new ChannelTypeUID(MegaDBindingConstants.BINDING_ID,
                                                    MegaDBindingConstants.CHANNEL_OUT))
                                            .withLabel(label + " Output").build();
                                    channelList.add(out);
                                    if (mode.toUpperCase(Locale.ROOT).contains("SW LINK")) {
                                        bridgeDeviceHandler.megaDHardware.setMode(configuration.port,
                                                MegaDModesEnum.SWLINK);
                                    } else {
                                        bridgeDeviceHandler.megaDHardware.setMode(configuration.port,
                                                MegaDModesEnum.SW);
                                    }
                                } else if (mode.toUpperCase(Locale.ROOT).contains("PWM")) {
                                    ChannelUID pwmUID = new ChannelUID(thing.getUID(),
                                            MegaDBindingConstants.CHANNEL_PWM);
                                    Channel pwm = ChannelBuilder.create(pwmUID)
                                            .withType(new ChannelTypeUID(MegaDBindingConstants.BINDING_ID,
                                                    MegaDBindingConstants.CHANNEL_PWM))
                                            .withLabel(label + " PWM").withAcceptedItemType("Number").build();
                                    channelList.add(pwm);
                                    ChannelUID dimmerUID = new ChannelUID(thing.getUID(),
                                            MegaDBindingConstants.CHANNEL_DIMMER);
                                    Channel dimmer = ChannelBuilder.create(dimmerUID)
                                            .withType(new ChannelTypeUID(MegaDBindingConstants.BINDING_ID,
                                                    MegaDBindingConstants.CHANNEL_DIMMER))
                                            .withLabel(label + " Dimmer").withAcceptedItemType("Dimmer").build();
                                    channelList.add(dimmer);
                                    bridgeDeviceHandler.megaDHardware.setMode(configuration.port, MegaDModesEnum.PWM);
                                } else if (mode.toUpperCase(Locale.ROOT).contains("DS2413")) {
                                    ChannelUID ds2413aUID = new ChannelUID(thing.getUID(),
                                            MegaDBindingConstants.CHANNEL_DS2413_A);
                                    Channel ds2413a = ChannelBuilder.create(ds2413aUID)
                                            .withType(new ChannelTypeUID(MegaDBindingConstants.BINDING_ID,
                                                    MegaDBindingConstants.CHANNEL_DS2413))
                                            .withLabel(label + " DS2413 PORT A").withAcceptedItemType("Switch").build();
                                    channelList.add(ds2413a);
                                    ChannelUID ds2413bUID = new ChannelUID(thing.getUID(),
                                            MegaDBindingConstants.CHANNEL_DS2413_B);
                                    Channel ds2413b = ChannelBuilder.create(ds2413bUID)
                                            .withType(new ChannelTypeUID(MegaDBindingConstants.BINDING_ID,
                                                    MegaDBindingConstants.CHANNEL_DS2413))
                                            .withLabel(label + " DS2413 PORT B").withAcceptedItemType("Switch").build();
                                    channelList.add(ds2413b);
                                    bridgeDeviceHandler.megaDHardware.setMode(configuration.port,
                                            MegaDModesEnum.DS2413);
                                }
                            }
                        }
                        properties.put("Mode:",
                                Objects.requireNonNull(bridgeDeviceHandler.megaDHardware.getMode(configuration.port))
                                        .toString());
                    }
                    case ADC -> {
                        response = readLabel("http://" + bridgeDeviceHandler.config.hostname + "/"
                                + bridgeDeviceHandler.config.password + "/?pt=" + configuration.port);
                        int labelStartIndex = response.indexOf("name=emt size=25 value=")
                                + "name=emt size=25 value=".length();
                        int labelEndIndex = response.substring(labelStartIndex).indexOf("><br>") + labelStartIndex;
                        String label = response.substring(labelStartIndex, labelEndIndex).replace("\"", "");
                        ChannelUID adcUID = new ChannelUID(thing.getUID(), MegaDBindingConstants.CHANNEL_ADC);
                        Channel adc = ChannelBuilder.create(adcUID).withType(
                                new ChannelTypeUID(MegaDBindingConstants.BINDING_ID, MegaDBindingConstants.CHANNEL_ADC))
                                .withLabel(label + " ADC").build();
                        channelList.add(adc);
                    }
                    case DSEN -> {
                        response = readLabel("http://" + bridgeDeviceHandler.config.hostname + "/"
                                + bridgeDeviceHandler.config.password + "/?pt=" + configuration.port);
                        int labelStartIndex = response.indexOf("name=emt size=25 value=")
                                + "name=emt size=25 value=".length();
                        int labelEndIndex = response.substring(labelStartIndex).indexOf("><br>") + labelStartIndex;
                        String label = response.substring(labelStartIndex, labelEndIndex).replace("\"", "");

                        int dStartIndex = response.indexOf("<select name=d>") + "<select name=d>".length();
                        int dEndIndex = response.substring(dStartIndex).indexOf("</select>") + dStartIndex;
                        String d = response.substring(dStartIndex, dEndIndex);

                        String[] sensorList = d.split("<option");
                        for (String sensor : sensorList) {
                            if (sensor.contains("selected")) {
                                if (sensor.toUpperCase(Locale.ROOT).contains("DHT11")) {
                                    ChannelUID dhtTempUID = new ChannelUID(thing.getUID(),
                                            MegaDBindingConstants.CHANNEL_TEMP);
                                    Channel dhtTemp = ChannelBuilder.create(dhtTempUID)
                                            .withType(new ChannelTypeUID(MegaDBindingConstants.BINDING_ID,
                                                    MegaDBindingConstants.CHANNEL_TEMP))
                                            .withLabel(label + " DHT 11 Temperature")
                                            .withAcceptedItemType("Number:Temperature").build();
                                    channelList.add(dhtTemp);
                                    ChannelUID dhtHumUID = new ChannelUID(thing.getUID(),
                                            MegaDBindingConstants.CHANNEL_HUM);
                                    Channel dhtHum = ChannelBuilder.create(dhtHumUID)
                                            .withType(new ChannelTypeUID(MegaDBindingConstants.BINDING_ID,
                                                    MegaDBindingConstants.CHANNEL_HUM))
                                            .withLabel(label + " DHT 11 Humidity")
                                            .withAcceptedItemType("Number:Dimensionless").build();
                                    channelList.add(dhtHum);
                                    bridgeDeviceHandler.megaDHardware.setDSensorType(configuration.port,
                                            MegaDDsenEnum.DHT11);
                                } else if (sensor.toUpperCase(Locale.ROOT).contains("DHT22")) {
                                    ChannelUID dhtTempUID = new ChannelUID(thing.getUID(),
                                            MegaDBindingConstants.CHANNEL_TEMP);
                                    Channel dhtTemp = ChannelBuilder.create(dhtTempUID)
                                            .withType(new ChannelTypeUID(MegaDBindingConstants.BINDING_ID,
                                                    MegaDBindingConstants.CHANNEL_TEMP))
                                            .withLabel(label + " DHT 22 Temperature")
                                            .withAcceptedItemType("Number:Temperature").build();
                                    channelList.add(dhtTemp);
                                    ChannelUID dhtHumUID = new ChannelUID(thing.getUID(),
                                            MegaDBindingConstants.CHANNEL_HUM);
                                    Channel dhtHum = ChannelBuilder.create(dhtHumUID)
                                            .withType(new ChannelTypeUID(MegaDBindingConstants.BINDING_ID,
                                                    MegaDBindingConstants.CHANNEL_HUM))
                                            .withLabel(label + " DHT 22 Humidity")
                                            .withAcceptedItemType("Number:Dimensionless").build();
                                    channelList.add(dhtHum);
                                    bridgeDeviceHandler.megaDHardware.setDSensorType(configuration.port,
                                            MegaDDsenEnum.DHT22);
                                } else if (sensor.toUpperCase(Locale.ROOT).contains("1WBUS")) {
                                    MegaDHttpHelpers httpRequest = new MegaDHttpHelpers();
                                    response = httpRequest.request("http://" + bridgeDeviceHandler.config.hostname + "/"
                                            + bridgeDeviceHandler.config.password + "/?pt=" + configuration.port
                                            + "&cmd=list").getResponseResult();
                                    String[] sensorsList = response.split(";");
                                    if (!"busy".equals(response)) {
                                        for (String onewireSensor : sensorsList) {
                                            if (!onewireSensor.isEmpty()) {
                                                String address = onewireSensor.split(":")[0];
                                                ChannelUID onewireTempUID = new ChannelUID(thing.getUID(),
                                                        MegaDBindingConstants.CHANNEL_1WTEMP + "_" + address);
                                                Configuration configuration = new Configuration();
                                                configuration.put("address", address);
                                                Channel onewireTemp = ChannelBuilder.create(onewireTempUID)
                                                        .withType(new ChannelTypeUID(MegaDBindingConstants.BINDING_ID,
                                                                MegaDBindingConstants.CHANNEL_1WTEMP))
                                                        .withLabel(label + " " + address + " Temperature")
                                                        .withAcceptedItemType("Number:Temperature")
                                                        .withConfiguration(configuration).build();
                                                channelList.add(onewireTemp);
                                            }
                                        }
                                    }
                                    bridgeDeviceHandler.megaDHardware.setDSensorType(configuration.port,
                                            MegaDDsenEnum.ONEWIREBUS);
                                } else if (sensor.toUpperCase(Locale.ROOT).contains("1W")) { // 1W
                                    ChannelUID onewireUID = new ChannelUID(thing.getUID(),
                                            MegaDBindingConstants.CHANNEL_TEMP);
                                    Channel onewire = ChannelBuilder.create(onewireUID)
                                            .withType(new ChannelTypeUID(MegaDBindingConstants.BINDING_ID,
                                                    MegaDBindingConstants.CHANNEL_TEMP))
                                            .withLabel(label + " 1w sensor").withAcceptedItemType("Number:Temperature")
                                            .build();
                                    channelList.add(onewire);
                                    ChannelUID directionUID = new ChannelUID(thing.getUID(),
                                            MegaDBindingConstants.CHANNEL_DIRECTION);
                                    Channel direction = ChannelBuilder.create(directionUID)
                                            .withType(new ChannelTypeUID(MegaDBindingConstants.BINDING_ID,
                                                    MegaDBindingConstants.CHANNEL_DIRECTION))
                                            .withLabel(label + " direction Trigger").withKind(ChannelKind.TRIGGER)
                                            .withAcceptedItemType("String").build();
                                    channelList.add(direction);
                                    bridgeDeviceHandler.megaDHardware.setDSensorType(configuration.port,
                                            MegaDDsenEnum.ONEWIRE);
                                } else if (sensor.toUpperCase(Locale.ROOT).contains("IB")) {
                                    bridgeDeviceHandler.megaDHardware.setDSensorType(configuration.port,
                                            MegaDDsenEnum.IB);
                                } else if (sensor.toUpperCase(Locale.ROOT).contains("W26")) {
                                    bridgeDeviceHandler.megaDHardware.setDSensorType(configuration.port,
                                            MegaDDsenEnum.W26);
                                }
                            }
                        }
                        if (Objects.requireNonNull(bridgeDeviceHandler).megaDHardware
                                .getDSensorType(configuration.port) != null) {
                            properties.put("Sensor:",
                                    Objects.requireNonNull(Objects.requireNonNull(bridgeDeviceHandler).megaDHardware
                                            .getDSensorType(configuration.port)).toString());
                        }
                    }
                    case I2C -> {
                        try {
                            response = readLabel("http://" + bridgeDeviceHandler.config.hostname + "/"
                                    + bridgeDeviceHandler.config.password + "/?pt=" + configuration.port);
                            String label = response.substring(
                                    response.indexOf("name=emt size=25 value=") + "name=emt size=25 value=".length(),
                                    response.indexOf("><br><input type=submit")).replace("\"", "");
                            int scl = Integer.parseInt(response.substring(
                                    response.indexOf("<input name=misc size=3 value=")
                                            + "<input name=misc size=3 value=".length(),
                                    response.substring(response.indexOf("<input name=misc size=3 value=")
                                            + "<input name=misc size=3 value=".length()).indexOf(">")
                                            + response.indexOf("<input name=misc size=3 value=")
                                            + "<input name=misc size=3 value=".length()));
                            bridgeDeviceHandler.megaDHardware.setScl(configuration.port, scl);
                            String d = response.substring(response.indexOf("name=d>") + "name=d>".length(),
                                    response.indexOf("name=d>") + "name=gr>".length()
                                            + response.substring(response.indexOf("name=d>") + "name=d>".length())
                                                    .indexOf("</select>"));
                            String[] sensorList = d.split("<option");
                            for (String sensor : sensorList) {
                                if (sensor.contains("selected")) {
                                    if (sensor.toUpperCase(Locale.ROOT).contains("MCP230XX")) {
                                        int in = Integer.parseInt(response.substring(
                                                response.indexOf("<input name=inta size=3 value=")
                                                        + "<input name=inta size=3 value=".length(),
                                                response.substring(response.indexOf("<input name=inta size=3 value=")
                                                        + "<input name=inta size=3 value=".length()).indexOf(">")
                                                        + response.indexOf("<input name=inta size=3 value=")
                                                        + "<input name=inta size=3 value=".length()));
                                        bridgeDeviceHandler.megaDHardware.setInt(configuration.port, in);
                                        for (int i = 0; i < 16; i++) {
                                            MegaDHttpHelpers httpRequest = new MegaDHttpHelpers();
                                            response = httpRequest
                                                    .request("http://" + bridgeDeviceHandler.config.hostname + "/"
                                                            + bridgeDeviceHandler.config.password + "/?pt="
                                                            + configuration.port + "&ext=" + i)
                                                    .getResponseResult();
                                            int dStartIndex = response.indexOf("<select name=ety>")
                                                    + "<select name=ety>".length();
                                            int dEndIndex = response.substring(dStartIndex).indexOf("</select>")
                                                    + dStartIndex;
                                            String ety = response.substring(dStartIndex, dEndIndex);
                                            String[] etyList = ety.split("<option");
                                            for (String etyOption : etyList) {
                                                if (etyOption.contains("selected")) {
                                                    if (etyOption.toUpperCase(Locale.ROOT).contains("IN")) {
                                                        Configuration channelConfiguration = new Configuration();
                                                        channelConfiguration.put("port", i);
                                                        ChannelUID extInUID = new ChannelUID(thing.getUID(),
                                                                MegaDBindingConstants.CHANNEL_EXTENDER_IN + "_" + i);
                                                        Channel extIn = ChannelBuilder.create(extInUID)
                                                                .withType(new ChannelTypeUID(
                                                                        MegaDBindingConstants.BINDING_ID,
                                                                        MegaDBindingConstants.CHANNEL_EXTENDER_IN))
                                                                .withLabel(label + "extender port " + i + " in")
                                                                .withAcceptedItemType("Switch")
                                                                .withConfiguration(channelConfiguration).build();
                                                        channelList.add(extIn);

                                                        bridgeDeviceHandler.megaDHardware.setEtyType(configuration.port,
                                                                MegaDExtendedType.IN);

                                                    } else if (etyOption.toUpperCase(Locale.ROOT).contains("OUT")) {
                                                        Configuration channelConfiguration = new Configuration();
                                                        channelConfiguration.put("port", i);
                                                        ChannelUID extInUID = new ChannelUID(thing.getUID(),
                                                                MegaDBindingConstants.CHANNEL_EXTENDER_OUT + "_" + i);
                                                        Channel extIn = ChannelBuilder.create(extInUID)
                                                                .withType(new ChannelTypeUID(
                                                                        MegaDBindingConstants.BINDING_ID,
                                                                        MegaDBindingConstants.CHANNEL_EXTENDER_OUT))
                                                                .withLabel(label + "extender port " + i + " out")
                                                                .withAcceptedItemType("Switch")
                                                                .withConfiguration(channelConfiguration).build();
                                                        channelList.add(extIn);
                                                        bridgeDeviceHandler.megaDHardware.setEtyType(configuration.port,
                                                                MegaDExtendedType.OUT);
                                                    }
                                                }
                                            }
                                            properties.put("Mode:", "Extender MCP230XX");
                                        }
                                        bridgeDeviceHandler.megaDHardware.setDI2CType(configuration.port,
                                                MegaDI2CSensorsEnum.MCP230XX);
                                    } else if (sensor.toUpperCase(Locale.ROOT).contains("PCA9685")) {
                                        List<Channel> existingChannelList = new LinkedList<>(thing.getChannels());
                                        for (int i = 0; i < 16; i++) {
                                            MegaDHttpHelpers httpRequest = new MegaDHttpHelpers();
                                            response = httpRequest
                                                    .request("http://" + bridgeDeviceHandler.config.hostname + "/"
                                                            + bridgeDeviceHandler.config.password + "/?pt="
                                                            + configuration.port + "&ext=" + i)
                                                    .getResponseResult();
                                            int dStartIndex = response.indexOf("<select name=ety>")
                                                    + "<select name=ety>".length();
                                            int dEndIndex = response.substring(dStartIndex).indexOf("</select>")
                                                    + dStartIndex;
                                            String ety = response.substring(dStartIndex, dEndIndex);
                                            String[] etyList = ety.split("<option");
                                            for (String etyOption : etyList) {
                                                if (etyOption.contains("selected")) {
                                                    if (etyOption.toUpperCase(Locale.ROOT).contains("SW")) {
                                                        Configuration channelConfiguration = new Configuration();
                                                        channelConfiguration.put("port", i);
                                                        ChannelUID extInUID = new ChannelUID(thing.getUID(),
                                                                MegaDBindingConstants.CHANNEL_EXTENDER_OUT + "_" + i);
                                                        Channel extIn = ChannelBuilder.create(extInUID)
                                                                .withType(new ChannelTypeUID(
                                                                        MegaDBindingConstants.BINDING_ID,
                                                                        MegaDBindingConstants.CHANNEL_EXTENDER_OUT))
                                                                .withLabel(label + "extender port " + i + " out")
                                                                .withAcceptedItemType("Switch")
                                                                .withConfiguration(channelConfiguration).build();
                                                        // channelList.add(extIn);
                                                        if (existingChannelList.stream()
                                                                .anyMatch(cn -> cn.getUID().equals(extIn.getUID()))) {
                                                            Channel foundedChannel = existingChannelList.stream()
                                                                    .filter(cn -> cn.getUID().equals(extIn.getUID()))
                                                                    .findFirst().get();
                                                            channelList.add(foundedChannel);
                                                            existingChannelList.remove(foundedChannel);
                                                        } else {
                                                            channelList.add(extIn);
                                                        }
                                                        bridgeDeviceHandler.megaDHardware.setEtyType(configuration.port,
                                                                MegaDExtendedType.OUT);
                                                    } else if (etyOption.toUpperCase(Locale.ROOT).contains("PWM")) {
                                                        Configuration channelConfiguration = new Configuration();
                                                        channelConfiguration.put("port", i);
                                                        ChannelUID extPwmUID = new ChannelUID(thing.getUID(),
                                                                MegaDBindingConstants.CHANNEL_EXTENDER_PWM + "_" + i);
                                                        Channel extPwm = ChannelBuilder.create(extPwmUID)
                                                                .withType(new ChannelTypeUID(
                                                                        MegaDBindingConstants.BINDING_ID,
                                                                        MegaDBindingConstants.CHANNEL_EXTENDER_PWM))
                                                                .withLabel(label + "extender port " + i + " PWM")
                                                                .withAcceptedItemType("Number")
                                                                .withConfiguration(channelConfiguration).build();
                                                        if (existingChannelList.stream()
                                                                .anyMatch(cn -> cn.getUID().equals(extPwm.getUID()))) {
                                                            Channel foundedChannel = existingChannelList.stream()
                                                                    .filter(cn -> cn.getUID().equals(extPwm.getUID()))
                                                                    .findFirst().get();
                                                            channelList.add(foundedChannel);
                                                            existingChannelList.remove(foundedChannel);
                                                        } else {
                                                            channelList.add(extPwm);
                                                        }
                                                        bridgeDeviceHandler.megaDHardware.setEtyType(configuration.port,
                                                                MegaDExtendedType.PWM);
                                                    }
                                                }
                                            }
                                        }
                                        channelList.addAll(existingChannelList);
                                        properties.put("Mode:", "Extender PCA9685");
                                        bridgeDeviceHandler.megaDHardware.setDI2CType(configuration.port,
                                                MegaDI2CSensorsEnum.PCA9685);
                                    }
                                }
                            }
                            MegaDHttpHelpers httpRequest = new MegaDHttpHelpers();
                            response = httpRequest.request("http://" + bridgeDeviceHandler.config.hostname + "/"
                                    + bridgeDeviceHandler.config.password + "/?pt=" + configuration.port + "&cmd=scan")
                                    .getResponseResult();
                            int dStartIndex = response.indexOf("<br>") + "<br>".length();
                            String[] splittedSensors = response.substring(dStartIndex).split("<br>");
                            for (String sensor : splittedSensors) {
                                if (!sensor.isEmpty()) {
                                    sensor = sensor.substring(0, sensor.indexOf("-")).strip();
                                    if (bridgeDeviceHandler.megaDHardware.getDI2cType(configuration.port) == null) {
                                        String finalSensor = sensor;
                                        List<Channel> lambdaCannel = new ArrayList<>();
                                        Objects.requireNonNull(megaDI2CSensorsList).forEach((k, v) -> {
                                            if (v.getSensorAddress().equals(finalSensor)) {
                                                for (MegaDI2CSensors.I2CSensorParams params : v.getParameters()) {
                                                    Configuration configuration = new Configuration();
                                                    configuration.put("type", v.getSensorType());
                                                    configuration.put("path", params.getPath());
                                                    ChannelUID i2cUID = new ChannelUID(thing.getUID(),
                                                            v.getSensorType() + "_" + params.getId());
                                                    Set<String> tags = new HashSet<>();
                                                    tags.add("Point");
                                                    if (params.getId().equals("humidity")) {
                                                        tags.add("Humidity");
                                                    } else if (params.getId().equals("temperature")) {
                                                        tags.add("Temperature");
                                                    }
                                                    Channel i2c = ChannelBuilder.create(i2cUID)
                                                            .withType(
                                                                    new ChannelTypeUID(MegaDBindingConstants.BINDING_ID,
                                                                            MegaDBindingConstants.CHANNEL_I2C))
                                                            .withLabel(label + " " + v.getSensorLabel() + " "
                                                                    + params.getName())
                                                            .withConfiguration(configuration)
                                                            .withAcceptedItemType(params.getOh()).withDefaultTags(tags)
                                                            .build();
                                                    lambdaCannel.add(i2c);
                                                }
                                            }
                                        });
                                        channelList.addAll(lambdaCannel);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            logger.error("I2C init exception {}", e.getLocalizedMessage());
                        }
                    }
                    case NC -> {
                        bridgeDeviceHandler.megaDHardware.setPortType(configuration.port, MegaDTypesEnum.NC);
                    }
                }
                ThingBuilder thingBuilder = editThing();
                thingBuilder.withChannels(channelList);
                updateThing(thingBuilder.build());
            }

            properties.put("Mega URL:", "http://" + Objects.requireNonNull(bridgeDeviceHandler).config.hostname + "/"
                    + bridgeDeviceHandler.config.password + "/?pt=" + configuration.port);
            properties.put("Type:", Objects
                    .requireNonNull(bridgeDeviceHandler.megaDHardware.getPortsType(configuration.port)).toString());
            updateProperties(properties);
            updateStatus(ThingStatus.ONLINE);
        } else {
            updateStatus(ThingStatus.UNINITIALIZED, ThingStatusDetail.BRIDGE_UNINITIALIZED, "Bridge is not defined");
        }
    }

    private String getType(MegaDDeviceHandler bridgeDeviceHandler) {
        MegaDHttpHelpers httpRequest = new MegaDHttpHelpers();
        String response = httpRequest.request("http://" + bridgeDeviceHandler.config.hostname + "/"
                + bridgeDeviceHandler.config.password + "/?pt=" + configuration.port).getResponseResult();
        response = response.substring(response.indexOf("name=pty>") + "name=pty>".length(),
                response.indexOf("</select><br>"));
        return response;
    }

    public void updatePort(String input) {
        if (input.contains("=")) {
            logger.debug("Incoming port {} value {}", configuration.port, input);
            if (input.contains("m=1")) {
                updateChannel(MegaDBindingConstants.CHANNEL_IN, "OFF");
                updateChannel(MegaDBindingConstants.CHANNEL_CONTACT, "OFF");
                triggerChannel(MegaDBindingConstants.CHANNEL_CLICK, "CLICK");
            } else if (input.contains("m=2")) {
                triggerChannel(MegaDBindingConstants.CHANNEL_LONGPRESS, "LONGPRESS");
            } else if (input.contains("&ext")) {
                List<String> extportslist = Arrays.stream(input.split("&")).filter(ep -> ep.contains("ext")).toList();
                extportslist.forEach(port -> {
                    String extPort = port.split("=")[0].substring(3);
                    String extPortState = port.split("=")[1];
                    List<Channel> channels = thing.getChannels();
                    for (Channel channel : channels) {
                        if (channel.getConfiguration().get("port").toString().equals(extPort)) {
                            String channelType = channel.getAcceptedItemType();
                            if (channelType != null) {
                                if ("Switch".equals(channelType)) {
                                    if ("1".equals(extPortState)) {
                                        updateChannel(channel.getUID().getId(), "ON");
                                    } else if ("0".equals(extPortState)) {
                                        updateChannel(channel.getUID().getId(), "OFF");
                                    }
                                }
                            }
                        }
                    }
                });
            } else {
                updateChannel(MegaDBindingConstants.CHANNEL_IN, "ON");
                updateChannel(MegaDBindingConstants.CHANNEL_CONTACT, "ON");
            }
            if (input.contains("cnt=")) {
                String[] counterSelect = input.split("&");
                for (String counter : counterSelect) {
                    if (counter.contains("cnt=")) {
                        updateChannel(MegaDBindingConstants.CHANNEL_INCOUNT, counter.split("=")[1]);
                    }
                }
            }
            if (input.contains("v=0")) {
                updateChannel(MegaDBindingConstants.CHANNEL_OUT, "OFF");
            } else if (input.contains("v=1")) {
                updateChannel(MegaDBindingConstants.CHANNEL_OUT, "ON");
            }
            if (input.contains("dir=0")) {
                triggerChannel(MegaDBindingConstants.CHANNEL_DIRECTION, "DOWN");
            } else if (input.contains("dir=1")) {
                triggerChannel(MegaDBindingConstants.CHANNEL_DIRECTION, "UP");
            }
        } else {
            // logger.debug("loop input port {} value {}", configuration.port, input);
            if (input.contains("/")) {
                String[] values = input.split("/");
                if (values[0].contains("ON") || values[0].contains("OFF")) {
                    updateChannel(MegaDBindingConstants.CHANNEL_IN, values[0]);
                    updateChannel(MegaDBindingConstants.CHANNEL_OUT, values[0]);
                    updateChannel(MegaDBindingConstants.CHANNEL_CONTACT, values[0]);
                    if (values.length == 2) {
                        updateChannel(MegaDBindingConstants.CHANNEL_INCOUNT, values[1]);
                    }
                } else {
                    // logger.debug("Incoming port {} is sensor, value {}", configuration.port, values);
                    updateChannel(MegaDBindingConstants.CHANNEL_TEMP, input);
                    updateChannel(MegaDBindingConstants.CHANNEL_HUM, input);
                }
            } else {
                if (input.contains("ON") || input.contains("OFF")) {
                    updateChannel(MegaDBindingConstants.CHANNEL_IN, input);
                    updateChannel(MegaDBindingConstants.CHANNEL_OUT, input);
                    updateChannel(MegaDBindingConstants.CHANNEL_CONTACT, input);
                } else {
                    updateChannel(MegaDBindingConstants.CHANNEL_PWM, input);
                    updateChannel(MegaDBindingConstants.CHANNEL_DIMMER, input);
                    updateChannel(MegaDBindingConstants.CHANNEL_TEMP, input);
                    updateChannel(MegaDBindingConstants.CHANNEL_HUM, input);
                }
            }
        }
    }

    private void updateChannel(String channelName, String value) {
        for (Channel channel : getThing().getChannels()) {
            if (isLinked(channel.getUID().getId())) {
                if (channel.getUID().getId().equals(channelName)) {
                    switch (channelName) {
                        case MegaDBindingConstants.CHANNEL_IN:
                        case MegaDBindingConstants.CHANNEL_OUT:
                            if (value.contains("ON")) {
                                updateState(channel.getUID().getId(), OnOffType.ON);
                            } else if (value.contains("OFF")) {
                                updateState(channel.getUID().getId(), OnOffType.OFF);
                            }
                            break;
                        case MegaDBindingConstants.CHANNEL_INCOUNT:
                            updateState(channel.getUID().getId(), DecimalType.valueOf(value));
                            break;
                        case MegaDBindingConstants.CHANNEL_CONTACT:
                            if (value.contains("ON")) {
                                updateState(channel.getUID().getId(), OpenClosedType.CLOSED);
                            } else if (value.contains("OFF")) {
                                updateState(channel.getUID().getId(), OpenClosedType.OPEN);
                            }
                            break;
                        case MegaDBindingConstants.CHANNEL_PWM:
                            try {
                                updateState(channel.getUID().getId(), DecimalType.valueOf(value));
                            } catch (Exception ignored) {
                            }
                            break;
                        case MegaDBindingConstants.CHANNEL_TEMP:
                            String[] valTempSplit = value.split("/");
                            for (String splittedValues : valTempSplit) {
                                if (splittedValues.contains("temp")) {
                                    String[] tempValue = splittedValues.split(":");
                                    try {
                                        updateState(channel.getUID().getId(), DecimalType.valueOf(tempValue[1]));
                                    } catch (Exception ignored) {
                                    }
                                }
                            }
                            break;
                        case MegaDBindingConstants.CHANNEL_HUM:
                            String[] valHumSplit = value.split("/");
                            for (String splittedValues : valHumSplit) {
                                if (splittedValues.contains("hum")) {
                                    String[] humValue = splittedValues.split(":");
                                    try {
                                        updateState(channel.getUID().getId(), DecimalType.valueOf(humValue[1]));
                                    } catch (Exception ignored) {
                                    }
                                }
                            }
                            break;
                        case MegaDBindingConstants.CHANNEL_DIMMER:
                            try {
                                if ("0".equals(value)) {
                                    // logger.debug("dimmer value is 0, do not save dimmer value");
                                    updateState(channel.getUID().getId(), PercentType.valueOf(Integer.toString(0)));
                                    return;
                                } else {
                                    dimmervalue = Integer.parseInt(value);
                                }
                            } catch (Exception ignored) {
                            }

                            int percent = 0;
                            try {
                                int minval = 0;// Integer.parseInt(getThing().getConfiguration().get("min_pwm").toString());
                                if (minval != 0) {
                                    if (minval == dimmervalue) {
                                        percent = 1;
                                    } else {
                                        int realval = (dimmervalue - minval);// * 0.01;
                                        double divVal = (255 - minval) * 0.01;
                                        percent = (int) Math.round(realval / divVal);
                                    }
                                } else {
                                    percent = (int) Math.round(dimmervalue / 2.55);
                                }
                            } catch (Exception ex) {
                                logger.debug("Cannot convert to dimmer values. Error: '{}'", ex.toString());
                            }
                            updateState(channel.getUID().getId(), PercentType.valueOf(Integer.toString(percent)));
                            break;
                        case MegaDBindingConstants.CHANNEL_DIRECTION:
                            break;
                        default:
                    }
                    MegaDDeviceHandler bridgeDeviceHandler = this.bridgeDeviceHandler;
                    if (bridgeDeviceHandler != null) {
                        MegaDDsenEnum sensorDsenType = bridgeDeviceHandler.megaDHardware
                                .getDSensorType(configuration.port);
                        if (sensorDsenType != null) {
                            if (sensorDsenType.equals(MegaDDsenEnum.ONEWIREBUS)) {
                                try {
                                    updateState(channel.getUID().getId(), DecimalType.valueOf(value));
                                } catch (Exception ignored) {
                                }
                            }
                        }
                        MegaDTypesEnum megaDTypes = bridgeDeviceHandler.megaDHardware.getPortsType(configuration.port);
                        if (megaDTypes != null) {
                            if (megaDTypes.equals(MegaDTypesEnum.I2C)) {
                                MegaDI2CSensorsEnum megaDI2CSensorsEnum = bridgeDeviceHandler.megaDHardware
                                        .getDI2cType(configuration.port);
                                if (megaDI2CSensorsEnum != null) {
                                    if (megaDI2CSensorsEnum.equals(MegaDI2CSensorsEnum.MCP230XX)) {
                                        if (value.contains("ON")) {
                                            updateState(channel.getUID().getId(), OnOffType.ON);
                                        } else if (value.contains("OFF")) {
                                            updateState(channel.getUID().getId(), OnOffType.OFF);
                                        }
                                    } else if (megaDI2CSensorsEnum.equals(MegaDI2CSensorsEnum.PCA9685)) {
                                        if (value.contains("ON")) {
                                            updateState(channel.getUID().getId(), OnOffType.ON);
                                        } else if (value.contains("OFF")) {
                                            updateState(channel.getUID().getId(), OnOffType.OFF);
                                        } else {
                                            try {
                                                if (channel.getConfiguration().get("type") != null) {
                                                    String channelType = channel.getConfiguration().get("type")
                                                            .toString();
                                                    if ("PWM".equals(channelType)) {
                                                        updateState(channel.getUID().getId(),
                                                                DecimalType.valueOf(value));
                                                    } else if ("DIMMER".equals(channelType)) {
                                                        try {
                                                            if ("0".equals(value)) {
                                                                // logger.debug("dimmer value is 0, do not save dimmer
                                                                // value");
                                                                updateState(channel.getUID().getId(),
                                                                        PercentType.valueOf(Integer.toString(0)));
                                                                return;
                                                            } else {
                                                                dimmervalue = Integer.parseInt(value);
                                                            }
                                                        } catch (Exception ignored) {
                                                        }

                                                        int percent = 0;
                                                        try {
                                                            int minval = 0;// Integer.parseInt(getThing().getConfiguration().get("min_pwm").toString());
                                                            if (minval != 0) {
                                                                if (minval == dimmervalue) {
                                                                    percent = 1;
                                                                } else {
                                                                    int realval = (dimmervalue - minval);// * 0.01;
                                                                    double divVal = (4095 - minval) * 0.01;
                                                                    percent = (int) Math.round(realval / divVal);
                                                                }
                                                            } else {
                                                                percent = (int) Math.round(dimmervalue / 40.95);
                                                            }
                                                        } catch (Exception ex) {
                                                            logger.debug("Cannot convert to dimmer values. Error: '{}'",
                                                                    ex.toString());
                                                        }
                                                    }
                                                } else {
                                                    try {
                                                        if ("0".equals(value)) {
                                                            // logger.debug("dimmer value is 0, do not save dimmer
                                                            // value");
                                                            updateState(channel.getUID().getId(),
                                                                    PercentType.valueOf(Integer.toString(0)));
                                                            return;
                                                        } else {
                                                            dimmervalue = Integer.parseInt(value);
                                                        }
                                                    } catch (Exception ignored) {
                                                    }

                                                    int percent = 0;
                                                    try {
                                                        int minval = 0;// Integer.parseInt(getThing().getConfiguration().get("min_pwm").toString());
                                                        if (minval != 0) {
                                                            if (minval == dimmervalue) {
                                                                percent = 1;
                                                            } else {
                                                                int realval = (dimmervalue - minval);// * 0.01;
                                                                double divVal = (4096 - minval) * 0.01;
                                                                percent = (int) Math.round(realval / divVal);
                                                            }
                                                        } else {
                                                            percent = (int) Math.round(dimmervalue / 40.96);
                                                        }
                                                    } catch (Exception ex) {
                                                        logger.debug("Cannot convert to dimmer values. Error: '{}'",
                                                                ex.toString());
                                                    }
                                                    updateState(channel.getUID().getId(),
                                                            PercentType.valueOf(Integer.toString(percent)));
                                                }
                                            } catch (Exception ignored) {
                                            }
                                        }
                                    } else {
                                        try {
                                            updateState(channel.getUID().getId(), DecimalType.valueOf(value));
                                        } catch (Exception ignored) {
                                        }
                                    }
                                } else {
                                    try {
                                        updateState(channel.getUID().getId(), DecimalType.valueOf(value));
                                    } catch (Exception ignored) {
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public void refresh() {
        logger.debug("Refresh port {}", configuration.port);
        MegaDHttpHelpers httpRequest = new MegaDHttpHelpers();
        MegaDDeviceHandler bridgeDeviceHandler = this.bridgeDeviceHandler;
        if (bridgeDeviceHandler != null) {
            MegaDTypesEnum portType = bridgeDeviceHandler.megaDHardware.getPortsType(configuration.port);
            if (portType != null) {
                if (portType.equals(MegaDTypesEnum.DSEN)) {
                    MegaDDsenEnum dDenType = bridgeDeviceHandler.megaDHardware.getDSensorType(configuration.port);
                    if (dDenType != null) {
                        if (dDenType.equals(MegaDDsenEnum.ONEWIREBUS)) {
                            List<Channel> channels = thing.getChannels();
                            int responseCode = httpRequest
                                    .request(
                                            "http://"
                                                    + bridgeDeviceHandler
                                                            .getThing().getConfiguration().get("hostname").toString()
                                                    + "/"
                                                    + bridgeDeviceHandler.getThing().getConfiguration().get("password")
                                                            .toString()
                                                    + "/?pt=" + configuration.port + "?cmd=conv")
                                    .getResponseCode();
                            if (responseCode == 200) {
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException ignored) {
                                }
                                String response = httpRequest
                                        .request("http://"
                                                + bridgeDeviceHandler.getThing().getConfiguration().get("hostname")
                                                        .toString()
                                                + "/"
                                                + bridgeDeviceHandler.getThing().getConfiguration().get("password")
                                                        .toString()
                                                + "/?pt=" + configuration.port + "?cmd=list")
                                        .getResponseResult();
                                logger.debug("response port {} is {}", configuration.port, response);
                                String[] sensorsList = response.split(";");
                                if (!"busy".equals(response)) {
                                    for (Channel channel : channels) {
                                        for (String oneWireSensor : sensorsList) {
                                            if (!oneWireSensor.isEmpty()) {
                                                String address = oneWireSensor.split(":")[0];
                                                String value = oneWireSensor.split(":")[1];
                                                if (channel.getConfiguration().get("address").toString()
                                                        .equals(address)) {
                                                    updateChannel(channel.getUID().getId(), value);
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                logger.error("Can not send conv to {}",
                                        bridgeDeviceHandler.getThing().getConfiguration().get("hostname").toString());
                            }
                        }
                    }
                } else if (portType.equals(MegaDTypesEnum.IN)) {
                    String response = httpRequest
                            .request("http://" + bridgeDeviceHandler.config.hostname + "/"
                                    + bridgeDeviceHandler.config.password + "/?pt=" + configuration.port + "&cmd=get")
                            .getResponseResult();
                    if (response.contains("/")) {
                        String[] values = response.split("/");
                        if (values[0].contains("ON") || values[0].contains("OFF")) {
                            updateChannel(MegaDBindingConstants.CHANNEL_IN, values[0]);
                            updateChannel(MegaDBindingConstants.CHANNEL_CONTACT, values[0]);
                            if (values.length == 2) {
                                updateChannel(MegaDBindingConstants.CHANNEL_INCOUNT, values[1]);
                            }
                        }
                    } else {
                        if (response.contains("ON") || response.contains("OFF")) {
                            updateChannel(MegaDBindingConstants.CHANNEL_IN, response);
                            updateChannel(MegaDBindingConstants.CHANNEL_CONTACT, response);
                        }
                    }
                } else if (portType.equals(MegaDTypesEnum.OUT)) {
                    String response = httpRequest
                            .request("http://" + bridgeDeviceHandler.config.hostname + "/"
                                    + bridgeDeviceHandler.config.password + "/?pt=" + configuration.port + "&cmd=get")
                            .getResponseResult();
                    if (response.contains("/")) {
                        String[] values = response.split("/");
                        if (values[0].contains("ON") || values[0].contains("OFF")) {
                            updateChannel(MegaDBindingConstants.CHANNEL_OUT, values[0]);
                        }
                    } else {
                        if (response.contains("ON") || response.contains("OFF")) {
                            updateChannel(MegaDBindingConstants.CHANNEL_OUT, response);
                        } else {
                            MegaDModesEnum mode = bridgeDeviceHandler.megaDHardware.getMode(configuration.port);
                            if (mode != null) {
                                if (mode.equals(MegaDModesEnum.PWM)) {
                                    updateChannel(MegaDBindingConstants.CHANNEL_PWM, response);
                                    updateChannel(MegaDBindingConstants.CHANNEL_DIMMER, response);
                                }
                            }
                        }
                    }
                } else if (portType.equals(MegaDTypesEnum.I2C)) {
                    MegaDI2CSensorsEnum megaDI2CSensorsEnum = bridgeDeviceHandler.megaDHardware
                            .getDI2cType(configuration.port);
                    if (megaDI2CSensorsEnum != null) {
                        if (megaDI2CSensorsEnum.equals(MegaDI2CSensorsEnum.MCP230XX)) {
                            String response = httpRequest.request("http://" + bridgeDeviceHandler.config.hostname + "/"
                                    + bridgeDeviceHandler.config.password + "/?pt=" + configuration.port + "&cmd=get")
                                    .getResponseResult();
                            String[] portsStatus = response.split(";");
                            List<Channel> channels = thing.getChannels();
                            for (Channel channel : channels) {
                                BigDecimal port = (BigDecimal) channel.getConfiguration().get("port");
                                String acceptType = channel.getAcceptedItemType();
                                if (acceptType != null) {
                                    if ("Switch".equals(acceptType)) {
                                        updateChannel(channel.getUID().getId(), portsStatus[port.intValue()]);
                                    }
                                }
                            }
                        } else if (megaDI2CSensorsEnum.equals(MegaDI2CSensorsEnum.PCA9685)) {
                            String response = httpRequest.request("http://" + bridgeDeviceHandler.config.hostname + "/"
                                    + bridgeDeviceHandler.config.password + "/?pt=" + configuration.port + "&cmd=get")
                                    .getResponseResult();
                            String[] portsStatus = response.split(";");
                            List<Channel> channels = thing.getChannels();
                            for (Channel channel : channels) {
                                if (channel.getConfiguration().get("port") != null) {
                                    BigDecimal port = (BigDecimal) channel.getConfiguration().get("port");
                                    updateChannel(channel.getUID().getId(), portsStatus[port.intValue()]);
                                }
                            }
                        }
                    } else {
                        List<Channel> channels = thing.getChannels();
                        for (Channel channel : channels) {
                            if ((channel.getConfiguration().get("type") != null)
                                    && (channel.getConfiguration().get("path") != null)) {
                                String sensortype = channel.getConfiguration().get("type").toString();
                                String sensorPath = channel.getConfiguration().get("path").toString();
                                String response = httpRequest.request("http://" + bridgeDeviceHandler.config.hostname
                                        + "/" + bridgeDeviceHandler.config.password + "/?pt=" + configuration.port
                                        + "&scl=" + bridgeDeviceHandler.megaDHardware.getScl(configuration.port)
                                        + "&i2c_dev=" + sensortype + "&" + sensorPath).getResponseResult();
                                updateChannel(channel.getUID().getId(), response);
                            }
                            if (channel.getConfiguration().get("port") != null) {
                                String response = httpRequest.request("http://" + bridgeDeviceHandler.config.hostname
                                        + "/" + bridgeDeviceHandler.config.password + "/?pt=" + configuration.port
                                        + "&ext=" + channel.getConfiguration().get("port").toString() + "cmg=get")
                                        .getResponseResult();
                                updateChannel(channel.getUID().getId(), response);
                            }
                        }
                    }
                }
            }
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

    private String readLabel(final String urlAsString) {
        final StringBuilder content = new StringBuilder();
        BufferedReader reader = null;
        InputStream inputStream = null;
        try {
            final URL url = new URL(urlAsString);
            inputStream = url.openStream();
            reader = new BufferedReader(new InputStreamReader(inputStream, "Windows-1251"));

            String inputLine;
            while ((inputLine = reader.readLine()) != null) {
                content.append(inputLine);
            }
        } catch (final IOException ignored) {
        }
        return content.toString();
    }

    @Override
    public void dispose() {
        ScheduledFuture<?> refreshPollingJob = this.refreshPollingJob;
        if (refreshPollingJob != null && !refreshPollingJob.isCancelled()) {
            refreshPollingJob.cancel(true);
        }
        this.refreshPollingJob = refreshPollingJob;
        this.refreshPollingJob = null;
        MegaDHTTPCallback.portListener.remove(this);
        super.dispose();
    }
}
