/**
 * Copyright (c) 2014 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.megad.handler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.OpenClosedType;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.megad.MegaDBindingConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link MegadHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Petr Shatsillo - Initial contribution
 */
public class MegaDHandler extends BaseThingHandler {

    private Logger logger = LoggerFactory.getLogger(MegaDHandler.class);

    private ScheduledFuture<?> refreshPollingJob;

    MegaDBridgeHandler bridgeHandler;

    public MegaDHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        int state = 0;

        String Result = "";

        if (channelUID.getId().equals(MegaDBindingConstants.CHANNEL_OUT)) {
            if (command.toString().equals("ON")) {
                state = 1;
            } else if (command.toString().equals("OFF")) {
                state = 0;
            }
            Result = "http://" + getThing().getConfiguration().get("hostname").toString() + "/"
                    + getThing().getConfiguration().get("password").toString() + "/?cmd="
                    + getThing().getConfiguration().get("port").toString() + ":" + state;
            logger.info("Switch: {}", Result);
            sendCommand(Result);

        } else if (channelUID.getId().equals(MegaDBindingConstants.CHANNEL_DIMMER)) {
            int result = (int) Math.round(Integer.parseInt(command.toString()) * 2.55);
            Result = "http://" + getThing().getConfiguration().get("hostname").toString() + "/"
                    + getThing().getConfiguration().get("password").toString() + "/?cmd="
                    + getThing().getConfiguration().get("port").toString() + ":" + result;
            logger.info("Dimmer: {}", Result);
            sendCommand(Result);
        }
        // updateStatus(ThingStatus.ONLINE);
    }

    public void sendCommand(String Result) {

        HttpURLConnection con;

        URL MegaURL;

        try {
            MegaURL = new URL(Result);
            con = (HttpURLConnection) MegaURL.openConnection();
            // optional default is GET
            // con.setReadTimeout(500);
            con.setRequestMethod("GET");

            // add request header
            con.setRequestProperty("User-Agent", "Mozilla/5.0");
            if (con.getResponseCode() == 200) {
                logger.debug("OK");
            }
            con.disconnect();
        } catch (MalformedURLException e) {
            logger.error("1" + e);
            e.printStackTrace();
        } catch (ProtocolException e) {
            logger.error("2" + e);
            e.printStackTrace();
        } catch (IOException e) {
            logger.error("Connect to megadevice " + getThing().getConfiguration().get("hostname").toString()
                    + " error: " + e.getLocalizedMessage());
        }

    }

    public void updateValues(String hostAddress, String[] getCommands, OnOffType OnOff) {
        // logger.debug("{},{},{}", hostAddress, getCommands, OnOff);
        // logger.debug("getThing() -> {}", getThing().getUID().getId());
        // logger.debug("getActiveChannelListAsString -> {}", getActiveChannelListAsString());
        for (Channel channel : getThing().getChannels()) {

            if (isLinked(channel.getUID().getId())) {
                if ((channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_IN))
                        || (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_OUT))) {
                    updateState(channel.getUID().getId(), OnOff);
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_IB)) {
                    updateState(channel.getUID().getId(), StringType.valueOf(getCommands[4]));
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_AT)) {
                    updateState(channel.getUID().getId(), DecimalType.valueOf(getCommands[2]));
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_ST)) {
                    updateState(channel.getUID().getId(), DecimalType.valueOf(getCommands[2]));
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_ADC)) {
                    updateState(channel.getUID().getId(), DecimalType.valueOf(getCommands[2]));
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_INCOUNT)) {

                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_CONTACT)) {
                    if (OnOff.name() == "ON") {
                        updateState(channel.getUID().getId(), OpenClosedType.CLOSED);
                    } else if (OnOff.name() == "OFF") {
                        updateState(channel.getUID().getId(), OpenClosedType.OPEN);
                    }
                } else {
                    try {
                        updateState(channel.getUID().getId(), DecimalType.valueOf(getCommands[2]));
                    } catch (Exception ex) {
                        logger.error("Error: value is not Decimal!!");
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
    public void initialize() {
        bridgeHandler = getBridgeHandler();
        logger.debug("Thing Handler for {} started", getThing().getUID().getId());
        registerMegadThingListener(bridgeHandler);

        int pollingPeriod = Integer.parseInt(getThing().getConfiguration().get("refresh").toString());
        if (pollingPeriod != 0) {
            if (refreshPollingJob == null || refreshPollingJob.isCancelled()) {
                refreshPollingJob = scheduler.scheduleWithFixedDelay(new Runnable() {
                    @Override
                    public void run() {
                        updateData();
                    }
                }, 0, pollingPeriod, TimeUnit.SECONDS);
            }
        }

    }

    protected void updateData() {
        logger.debug("Updating Megadevice things...");
        String[] Result = {
                "http://" + getThing().getConfiguration().get("hostname").toString() + "/"
                        + getThing().getConfiguration().get("password").toString() + "/?pt="
                        + getThing().getConfiguration().get("port").toString() + "&cmd=get",
                "http://" + getThing().getConfiguration().get("hostname").toString() + "/"
                        + getThing().getConfiguration().get("password").toString() + "/?tget=1",
                "http://" + getThing().getConfiguration().get("hostname").toString() + "/"
                        + getThing().getConfiguration().get("password").toString() + "/?pt="
                        + getThing().getConfiguration().get("port").toString() + "&scl="
                        + getThing().getConfiguration().get("scl").toString() + "&i2c_dev="
                        + getThing().getConfiguration().get("i2c_dev").toString() };

        for (Channel channel : getThing().getChannels()) {
            if (isLinked(channel.getUID().getId())) {
                if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_TGET)) {
                    Result[0] = "";
                    Result[2] = "";
                }
            } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_I2C)) {
                Result[0] = "";
                Result[1] = "";
            } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_ST)) {
                return;
            }

            String updateRequest = sendRequest(Result);

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

                    updateState(channel.getUID().getId(), DecimalType.valueOf(value[1]));
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
                    int percent = (int) Math.round(Integer.parseInt(updateRequest) / 2.55);
                    updateState(channel.getUID().getId(), PercentType.valueOf(Integer.toString(percent)));
                    logger.debug(getThing().getUID().getId() + " " + percent);
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_DHTTEMP)) {
                    String[] ResponseParse = updateRequest.split("[:/]");
                    if (ResponseParse.length > 2) {
                        if (ResponseParse[0].contains("temp")) {
                            updateState(channel.getUID().getId(), DecimalType.valueOf(ResponseParse[1]));
                        }
                    } else {
                        updateState(channel.getUID().getId(), DecimalType.valueOf(ResponseParse[0]));
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_DHTHUM)) {
                    String[] ResponseParse = updateRequest.split("[:/]");
                    if (ResponseParse.length > 2) {
                        if (ResponseParse[2].contains("hum")) {
                            updateState(channel.getUID().getId(), DecimalType.valueOf(ResponseParse[3]));
                        }
                    } else {
                        updateState(channel.getUID().getId(), DecimalType.valueOf(ResponseParse[1]));
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_TGET)) {
                    // Result[1];

                    updateState(channel.getUID().getId(), DecimalType.valueOf(updateRequest));
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_ADC)) {
                    updateState(channel.getUID().getId(), DecimalType.valueOf(updateRequest));
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_ONEWIRE)) {
                    String[] ResponseParse = updateRequest.split("[:]");
                    if (ResponseParse.length > 1) {
                        logger.debug(ResponseParse[1]);
                        if (!(updateRequest.equals("NA"))) {
                            updateState(channel.getUID().getId(), DecimalType.valueOf(ResponseParse[1]));
                        }
                    } else {
                        if (!(updateRequest.equals("NA"))) {
                            updateState(channel.getUID().getId(), DecimalType.valueOf(updateRequest));
                        }
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_I2C)) {
                    // Result =;

                    updateState(channel.getUID().getId(), DecimalType.valueOf(updateRequest));
                }
            }
        }
    }

    private void registerMegadThingListener(MegaDBridgeHandler bridgeHandler) {
        if (bridgeHandler != null) {
            bridgeHandler.registerMegadThingListener(this);
        } else {
            logger.debug("Can't register {} at bridge bridgeHandler is null.", this.getThing().getUID());
        }
    }

    private synchronized MegaDBridgeHandler getBridgeHandler() {

        Bridge bridge = getBridge();
        if (bridge == null) {
            logger.debug("Required bridge not defined for device {}.");
            return null;
        } else {
            return getBridgeHandler(bridge);
        }

    }

    private synchronized MegaDBridgeHandler getBridgeHandler(Bridge bridge) {

        MegaDBridgeHandler bridgeHandler = null;

        ThingHandler handler = bridge.getHandler();
        if (handler instanceof MegaDBridgeHandler) {
            bridgeHandler = (MegaDBridgeHandler) handler;
        } else {
            logger.debug("No available bridge handler found yet. Bridge: {} .", bridge.getUID());
            bridgeHandler = null;
        }
        return bridgeHandler;
    }

    public String getActiveChannelListAsString() {
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
        return channelList;
    }

    @Override
    protected Configuration editConfiguration() {
        logger.debug("config changed");
        if (refreshPollingJob != null && !refreshPollingJob.isCancelled()) {
            refreshPollingJob.cancel(true);
            refreshPollingJob = null;
        }
        return super.editConfiguration();
    }

    @Override
    public void dispose() {
        logger.debug("Thing Handler for {} stop", getThing().getUID().getId());
        if (refreshPollingJob != null && !refreshPollingJob.isCancelled()) {
            refreshPollingJob.cancel(true);
            refreshPollingJob = null;
        }
        unregisterMegadThingListener(bridgeHandler);
    }

    private void unregisterMegadThingListener(MegaDBridgeHandler bridgeHandler) {
        logger.debug("unregister");
        if (bridgeHandler != null) {
            bridgeHandler.unregisterThingListener(this);
        } else {
            logger.debug("Can't unregister {} at bridge bridgeHandler is null.", this.getThing().getUID());
        }

    }

    private String sendRequest(String[] URL) {
        String result = "";
        if (getThing().getConfiguration().get("i2c_par") != null) {
            URL[2] += "&i2c_par=" + getThing().getConfiguration().get("i2c_par").toString();
        }
        try {
            URL obj = new URL(URL[0]);
            HttpURLConnection con;

            con = (HttpURLConnection) obj.openConnection();

            logger.debug(URL[0]);

            con.setRequestMethod("GET");
            // con.setReadTimeout(500);

            // add request header
            con.setRequestProperty("User-Agent", "Mozilla/5.0");

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            logger.debug("input string-> {}", response.toString());
        } catch (IOException e) {
            logger.error("Connect to megadevice " + getThing().getConfiguration().get("hostname").toString()
                    + " error: " + e.getLocalizedMessage());
        }
        return result;
    }
}
