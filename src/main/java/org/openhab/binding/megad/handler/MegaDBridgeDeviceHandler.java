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

import org.apache.commons.lang.SystemUtils;
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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * The {@link MegaDBridgeIncomingHandler} is responsible for creating things and thing
 * handlers.
 *
 * @author Petr Shatsillo - Initial contribution
 */
@NonNullByDefault
public class MegaDBridgeDeviceHandler extends BaseBridgeHandler {
    private Logger logger = LoggerFactory.getLogger(MegaDBridgeDeviceHandler.class);
    private @Nullable Map<String, MegaDPortsHandler> portsHandlerMap = new HashMap<>();
    private @Nullable Map<String, MegaDItoCHandler> itoCHandlerMap = new HashMap<>();
    private @Nullable Map<String, MegaDBridge1WireBusHandler> oneWireBusBridgeHandlerMap = new HashMap<>();
    private @Nullable Map<String, MegaDBridgeExtenderPortHandler> extenderBridgeHandlerMap = new HashMap<>();
    private Map<String, String> portsvalues = new HashMap<>();
    private @Nullable ScheduledFuture<?> refreshPollingJob;
    int pingCount;

    @Nullable
    MegaDBridgeIncomingHandler bridgeIncomingHandler;
    @Nullable
    MegaDPortsHandler megaportsHandler;

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

        if (refreshPollingJob == null || refreshPollingJob.isCancelled()) {
            refreshPollingJob = scheduler.scheduleWithFixedDelay(new Runnable() {
                @Override
                public void run() {
                    refresh();
                }
            }, 0, 1000, TimeUnit.MILLISECONDS);
        }

    }

    private void refresh(){
        Process proc = null;
            try {
                if(SystemUtils.IS_OS_WINDOWS) {
                    proc = new ProcessBuilder("ping", "-w", "1000", "-n", "1", getThing().getConfiguration().get("hostname").toString()).start();
                } else if (SystemUtils.IS_OS_LINUX) {
                    proc = new ProcessBuilder("ping", "-w", "1", "-c", "1", getThing().getConfiguration().get("hostname").toString()).start();
                } else if (SystemUtils.IS_OS_MAC_OSX){
                    proc = new ProcessBuilder("ping", "-t", "1", "-c", "1", getThing().getConfiguration().get("hostname").toString()).start();
                }
                int result = proc.waitFor();
                if (result == 0) {
                    updateStatus(ThingStatus.ONLINE);
                    pingCount = 0;
                } else {
                    if(pingCount >= 3) {
                        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Device not responding on ping");
                    } else {
                        pingCount++;
                    }
                }
                //logger.debug("ping {} result {}",getThing().getConfiguration().get("hostname").toString(), result);
            } catch (IOException | InterruptedException e) {
                logger.debug("proc error {}", e.getMessage());
            }
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
    public void manageValues(String command) {
        logger.debug("command: {}", command);
        logger.debug("host: {}", getThing().getConfiguration().get("hostname").toString());
        if(command != null) {
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
                                String[] commandsAdapt = {"", "", parsedStatus[i]};
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
                                if (megaportsHandler != null) {
                                    megaportsHandler.updateValues(mode, OnOffType.ON);
                                }
                            } else if (mode[0].contains("OFF")) {
                                if (megaportsHandler != null) {
                                    megaportsHandler.updateValues(mode, OnOffType.OFF);
                                }
                            } else {
                                if (megaportsHandler != null) {
                                    megaportsHandler.updateValues(mode, null);
                                }
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
                    } else if (command.contains("v=")) { // slave mode
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
            if (extenderBridgeHandlerMap != null) {
                if (command.contains("ext")) {
                    extenderBridgeHandlerMap.forEach((k, v) -> {
                        if (v.getThing().getConfiguration().get("int").equals(getCommands[1])) {
                            v.updateValues(getCommands);
                        }
                    });
                }
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

    @SuppressWarnings("null")
    @Override
    public void dispose() {
        if (refreshPollingJob != null && !refreshPollingJob.isCancelled()) {
            refreshPollingJob.cancel(true);
            refreshPollingJob = null;
        }
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

    //STANDART PORTS------------------------------------------------
    @SuppressWarnings({ "unused", "null" })
    public void registerMegadPortsListener(MegaDPortsHandler megaportsHandlerD) {
        String ip = megaportsHandlerD.getThing().getConfiguration().get("port").toString();
        logger.debug("Register Device with ip {} and port {}", getThing().getConfiguration().get("hostname").toString(),
                megaportsHandlerD.getThing().getConfiguration().get("port").toString());
        if (portsHandlerMap.get(ip) != null) {
            updateThingHandlerStatus(megaportsHandlerD, ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "port already exists");
        } else {
            portsHandlerMap.put(ip, megaportsHandlerD);
            updateThingHandlerStatus(megaportsHandlerD, ThingStatus.ONLINE);
        }
    }
    @SuppressWarnings("null")
    public void unregisterMegaDPortsListener(MegaDPortsHandler megaportsHandlerD) {
        String ip = megaportsHandlerD.getThing().getConfiguration().get("port").toString();
        if (portsHandlerMap.get(ip) != null) {
            portsHandlerMap.remove(ip);
            updateThingHandlerStatus(megaportsHandlerD, ThingStatus.OFFLINE);
        }
    }
    private void updateThingHandlerStatus(MegaDPortsHandler megaportsHandlerD, ThingStatus status,
                                          ThingStatusDetail statusDetail, String decript) {
        megaportsHandlerD.updateStatus(status, statusDetail, decript);
    }

    private void updateThingHandlerStatus(MegaDPortsHandler thingHandler, ThingStatus status) {
        thingHandler.updateStatus(status);
    }
    //STANDART PORTS------------------------------------------------
    //I2C --------------------------------------
    @SuppressWarnings({ "unused", "null" })
    public void registerMegadItoCListener(MegaDItoCHandler megaDItoCHandler) {
        String ip = megaDItoCHandler.getThing().getConfiguration().get("port").toString();
        logger.debug("Register Device with ip {} and port {}", getThing().getConfiguration().get("hostname").toString(),
                megaDItoCHandler.getThing().getConfiguration().get("port").toString());
        if (itoCHandlerMap.get(ip) != null) {
            updateThingHandlerStatus(megaDItoCHandler, ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "port already exists");
        } else {
            itoCHandlerMap.put(ip, megaDItoCHandler);
            updateThingHandlerStatus(megaDItoCHandler, ThingStatus.ONLINE);
        }
    }
    @SuppressWarnings("null")
    public void unregisterItoCListener(MegaDItoCHandler megaDItoCHandler) {
        String ip = megaDItoCHandler.getThing().getConfiguration().get("port").toString();
        if (itoCHandlerMap.get(ip) != null) {
            itoCHandlerMap.remove(ip);
            updateThingHandlerStatus(megaDItoCHandler, ThingStatus.OFFLINE);
        }
    }
    private void updateThingHandlerStatus(MegaDItoCHandler megaDItoCHandler, ThingStatus status,
                                          ThingStatusDetail statusDetail, String decript) {
        megaDItoCHandler.updateStatus(status, statusDetail, decript);
    }

    private void updateThingHandlerStatus(MegaDItoCHandler thingHandler, ThingStatus status) {
        thingHandler.updateStatus(status);
    }

    //I2C------------------------------------------
    //Extender
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
    public void unregisterMegaDPortsListener(MegaDBridgeExtenderPortHandler megaDBridgeExtenderPortHandler) {
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
    //Extender

//1WBRIDGE --------------------------------------------------------------------
    @SuppressWarnings({ "null", "unused" })
    public void registerMega1WireBusListener(MegaDBridge1WireBusHandler megaDBridge1WireBusHandler) {
        String oneWirePort = megaDBridge1WireBusHandler.getThing().getConfiguration().get("port").toString();

        if (oneWireBusBridgeHandlerMap.get(oneWirePort) != null) {
            updateThingHandlerStatus(megaDBridge1WireBusHandler, ThingStatus.OFFLINE,
                    ThingStatusDetail.CONFIGURATION_ERROR, "Device already exist");
        } else {
            oneWireBusBridgeHandlerMap.put(oneWirePort, megaDBridge1WireBusHandler);
            updateThingHandlerStatus(megaDBridge1WireBusHandler, ThingStatus.ONLINE);
            // megaDBridgeDeviceHandler.getAllPortsStatus();
        }
    }
    @SuppressWarnings("null")
    public void unregisterMegad1WireBridgeListener(MegaDBridge1WireBusHandler megaDBridge1WireBusHandler) {
        String ip = megaDBridge1WireBusHandler.getThing().getConfiguration().get("port").toString();
        if (oneWireBusBridgeHandlerMap.get(ip) != null) {
            oneWireBusBridgeHandlerMap.remove(ip);
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
//1WBRIDGE --------------------------------------------------------------------
}
