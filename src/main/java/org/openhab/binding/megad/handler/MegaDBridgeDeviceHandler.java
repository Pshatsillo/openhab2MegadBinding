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
import org.openhab.binding.megad.internal.MegaHttpHelpers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private @Nullable Map<String, MegaDBridgeExtenderPortHandler> extenderBridgeHandlerMap = new HashMap<>();
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
        if(extenderBridgeHandlerMap != null) {
            logger.debug("command: {}", command);
            if (command.contains("ext")) {
                extenderBridgeHandlerMap.forEach((k, v) -> {
                    if (v.getThing().getConfiguration().get("int").equals(getCommands[1])) {
                        v.updateValues(getCommands);
                    }
                });
            }
        }
    }

    // @SuppressWarnings("null")
    public void getAllPortsStatus() {
        String request = "http://" + getThing().getConfiguration().get("hostname").toString() + "/"
                + getThing().getConfiguration().get("password").toString() + "/?cmd=all";
        String updateRequest = MegaHttpHelpers.sendRequest(request);
        String[] getValues = updateRequest.split("[;]");
        for (int i = 0; getValues.length > i; i++) {
            setPortsvalues(String.valueOf(i), getValues[i]);
        }

        logger.debug("All ports is {}", updateRequest);
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
    @SuppressWarnings("null")
    public void unregisterItoCListener(MegaDMegaItoCHandler megaDMegaItoCHandler) {
        String ip = megaDMegaItoCHandler.getThing().getConfiguration().get("port").toString();
        if (itoCHandlerMap.get(ip) != null) {
            itoCHandlerMap.remove(ip);
            updateThingHandlerStatus(megaDMegaItoCHandler, ThingStatus.OFFLINE);
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
    @SuppressWarnings({"unused","null"})
    public void registerMegaExtenderPortListener(MegaDBridgeExtenderPortHandler megaDBridgeExtenderPortHandler) {
        String extenderPort = megaDBridgeExtenderPortHandler.getThing().getConfiguration().get("port").toString();
        if (extenderBridgeHandlerMap.get(extenderPort) != null) {
            updateThingHandlerStatus(megaDBridgeExtenderPortHandler, ThingStatus.OFFLINE,
                    ThingStatusDetail.CONFIGURATION_ERROR, "Device already exist");
        } else {
            extenderBridgeHandlerMap.put(extenderPort, megaDBridgeExtenderPortHandler);
            updateThingHandlerStatus(megaDBridgeExtenderPortHandler, ThingStatus.ONLINE);
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
    @SuppressWarnings("null")
    public void unregisterMegad1WireListener(MegaDBridge1WireBusHandler megaDBridge1WireBusHandler) {
        String ip = megaDBridge1WireBusHandler.getThing().getConfiguration().get("port").toString();
        if (oneWireHandlerMap.get(ip) != null) {
            oneWireHandlerMap.remove(ip);
            updateThingHandlerStatus(megaDBridge1WireBusHandler, ThingStatus.OFFLINE);
        }
    }
    @SuppressWarnings("null")
    public void unregisterMegaDeviceListener(MegaDBridgeExtenderPortHandler megaDBridgeExtenderPortHandler) {
        String ip = megaDBridgeExtenderPortHandler.getThing().getConfiguration().get("port").toString();
        if (extenderBridgeHandlerMap.get(ip) != null) {
            extenderBridgeHandlerMap.remove(ip);
            updateThingHandlerStatus(megaDBridgeExtenderPortHandler, ThingStatus.OFFLINE);
        }
    }

    private void updateThingHandlerStatus(MegaDBridgeExtenderPortHandler thingHandler, ThingStatus status) {
        thingHandler.updateStatus(status);
    }

    private void updateThingHandlerStatus(MegaDBridgeExtenderPortHandler megaDBridgeExtenderPortHandler, ThingStatus status,
            ThingStatusDetail statusDetail, String decript) {
        megaDBridgeExtenderPortHandler.updateStatus(status, statusDetail, decript);
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

    private void updateThingHandlerStatus(MegaDBridge1WireBusHandler thingHandler, ThingStatus status) {
        thingHandler.updateStatus(status);
    }

    private void updateThingHandlerStatus(MegaDBridge1WireBusHandler megaDBridge1WireBusHandler, ThingStatus status,
                                          ThingStatusDetail statusDetail, String decript) {
        megaDBridge1WireBusHandler.updateStatus(status, statusDetail, decript);
    }
    @SuppressWarnings("null")
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
