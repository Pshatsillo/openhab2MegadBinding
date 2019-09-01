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
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link MegaDBridgeIncomingHandler} is responsible for creating things and thing
 * handlers.
 *
 * @author Petr Shatsillo - Initial contribution
 */
@NonNullByDefault
public class MegaDBridgeDeviceHandler extends BaseBridgeHandler {
    private Logger logger = LoggerFactory.getLogger(MegaDBridgeDeviceHandler.class);
    private @Nullable Map<String, MegaDMegaportsHandler> portsHandlerMap = new HashMap<String, MegaDMegaportsHandler>();

    @Nullable
    MegaDBridgeIncomingHandler bridgeIncomingHandler;

    public MegaDBridgeDeviceHandler(Bridge bridge) {
        super(bridge);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
    }

    @Override
    public void initialize() {

        bridgeIncomingHandler = getBridgeHandler();
        logger.debug("Thing Handler for {} started", getThing().getUID().getId());

        if (bridgeIncomingHandler != null) {
            registerMegaDeviceListener(bridgeIncomingHandler);
        } else {
            logger.debug("Can't register {} at bridge. BridgeHandler is null.", this.getThing().getUID());
        }
    }

    private synchronized @Nullable MegaDBridgeIncomingHandler getBridgeHandler() {
        Bridge bridge = getBridge();
        if (bridge == null) {
            logger.error("Required bridge not defined for device {}.");
            // throw new NullPointerException("Required bridge not defined for device");
            return null;
        } else {
            return getBridgeHandler(bridge);
        }
    }

    private synchronized @Nullable MegaDBridgeIncomingHandler getBridgeHandler(Bridge bridge) {
        ThingHandler handler = bridge.getHandler();
        if (handler instanceof MegaDBridgeIncomingHandler) {
            return (MegaDBridgeIncomingHandler) handler;
        } else {
            logger.debug("No available bridge handler found yet. Bridge: {} .", bridge.getUID());
            return null;
        }
    }

    private void registerMegaDeviceListener(@Nullable MegaDBridgeIncomingHandler bridgeHandler) {
        if (bridgeHandler != null) {
            bridgeHandler.registerMegaDeviceListener(this);
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

    public void updateValues(String command) {
        // TODO updateValues
        logger.debug("command: {}", command);
        logger.debug("host: {}", getThing().getConfiguration().get("hostname").toString());
    }

    @SuppressWarnings("null")
    public void getAllPortsStatus() {

        String request = "http://" + getThing().getConfiguration().get("hostname").toString() + "/"
                + getThing().getConfiguration().get("password").toString() + "/?cmd=all";
        String updateRequest = sendRequest(request);
        String[] getValues = updateRequest.split("[;]");
        for (int i = 0; getValues.length > i; i++) {
            if (portsHandlerMap.get(String.valueOf(i)) != null) {
                portsHandlerMap.get(String.valueOf(i)).updateValues(getValues[i]);
            }

        }

        logger.debug("{}", updateRequest);
    }

    private String sendRequest(String URL) {
        String result = "";
        if (!URL.equals("")) {
            try {
                URL obj = new URL(URL);
                HttpURLConnection con;

                con = (HttpURLConnection) obj.openConnection();

                logger.debug(URL);

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
                result = response.toString().trim();
                con.disconnect();
            } catch (IOException e) {
                logger.error("Connect to megadevice {} error: {}",
                        getThing().getConfiguration().get("hostname").toString(), e.getLocalizedMessage());
            }
        }
        return result;
    }

    @SuppressWarnings("unused")
    public void registerMegadPortsListener(MegaDMegaportsHandler megaDMegaportsHandler) {
        String ip = megaDMegaportsHandler.getThing().getConfiguration().get("port").toString();
        logger.debug("Register Device with ip {} and port {}", getThing().getConfiguration().get("hostname").toString(),
                megaDMegaportsHandler.getThing().getConfiguration().get("port").toString());
        if (portsHandlerMap.get(ip) != null) {
            updateThingHandlerStatus(megaDMegaportsHandler, ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "port already exists");
        } else {
            portsHandlerMap.put(ip, megaDMegaportsHandler);
            updateThingHandlerStatus(megaDMegaportsHandler, ThingStatus.ONLINE);
        }
    }

    @SuppressWarnings("null")
    public void unregisterMegaDeviceListener(MegaDMegaportsHandler megaDMegaportsHandler) {
        String ip = megaDMegaportsHandler.getThing().getConfiguration().get("port").toString();
        if (portsHandlerMap.get(ip) != null) {
            portsHandlerMap.remove(ip);
            updateThingHandlerStatus(megaDMegaportsHandler, ThingStatus.OFFLINE);
        }
    }

    private void updateThingHandlerStatus(MegaDMegaportsHandler megaDMegaportsHandler, ThingStatus status,
            ThingStatusDetail statusDetail, String decript) {
        megaDMegaportsHandler.updateStatus(status, statusDetail, decript);

    }

    private void updateThingHandlerStatus(MegaDMegaportsHandler thingHandler, ThingStatus status) {
        thingHandler.updateStatus(status);
    }

    @Override
    public void dispose() {
        if (bridgeIncomingHandler != null) {
            bridgeIncomingHandler.unregisterMegaDeviceListener(this);
        }
        super.dispose();
    }

}
