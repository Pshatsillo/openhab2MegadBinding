/**
 * Copyright (c) 2014 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.megadonetry.handler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.OnOffType;
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
import org.openhab.binding.megadonetry.MegadOneTryBindingConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link MegadOneTryHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Petr Shatsillo - Initial contribution
 */
public class MegadOneTryHandler extends BaseThingHandler {

    private Logger logger = LoggerFactory.getLogger(MegadOneTryHandler.class);

    private ScheduledFuture<?> refreshPollingJob;

    MegadOneTryBridgeHandler bridgeHandler;

    public MegadOneTryHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {

        int state = 0;
        HttpURLConnection con;
        String Result = "";

        if (channelUID.getId().equals(MegadOneTryBindingConstants.CHANNEL_OUT)) {
            if (command.toString().equals("ON")) {
                state = 1;
            } else if (command.toString().equals("OFF")) {
                state = 0;
            }

            Result = "http://" + getThing().getConfiguration().get("hostname").toString() + "/"
                    + getThing().getConfiguration().get("password").toString() + "/?cmd="
                    + getThing().getConfiguration().get("port").toString() + ":" + state;
            logger.info("Switch: {}", Result);

        } else if (channelUID.getId().equals(MegadOneTryBindingConstants.CHANNEL_DIMMER)) {

            int result = (int) Math.round(Integer.parseInt(command.toString()) * 2.55);
            Result = "http://" + getThing().getConfiguration().get("hostname").toString() + "/"
                    + getThing().getConfiguration().get("password").toString() + "/?cmd="
                    + getThing().getConfiguration().get("port").toString() + ":" + result;
            logger.info("dimmer: {}", Result);
        } else {

        }

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
            logger.debug("1" + e);
            e.printStackTrace();
        } catch (ProtocolException e) {
            logger.debug("2" + e);
            e.printStackTrace();
        } catch (IOException e) {
            logger.debug(e.getLocalizedMessage());
            e.printStackTrace();
        }

    }

    public void updateValues(String hostAddress, String[] getCommands, OnOffType OnOff) {
        // logger.debug("{},{},{}", hostAddress, getCommands, OnOff);
        logger.debug("{}", getThing().getUID().getId());
        logger.debug("{}", getActiveChannelListAsString());

        if ((getActiveChannelListAsString().equals(MegadOneTryBindingConstants.CHANNEL_IN))
                || (getActiveChannelListAsString().equals(MegadOneTryBindingConstants.CHANNEL_OUT))) {
            updateState(getActiveChannelListAsString(), OnOff);
        } else if (getActiveChannelListAsString().equals(MegadOneTryBindingConstants.CHANNEL_IB)) {
            updateState(getActiveChannelListAsString(), StringType.valueOf(getCommands[4]));
        } else {
            try {
                updateState(getActiveChannelListAsString(), DecimalType.valueOf(getCommands[2]));
            } catch (Exception ex) {
                logger.error("Error: value is not Decimal!!");
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
        if (refreshPollingJob == null || refreshPollingJob.isCancelled()) {
            refreshPollingJob = scheduler.scheduleWithFixedDelay(new Runnable() {
                @Override
                public void run() {
                    updateData();
                }
            }, 0, pollingPeriod, TimeUnit.SECONDS);
        }

    }

    protected void updateData() {
        logger.debug("Updating...");
        String Result;

        if (getActiveChannelListAsString().equals(MegadOneTryBindingConstants.CHANNEL_TGET)) {
            Result = "http://" + getThing().getConfiguration().get("hostname").toString() + "/"
                    + getThing().getConfiguration().get("password").toString() + "/?tget=1";
        } else {
            Result = "http://" + getThing().getConfiguration().get("hostname").toString() + "/"
                    + getThing().getConfiguration().get("password").toString() + "/?pt="
                    + getThing().getConfiguration().get("port").toString() + "&cmd=get";
        }
        if (getActiveChannelListAsString().equals(MegadOneTryBindingConstants.CHANNEL_ST)) {
            return;
        }

        try {
            URL obj = new URL(Result);
            HttpURLConnection con;

            con = (HttpURLConnection) obj.openConnection();

            logger.debug(Result);

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
            logger.debug("input string->" + response.toString());

            if (response.toString().contains("ON")) {
                updateState(getActiveChannelListAsString(), OnOffType.ON);
            } else if (response.toString().contains("OFF")) {
                updateState(getActiveChannelListAsString(), OnOffType.OFF);
            } else if (getActiveChannelListAsString().equals(MegadOneTryBindingConstants.CHANNEL_DIMMER)) {
                int percent = (int) Math.round(Integer.parseInt(response.toString()) / 2.55);
                updateState(getActiveChannelListAsString(), PercentType.valueOf(Integer.toString(percent)));
                logger.debug(getThing().getUID().getId() + " " + percent);
            } else if (getActiveChannelListAsString().equals(MegadOneTryBindingConstants.CHANNEL_DHTTEMP)) {
                String[] ResponseParse = response.toString().split("[:/]");
                updateState(getActiveChannelListAsString(), DecimalType.valueOf(ResponseParse[1]));
            } else if (getActiveChannelListAsString().equals(MegadOneTryBindingConstants.CHANNEL_DHTHUM)) {
                String[] ResponseParse = response.toString().split("[:/]");
                updateState(getActiveChannelListAsString(), DecimalType.valueOf(ResponseParse[3]));
            } else {
                updateState(getActiveChannelListAsString(), DecimalType.valueOf(response.toString()));
            }

        } catch (IOException e) {
            logger.debug("Connect to megadevice " + getThing().getConfiguration().get("hostname").toString()
                    + " error: " + e.getLocalizedMessage());
        }
    }

    private void registerMegadThingListener(MegadOneTryBridgeHandler bridgeHandler) {
        if (bridgeHandler != null) {
            bridgeHandler.registerMegadThingListener(this);
        } else {
            logger.debug("Can't register {} at bridge bridgeHandler is null.", this.getThing().getUID());
        }
    }

    private synchronized MegadOneTryBridgeHandler getBridgeHandler() {

        Bridge bridge = getBridge();
        if (bridge == null) {
            logger.debug("Required bridge not defined for device {}.");
            return null;
        } else {
            return getBridgeHandler(bridge);
        }

    }

    private synchronized MegadOneTryBridgeHandler getBridgeHandler(Bridge bridge) {

        MegadOneTryBridgeHandler bridgeHandler = null;

        ThingHandler handler = bridge.getHandler();
        if (handler instanceof MegadOneTryBridgeHandler) {
            bridgeHandler = (MegadOneTryBridgeHandler) handler;
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
}
