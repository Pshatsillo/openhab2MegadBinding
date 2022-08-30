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
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.megad.MegaDBindingConstants;
import org.openhab.binding.megad.internal.MegaHttpHelpers;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.OpenClosedType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.library.types.StringType;
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
 * The {@link MegaDPortsHandler} is responsible for standart features of megsd
 *
 * @author Petr Shatsillo - Initial contribution
 */
@NonNullByDefault
public class MegaDPortsHandler extends BaseThingHandler {

    private Logger logger = LoggerFactory.getLogger(MegaDPortsHandler.class);

    private @Nullable ScheduledFuture<?> refreshPollingJob;
    @Nullable
    MegaDBridgeDeviceHandler bridgeDeviceHandler;
    protected long lastRefresh = 0;
    boolean startup = true;
    protected int dimmervalue = 150;
    int smooth;

    public MegaDPortsHandler(Thing thing) {
        super(thing);
    }

    @SuppressWarnings("null")
    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        int state = 0;
        String result = "";
        if (channelUID.getId().equals(MegaDBindingConstants.CHANNEL_OUT)) {
            if (!command.toString().equals("REFRESH")) {
                if (command.toString().equals("ON")) {
                    state = 1;
                } else if (command.toString().equals("OFF")) {
                    state = 0;
                }
                assert bridgeDeviceHandler != null;
                result = "http://" + bridgeDeviceHandler.getThing().getConfiguration().get("hostname").toString() + "/"
                        + bridgeDeviceHandler.getThing().getConfiguration().get("password").toString() + "/?cmd="
                        + getThing().getConfiguration().get("port").toString() + ":" + state;
                logger.debug("Switch: {}", result);
                sendCommand(result);
            }
        } else if (channelUID.getId().equals(MegaDBindingConstants.CHANNEL_DS2413)) {
            if (command.toString().equals("ON")) {
                state = 1;
            } else if (command.toString().equals("OFF")) {
                state = 0;
            }
            result = "http://" + bridgeDeviceHandler.getThing().getConfiguration().get("hostname").toString() + "/"
                    + bridgeDeviceHandler.getThing().getConfiguration().get("password").toString() + "/?cmd="
                    + getThing().getConfiguration().get("port").toString()
                    + getThing().getConfiguration().get("ds2413_ch") + ":" + state;
            logger.info("Switch: {}", result);
            sendCommand(result);

        } else if (channelUID.getId().equals(MegaDBindingConstants.CHANNEL_DIMMER)) {
            if (!command.toString().equals("REFRESH")) {
                try {
                    int uivalue = Integer.parseInt(command.toString().split("[.]")[0]);
                    int resultInt = 0;
                    if (uivalue != 0) {
                        int minval = Integer.parseInt(getThing().getConfiguration().get("min_pwm").toString());
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
                    if (isLinked(getThing().getChannel(MegaDBindingConstants.CHANNEL_SMOOTH).getUID().getId())) {
                        logger.debug("Smooth linked");
                    } else {
                        logger.debug("Smooth unlinked");
                    }

                    result = "http://" + bridgeDeviceHandler.getThing().getConfiguration().get("hostname").toString()
                            + "/" + bridgeDeviceHandler.getThing().getConfiguration().get("password").toString()
                            + "/?cmd=" + getThing().getConfiguration().get("port").toString() + ":" + resultInt;
                    logger.info("Dimmer: {}", result);
                    sendCommand(result);
                } catch (Exception e) {
                    if (command.toString().equals("OFF")) {
                        assert bridgeDeviceHandler != null;
                        result = "http://"
                                + bridgeDeviceHandler.getThing().getConfiguration().get("hostname").toString() + "/"
                                + bridgeDeviceHandler.getThing().getConfiguration().get("password").toString()
                                + "/?cmd=" + getThing().getConfiguration().get("port").toString() + ":0";
                        logger.info("Dimmer set to OFF");
                        sendCommand(result);
                        updateState(channelUID.getId(), PercentType.valueOf("0"));
                    } else if (command.toString().equals("ON")) {
                        assert bridgeDeviceHandler != null;
                        result = "http://"
                                + bridgeDeviceHandler.getThing().getConfiguration().get("hostname").toString() + "/"
                                + bridgeDeviceHandler.getThing().getConfiguration().get("password").toString()
                                + "/?cmd=" + getThing().getConfiguration().get("port").toString() + ":" + dimmervalue;
                        logger.info("Dimmer restored to previous value: {}", result);
                        sendCommand(result);
                        int percent = 0;
                        try {
                            percent = (int) Math.round(dimmervalue / 2.55);
                        } catch (Exception ex) {
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
                    assert bridgeDeviceHandler != null;
                    result = "http://" + bridgeDeviceHandler.getThing().getConfiguration().get("hostname").toString()
                            + "/" + bridgeDeviceHandler.getThing().getConfiguration().get("password").toString()
                            + "/?cmd=" + getThing().getConfiguration().get("port").toString() + ":" + uivalue;
                    logger.info("PWM: {}", result);
                    sendCommand(result);
                } catch (Exception e) {
                    assert bridgeDeviceHandler != null;
                    result = "http://" + bridgeDeviceHandler.getThing().getConfiguration().get("hostname").toString()
                            + "/" + bridgeDeviceHandler.getThing().getConfiguration().get("password").toString()
                            + "/?cmd=" + getThing().getConfiguration().get("port").toString() + ":" + dimmervalue;
                    logger.info("PWM restored to previous value: {}", result);
                    sendCommand(result);
                    updateState(channelUID.getId(), DecimalType.valueOf(Integer.toString(dimmervalue)));
                }
            }
        }
    }

    @SuppressWarnings("null")
    @Override
    public void initialize() {
        bridgeDeviceHandler = getBridgeHandler();
        if (bridgeDeviceHandler != null) {
            registerMegadPortsListener(bridgeDeviceHandler);
        } else {
            logger.debug("Can't register {} at bridge. BridgeHandler is null.", this.getThing().getUID());
        }
        String[] rr = { getThing().getConfiguration().get("refresh").toString() };// .split("[.]");
        logger.debug("Thing {}, refresh interval is {} sec", getThing().getUID().toString(), rr[0]);
        float msec = Float.parseFloat(rr[0]);
        int pollingPeriod = (int) (msec * 1000);
        if (refreshPollingJob == null || refreshPollingJob.isCancelled()) {
            refreshPollingJob = scheduler.scheduleWithFixedDelay(new Runnable() {
                @Override
                public void run() {
                    refresh(pollingPeriod);
                }
            }, 0, 100, TimeUnit.MILLISECONDS);
        }
    }

    @SuppressWarnings("null")
    public void refresh(int interval) {
        long now = System.currentTimeMillis();
        if (startup) {
            int counter = 0;
            if (bridgeDeviceHandler != null) {
                while ((bridgeDeviceHandler
                        .getPortsvalues(getThing().getConfiguration().get("port").toString())[2] == null)
                        && (counter != 300)) {
                    String[] portStatus = bridgeDeviceHandler
                            .getPortsvalues(getThing().getConfiguration().get("port").toString());
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        logger.error("{}", e.getMessage());
                    }
                    logger.debug("waiting for value");
                    logger.debug("Port status of {} at startup is {}", getThing().getUID().toString(), portStatus);
                    counter++;
                }
                String[] portStatus = bridgeDeviceHandler
                        .getPortsvalues(getThing().getConfiguration().get("port").toString());
                logger.debug("Port status of {} at startup is {}", getThing().getUID().toString(), portStatus);
                assert portStatus[2] != null;
                try {
                    if (portStatus[2].contains("ON")) {
                        updateValues(portStatus, OnOffType.ON);
                    } else {
                        updateValues(portStatus, OnOffType.OFF);
                    }
                } catch (Exception e) {
                    logger.debug("cannot set value for thing {}", getThing().getUID().toString());
                }
            }

            startup = false;
        }
        if (interval != 0) {
            if (now >= (lastRefresh + interval)) {
                updateData();
                lastRefresh = now;
            }
        }
    }

    private void registerMegadPortsListener(@Nullable MegaDBridgeDeviceHandler bridgeHandler) {
        if (bridgeHandler != null) {
            bridgeHandler.registerMegadPortsListener(this);
        }
    }

    private synchronized @Nullable MegaDBridgeDeviceHandler getBridgeHandler() {
        Bridge bridge = getBridge();
        if (bridge == null) {
            logger.error("Required bridge not defined for device.");
            return null;
        } else {
            return getBridgeHandler(bridge);
        }
    }

    private synchronized @Nullable MegaDBridgeDeviceHandler getBridgeHandler(Bridge bridge) {
        ThingHandler handler = bridge.getHandler();
        if (handler instanceof MegaDBridgeDeviceHandler) {
            return (MegaDBridgeDeviceHandler) handler;
        } else {
            logger.debug("No available bridge handler found yet. Bridge: {} .", bridge.getUID());
            return null;
        }
    }

    @SuppressWarnings({ "null" })
    protected void updateData() {
        logger.debug("Updating Megadevice thing {}...", getThing().getUID().toString());
        String result = "http://" + getBridgeHandler().getThing().getConfiguration().get("hostname").toString() + "/"
                + getBridgeHandler().getThing().getConfiguration().get("password").toString() + "/?pt="
                + getThing().getConfiguration().get("port").toString() + "&cmd=get";
        String updateRequest = MegaHttpHelpers.sendRequest(result);

        for (Channel channel : getThing().getChannels()) {
            if (isLinked(channel.getUID().getId())) {
                if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_ST)) {
                    return;
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_IN)) {
                    if (updateRequest.contains("ON")) {
                        updateState(channel.getUID().getId(), OnOffType.ON);
                    } else if (updateRequest.contains("OFF")) {
                        updateState(channel.getUID().getId(), OnOffType.OFF);
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_INCOUNT)) {
                    String[] value = updateRequest.split("[/]");
                    for (int i = 0; i < value.length; i++) {

                        logger.debug("{} - {}", i, value[i]);
                    }
                    try {
                        if (value.length == 2) {
                            updateState(channel.getUID().getId(), DecimalType.valueOf(value[1]));
                        } else if (value.length == 3) {
                            updateState(channel.getUID().getId(), DecimalType.valueOf(value[2]));
                        }
                    } catch (Exception ex) {
                        logger.debug("this is not inputs count!");
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_OUT)) {
                    if (updateRequest.contains("ON")) {
                        updateState(channel.getUID().getId(), OnOffType.ON);
                    } else if (updateRequest.contains("OFF")) {
                        updateState(channel.getUID().getId(), OnOffType.OFF);
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_CONTACT)) {
                    if (updateRequest.contains("ON")) {
                        updateState(channel.getUID().getId(), OpenClosedType.CLOSED);
                    } else if (updateRequest.contains("OFF")) {
                        updateState(channel.getUID().getId(), OpenClosedType.OPEN);
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_DIMMER)) {
                    if ("0".equals(updateRequest)) {
                        logger.debug("dimmer value is 0, do not save dimmer value");
                        updateState(channel.getUID().getId(), PercentType.valueOf(Integer.toString(0)));
                        return;
                    } else {
                        try {
                            dimmervalue = Integer.parseInt(updateRequest);
                        } catch (Exception ignored) {
                        }
                    }
                    int percent = 0;
                    try {
                        int minval = Integer.parseInt(getThing().getConfiguration().get("min_pwm").toString());
                        if (minval != 0) {
                            int realval = (Integer.parseInt(updateRequest) - minval);// * 0.01;
                            double divVal = (255 - minval) * 0.01;
                            percent = (int) Math.round(realval / divVal);
                        } else {
                            percent = (int) Math.round(Integer.parseInt(updateRequest) / 2.55);
                        }
                    } catch (Exception ex) {
                        logger.debug("Cannot convert to dimmer values string: '{}'", updateRequest);
                    }
                    updateState(channel.getUID().getId(), PercentType.valueOf(Integer.toString(percent)));
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_PWM)) {
                    try {
                        updateState(channel.getUID().getId(),
                                PercentType.valueOf(Integer.toString(Integer.parseInt(updateRequest))));
                    } catch (Exception e) {
                        logger.debug("Cannot update PWM value");
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_TGET)) {
                    try {
                        String tempresult = "   http://"
                                + getBridgeHandler().getThing().getConfiguration().get("hostname").toString() + "/"
                                + getBridgeHandler().getThing().getConfiguration().get("password").toString()
                                + "/?tget=1 ";
                        updateState(channel.getUID().getId(),
                                DecimalType.valueOf(MegaHttpHelpers.sendRequest(tempresult)));
                    } catch (Exception ex) {
                        logger.debug("Cannot update TGET value at channel: '{}'", channel.getUID().getId());
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_ADC)) {
                    try {
                        updateState(channel.getUID().getId(), DecimalType.valueOf(updateRequest));
                    } catch (Exception ex) {
                        logger.debug("Cannot update ADC value at channel: '{}'", channel.getUID().getId());
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_ONEWIRE)) {
                    String[] responseParse = updateRequest.split("[:]");
                    if (responseParse.length > 1) {
                        logger.debug("{}", responseParse[1]);
                        if (!("NA".equals(updateRequest))) {
                            try {
                                updateState(channel.getUID().getId(), DecimalType.valueOf(responseParse[1]));
                            } catch (Exception ex) {
                                logger.debug("Cannot update One wire temperature at channel: '{}'",
                                        channel.getUID().getId());
                            }
                        }
                    } else {
                        if (!("NA".equals(updateRequest))) {
                            try {
                                updateState(channel.getUID().getId(), DecimalType.valueOf(updateRequest));
                            } catch (Exception ex) {
                                logger.debug("Cannot update One wire temperature at channel: '{}'",
                                        channel.getUID().getId());
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

    public void updateValues(String[] getCommands, @Nullable OnOffType OnOff) {
        logger.debug("updateValues of thing {}: {},{}", getThing().getUID().toString(), getCommands, OnOff);
        // logger.debug("getThing() -> {}" ng().getUID().getId());
        logger.debug("thing {}, active Channels is -> {}", getThing().getUID().toString(),
                getActiveChannelListAsString());
        int counter = 0;
        while ((getActiveChannelListAsString() == null) && (counter != 10)) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                logger.error("{}", e.getMessage());
            }
            counter++;
            logger.warn("thing {} has no active channels ", getThing().getUID().toString());
        }

        for (Channel channel : getThing().getChannels()) {
            if (isLinked(channel.getUID().getId())) {
                if ((channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_IN))
                        || (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_OUT))) {
                    if (OnOff != null) {
                        updateState(channel.getUID().getId(), OnOff);
                        if (Boolean.parseBoolean(this.getThing().getConfiguration().get("correction").toString())) {
                            String result = "http://"
                                    + getBridgeHandler().getThing().getConfiguration().get("hostname").toString() + "/"
                                    + getBridgeHandler().getThing().getConfiguration().get("password").toString()
                                    + "/?pt=" + getThing().getConfiguration().get("port").toString() + "&cmd=get";
                            try {
                                String updateRequest = MegaHttpHelpers.sendRequest(result);
                                updateState(channel.getUID().getId(), OnOffType.valueOf(updateRequest));
                            } catch (Exception ex) {
                                logger.debug("connect error");
                            }
                        } else {
                            updateState(channel.getUID().getId(), OnOff);
                        }
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_M2)) {
                    try {
                        if (getCommands[3].equals("2") && getCommands[2].equals("m")) {
                            updateState(channel.getUID().getId(), OnOffType.ON);
                        } else if (OnOff == OnOffType.OFF) {
                            updateState(channel.getUID().getId(), OnOffType.OFF);
                        }
                    } catch (Exception e) {
                        logger.debug(" Not m2 signal {}", e.getLocalizedMessage());
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_CLICK)) {
                    try {
                        if (getCommands[2].equals("click")) {
                            try {
                                updateState(channel.getUID().getId(), DecimalType.valueOf(getCommands[3]));
                            } catch (Exception ex) {
                                logger.debug(" Cannot update click {}", ex.getLocalizedMessage());
                            }
                        }
                    } catch (Exception ex) {
                        logger.debug(" Cannot update click {}", ex.getLocalizedMessage());
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_DIMMER)) {
                    if (!getCommands[0].equals("st")) {
                        try {
                            if (getCommands[2].equals("0")) {
                                logger.debug("dimmer value is 0, do not save dimmer value");
                                updateState(channel.getUID().getId(), PercentType.valueOf(Integer.toString(0)));
                                return;
                            } else {
                                dimmervalue = Integer.parseInt(getCommands[2]);
                            }
                        } catch (Exception ignored) {
                        }

                        int percent = 0;
                        try {
                            int minval = Integer.parseInt(getThing().getConfiguration().get("min_pwm").toString());
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
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_PWM)) {
                    if (!getCommands[0].equals("st")) {
                        try {
                            updateState(channel.getUID().getId(), DecimalType.valueOf(getCommands[2]));
                        } catch (Exception ignored) {
                        }
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_IB)) {
                    try {
                        updateState(channel.getUID().getId(), StringType.valueOf(getCommands[3]));
                    } catch (Exception ignored) {
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_WIEGAND)) {
                    try {
                        updateState(channel.getUID().getId(), StringType.valueOf(getCommands[3]));
                    } catch (Exception ignored) {
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_AT)) {
                    try {
                        updateState(channel.getUID().getId(), DecimalType.valueOf(getCommands[2]));
                    } catch (Exception ignored) {
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_ADC)) {
                    try {
                        updateState(channel.getUID().getId(), DecimalType.valueOf(getCommands[3]));
                    } catch (Exception ex) {
                        try {
                            updateState(channel.getUID().getId(), DecimalType.valueOf(getCommands[2]));
                        } catch (Exception ignored) {
                        }
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_INCOUNT)) {
                    try {
                        if (getCommands[2].equals("cnt")) {
                            updateState(channel.getUID().getId(), DecimalType.valueOf(getCommands[3]));
                        } else if (getCommands[4].equals("cnt")) {
                            updateState(channel.getUID().getId(), DecimalType.valueOf(getCommands[5]));
                        } else if (getCommands[2].contains("/")) {
                            String[] cnt = getCommands[2].split("/");
                            updateState(channel.getUID().getId(), DecimalType.valueOf(cnt[1]));
                        }
                    } catch (Exception ex) {
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_ONEWIRE)) {
                    logger.debug("Does not accept incoming values");
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_SMS_PHONE)) {
                    try {
                        updateState(channel.getUID().getId(), StringType.valueOf(getCommands[1]));
                    } catch (Exception ex) {
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_SMS_TEXT)) {
                    try {
                        updateState(channel.getUID().getId(), StringType.valueOf(getCommands[3]));
                    } catch (Exception ex) {
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_CONTACT)) {
                    if (OnOff != null) {
                        if (OnOff.name().equals("ON")) {
                            updateState(channel.getUID().getId(), OpenClosedType.CLOSED);
                        } else if (OnOff.name().equals("OFF")) {
                            updateState(channel.getUID().getId(), OpenClosedType.OPEN);
                        }
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_ST)) {
                    try {
                        updateState(channel.getUID().getId(), DecimalType.valueOf(getCommands[1]));
                    } catch (Exception ex) {
                    }
                } else {
                    try {
                        updateState(channel.getUID().getId(), DecimalType.valueOf(getCommands[4]));
                    } catch (Exception ex) {
                    }
                }
            }
        }
    }

    @SuppressWarnings("null")
    @Override
    public void dispose() {
        if (refreshPollingJob != null && !refreshPollingJob.isCancelled()) {
            refreshPollingJob.cancel(true);
            refreshPollingJob = null;
        }
        if (bridgeDeviceHandler != null) {
            bridgeDeviceHandler.unregisterMegaDPortsListener(this);
        }
        super.dispose();
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
            logger.error("Connect to megadevice {}  error: {} ",
                    bridgeDeviceHandler.getThing().getConfiguration().get("hostname").toString(),
                    e.getLocalizedMessage());
        }
    }

    public @Nullable String getActiveChannelListAsString() {
        String channelList = "";
        for (Channel channel : getThing().getChannels()) {
            // logger.debug("Channel ID {}", channel.getUID().getId());
            if (isLinked(channel.getUID().getId())) {
                if (channelList.length() > 0) {
                    channelList = channelList + "," + channel.getUID().getId();
                } else {
                    channelList = channel.getUID().getId();
                }
            }
        }
        if ("".equals(channelList)) {
            return null;
        } else {
            return channelList;
        }
    }
}
