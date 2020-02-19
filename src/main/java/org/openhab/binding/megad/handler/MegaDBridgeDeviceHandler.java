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
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * The {@link MegaDBridgeIncomingHandler} is responsible for creating things and thing
 * handlers.
 *
 * @author Petr Shatsillo - Initial contribution
 */
@NonNullByDefault
public class MegaDBridgeDeviceHandler extends BaseBridgeHandler {
    private Logger logger = LoggerFactory.getLogger(MegaDBridgeDeviceHandler.class);
    private @Nullable Map<String, MegaDMegaPortsHandler> portsHandlerMap = new HashMap<>();
    private @Nullable Map<String, MegaDMegaItoCHandler> itoCHandlerMap = new HashMap<>();
    private @Nullable Map<String, MegaDBridge1WireBusHandler> oneWireHandlerMap = new HashMap<>();
    private Map<String, String> portsvalues = new HashMap<>();

    @Nullable
    MegaDBridgeIncomingHandler bridgeIncomingHandler;
    @Nullable
    MegaDMegaPortsHandler megaportsHandler;

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

        logger.debug("Device {} init", getThing().getConfiguration().get("hostname").toString());
        getAllPortsStatus();
    }

    private synchronized @Nullable MegaDBridgeIncomingHandler getBridgeHandler() {
        Bridge bridge = getBridge();
        if (bridge == null) {
            logger.warn("Required bridge not defined for device.");
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

    @SuppressWarnings("null")
    public void updateValues(String command) {
        logger.debug("command: {}", command);
        logger.debug("host: {}", getThing().getConfiguration().get("hostname").toString());
        String[] getCommands = command.split("[?&>=]");
        if (portsHandlerMap != null) {
            if (command.contains("all=")) { // loop incoming
                logger.debug("Loop incoming from Megad: {} {}",
                        getThing().getConfiguration().get("hostname").toString(), command);

                if (getCommands.length == 4) {
                    String[] parsedStatus = getCommands[3].split("[;]");
                    for (int i = 0; parsedStatus.length > i; i++) {
                        megaportsHandler = portsHandlerMap.get(String.valueOf(i));
                        if (megaportsHandler != null) {
                            String[] mode = parsedStatus[i].split("[/]");
                            String[] commandsAdapt = { "", "", parsedStatus[i] };
                            if (mode[0].contains("ON")) {
                                megaportsHandler.updateValues(commandsAdapt, OnOffType.ON);
                            } else if (mode[0].contains("OFF")) {
                                megaportsHandler.updateValues(commandsAdapt, OnOffType.OFF);
                            } else {
                                megaportsHandler.updateValues(commandsAdapt, null);
                            }
                        }
                    }
                } else {
                    String[] parsedStatus = getCommands[2].split("[;]");
                    for (int i = 0; parsedStatus.length > i; i++) {
                        megaportsHandler = portsHandlerMap.get(String.valueOf(i));
                        String[] mode = parsedStatus[i].split("[/]");
                        if (mode[0].contains("ON")) {
                            logger.debug("Updating port {} Value is: {}", i, mode[0]);
                            if (megaportsHandler != null) {
                                megaportsHandler.updateValues(mode, OnOffType.ON);
                            }
                        } else if (mode[0].contains("OFF")) {
                            logger.debug("Updating port {} Value is: {}", i, mode[0]);
                            if (megaportsHandler != null) {
                                megaportsHandler.updateValues(mode, OnOffType.OFF);
                            }
                        } else {
                            logger.debug("Not a switch");
                        }
                    }
                }
            } else {
                megaportsHandler = portsHandlerMap.get(getCommands[1]);
                if (command.contains("m=1")) { // press button
                    if (megaportsHandler != null) {
                        megaportsHandler.updateValues(getCommands, OnOffType.OFF);
                    }
                } else if (command.contains("click")) {
                    if (megaportsHandler != null) {
                        megaportsHandler.updateValues(getCommands, null);
                    }
                } else if (command.contains("m=2")) {
                    if (megaportsHandler != null) {
                        megaportsHandler.updateValues(getCommands, null);
                    }
                }

                else if (command.contains("v=")) { // slave mode
                    if (megaportsHandler != null) {
                        if (command.contains("v=1")) {
                            megaportsHandler.updateValues(getCommands, OnOffType.ON);
                        } else {
                            megaportsHandler.updateValues(getCommands, OnOffType.OFF);
                        }
                    }
                } else {
                    if ((getCommands[0].equals("st")) || (getCommands[2].equals("sms_phone"))) {
                        logger.debug("{}", portsHandlerMap.size());

                        for (int i = 0; portsHandlerMap.size() > i; i++) {
                            megaportsHandler = portsHandlerMap.get(portsHandlerMap.keySet().toArray()[i].toString());
                            if (megaportsHandler != null) {
                                megaportsHandler.updateValues(getCommands, null);
                            }
                        }
                    } else {
                        if (megaportsHandler != null) {
                            megaportsHandler.updateValues(getCommands, OnOffType.ON);
                        }
                    }
                }
            }
        }
    }

    // @SuppressWarnings("null")
    public void getAllPortsStatus() {
        String request = "http://" + getThing().getConfiguration().get("hostname").toString() + "/"
                + getThing().getConfiguration().get("password").toString() + "/?cmd=all";
        String updateRequest = sendRequest(request);
        String[] getValues = updateRequest.split("[;]");
        for (int i = 0; getValues.length > i; i++) {
            setPortsvalues(String.valueOf(i), getValues[i]);
        }

        logger.debug("All ports is {}", updateRequest);
    }

    private String sendRequest(String URL) {
        String result = "";
        if (!URL.equals("")) {
            try {
                URL obj = new URL(URL);
                HttpURLConnection con;

                con = (HttpURLConnection) obj.openConnection();

                logger.debug("URL: {}", URL);

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

    @SuppressWarnings({ "unused", "null" })
    public void registerMegadPortsListener(MegaDMegaPortsHandler megaDMegaportsHandler) {
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

    @SuppressWarnings({ "unused", "null" })
    public void registerMegadItoCListener(MegaDMegaItoCHandler megaDMegaItoCHandler) {
        String ip = megaDMegaItoCHandler.getThing().getConfiguration().get("port").toString();
        logger.debug("Register Device with ip {} and port {}", getThing().getConfiguration().get("hostname").toString(),
                megaDMegaItoCHandler.getThing().getConfiguration().get("port").toString());
        if (itoCHandlerMap.get(ip) != null) {
            updateThingHandlerStatus(megaDMegaItoCHandler, ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "port already exists");
        } else {
            itoCHandlerMap.put(ip, megaDMegaItoCHandler);
            updateThingHandlerStatus(megaDMegaItoCHandler, ThingStatus.ONLINE);
        }
    }

    @SuppressWarnings({ "null", "unused" })
    public void registerMega1WireBusListener(MegaDBridge1WireBusHandler megaDBridge1WireBusHandler) {
        String oneWirePort = megaDBridge1WireBusHandler.getThing().getConfiguration().get("port").toString();

        if (oneWireHandlerMap.get(oneWirePort) != null) {
            updateThingHandlerStatus(megaDBridge1WireBusHandler, ThingStatus.OFFLINE,
                    ThingStatusDetail.CONFIGURATION_ERROR, "Device already exist");
        } else {
            oneWireHandlerMap.put(oneWirePort, megaDBridge1WireBusHandler);
            updateThingHandlerStatus(megaDBridge1WireBusHandler, ThingStatus.ONLINE);
            // megaDBridgeDeviceHandler.getAllPortsStatus();
        }
    }

    @SuppressWarnings("null")
    public void unregisterMegaDeviceListener(MegaDMegaPortsHandler megaDMegaportsHandler) {
        String ip = megaDMegaportsHandler.getThing().getConfiguration().get("port").toString();
        if (portsHandlerMap.get(ip) != null) {
            portsHandlerMap.remove(ip);
            updateThingHandlerStatus(megaDMegaportsHandler, ThingStatus.OFFLINE);
        }
    }

    public void unregisterMegad1WireListener(MegaDBridge1WireBusHandler megaDBridge1WireBusHandler) {
        String ip = megaDBridge1WireBusHandler.getThing().getConfiguration().get("port").toString();
        if (oneWireHandlerMap.get(ip) != null) {
            oneWireHandlerMap.remove(ip);
            updateThingHandlerStatus(megaDBridge1WireBusHandler, ThingStatus.OFFLINE);
        }
    }

    private void updateThingHandlerStatus(MegaDBridge1WireBusHandler thingHandler, ThingStatus status) {
        thingHandler.updateStatus(status);
    }

    private void updateThingHandlerStatus(MegaDBridge1WireBusHandler megaDBridge1WireBusHandler, ThingStatus status,
            ThingStatusDetail statusDetail, String decript) {
        megaDBridge1WireBusHandler.updateStatus(status, statusDetail, decript);
    }

    private void updateThingHandlerStatus(MegaDMegaItoCHandler megaDMegaItoCHandler, ThingStatus status,
            ThingStatusDetail statusDetail, String decript) {
        megaDMegaItoCHandler.updateStatus(status, statusDetail, decript);
    }

    private void updateThingHandlerStatus(MegaDMegaItoCHandler thingHandler, ThingStatus status) {
        thingHandler.updateStatus(status);
    }

    private void updateThingHandlerStatus(MegaDMegaPortsHandler megaDMegaportsHandler, ThingStatus status,
            ThingStatusDetail statusDetail, String decript) {
        megaDMegaportsHandler.updateStatus(status, statusDetail, decript);
    }

    private void updateThingHandlerStatus(MegaDMegaPortsHandler thingHandler, ThingStatus status) {
        thingHandler.updateStatus(status);
    }

    @Override
    public void dispose() {
        if (bridgeIncomingHandler != null) bridgeIncomingHandler.unregisterMegaDeviceListener(this);
        super.dispose();
    }

    @SuppressWarnings({ "unused", "null" })
    public String[] getPortsvalues(String port) {
        String[] portvalue = { "", "", "", "", "" };
        portvalue[0] = "";
        portvalue[1] = "pt";
        if (portsvalues.get(port) != null) {
            portvalue[2] = portsvalues.get(port).toString();
        } else {
            portvalue[2] = "0";
        }
        ;
        portvalue[3] = "";
        portvalue[4] = "";
        return portvalue;
    }

    public void setPortsvalues(String key, String value) {
        portsvalues.put(key, value);
    }

}
