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

import static org.openhab.binding.megad.discovery.MegaDDiscoveryService.megaDI2CSensorsList;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
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
import org.openhab.binding.megad.dto.MegaDHardware;
import org.openhab.binding.megad.dto.MegaDI2CSensors;
import org.openhab.binding.megad.enums.MegaDDsenEnum;
import org.openhab.binding.megad.enums.MegaDExtendedTypeEnum;
import org.openhab.binding.megad.enums.MegaDExtendersEnum;
import org.openhab.binding.megad.enums.MegaDI2CDevicesEnum;
import org.openhab.binding.megad.enums.MegaDModesEnum;
import org.openhab.binding.megad.enums.MegaDTypesEnum;
import org.openhab.binding.megad.internal.MegaDHTTPCallback;
import org.openhab.binding.megad.internal.MegaDHTTPResponse;
import org.openhab.binding.megad.internal.MegaDHttpHelpers;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemRegistry;
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
import org.openhab.core.thing.link.ItemChannelLinkRegistry;
import org.openhab.core.thing.type.ChannelKind;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.core.types.Command;
import org.openhab.core.types.StateDescription;
import org.openhab.core.types.StateOption;
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
    private final ItemRegistry itemRegistry;
    private final ItemChannelLinkRegistry link;
    private Logger logger = LoggerFactory.getLogger(MegaDPortsHandler.class);
    private @Nullable ScheduledFuture<?> refreshPollingJob;
    @Nullable
    public MegaDDeviceHandler bridgeDeviceHandler;
    MegaDConfiguration configuration = getConfigAs(MegaDConfiguration.class);
    String line1 = "";
    String line2 = "";
    MegaDHardware.Port port = new MegaDHardware.Port();

    public MegaDPortsHandler(Thing thing, ItemRegistry itemRegistry, ItemChannelLinkRegistry link) {
        super(thing);
        this.itemRegistry = itemRegistry;
        this.link = link;
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        int state = 0;
        String result = "";
        Set<Item> li = link.getLinkedItems(channelUID);
        var opt = new Object() {
            String smooth = "";
        };
        if (li.stream().anyMatch(i -> i.getState().toString().equals(command.toString()))) {
            Item triggeredItem = li.stream().filter(i -> i.getState().toString().equals(command.toString())).findFirst()
                    .get();
            StateDescription triggeredStateDescription = triggeredItem.getStateDescription();
            if (triggeredStateDescription != null) {
                if (!triggeredStateDescription.getOptions().isEmpty()) {
                    List<StateOption> options = triggeredStateDescription.getOptions();
                    options.forEach(op -> {
                        if (op.getLabel() != null) {
                            String label = op.getLabel();
                            if (label != null) {
                                if (label.equals("smooth")) {
                                    opt.smooth = op.getValue();
                                }
                            }
                        }
                    });
                }
            }
        }
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
                            int minval = port.getPwmm();// Integer.parseInt(getThing().getConfiguration().get("min_pwm").toString());
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
                        StringBuilder resBuild = new StringBuilder().append("http://")
                                .append(bridgeDeviceHandler.getThing().getConfiguration().get("hostname").toString())
                                .append("/")
                                .append(bridgeDeviceHandler.getThing().getConfiguration().get("password").toString())
                                .append("/?pt=").append(getThing().getConfiguration().get("port").toString())
                                .append("&pwm=").append(resultInt);
                        if (!opt.smooth.isBlank()) {
                            resBuild.append("&cnt=").append(opt.smooth);
                        } else
                            resBuild.append("&cnt=").append("0");
                        result = resBuild.toString();
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
                MegaDTypesEnum portType = port.getPty();
                if (portType.equals(MegaDTypesEnum.I2C)) {
                    MegaDExtendersEnum megaDExtendersEnum = port.getExtenders();
                    MegaDI2CDevicesEnum megaDI2CDevicesEnum = port.getI2CDevicesList();
                    if (megaDExtendersEnum.equals(MegaDExtendersEnum.MCP230XX)) {
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
                                + bridgeDeviceHandler.config.password + "/?cmd=" + thingPort + "e" + port + ":" + cmd;
                        MegaDHttpHelpers httpRequest = new MegaDHttpHelpers();
                        int responseCode = httpRequest.request(request).getResponseCode();
                        if (responseCode != 200) {
                            logger.error("Send command at port {} error, check your mega {}", configuration.port,
                                    bridgeDeviceHandler.config.hostname);
                        }
                        logger.debug("MCP230XX request to mega: {}", request);
                    }
                    if (megaDExtendersEnum.equals(MegaDExtendersEnum.PCA9685)) {
                        BigDecimal port = (BigDecimal) Objects.requireNonNull(thing.getChannel(channelUID))
                                .getConfiguration().get("port");
                        BigDecimal thingPort = (BigDecimal) thing.getConfiguration().get("port");
                        String cmd = "";
                        if (command.equals(OnOffType.ON)) {
                            cmd = "1";
                        } else if (command.equals(OnOffType.OFF)) {
                            cmd = "0";
                        } else if ((!command.toString().equals("REFRESH")) || (!command.toString().equals("ADDED"))) {
                            if (Objects.requireNonNull(thing.getChannel(channelUID)).getConfiguration()
                                    .get("type") != null) {
                                String channelType = Objects.requireNonNull(thing.getChannel(channelUID))
                                        .getConfiguration().get("type").toString();
                                if ("PWM".equals(channelType)) {
                                    cmd = command.toString();
                                } else if ("DIMMER".equals(channelType)) {
                                    try {
                                        int value = Integer.parseInt(command.toString());
                                        cmd = String.valueOf(Math.round(value * 40.95));
                                    } catch (Exception e) {
                                    }
                                }
                            } else {
                                try {
                                    int value = Integer.parseInt(command.toString());
                                    cmd = String.valueOf(Math.round(value * 40.95));
                                } catch (Exception e) {
                                }
                            }
                        }
                        String request = "http://" + bridgeDeviceHandler.config.hostname + "/"
                                + bridgeDeviceHandler.config.password + "/?cmd=" + thingPort + "e" + port + ":" + cmd;
                        MegaDHttpHelpers httpRequest = new MegaDHttpHelpers();
                        int responseCode = httpRequest.request(request).getResponseCode();
                        if (responseCode != 200) {
                            logger.error("Send command at port {} error, check your mega {}", configuration.port,
                                    bridgeDeviceHandler.config.hostname);
                        }
                        logger.debug("PCA9685 request to mega: {}", request);
                    }
                    if (megaDI2CDevicesEnum.equals(MegaDI2CDevicesEnum.LCD1602)) {
                        if (channelUID.getId().equals(MegaDBindingConstants.CHANNEL_LINE1)) {
                            line1 = command.toString();
                        } else if (channelUID.getId().equals(MegaDBindingConstants.CHANNEL_LINE2)) {
                            line2 = command.toString();
                        }
                        String request = "http://" + bridgeDeviceHandler.config.hostname + "/"
                                + bridgeDeviceHandler.config.password + "/?pt=" + configuration.port + "&disp_cmd=1";
                        MegaDHttpHelpers httpRequest = new MegaDHttpHelpers();
                        int responseCode = httpRequest.request(request).getResponseCode();
                        if (responseCode == 200) {
                            request = "/" + bridgeDeviceHandler.config.password + "/?pt=" + configuration.port
                                    + "&text=" + line1.replace(" ", "_");
                            httpRequest.sendToLCDrawStream(bridgeDeviceHandler.config.hostname, request);
                            request = "/" + bridgeDeviceHandler.config.password + "/?pt=" + configuration.port
                                    + "&text=" + line2.replace(" ", "_") + "&col=0&row=1";
                            httpRequest.sendToLCDrawStream(bridgeDeviceHandler.config.hostname, request);
                        }
                        logger.debug("LCD1602 request to mega: {}", request);
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
        MegaDDiscoveryService.readSensorsFile(true);
        Map<String, String> properties = new HashMap<>();
        List<Channel> channelList = new ArrayList<>();
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
                    updateStatus(ThingStatus.UNINITIALIZED, ThingStatusDetail.BRIDGE_UNINITIALIZED,
                            "Bridge is offline during 10 seconds");
                    break;
                }
                reconnect++;
            }
            if (bridgeDeviceHandler.getThing().getStatus().equals(ThingStatus.ONLINE)) {
                MegaDHardware.Port mega = bridgeDeviceHandler.megaDHardware.getPort(configuration.port);
                if (mega != null) {
                    port = mega;
                    MegaDHTTPCallback.portListener.add(this);
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
                    String label = port.getEmt();
                    MegaDTypesEnum portType = port.getPty();
                    if (portType.equals(MegaDTypesEnum.IN)) {
                        if (port.getM().equals(MegaDModesEnum.C)) {
                            ChannelUID clickUID = new ChannelUID(thing.getUID(), MegaDBindingConstants.CHANNEL_CLICK);
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
                        } else if (port.getM().equals(MegaDModesEnum.P) || port.getM().equals(MegaDModesEnum.R)
                                || port.getM().equals(MegaDModesEnum.PR)) {
                            List<Channel> existingChannelList = new LinkedList<>(thing.getChannels());
                            ChannelUID inUID = new ChannelUID(thing.getUID(), MegaDBindingConstants.CHANNEL_IN);
                            Channel in = ChannelBuilder.create(inUID)
                                    .withType(new ChannelTypeUID(MegaDBindingConstants.BINDING_ID,
                                            MegaDBindingConstants.CHANNEL_IN))
                                    .withLabel(label + " Input").withAcceptedItemType("Switch").build();
                            if (existingChannelList.stream().anyMatch(cn -> cn.getUID().equals(in.getUID()))) {
                                Channel foundedChannel = existingChannelList.stream()
                                        .filter(cn -> cn.getUID().equals(in.getUID())).findFirst().get();
                                channelList.add(foundedChannel);
                                existingChannelList.remove(foundedChannel);
                            } else {
                                channelList.add(in);
                            }
                            ChannelUID lpUID = new ChannelUID(thing.getUID(), MegaDBindingConstants.CHANNEL_LONGPRESS);
                            Channel lp = ChannelBuilder.create(lpUID)
                                    .withType(new ChannelTypeUID(MegaDBindingConstants.BINDING_ID,
                                            MegaDBindingConstants.CHANNEL_LONGPRESS))
                                    .withLabel(label + " Long press Trigger").withKind(ChannelKind.TRIGGER)
                                    .withAcceptedItemType("String").build();
                            if (existingChannelList.stream().anyMatch(cn -> cn.getUID().equals(lp.getUID()))) {
                                Channel foundedChannel = existingChannelList.stream()
                                        .filter(cn -> cn.getUID().equals(lp.getUID())).findFirst().get();
                                channelList.add(foundedChannel);
                                existingChannelList.remove(foundedChannel);
                            } else {
                                channelList.add(lp);
                            }
                            ChannelUID inCountUID = new ChannelUID(thing.getUID(),
                                    MegaDBindingConstants.CHANNEL_INCOUNT);
                            Channel inCount = ChannelBuilder.create(inCountUID)
                                    .withType(new ChannelTypeUID(MegaDBindingConstants.BINDING_ID,
                                            MegaDBindingConstants.CHANNEL_INCOUNT))
                                    .withLabel(label + " Counter").withAcceptedItemType("Number").build();
                            if (existingChannelList.stream().anyMatch(cn -> cn.getUID().equals(inCount.getUID()))) {
                                Channel foundedChannel = existingChannelList.stream()
                                        .filter(cn -> cn.getUID().equals(inCount.getUID())).findFirst().get();
                                channelList.add(foundedChannel);
                                existingChannelList.remove(foundedChannel);
                            } else {
                                channelList.add(inCount);
                            }
                            ChannelUID inContactUID = new ChannelUID(thing.getUID(),
                                    MegaDBindingConstants.CHANNEL_CONTACT);
                            Channel inContact = ChannelBuilder.create(inContactUID)
                                    .withType(new ChannelTypeUID(MegaDBindingConstants.BINDING_ID,
                                            MegaDBindingConstants.CHANNEL_CONTACT))
                                    .withLabel(label + " Contact").withAcceptedItemType("Contact").build();
                            if (existingChannelList.stream().anyMatch(cn -> cn.getUID().equals(inContact.getUID()))) {
                                Channel foundedChannel = existingChannelList.stream()
                                        .filter(cn -> cn.getUID().equals(inContact.getUID())).findFirst().get();
                                channelList.add(foundedChannel);
                                existingChannelList.remove(foundedChannel);
                            } else {
                                channelList.add(inContact);
                            }
                            channelList.addAll(existingChannelList);
                            properties.put("Mode:", "P, R, P&R (checkbox enabled)");
                        }

                        if (!port.isMiscChecked().contains("1")) {
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
                    } else if (portType.equals(MegaDTypesEnum.OUT)) {
                        if (port.getM().equals(MegaDModesEnum.SW)) {
                            List<Channel> existingChannelList = new LinkedList<>(thing.getChannels());
                            ChannelUID outUID = new ChannelUID(thing.getUID(), MegaDBindingConstants.CHANNEL_OUT);
                            Channel out = ChannelBuilder.create(outUID)
                                    .withType(new ChannelTypeUID(MegaDBindingConstants.BINDING_ID,
                                            MegaDBindingConstants.CHANNEL_OUT))
                                    .withLabel(label + " Output").build();
                            if (existingChannelList.stream().anyMatch(cn -> cn.getUID().equals(out.getUID()))) {
                                Channel foundedChannel = existingChannelList.stream()
                                        .filter(cn -> cn.getUID().equals(out.getUID())).findFirst().get();
                                channelList.add(foundedChannel);
                                existingChannelList.remove(foundedChannel);
                            } else {
                                channelList.add(out);
                            }
                            channelList.addAll(existingChannelList);
                        } else if (port.getM().equals(MegaDModesEnum.PWM)) {
                            ChannelUID pwmUID = new ChannelUID(thing.getUID(), MegaDBindingConstants.CHANNEL_PWM);
                            Channel pwm = ChannelBuilder.create(pwmUID)
                                    .withType(new ChannelTypeUID(MegaDBindingConstants.BINDING_ID,
                                            MegaDBindingConstants.CHANNEL_PWM))
                                    .withLabel(label + " PWM").withAcceptedItemType("Number").build();
                            channelList.add(pwm);
                            ChannelUID dimmerUID = new ChannelUID(thing.getUID(), MegaDBindingConstants.CHANNEL_DIMMER);
                            Channel dimmer = ChannelBuilder.create(dimmerUID)
                                    .withType(new ChannelTypeUID(MegaDBindingConstants.BINDING_ID,
                                            MegaDBindingConstants.CHANNEL_DIMMER))
                                    .withLabel(label + " Dimmer").withAcceptedItemType("Dimmer").build();
                            channelList.add(dimmer);
                        } else if (port.getM().equals(MegaDModesEnum.DS2413)) {
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
                        }

                        properties.put("Mode:", port.getM().toString());
                    } else if (portType.equals(MegaDTypesEnum.ADC)) {
                        ChannelUID adcUID = new ChannelUID(thing.getUID(), MegaDBindingConstants.CHANNEL_ADC);
                        Channel adc = ChannelBuilder.create(adcUID).withType(
                                new ChannelTypeUID(MegaDBindingConstants.BINDING_ID, MegaDBindingConstants.CHANNEL_ADC))
                                .withLabel(label + " ADC").build();
                        channelList.add(adc);
                    } else if (portType.equals(MegaDTypesEnum.DSEN)) {
                        if (port.getSenType().equals(MegaDDsenEnum.DHT11)) {
                            ChannelUID dhtTempUID = new ChannelUID(thing.getUID(), MegaDBindingConstants.CHANNEL_TEMP);
                            Channel dhtTemp = ChannelBuilder.create(dhtTempUID)
                                    .withType(new ChannelTypeUID(MegaDBindingConstants.BINDING_ID,
                                            MegaDBindingConstants.CHANNEL_TEMP))
                                    .withLabel(label + " DHT 11 Temperature").withAcceptedItemType("Number:Temperature")
                                    .build();
                            channelList.add(dhtTemp);
                            ChannelUID dhtHumUID = new ChannelUID(thing.getUID(), MegaDBindingConstants.CHANNEL_HUM);
                            Channel dhtHum = ChannelBuilder.create(dhtHumUID)
                                    .withType(new ChannelTypeUID(MegaDBindingConstants.BINDING_ID,
                                            MegaDBindingConstants.CHANNEL_HUM))
                                    .withLabel(label + " DHT 11 Humidity").withAcceptedItemType("Number:Dimensionless")
                                    .build();
                            channelList.add(dhtHum);
                        } else if (port.getSenType().equals(MegaDDsenEnum.DHT22)) {
                            ChannelUID dhtTempUID = new ChannelUID(thing.getUID(), MegaDBindingConstants.CHANNEL_TEMP);
                            Channel dhtTemp = ChannelBuilder.create(dhtTempUID)
                                    .withType(new ChannelTypeUID(MegaDBindingConstants.BINDING_ID,
                                            MegaDBindingConstants.CHANNEL_TEMP))
                                    .withLabel(label + " DHT 22 Temperature").withAcceptedItemType("Number:Temperature")
                                    .build();
                            channelList.add(dhtTemp);
                            ChannelUID dhtHumUID = new ChannelUID(thing.getUID(), MegaDBindingConstants.CHANNEL_HUM);
                            Channel dhtHum = ChannelBuilder.create(dhtHumUID)
                                    .withType(new ChannelTypeUID(MegaDBindingConstants.BINDING_ID,
                                            MegaDBindingConstants.CHANNEL_HUM))
                                    .withLabel(label + " DHT 22 Humidity").withAcceptedItemType("Number:Dimensionless")
                                    .build();
                            channelList.add(dhtHum);
                        } else if (port.getSenType().equals(MegaDDsenEnum.ONEWIREBUS)) {
                            MegaDHttpHelpers httpRequest = new MegaDHttpHelpers();
                            String response = httpRequest.request("http://" + bridgeDeviceHandler.config.hostname + "/"
                                    + bridgeDeviceHandler.config.password + "/?pt=" + configuration.port + "&cmd=list")
                                    .getResponseResult();
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
                        } else if (port.getSenType().equals(MegaDDsenEnum.ONEWIRE)) {
                            ChannelUID onewireUID = new ChannelUID(thing.getUID(), MegaDBindingConstants.CHANNEL_TEMP);
                            Channel onewire = ChannelBuilder.create(onewireUID)
                                    .withType(new ChannelTypeUID(MegaDBindingConstants.BINDING_ID,
                                            MegaDBindingConstants.CHANNEL_TEMP))
                                    .withLabel(label + " 1w sensor").withAcceptedItemType("Number:Temperature").build();
                            channelList.add(onewire);
                            ChannelUID directionUID = new ChannelUID(thing.getUID(),
                                    MegaDBindingConstants.CHANNEL_DIRECTION);
                            Channel direction = ChannelBuilder.create(directionUID)
                                    .withType(new ChannelTypeUID(MegaDBindingConstants.BINDING_ID,
                                            MegaDBindingConstants.CHANNEL_DIRECTION))
                                    .withLabel(label + " direction Trigger").withKind(ChannelKind.TRIGGER)
                                    .withAcceptedItemType("String").build();
                            channelList.add(direction);
                        } else if (port.getSenType().equals(MegaDDsenEnum.IB)) {
                            ChannelUID ibuttonUID = new ChannelUID(thing.getUID(), MegaDBindingConstants.CHANNEL_IB);
                            Channel ibutton = ChannelBuilder.create(ibuttonUID)
                                    .withType(new ChannelTypeUID(MegaDBindingConstants.BINDING_ID,
                                            MegaDBindingConstants.CHANNEL_IB))
                                    .withKind(ChannelKind.TRIGGER).withLabel(label + " IButton")
                                    .withAcceptedItemType("String").build();
                            channelList.add(ibutton);
                        }
                        properties.put("Sensor:", port.getSenType().toString());
                    } else if (portType.equals(MegaDTypesEnum.I2C)) {
                        if (port.getExtenders().equals(MegaDExtendersEnum.PCA9685)) {
                            List<Channel> existingChannelList = new LinkedList<>(thing.getChannels());
                            for (int i = 0; i < 16; i++) {
                                MegaDHardware.ExtPort extPort = port.getExtPorts().get(i);
                                if (extPort != null) {
                                    if (extPort.getEty().equals(MegaDExtendedTypeEnum.SW)) {
                                        Configuration channelConfiguration = new Configuration();
                                        channelConfiguration.put("port", i);
                                        ChannelUID extInUID = new ChannelUID(thing.getUID(),
                                                MegaDBindingConstants.CHANNEL_EXTENDER_OUT + "_" + i);
                                        Channel extIn = ChannelBuilder.create(extInUID)
                                                .withType(new ChannelTypeUID(MegaDBindingConstants.BINDING_ID,
                                                        MegaDBindingConstants.CHANNEL_EXTENDER_OUT))
                                                .withLabel(label + "extender port " + i + " out")
                                                .withAcceptedItemType("Switch").withConfiguration(channelConfiguration)
                                                .build();
                                        if (existingChannelList.stream()
                                                .anyMatch(cn -> cn.getUID().equals(extIn.getUID()))) {
                                            Channel foundedChannel = existingChannelList.stream()
                                                    .filter(cn -> cn.getUID().equals(extIn.getUID())).findFirst().get();
                                            channelList.add(foundedChannel);
                                            existingChannelList.remove(foundedChannel);
                                        } else {
                                            channelList.add(extIn);
                                        }
                                    } else if (extPort.getEty().equals(MegaDExtendedTypeEnum.PWM)) {
                                        Configuration channelConfiguration = new Configuration();
                                        channelConfiguration.put("port", i);
                                        channelConfiguration.put("type", "DIMMER");
                                        ChannelUID extPwmUID = new ChannelUID(thing.getUID(),
                                                MegaDBindingConstants.CHANNEL_EXTENDER_PWM + "_" + i);
                                        Channel extPwm = ChannelBuilder.create(extPwmUID)
                                                .withType(new ChannelTypeUID(MegaDBindingConstants.BINDING_ID,
                                                        MegaDBindingConstants.CHANNEL_EXTENDER_PWM))
                                                .withLabel(label + "extender port " + i + " PWM")
                                                .withConfiguration(channelConfiguration).withAcceptedItemType("Dimmer")
                                                .build();
                                        if (existingChannelList.stream()
                                                .anyMatch(cn -> cn.getUID().equals(extPwm.getUID()))) {
                                            Channel foundedChannel = existingChannelList.stream()
                                                    .filter(cn -> cn.getUID().equals(extPwm.getUID())).findFirst()
                                                    .get();
                                            if (foundedChannel.getConfiguration().get("type") != null) {
                                                if (foundedChannel.getConfiguration().get("type").equals("PWM")) {
                                                    existingChannelList.remove(foundedChannel);
                                                    foundedChannel = ChannelBuilder.create(foundedChannel)
                                                            .withAcceptedItemType("Number").build();
                                                } else if (foundedChannel.getConfiguration().get("type")
                                                        .equals("DIMMER")) {
                                                    existingChannelList.remove(foundedChannel);
                                                    foundedChannel = ChannelBuilder.create(foundedChannel)
                                                            .withAcceptedItemType("Dimmer").build();
                                                }
                                            } else {
                                                foundedChannel = ChannelBuilder.create(foundedChannel)
                                                        .withAcceptedItemType("Dimmer").build();
                                            }
                                            channelList.add(foundedChannel);
                                            existingChannelList.remove(foundedChannel);
                                        } else {
                                            channelList.add(extPwm);
                                        }
                                    }
                                }
                            }
                            channelList.addAll(existingChannelList);
                            properties.put("Mode:", "Extender PCA9685");
                        } else if (port.getExtenders().equals(MegaDExtendersEnum.MCP230XX)) {
                            for (int i = 0; i < 16; i++) {
                                MegaDHardware.ExtPort extPort = port.getExtPorts().get(i);
                                if (extPort != null) {
                                    if (extPort.getEty().equals(MegaDExtendedTypeEnum.IN)) {
                                        Configuration channelConfiguration = new Configuration();
                                        channelConfiguration.put("port", i);
                                        ChannelUID extInUID = new ChannelUID(thing.getUID(),
                                                MegaDBindingConstants.CHANNEL_EXTENDER_IN + "_" + i);
                                        Channel extIn = ChannelBuilder.create(extInUID)
                                                .withType(new ChannelTypeUID(MegaDBindingConstants.BINDING_ID,
                                                        MegaDBindingConstants.CHANNEL_EXTENDER_IN))
                                                .withLabel(label + "extender port " + i + " in")
                                                .withAcceptedItemType("Switch").withConfiguration(channelConfiguration)
                                                .build();
                                        channelList.add(extIn);
                                    } else if (extPort.getEty().equals(MegaDExtendedTypeEnum.OUT)) {
                                        Configuration channelConfiguration = new Configuration();
                                        channelConfiguration.put("port", i);
                                        ChannelUID extInUID = new ChannelUID(thing.getUID(),
                                                MegaDBindingConstants.CHANNEL_EXTENDER_OUT + "_" + i);
                                        Channel extIn = ChannelBuilder.create(extInUID)
                                                .withType(new ChannelTypeUID(MegaDBindingConstants.BINDING_ID,
                                                        MegaDBindingConstants.CHANNEL_EXTENDER_OUT))
                                                .withLabel(label + "extender port " + i + " out")
                                                .withAcceptedItemType("Switch").withConfiguration(channelConfiguration)
                                                .build();
                                        channelList.add(extIn);
                                    }
                                    properties.put("Mode:", "Extender MCP230XX");
                                }
                            }
                        }
                        try {
                            MegaDHttpHelpers httpRequest = new MegaDHttpHelpers();
                            String response = httpRequest.request("http://" + bridgeDeviceHandler.config.hostname + "/"
                                    + bridgeDeviceHandler.config.password + "/?pt=" + configuration.port + "&cmd=scan")
                                    .getResponseResult();
                            int dStartIndex = response.indexOf("<br>") + "<br>".length();
                            String[] splittedSensors = response.substring(dStartIndex).split("<br>");
                            for (String sensor : splittedSensors) {
                                if (!sensor.isEmpty()) {
                                    sensor = sensor.substring(0, sensor.indexOf("-")).strip();
                                    if (Objects
                                            .requireNonNull(
                                                    bridgeDeviceHandler.megaDHardware.getPort(configuration.port))
                                            .getExtenders() == MegaDExtendersEnum.NC) {
                                        String finalSensor = sensor;
                                        List<Channel> lambdaCannel = new ArrayList<>();
                                        if ("0x27".equals(finalSensor)) {
                                            ChannelUID lcd1602Line1UID = new ChannelUID(thing.getUID(),
                                                    MegaDBindingConstants.CHANNEL_LINE1);
                                            Channel lcd1602Line1 = ChannelBuilder.create(lcd1602Line1UID)
                                                    .withType(new ChannelTypeUID(MegaDBindingConstants.BINDING_ID,
                                                            MegaDBindingConstants.CHANNEL_LINE1))
                                                    .withLabel(label + " " + "LCD1602 line 1")
                                                    .withAcceptedItemType("String").build();
                                            channelList.add(lcd1602Line1);
                                            ChannelUID lcd1602Line2UID = new ChannelUID(thing.getUID(),
                                                    MegaDBindingConstants.CHANNEL_LINE2);
                                            Channel lcd1602Line2 = ChannelBuilder.create(lcd1602Line2UID)
                                                    .withType(new ChannelTypeUID(MegaDBindingConstants.BINDING_ID,
                                                            MegaDBindingConstants.CHANNEL_LINE2))
                                                    .withLabel(label + " " + "LCD1602 line 2")
                                                    .withAcceptedItemType("String").build();
                                            channelList.add(lcd1602Line2);
                                        } else {
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
                                                                .withType(new ChannelTypeUID(
                                                                        MegaDBindingConstants.BINDING_ID,
                                                                        MegaDBindingConstants.CHANNEL_I2C))
                                                                .withLabel(label + " " + v.getSensorLabel() + " "
                                                                        + params.getName())
                                                                .withConfiguration(configuration)
                                                                .withAcceptedItemType(params.getOh())
                                                                .withDefaultTags(tags).build();
                                                        lambdaCannel.add(i2c);
                                                    }
                                                }
                                            });
                                            channelList.addAll(lambdaCannel);
                                        }
                                    }
                                }
                            }
                            properties.put("Mode:", "Sensor");
                        } catch (Exception e) {
                            logger.error("I2C init exception {}", e.getLocalizedMessage());
                        }
                    }
                    ThingBuilder thingBuilder = editThing();
                    thingBuilder.withChannels(channelList);
                    updateThing(thingBuilder.build());

                    properties.put("Mega URL:", "http://" + Objects.requireNonNull(bridgeDeviceHandler).config.hostname
                            + "/" + bridgeDeviceHandler.config.password + "/?pt=" + configuration.port);
                    properties.put("Type:", port.getPty().toString());
                    updateProperties(properties);
                    updateStatus(ThingStatus.ONLINE);
                } else {
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Port not found");
                }
            }

            this.bridgeDeviceHandler = bridgeDeviceHandler;
        } else {
            updateStatus(ThingStatus.UNINITIALIZED, ThingStatusDetail.BRIDGE_UNINITIALIZED, "Bridge is not defined");
        }
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
            } else if (input.contains("ib=")) {
                String[] inputSplit = input.split("&");
                for (String inputValue : inputSplit) {
                    if (inputValue.contains("ib=")) {
                        triggerChannel(MegaDBindingConstants.CHANNEL_IB, inputValue.split("=")[1]);
                    }
                }
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
                                if (channel.getConfiguration().get("invert").equals("false")) {
                                    updateState(channel.getUID().getId(), OnOffType.ON);
                                } else {
                                    updateState(channel.getUID().getId(), OnOffType.OFF);
                                }
                            } else if (value.contains("OFF")) {
                                if (channel.getConfiguration().get("invert").equals("false")) {
                                    updateState(channel.getUID().getId(), OnOffType.OFF);
                                } else {
                                    updateState(channel.getUID().getId(), OnOffType.ON);
                                }
                            }
                            break;
                        case MegaDBindingConstants.CHANNEL_INCOUNT:
                            updateState(channel.getUID().getId(), DecimalType.valueOf(value));
                            break;
                        case MegaDBindingConstants.CHANNEL_CONTACT:
                            if (value.contains("ON")) {
                                if (channel.getConfiguration().get("invert").equals("false")) {
                                    updateState(channel.getUID().getId(), OpenClosedType.CLOSED);
                                } else {
                                    updateState(channel.getUID().getId(), OpenClosedType.OPEN);
                                }
                            } else if (value.contains("OFF")) {
                                if (channel.getConfiguration().get("invert").equals("false")) {
                                    updateState(channel.getUID().getId(), OpenClosedType.OPEN);
                                } else {
                                    updateState(channel.getUID().getId(), OpenClosedType.CLOSED);
                                }
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
                                int minval = port.getPwmm();// Integer.parseInt(getThing().getConfiguration().get("min_pwm").toString());
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
                                updateState(channel.getUID().getId(), PercentType.valueOf(Integer.toString(percent)));
                            } catch (Exception ex) {
                                logger.debug("Cannot convert to dimmer values. Error: '{}'", ex.toString());
                            }
                            break;
                        case MegaDBindingConstants.CHANNEL_DIRECTION:
                            break;
                        default:
                    }
                    MegaDDeviceHandler bridgeDeviceHandler = this.bridgeDeviceHandler;
                    if (bridgeDeviceHandler != null) {
                        MegaDDsenEnum sensorDsenType = port.getSenType();
                        if (sensorDsenType.equals(MegaDDsenEnum.ONEWIREBUS)
                                || sensorDsenType.equals(MegaDDsenEnum.ONEWIRE)) {
                            try {
                                updateState(channel.getUID().getId(), DecimalType.valueOf(value));
                            } catch (Exception ignored) {
                            }
                        }
                        MegaDTypesEnum megaDTypes = port.getPty();
                        if (megaDTypes.equals(MegaDTypesEnum.I2C)) {
                            MegaDExtendersEnum megaDExtendersEnum = port.getExtenders();
                            if (megaDExtendersEnum != MegaDExtendersEnum.NC) {
                                if (megaDExtendersEnum.equals(MegaDExtendersEnum.MCP230XX)) {
                                    if (value.contains("ON")) {
                                        updateState(channel.getUID().getId(), OnOffType.ON);
                                    } else if (value.contains("OFF")) {
                                        updateState(channel.getUID().getId(), OnOffType.OFF);
                                    }
                                } else if (megaDExtendersEnum.equals(MegaDExtendersEnum.PCA9685)) {
                                    if (value.contains("ON")) {
                                        updateState(channel.getUID().getId(), OnOffType.ON);
                                    } else if (value.contains("OFF")) {
                                        updateState(channel.getUID().getId(), OnOffType.OFF);
                                    } else {
                                        try {
                                            if (channel.getConfiguration().get("type") != null) {
                                                String channelType = channel.getConfiguration().get("type").toString();
                                                if ("PWM".equals(channelType)) {
                                                    updateState(channel.getUID().getId(), DecimalType.valueOf(value));
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
                                                        int minval = port.getPwmm();// Integer.parseInt(getThing().getConfiguration().get("min_pwm").toString());
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
                                                    int minval = port.getPwmm();// Integer.parseInt(getThing().getConfiguration().get("min_pwm").toString());
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

    public void refresh() {
        MegaDHttpHelpers httpRequest = new MegaDHttpHelpers();
        MegaDDeviceHandler bridgeDeviceHandler = this.bridgeDeviceHandler;
        if ((bridgeDeviceHandler != null) && (bridgeDeviceHandler.getThing().getStatus().equals(ThingStatus.ONLINE))) {
            logger.debug("Refresh port {} at {}", configuration.port, thing.getLabel());
            MegaDTypesEnum portType = port.getPty();
            if (portType.equals(MegaDTypesEnum.DSEN)) {
                MegaDDsenEnum dDenType = port.getSenType();
                if (dDenType.equals(MegaDDsenEnum.ONEWIREBUS)) {
                    List<Channel> channels = thing.getChannels();
                    int responseCode = httpRequest.request(
                            "http://" + bridgeDeviceHandler.getThing().getConfiguration().get("hostname").toString()
                                    + "/" + bridgeDeviceHandler.getThing().getConfiguration().get("password").toString()
                                    + "/?pt=" + configuration.port + "?cmd=conv")
                            .getResponseCode();
                    if (responseCode == 200) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException ignored) {
                        }
                        String response = httpRequest.request("http://"
                                + bridgeDeviceHandler.getThing().getConfiguration().get("hostname").toString() + "/"
                                + bridgeDeviceHandler.getThing().getConfiguration().get("password").toString() + "/?pt="
                                + configuration.port + "?cmd=list").getResponseResult();
                        logger.debug("response port {} is {}", configuration.port, response);
                        String[] sensorsList = response.split(";");
                        if (!"busy".equals(response)) {
                            for (Channel channel : channels) {
                                for (String oneWireSensor : sensorsList) {
                                    if (!oneWireSensor.isEmpty()) {
                                        String address = oneWireSensor.split(":")[0];
                                        String value = oneWireSensor.split(":")[1];
                                        if (channel.getConfiguration().get("address").toString().equals(address)) {
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
                } else if (dDenType.equals(MegaDDsenEnum.ONEWIRE)) {
                    MegaDHTTPResponse response = httpRequest.request(
                            "http://" + bridgeDeviceHandler.getThing().getConfiguration().get("hostname").toString()
                                    + "/" + bridgeDeviceHandler.getThing().getConfiguration().get("password").toString()
                                    + "/?pt=" + configuration.port + "?cmd=get");
                    if (response.getResponseCode() == 200) {
                        String resp = response.getResponseResult();
                        String[] sensorsList = resp.split(":");
                        List<Channel> channels = thing.getChannels();
                        for (Channel channel : channels) {
                            if (isLinked(channel.getUID())) {
                                updateChannel(channel.getUID().getId(), sensorsList[1]);
                            }
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
                        MegaDModesEnum mode = port.getM();
                        if (mode.equals(MegaDModesEnum.PWM)) {
                            updateChannel(MegaDBindingConstants.CHANNEL_PWM, response);
                            updateChannel(MegaDBindingConstants.CHANNEL_DIMMER, response);
                        }
                    }
                }
            } else if (portType.equals(MegaDTypesEnum.I2C)) {
                MegaDExtendersEnum megaDExtendersEnum = port.getExtenders();
                if (megaDExtendersEnum.equals(MegaDExtendersEnum.MCP230XX)) {
                    String response = httpRequest
                            .request("http://" + bridgeDeviceHandler.config.hostname + "/"
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
                } else if (megaDExtendersEnum.equals(MegaDExtendersEnum.PCA9685)) {
                    String response = httpRequest
                            .request("http://" + bridgeDeviceHandler.config.hostname + "/"
                                    + bridgeDeviceHandler.config.password + "/?pt=" + configuration.port + "&cmd=get")
                            .getResponseResult();
                    String[] portsStatus = response.split(";");
                    List<Channel> channels = thing.getChannels();
                    for (Channel channel : channels) {
                        if (channel.getConfiguration().get("port") != null) {
                            BigDecimal port = (BigDecimal) channel.getConfiguration().get("port");
                            if (portsStatus.length != 1) {
                                if (!portsStatus[port.intValue()].isEmpty()) {
                                    updateChannel(channel.getUID().getId(), portsStatus[port.intValue()]);
                                }
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
                            String response = httpRequest
                                    .request("http://" + bridgeDeviceHandler.config.hostname + "/"
                                            + bridgeDeviceHandler.config.password + "/?pt=" + configuration.port
                                            + "&scl="
                                            + Objects.requireNonNull(
                                                    bridgeDeviceHandler.megaDHardware.getPort(configuration.port))
                                                    .getScl()
                                            + "&i2c_dev=" + sensortype + "&" + sensorPath)
                                    .getResponseResult();
                            try {
                                Thread.sleep(200);
                            } catch (InterruptedException ignored) {
                            }
                            updateChannel(channel.getUID().getId(), response);
                        }
                        if (channel.getConfiguration().get("port") != null) {
                            String response = httpRequest
                                    .request("http://" + bridgeDeviceHandler.config.hostname + "/"
                                            + bridgeDeviceHandler.config.password + "/?pt=" + configuration.port
                                            + "&ext=" + channel.getConfiguration().get("port").toString() + "cmg=get")
                                    .getResponseResult();
                            updateChannel(channel.getUID().getId(), response);
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

    // private String readLabel(final String urlAsString) {
    // final StringBuilder content = new StringBuilder();
    // BufferedReader reader = null;
    // InputStream inputStream = null;
    // try {
    // final URL url = new URL(urlAsString);
    // inputStream = url.openStream();
    // reader = new BufferedReader(new InputStreamReader(inputStream, "Windows-1251"));
    //
    // String inputLine;
    // while ((inputLine = reader.readLine()) != null) {
    // content.append(inputLine);
    // }
    // } catch (final IOException ignored) {
    // }
    // return content.toString();
    // }

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
