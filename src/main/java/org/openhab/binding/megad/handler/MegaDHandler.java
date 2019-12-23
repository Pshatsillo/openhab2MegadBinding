/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
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
import org.openhab.binding.megad.i2c.I2C;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link MegadHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Petr Shatsillo - Initial contribution
 */
@NonNullByDefault
public class MegaDHandler extends BaseThingHandler {

    private Logger logger = LoggerFactory.getLogger(MegaDHandler.class);
    protected long lastRefresh = 0;
    private @Nullable ScheduledFuture<?> refreshPollingJob;
    protected int dimmervalue;
    @Nullable
    MegaDBridgeHandler bridgeHandler;
    boolean isI2cInit = false;

    public MegaDHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        int state = 0;

        String result = "";

        if (channelUID.getId().equals(MegaDBindingConstants.CHANNEL_OUT)) {
            if (command.toString().equals("ON")) {
                state = 1;
            } else if (command.toString().equals("OFF")) {
                state = 0;
            }
            result = "http://" + getThing().getConfiguration().get("hostname").toString() + "/"
                    + getThing().getConfiguration().get("password").toString() + "/?cmd="
                    + getThing().getConfiguration().get("port").toString() + ":" + state;
            logger.info("Switch: {}", result);
            sendCommand(result);
        } else if (channelUID.getId().equals(MegaDBindingConstants.CHANNEL_DIMMER)) {
            if (!command.toString().equals("REFRESH")) {
                try {
                    int resultInt = (int) Math.round(Integer.parseInt(command.toString()) * 2.55);
                    dimmervalue = resultInt;
                    result = "http://" + getThing().getConfiguration().get("hostname").toString() + "/"
                            + getThing().getConfiguration().get("password").toString() + "/?cmd="
                            + getThing().getConfiguration().get("port").toString() + ":" + dimmervalue;
                    logger.info("Dimmer: {}", result);
                    sendCommand(result);
                } catch (Exception e) {
                    if (command.toString().equals("OFF")) {
                        result = "http://" + getThing().getConfiguration().get("hostname").toString() + "/"
                                + getThing().getConfiguration().get("password").toString() + "/?cmd="
                                + getThing().getConfiguration().get("port").toString() + ":0";
                        logger.info("Dimmer set to OFF");
                        sendCommand(result);
                    } else if (command.toString().equals("ON")) {
                        result = "http://" + getThing().getConfiguration().get("hostname").toString() + "/"
                                + getThing().getConfiguration().get("password").toString() + "/?cmd="
                                + getThing().getConfiguration().get("port").toString() + ":" + dimmervalue;
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
        } else if (channelUID.getId().equals(MegaDBindingConstants.CHANNEL_I2C_DISPLAY)) {
            logger.debug("display changed");
            try {
                I2C disp = new I2C(getThing().getConfiguration().get("hostname").toString(),
                        getThing().getConfiguration().get("password").toString(),
                        getThing().getConfiguration().get("port").toString(),
                        getThing().getConfiguration().get("scl").toString());

                if (!isI2cInit) {
                    logger.debug("preparingDisplay");
                    disp.prepareDisplay();
                    isI2cInit = true;
                }
                if (!command.toString().equals("REFRESH")) {
                    disp.writeText(command.toString(), "default", 0, 0);
                }
            } catch (Exception ex) {
                logger.error("I2C config error. Scl parameter not found");
            }
            // updateStatus(ThingStatus.ONLINE);
        }
    }

    public void sendCommand(String Result) {
        HttpURLConnection con;

        URL megaURL;

        try {
            megaURL = new URL(Result);
            con = (HttpURLConnection) megaURL.openConnection();
            // optional default is GET
            con.setReadTimeout(1500);
            con.setConnectTimeout(1500);
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
            logger.warn("Connect to megadevice {} {} error: ", getThing().getConfiguration().get("hostname").toString(),
                    e.getLocalizedMessage());
        }
    }

    public void updateValues(String hostAddress, String[] getCommands, @Nullable OnOffType OnOff) {
        for (Channel channel : getThing().getChannels()) {
            if (isLinked(channel.getUID().getId())) {
                if ((channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_IN))
                        || (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_OUT))) {
                    if (OnOff != null) {
                        updateState(channel.getUID().getId(), OnOff);
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_M2)) {
                    try {
                        if (getCommands[4].equals("2") && getCommands[3].equals("m")) {
                            updateState(channel.getUID().getId(), OnOffType.ON);
                        } else if (OnOff == OnOffType.OFF) {
                            updateState(channel.getUID().getId(), OnOffType.OFF);
                        }
                    } catch (Exception e) {
                        logger.debug(" Not m2 signal ", e.getLocalizedMessage());
                    }

                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_CLICK)) {

                    if (getCommands[3].equals("click")) {
                        try {
                            updateState(channel.getUID().getId(), DecimalType.valueOf(getCommands[4]));
                        } catch (Exception ex) {
                            logger.debug(" Cannot update click ", ex.getLocalizedMessage());
                        }
                    }

                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_DIMMER)) {
                    int percent = 0;
                    try {
                        percent = (int) Math.round(Integer.parseInt(getCommands[4]) / 2.55);
                    } catch (Exception ex) {
                    }
                    updateState(channel.getUID().getId(), PercentType.valueOf(Integer.toString(percent)));
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_IB)) {
                    try {
                        updateState(channel.getUID().getId(), StringType.valueOf(getCommands[4]));
                    } catch (Exception ex) {
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_WIEGAND)) {
                    try {
                        updateState(channel.getUID().getId(), StringType.valueOf(getCommands[4]));
                    } catch (Exception ex) {
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_AT)) {
                    try {
                        updateState(channel.getUID().getId(), DecimalType.valueOf(getCommands[2]));
                    } catch (Exception ex) {
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_ADC)) {
                    try {
                        updateState(channel.getUID().getId(), DecimalType.valueOf(getCommands[2]));
                    } catch (Exception ex) {
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_INCOUNT)) {
                    try {
                        if (getCommands[5].equals("cnt")) {
                            updateState(channel.getUID().getId(), DecimalType.valueOf(getCommands[6]));

                        } else if (getCommands[3].equals("cnt")) {
                            updateState(channel.getUID().getId(), DecimalType.valueOf(getCommands[4]));
                        }
                    } catch (Exception ex) {
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_ONEWIRE)) {
                    logger.debug("Does not accept incoming values");
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_SMS_PHONE)) {
                    try {
                        updateState(channel.getUID().getId(), StringType.valueOf(getCommands[2]));
                    } catch (Exception ex) {
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_SMS_TEXT)) {
                    try {
                        updateState(channel.getUID().getId(), StringType.valueOf(getCommands[4]));
                    } catch (Exception ex) {
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_CONTACT)) {
                    if (OnOff != null) {
                        if (OnOff.name() == "ON") {
                            updateState(channel.getUID().getId(), OpenClosedType.CLOSED);
                        } else if (OnOff.name() == "OFF") {
                            updateState(channel.getUID().getId(), OpenClosedType.OPEN);
                        }
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_I2C)) {
                    String commands = "";
                    logger.debug(commands);

                    if (getCommands.length % 2 != 0) {
                        for (int i = 3; getCommands.length > i;) {
                            commands += getCommands[i] + "=" + getCommands[i + 1] + "&";
                            i = i + 2;
                        }
                    }
                    try {
                        commands = commands.substring(0, commands.length() - 1);
                    } catch (Exception ex) {
                        // logger.error("cannot parse. wrong incoming: " + commands + " Error: " + ex);
                    }
                    updateState(channel.getUID().getId(), StringType.valueOf(commands));

                    logger.debug("i2c command receive is: {}", commands);
                } else {
                    try {
                        updateState(channel.getUID().getId(), DecimalType.valueOf(getCommands[4]));
                    } catch (Exception ex) {
                    }
                }
            } else {
                if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_ST)) {
                    try {
                        updateState(channel.getUID().getId(), DecimalType.valueOf(getCommands[2]));
                    } catch (Exception ex) {
                    }
                }
            }
        }
    }

    @Override
    public void updateStatus(ThingStatus status) {
        super.updateStatus(status);
    }

    @SuppressWarnings("null")
    @Override
    public void initialize() {
        bridgeHandler = getBridgeHandler();
        logger.debug("Thing Handler for {} started", getThing().getUID().getId());
        if (bridgeHandler != null) {
            registerMegadThingListener(bridgeHandler);
        } else {
            logger.debug("Can't register {} at bridge. BridgeHandler is null.", this.getThing().getUID());
        }

        String[] rr = getThing().getConfiguration().get("refresh").toString().split("[.]");
        logger.debug("refresh: {}", rr[0]);
        int pollingPeriod = Integer.parseInt(rr[0]) * 1000;
        if (pollingPeriod != 0) {
            if (refreshPollingJob == null || refreshPollingJob.isCancelled()) {
                refreshPollingJob = scheduler.scheduleWithFixedDelay(new Runnable() {
                    @Override
                    public void run() {
                        refresh(pollingPeriod);
                    }
                }, 0, 1000, TimeUnit.MILLISECONDS);
            }
        }
    }

    public void refresh(int interval) {
        long now = System.currentTimeMillis();
        if (now >= (lastRefresh + interval)) {
            updateData();
            lastRefresh = now;
        }
    }

    @SuppressWarnings("unlikely-arg-type")
    protected void updateData() {
        logger.debug("Updating Megadevice things...");
        String[] result = { "http://" + getThing().getConfiguration().get("hostname").toString() + "/"
                + getThing().getConfiguration().get("password").toString() + "/?pt="
                + getThing().getConfiguration().get("port").toString() + "&cmd=get", "", "", "" };
        Channel tget = getThing().getChannel(MegaDBindingConstants.CHANNEL_TGET);
        if (tget != null) {
            if ((isLinked(tget.getUID()))) {
                result[1] = "   http://" + getThing().getConfiguration().get("hostname").toString() + "/"
                        + getThing().getConfiguration().get("password").toString() + "/?tget=1 ";
            }
        }

        if ((getThing().getConfiguration().get("scl") != null)
                && (getThing().getConfiguration().get("i2c_dev") != null)) {
            result[2] = "http://" + getThing().getConfiguration().get("hostname").toString() + "/"
                    + getThing().getConfiguration().get("password").toString() + "/?pt="
                    + getThing().getConfiguration().get("port").toString() + "&scl="
                    + getThing().getConfiguration().get("scl").toString() + "&i2c_dev="
                    + getThing().getConfiguration().get("i2c_dev").toString();
            result[3] = "http://" + getThing().getConfiguration().get("hostname").toString() + "/"
                    + getThing().getConfiguration().get("password").toString() + "/?pt="
                    + getThing().getConfiguration().get("port").toString() + "&cmd=list";
        }
        String[] updateRequest = sendRequest(result);

        for (Channel channel : getThing().getChannels()) {

            if (isLinked(channel.getUID().getId())) {
                if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_ST)) {
                    return;
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_IN)) {
                    if (updateRequest[0].contains("ON")) {
                        updateState(channel.getUID().getId(), OnOffType.ON);
                    } else if (updateRequest[0].contains("OFF")) {
                        updateState(channel.getUID().getId(), OnOffType.OFF);
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_INCOUNT)) {
                    String[] value = updateRequest[0].split("[/]");
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
                    if (updateRequest[0].contains("ON")) {
                        updateState(channel.getUID().getId(), OnOffType.ON);
                    } else if (updateRequest[0].contains("OFF")) {
                        updateState(channel.getUID().getId(), OnOffType.OFF);
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_CONTACT)) {
                    if (updateRequest[0].contains("ON")) {
                        updateState(channel.getUID().getId(), OpenClosedType.CLOSED);
                    } else if (updateRequest[0].contains("OFF")) {
                        updateState(channel.getUID().getId(), OpenClosedType.OPEN);
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_DIMMER)) {
                    int percent = 0;
                    try {
                        percent = (int) Math.round(Integer.parseInt(updateRequest[0]) / 2.55);
                    } catch (Exception ex) {
                        logger.debug("Cannot convert to dimmer values string: '{}'", updateRequest[0]);
                    }
                    updateState(channel.getUID().getId(), PercentType.valueOf(Integer.toString(percent)));
                    logger.debug("{} {}", getThing().getUID().getId(), percent);
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_DHTTEMP)) {
                    String[] responseParse = updateRequest[0].split("[:/]");
                    if (responseParse.length > 2) {
                        if (responseParse[0].contains("temp")) {
                            updateState(channel.getUID().getId(), DecimalType.valueOf(responseParse[1]));
                        }
                    } else {
                        try {
                            updateState(channel.getUID().getId(), DecimalType.valueOf(responseParse[0]));
                        } catch (Exception ex) {
                            logger.debug("Cannot update DHT temperature at channel: '{}'", channel.getUID().getId());
                        }
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_DHTHUM)) {
                    String[] responseParse = updateRequest[0].split("[:/]");
                    if (responseParse.length > 2) {
                        if (responseParse[2].contains("hum")) {
                            updateState(channel.getUID().getId(), DecimalType.valueOf(responseParse[3]));
                        }
                    } else {
                        if (responseParse.length >= 2) {
                            try {
                                updateState(channel.getUID().getId(), DecimalType.valueOf(responseParse[1]));
                            } catch (Exception ex) {
                                logger.debug("Cannot update DHT humidity at channel: '{}'", channel.getUID().getId());
                            }
                        }
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_TGET)) {
                    // Result[1];
                    try {
                        updateState(channel.getUID().getId(), DecimalType.valueOf(updateRequest[1]));
                    } catch (Exception ex) {
                        logger.debug("Cannot update TGET value at channel: '{}'", channel.getUID().getId());
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_ADC)) {
                    try {
                        updateState(channel.getUID().getId(), DecimalType.valueOf(updateRequest[0]));
                    } catch (Exception ex) {
                        logger.debug("Cannot update ADC value at channel: '{}'", channel.getUID().getId());
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_ONEWIRE)) {
                    String[] responseParse = updateRequest[0].split("[:]");
                    if (responseParse.length > 1) {
                        logger.debug("{}", responseParse[1]);
                        if (!(updateRequest[0].equals("NA"))) {
                            try {
                                updateState(channel.getUID().getId(), DecimalType.valueOf(responseParse[1]));
                            } catch (Exception ex) {
                                logger.debug("Cannot update One wire temperature at channel: '{}'",
                                        channel.getUID().getId());
                            }
                        }
                    } else {
                        if (!(updateRequest[0].equals("NA"))) {
                            try {
                                updateState(channel.getUID().getId(), DecimalType.valueOf(updateRequest[0]));
                            } catch (Exception ex) {
                                logger.debug("Cannot update One wire temperature at channel: '{}'",
                                        channel.getUID().getId());
                            }
                        }
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_I2C)) {
                    // Result =;
                    if (!updateRequest[2].equals("")) {
                        updateState(channel.getUID().getId(), StringType.valueOf(updateRequest[2]));
                    } else {
                        try {
                            updateState(channel.getUID().getId(), StringType.valueOf(updateRequest[0]));
                        } catch (Exception ex) {
                            logger.error("cannot update channel i2c state. input string does not match standarts");
                            for (String string : updateRequest) {
                                logger.error("parsed strings: {}", string);
                            }
                        }
                    }
                }
            }
        }
    }

    private void registerMegadThingListener(@Nullable MegaDBridgeHandler bridgeHandler) {
        if (bridgeHandler != null) {
            bridgeHandler.registerMegadThingListener(this);
        }
    }

    private synchronized @Nullable MegaDBridgeHandler getBridgeHandler() {
        Bridge bridge = getBridge();
        if (bridge == null) {
            logger.error("Required bridge not defined for device {}.");
            // throw new NullPointerException("Required bridge not defined for device");
            return null;
        } else {
            return getBridgeHandler(bridge);
        }
    }

    private synchronized @Nullable MegaDBridgeHandler getBridgeHandler(Bridge bridge) {
        ThingHandler handler = bridge.getHandler();
        if (handler instanceof MegaDBridgeHandler) {
            return (MegaDBridgeHandler) handler;
        } else {
            logger.debug("No available bridge handler found yet. Bridge: {} .", bridge.getUID());
            return null;
        }
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

    @SuppressWarnings("null")
    @Override
    protected Configuration editConfiguration() {
        logger.debug("config changed");
        if (refreshPollingJob != null && !refreshPollingJob.isCancelled()) {
            refreshPollingJob.cancel(true);
            refreshPollingJob = null;
        }
        return super.editConfiguration();
    }

    @SuppressWarnings("null")
    @Override
    public void dispose() {
        logger.debug("Thing Handler for {} stop", getThing().getUID().getId());
        if (refreshPollingJob != null && !refreshPollingJob.isCancelled()) {
            refreshPollingJob.cancel(true);
            refreshPollingJob = null;
        }
        if (bridgeHandler != null) {
            unregisterMegadThingListener(bridgeHandler);
        }
    }

    private void unregisterMegadThingListener(@Nullable MegaDBridgeHandler bridgeHandler) {
        logger.debug("unregister");
        if (bridgeHandler != null) {
            bridgeHandler.unregisterThingListener(this);
        } else {
            logger.debug("Can't unregister {} at bridge bridgeHandler is null.", this.getThing().getUID());
        }
    }

    private String[] sendRequest(String[] URL) {
        String[] result = { "", "", "", "" };
        int count = 0;
        if (getThing().getConfiguration().get("i2c_par") != null) {
            URL[2] += "&i2c_par=" + getThing().getConfiguration().get("i2c_par").toString();
        }
        for (String url : URL) {
            if (!url.equals("")) {
                try {
                    URL obj = new URL(url);
                    HttpURLConnection con;

                    con = (HttpURLConnection) obj.openConnection();

                    logger.debug(url);

                    con.setRequestMethod("GET");
                    // con.setReadTimeout(500);
                    con.setReadTimeout(1500);
                    con.setConnectTimeout(1500);
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
                    result[count] = response.toString().trim();
                    con.disconnect();
                } catch (IOException e) {
                    logger.error("Connect to megadevice {} error: {}",
                            getThing().getConfiguration().get("hostname").toString(), e.getLocalizedMessage());
                }
                count++;
            }
        }
        return result;
    }
}
