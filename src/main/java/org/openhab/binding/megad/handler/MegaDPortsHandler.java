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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.megad.MegaDBindingConstants;
import org.openhab.binding.megad.MegaDConfiguration;
import org.openhab.binding.megad.internal.MegaDHTTPCallback;
import org.openhab.binding.megad.internal.MegaDTypesEnum;
import org.openhab.binding.megad.internal.MegaHttpHelpers;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.OpenClosedType;
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
    }

    @Override
    public void initialize() {
        List<Channel> channelList = new ArrayList<>();
        configuration = getConfigAs(MegaDConfiguration.class);
        bridgeDeviceHandler = getBridgeHandler();
        if (bridgeDeviceHandler != null) {
            MegaDHTTPCallback.portListener.add(this);
            ScheduledFuture<?> refreshPollingJob = this.refreshPollingJob;
            if (configuration.refresh != 0) {
                logger.debug("Thing {}, refresh interval is {} sec", getThing().getUID(), configuration.refresh);
                if (refreshPollingJob == null || refreshPollingJob.isCancelled()) {
                    refreshPollingJob = scheduler.scheduleWithFixedDelay(this::refresh, 0, configuration.refresh,
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
                for (String mode : respSplit) {
                    if (mode.contains("selected")) {
                        if (!mode.contains("value=255")) {
                            if (mode.toUpperCase(Locale.ROOT).contains("IN")) {
                                bridgeDeviceHandler.megaDHardware.setPortType(configuration.port, MegaDTypesEnum.IN);
                                logger.info("port {} mode is {}", configuration.port, MegaDTypesEnum.IN);
                            } else if (mode.toUpperCase(Locale.ROOT).contains("OUT")) {
                                bridgeDeviceHandler.megaDHardware.setPortType(configuration.port, MegaDTypesEnum.OUT);
                                logger.info("port {} mode is {}", configuration.port, MegaDTypesEnum.OUT);
                            } else if (mode.toUpperCase(Locale.ROOT).contains("DSen".toUpperCase())) {
                                bridgeDeviceHandler.megaDHardware.setPortType(configuration.port, MegaDTypesEnum.DSEN);
                                logger.info("port {} mode is {}", configuration.port, MegaDTypesEnum.DSEN);
                            } else if (mode.toUpperCase(Locale.ROOT).contains("I2C")) {
                                bridgeDeviceHandler.megaDHardware.setPortType(configuration.port, MegaDTypesEnum.I2C);
                                logger.info("port {} mode is {}", configuration.port, MegaDTypesEnum.I2C);
                            } else if (mode.toUpperCase(Locale.ROOT).contains("ADC")) {
                                bridgeDeviceHandler.megaDHardware.setPortType(configuration.port, MegaDTypesEnum.ADC);
                                logger.info("port {} mode is {}", configuration.port, MegaDTypesEnum.ADC);
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

                        ChannelUID inUID = new ChannelUID(thing.getUID(), MegaDBindingConstants.CHANNEL_IN);
                        Channel in = ChannelBuilder.create(inUID).withType(
                                new ChannelTypeUID(MegaDBindingConstants.BINDING_ID, MegaDBindingConstants.CHANNEL_IN))
                                .withLabel(label + " Input").build();
                        channelList.add(in);

                        ChannelUID inCountUID = new ChannelUID(thing.getUID(), MegaDBindingConstants.CHANNEL_INCOUNT);
                        Channel inCount = ChannelBuilder.create(inCountUID)
                                .withType(new ChannelTypeUID(MegaDBindingConstants.BINDING_ID,
                                        MegaDBindingConstants.CHANNEL_INCOUNT))
                                .withLabel(label + " Contact").build();
                        channelList.add(inCount);

                        String misc = response.substring(
                                response.indexOf("type=checkbox name=misc value=1")
                                        + "type=checkbox name=misc value=1".length(),
                                response.indexOf("type=checkbox name=misc value=1")
                                        + "type=checkbox name=misc value=1".length() + 13);
                        if (!misc.contains("checked")) {
                            logger.debug("Set mode checkbox at port {}", configuration.port);
                            MegaHttpHelpers httpRequest = new MegaHttpHelpers();
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
                        ChannelUID outUID = new ChannelUID(thing.getUID(), MegaDBindingConstants.CHANNEL_OUT);
                        Channel out = ChannelBuilder.create(outUID).withType(
                                new ChannelTypeUID(MegaDBindingConstants.BINDING_ID, MegaDBindingConstants.CHANNEL_OUT))
                                .build();
                        channelList.add(out);
                    }
                    case ADC -> {
                        ChannelUID adcUID = new ChannelUID(thing.getUID(), MegaDBindingConstants.CHANNEL_ADC);
                        Channel adc = ChannelBuilder.create(adcUID).withType(
                                new ChannelTypeUID(MegaDBindingConstants.BINDING_ID, MegaDBindingConstants.CHANNEL_ADC))
                                .build();
                        channelList.add(adc);
                    }
                    case DSEN -> {
                        ChannelUID dsenUID = new ChannelUID(thing.getUID(), MegaDBindingConstants.CHANNEL_1WTEMP);
                        Channel dsen = ChannelBuilder.create(dsenUID)
                                .withType(new ChannelTypeUID(MegaDBindingConstants.BINDING_ID,
                                        MegaDBindingConstants.CHANNEL_1WTEMP))
                                .build();
                        channelList.add(dsen);
                    }
                    case I2C -> {
                        ChannelUID i2cUID = new ChannelUID(thing.getUID(), MegaDBindingConstants.CHANNEL_I2C_TEMP);
                        Channel i2c = ChannelBuilder.create(i2cUID)
                                .withType(new ChannelTypeUID(MegaDBindingConstants.BINDING_ID,
                                        MegaDBindingConstants.CHANNEL_I2C_TEMP))
                                .build();
                        channelList.add(i2c);
                    }
                    case NC -> {
                    }
                }
                ThingBuilder thingBuilder = editThing();
                thingBuilder.withChannels(channelList);
                updateThing(thingBuilder.build());
            }
            Map<String, String> properties = new HashMap<>();

            properties.put("Mega URL:", "http://" + Objects.requireNonNull(bridgeDeviceHandler).config.hostname + "/"
                    + bridgeDeviceHandler.config.password + "/?pt=" + configuration.port);
            properties.put("Mode:", Objects
                    .requireNonNull(bridgeDeviceHandler.megaDHardware.getPortsType(configuration.port)).toString());
            updateProperties(properties);
            updateStatus(ThingStatus.ONLINE);
        } else {
            updateStatus(ThingStatus.UNINITIALIZED, ThingStatusDetail.BRIDGE_UNINITIALIZED, "Bridge is not defined");
        }
    }

    private String getType(MegaDDeviceHandler bridgeDeviceHandler) {
        MegaHttpHelpers httpRequest = new MegaHttpHelpers();
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
        } else {
            logger.debug("loop input port {} value {}", configuration.port, input);
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
                    logger.debug("Incoming port {} is sensor", configuration.port);
                }
            } else {
                if (input.contains("ON") || input.contains("OFF")) {
                    updateChannel(MegaDBindingConstants.CHANNEL_IN, input);
                    updateChannel(MegaDBindingConstants.CHANNEL_OUT, input);
                    updateChannel(MegaDBindingConstants.CHANNEL_CONTACT, input);
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
                    }
                }
            }
        }
    }

    public void refresh() {
        logger.debug("Refresh port {}", configuration.port);
        // long now = System.currentTimeMillis();
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
        this.refreshPollingJob = null;
        MegaDHTTPCallback.portListener.remove(this);
        super.dispose();
    }
}
