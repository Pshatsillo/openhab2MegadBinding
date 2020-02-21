/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.thing.*;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.megad.MegaDBindingConstants;
import org.openhab.binding.megad.internal.MegaHttpHelpers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * The {@link MegaDBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Petr Shatsillo - Initial contribution
 */
@NonNullByDefault
public class MegaD2WHandler extends BaseThingHandler {
    private Logger logger = LoggerFactory.getLogger(MegaD2WHandler.class);
    private @Nullable ScheduledFuture<?> refreshPollingJob;
    @Nullable
    MegaDBridgeDeviceHandler bridgeDeviceHandler;
    protected long lastRefresh = 0;

    public MegaD2WHandler(Thing thing) {
        super(thing);
    }
    @SuppressWarnings({ "null" })
    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        int state = 0;
        String cmd ="";
        String addr = "";
        if (!command.toString().equals("REFRESH")) {
            if (command.toString().equals("ON")) {
                state = 1;
            } else if (command.toString().equals("OFF")) {
                state = 0;
            }
            if (channelUID.getId().equals(MegaDBindingConstants.CHANNEL_MEGAD2W_A)) {
               cmd = getThing().getConfiguration().get("port").toString() + "A:";
            } else  if (channelUID.getId().equals(MegaDBindingConstants.CHANNEL_MEGAD2W_B)) {
                cmd = getThing().getConfiguration().get("port").toString() + "B:";
            }
            if(!getThing().getConfiguration().get("addr").equals("0")){
                addr = "&addr=" + getThing().getConfiguration().get("addr").toString();
            }
            String result = "http://" + bridgeDeviceHandler.getThing().getConfiguration().get("hostname").toString() + "/"
                    + bridgeDeviceHandler.getThing().getConfiguration().get("password").toString() + "/?cmd="
                    + cmd + state + addr;
            logger.debug("Switch: {}", result);
            sendCommand(result);
        }
    }
    @SuppressWarnings({ "null" })
    @Override
    public void initialize() {
        bridgeDeviceHandler = getBridgeHandler();
        // logger.debug("Thing Handler for {} started", getThing().getUID().getId());
        if (bridgeDeviceHandler != null) {
            registerMegad2WListener(bridgeDeviceHandler);
        } else {
            logger.debug("Can't register {} at bridge. BridgeHandler is null.", this.getThing().getUID());
        }

        logger.debug("Getting values for first time from bridge...");
        String[] portStatus = bridgeDeviceHandler
                .getPortsvalues(getThing().getConfiguration().get("port").toString());
        logger.debug("Value is: {}", portStatus[2]);
            updateValues(portStatus[2]);

        String[] rr = getThing().getConfiguration().get("refresh").toString().split("[.]");
        logger.debug("refresh: {}", rr[0]);
        int pollingPeriod = Integer.parseInt(rr[0]) * 1000;
        if (refreshPollingJob == null || refreshPollingJob.isCancelled()) {
            refreshPollingJob = scheduler.scheduleWithFixedDelay(new Runnable() {
                @Override
                public void run() {
                    refresh(pollingPeriod);
                }
            }, 0, 1000, TimeUnit.MILLISECONDS);
        }
    }
    @SuppressWarnings("null")
    private void refresh(int pollingPeriod) {
        long now = System.currentTimeMillis();
        if (pollingPeriod != 0) {
            if (now >= (lastRefresh + pollingPeriod)) {
                String result = "http://" + getBridgeHandler().getThing().getConfiguration().get("hostname").toString()
                + "/" + getBridgeHandler().getThing().getConfiguration().get("password").toString() + "/?pt="
                + getThing().getConfiguration().get("port").toString() + "&cmd=get";
                String updateRequest = MegaHttpHelpers.sendRequest(result);
                updateValues(updateRequest);
                lastRefresh = now;
            }
        }
    }

    private void updateValues(String portStatus) {
        String[] ports = portStatus.split("[/]");
        for (Channel channel : getThing().getChannels()) {
            if (isLinked(channel.getUID().getId())) {
                if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_MEGAD2W_A)){
                    try {
                        if (ports[0].equals("ON")) {
                            updateState(channel.getUID().getId(), OnOffType.ON);
                        } else if (ports[0].equals("OFF")) {
                            updateState(channel.getUID().getId(), OnOffType.OFF);
                        } else {
                            logger.debug("Status {} is udefined", ports[0]);
                        }
                    } catch (Exception e){
                    logger.debug("Cannot find value for port A");
                    }
                } else if(channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_MEGAD2W_B)){
                    try {
                        if (ports[1].equals("ON")) {
                            updateState(channel.getUID().getId(), OnOffType.ON);
                        } else if (ports[1].equals("OFF")) {
                            updateState(channel.getUID().getId(), OnOffType.OFF);
                        } else {
                            logger.debug("Status {} is udefined", ports[1]);
                        }
                    } catch (Exception e){
                        logger.debug("Cannot find value for port B");
                    }
                }
            }
        }
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
        } catch (MalformedURLException | ProtocolException e) {
            logger.error("{}", e.getLocalizedMessage());
        } catch (IOException e) {
            logger.error("Connect to megadevice {} {} error: ",
                    bridgeDeviceHandler.getThing().getConfiguration().get("hostname").toString(),
                    e.getLocalizedMessage());
        }
    }
//--------------------------------------------------------------------------------------------

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

    private void registerMegad2WListener(@Nullable MegaDBridgeDeviceHandler bridgeHandler) {
        if (bridgeHandler != null) {
            bridgeHandler.registerMegad2WListener(this);
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
    @SuppressWarnings("null")
    @Override
    public void dispose() {
        if (refreshPollingJob != null && !refreshPollingJob.isCancelled()) {
            refreshPollingJob.cancel(true);
            refreshPollingJob = null;
        }
        if (bridgeDeviceHandler != null) {
        bridgeDeviceHandler.unregisterMegad2WListener(this);
    }
        super.dispose();
    }
}
