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
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.megad.MegaDBindingConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Objects;
import java.util.concurrent.ScheduledFuture;

/**
 * The {@link MegaDLcd1609Handler} is responsible for LCD1609 feature of megad
 *
 * @author Petr Shatsillo - Initial contribution
 */
@NonNullByDefault
public class MegaDLcd1609Handler extends BaseThingHandler {
    private final Logger logger = LoggerFactory.getLogger(MegaDLcd1609Handler.class);
    @Nullable MegaDBridgeDeviceHandler  bridgeDeviceHandler;

    public MegaDLcd1609Handler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        bridgeDeviceHandler = getBridgeHandler();
        updateStatus(ThingStatus.ONLINE);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.debug("{}", command);
        String result = "";
        if (channelUID.getId().equals(MegaDBindingConstants.CHANNEL_LINE1)){
            if (!command.toString().equals("REFRESH")) {
                result = "http://" + bridgeDeviceHandler.getThing().getConfiguration().get("hostname").toString() + "/"
                        + bridgeDeviceHandler.getThing().getConfiguration().get("password").toString() + "/?pt="
                        + getThing().getConfiguration().get("port").toString() + "&text=_________________";
                sendCommand(result);
                result = "http://" + bridgeDeviceHandler.getThing().getConfiguration().get("hostname").toString() + "/"
                        + bridgeDeviceHandler.getThing().getConfiguration().get("password").toString() + "/?pt="
                        + getThing().getConfiguration().get("port").toString() + "&text="+command;
                sendCommand(result);
            }
        } else if (channelUID.getId().equals(MegaDBindingConstants.CHANNEL_LINE2)){
            if (!command.toString().equals("REFRESH")) {
                result = "http://" + bridgeDeviceHandler.getThing().getConfiguration().get("hostname").toString() + "/"
                        + bridgeDeviceHandler.getThing().getConfiguration().get("password").toString() + "/?pt="
                        + getThing().getConfiguration().get("port").toString() + "&text=_________________&col=0&row=1";
                sendCommand(result);
                result = "http://" + bridgeDeviceHandler.getThing().getConfiguration().get("hostname").toString() + "/"
                        + bridgeDeviceHandler.getThing().getConfiguration().get("password").toString() + "/?pt="
                        + getThing().getConfiguration().get("port").toString() + "&text="+command+"&col=0&row=1";
                sendCommand(result);
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
            logger.error("Connect to megadevice {}  error: {} ",
                    bridgeDeviceHandler.getThing().getConfiguration().get("hostname").toString(),
                    e.getLocalizedMessage());
        }
    }

    //----------------------------------------------------------
    private synchronized @Nullable MegaDBridgeDeviceHandler getBridgeHandler() {
        Bridge bridge = Objects.requireNonNull(getBridge());
        return getBridgeHandler(bridge);
    }

    private synchronized @Nullable MegaDBridgeDeviceHandler getBridgeHandler(Bridge bridge) {
        ThingHandler handler = Objects.requireNonNull(bridge.getHandler());
        if (handler instanceof MegaDBridgeDeviceHandler) {
            return (MegaDBridgeDeviceHandler) handler;
        } else {
            logger.debug("No available bridge handler found yet. Bridge: {} .", bridge.getUID());
            return null;
        }
    }
    @SuppressWarnings("null")
    @Override
    public void dispose() {
        super.dispose();
    }
}
