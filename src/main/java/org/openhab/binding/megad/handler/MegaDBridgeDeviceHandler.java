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
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.megad.internal.MegaHttpHelpers;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseBridgeHandler;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.types.Command;
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
    private final Logger logger = LoggerFactory.getLogger(MegaDBridgeDeviceHandler.class);
    private @Nullable final Map<String, MegaDPortsHandler> portsHandlerMap = new HashMap<>();
    private @Nullable final Map<String, MegaDItoCHandler> itoCHandlerMap = new HashMap<>();
    private @Nullable final Map<String, MegaDBridge1WireBusHandler> oneWireBusBridgeHandlerMap = new HashMap<>();
    private @Nullable final Map<String, MegaDBridgeExtenderPortHandler> extenderBridgeHandlerMap = new HashMap<>();
    private @Nullable final Map<String, MegaDBridgeExtenderPCA9685Handler> extenderPCA9685BridgeHandlerMap = new HashMap<>();
    private @Nullable final Map<String, MegaDEncoderHandler> megaDEncoderHandlerMap = new HashMap<>();
    private @Nullable final ArrayList<MegaDRs485Handler> megaDRs485HandlerMap = new ArrayList<>();
    private final Map<String, String> portsvalues = new HashMap<>();
    private @Nullable ScheduledFuture<?> refreshPollingJob;
    protected long lastRefresh = 0;
    int pingCount;

    @Nullable
    MegaDBridgeIncomingHandler bridgeIncomingHandler;
    @Nullable
    MegaDPortsHandler megaportsHandler;
    @Nullable
    MegaDEncoderHandler megaDEncoderHandler;

    public MegaDBridgeDeviceHandler(Bridge bridge) {
        super(bridge);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
    }

    @SuppressWarnings("null")
    @Override
    public void initialize() {
        bridgeIncomingHandler = getBridgeHandler();
        logger.debug("Thing Handler for {} started", getThing().getUID().getId());

        if (bridgeIncomingHandler != null) {
            registerMegaDeviceListener(bridgeIncomingHandler);
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
        } else {
            logger.warn("Can't register {} at bridge. BridgeHandler is null.", this.getThing().getUID());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE,
                    "Bridge for incoming connections not selected");
        }
    }

    @SuppressWarnings("null")
    private void refresh() {
        try {
            Socket sck = new Socket(getThing().getConfiguration().get("hostname").toString(), 80);
            updateStatus(ThingStatus.ONLINE);
            sck.close();
        } catch (IOException e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Device not responding on ping");
            // logger.debug("proc error {}", e.getMessage());

        }

        long now = System.currentTimeMillis();
        if (megaDRs485HandlerMap != null && !megaDRs485HandlerMap.isEmpty()) {
            try {
                for (MegaDRs485Handler handler : megaDRs485HandlerMap) {
                    String address = handler.getThing().getConfiguration().get("address").toString();
                    // logger.debug("address: {}", address);
                    int interval = Integer.parseInt(handler.getThing().getConfiguration().get("refresh").toString());
                    if (interval != 0) {
                        if (now >= (handler.getLastRefresh() + (interval * 1000L))) {
                            handler.updateData();
                            handler.lastrefreshAdd(now);
                            try {
                                Thread.sleep(200);
                            } catch (InterruptedException ignored) {
                            }

                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }
    }

    private synchronized @Nullable MegaDBridgeIncomingHandler getBridgeHandler() {
        Bridge bridge = getBridge();
        if (bridge == null) {
            logger.warn("Required bridge not defined for device.");
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE,
                    "Bridge for incoming connections not selected");
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
        if (command != null) {
            String[] getCommands = command.split("[?&>=]");
            String[] prm = command.split("[&]");
            HashMap<String, String> params = new HashMap<>();

            for (String param : prm) {
                try {
                    params.put(param.split("=")[0], param.split("=")[1]);
                } catch (Exception e) {
                    params.put(param.split("=")[0], " ");
                }
            }

            assert portsHandlerMap != null;
            if (!portsHandlerMap.isEmpty()) {
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
                        String[] parsedStatus = {};
                        try {
                            parsedStatus = getCommands[2].split("[;]");
                        } catch (Exception ex) {
                            parsedStatus = getCommands[1].split("[;]");
                        }
                        for (int i = 0; parsedStatus.length > i; i++) {
                            megaportsHandler = portsHandlerMap.get(String.valueOf(i));
                            String[] mode = parsedStatus[i].split("[/]");
                            if (mode[0].equals("ON")) {
                                if (megaportsHandler != null) {
                                    megaportsHandler.updateValues(mode, OnOffType.ON);
                                }
                            } else if (mode[0].equals("OFF")) {
                                if (megaportsHandler != null) {
                                    megaportsHandler.updateValues(mode, OnOffType.OFF);
                                }
                            } else {
                                if (megaportsHandler != null) {
                                    String[] commands = { "", "", mode[0] };
                                    megaportsHandler.updateValues(commands, null);
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
                            if (params.get("v").equals("0")) {
                                String[] commandmod = { "", "v", params.get("v") };
                                megaportsHandler.updateValues(commandmod, OnOffType.OFF);
                            } else {
                                String[] commandmod = { "", "", params.get("v") };
                                megaportsHandler.updateValues(commandmod, OnOffType.ON);
                            }
                        }
                    } else {
                        if ((getCommands[0].equals("st")) || (getCommands[0].equals("sms_phone"))) {
                            logger.debug("{}", portsHandlerMap.size());

                            String request = "http://" + getThing().getConfiguration().get("hostname").toString() + "/"
                                    + getThing().getConfiguration().get("password").toString() + "/?cmd=all";
                            String updateRequest = MegaHttpHelpers.sendRequest(request);
                            String[] getValues = updateRequest.split("[;]");
                            for (int i = 0; getValues.length > i; i++) {
                                String[] val = { "", "", getValues[i] };
                                megaportsHandler = portsHandlerMap.get(String.valueOf(i));
                                if (megaportsHandler != null) {
                                    if (val[2].contains("ON")) {
                                        megaportsHandler.updateValues(val, OnOffType.ON);
                                    } else if (val[2].contains("OFF")) {
                                        megaportsHandler.updateValues(val, OnOffType.OFF);
                                    } else {
                                        megaportsHandler.updateValues(val, null);
                                    }
                                }
                            }

                            for (int i = 0; portsHandlerMap.size() > i; i++) {
                                megaportsHandler = portsHandlerMap
                                        .get(portsHandlerMap.keySet().toArray()[i].toString());
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
            if (!extenderBridgeHandlerMap.isEmpty()) {
                if (command.contains("ext")) {
                    extenderBridgeHandlerMap.forEach((k, v) -> {
                        String intprm = v.getThing().getConfiguration().get("int").toString();
                        if (intprm.equals(getCommands[1])) {
                            v.updateValues(getCommands);
                        }
                    });
                }
            }
            assert megaDEncoderHandlerMap != null;
            if (!megaDEncoderHandlerMap.isEmpty()) {
                megaDEncoderHandler = megaDEncoderHandlerMap.get(getCommands[1]);
                megaDEncoderHandler.updateValues(getCommands[3]);
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
        logger.debug("All ports of device {} is {}", getThing().getConfiguration().get("hostname").toString(),
                updateRequest);
    }

    @SuppressWarnings("null")
    @Override
    public void dispose() {
        if (refreshPollingJob != null && !refreshPollingJob.isCancelled()) {
            refreshPollingJob.cancel(true);
            refreshPollingJob = null;
        }
        if (bridgeIncomingHandler != null) {
            bridgeIncomingHandler.unregisterMegaDeviceListener(this);
        }
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
            portvalue[2] = null;
        }
        portvalue[3] = "";
        portvalue[4] = "";
        return portvalue;
    }

    public void setPortsvalues(String key, String value) {
        portsvalues.put(key, value);
    }

    // STANDART PORTS------------------------------------------------
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

    // STANDART PORTS------------------------------------------------
    // I2C --------------------------------------
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

    // I2C------------------------------------------
    // Extender
    @SuppressWarnings({ "unused", "null" })
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
    /* Maybe error, see unregisterMegaExtenderPCA9685Listener */
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

    private void updateThingHandlerStatus(MegaDBridgeExtenderPortHandler megaDBridgeExtenderPortHandler,
            ThingStatus status, ThingStatusDetail statusDetail, String decript) {
        megaDBridgeExtenderPortHandler.updateStatus(status, statusDetail, decript);
    }
    // Extender

    // PCA9685
    @SuppressWarnings({ "unused", "null" })
    public void registerMegaDBridgeExtenderPCA9685Listener(MegaDBridgeExtenderPCA9685Handler bridge) {
        String port = bridge.getThing().getConfiguration().get("port").toString();
        if (extenderPCA9685BridgeHandlerMap.get(port) != null) {
            updateThingHandlerStatus(bridge, ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "Device already exist");
        } else {
            extenderPCA9685BridgeHandlerMap.put(port, bridge);
            updateThingHandlerStatus(bridge, ThingStatus.ONLINE);
        }
    }

    @SuppressWarnings("null")
    public void unregisterMegaDBridgeExtenderPCA9685Listener(MegaDBridgeExtenderPCA9685Handler bridge) {
        String port = bridge.getThing().getConfiguration().get("port").toString();
        if (extenderPCA9685BridgeHandlerMap.get(port) != null) {
            extenderPCA9685BridgeHandlerMap.remove(port);
            updateThingHandlerStatus(bridge, ThingStatus.OFFLINE);
        }
    }

    private void updateThingHandlerStatus(MegaDBridgeExtenderPCA9685Handler thingHandler, ThingStatus status) {
        thingHandler.updateStatus(status);
    }

    private void updateThingHandlerStatus(MegaDBridgeExtenderPCA9685Handler megaDBridgeExtenderPCA9685Handler,
            ThingStatus status, ThingStatusDetail statusDetail, String decript) {
        megaDBridgeExtenderPCA9685Handler.updateStatus(status, statusDetail, decript);
    }

    // PCA9685

    // 1WBRIDGE --------------------------------------------------------------------
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

    // 1WBRIDGE --------------------------------------------------------------------

    // ENCODER

    @SuppressWarnings({ "null", "unused" })
    public void registerMegadEncoderListener(MegaDEncoderHandler megaDEncoderHandler) {
        String extenderPort = megaDEncoderHandler.getThing().getConfiguration().get("int").toString();

        if (megaDEncoderHandlerMap.get(extenderPort) != null) {
            updateThingHandlerStatus(megaDEncoderHandler, ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "Device already exist");
        } else {
            megaDEncoderHandlerMap.put(extenderPort, megaDEncoderHandler);
            updateThingHandlerStatus(megaDEncoderHandler, ThingStatus.ONLINE);
        }
    }

    public void unregisterMegaDEncoderListener(MegaDEncoderHandler megaDEncoderHandler) {
        String extenderPort = megaDEncoderHandler.getThing().getConfiguration().get("int").toString();
        if (oneWireBusBridgeHandlerMap.get(extenderPort) != null) {
            oneWireBusBridgeHandlerMap.remove(extenderPort);
            updateThingHandlerStatus(megaDEncoderHandler, ThingStatus.OFFLINE);
        }
    }

    private void updateThingHandlerStatus(MegaDEncoderHandler megaDEncoderHandler, ThingStatus status) {
        megaDEncoderHandler.updateStatus(status);
    }

    private void updateThingHandlerStatus(MegaDEncoderHandler megaDEncoderHandler, ThingStatus status,
            ThingStatusDetail statusDetail, String decript) {
        megaDEncoderHandler.updateStatus(status, statusDetail, decript);
    }

    // RS485
    public void registerMegaRs485Listener(MegaDRs485Handler megaDrs485Handler) {
        String rs485Address = megaDrs485Handler.getThing().getConfiguration().get("address").toString();

        if (megaDRs485HandlerMap != null && !megaDRs485HandlerMap.isEmpty()) {
            boolean isexist = false;
            for (MegaDRs485Handler handler : megaDRs485HandlerMap) {
                if (rs485Address.equals(handler.getThing().getConfiguration().get("address").toString())) {
                    logger.debug("Device already exist");
                    isexist = true;
                }
            }
            if (!isexist) {
                megaDRs485HandlerMap.add(megaDrs485Handler);
            }
        } else {
            megaDRs485HandlerMap.add(megaDrs485Handler);
        }
    }

    public void unregisterMegadRs485Listener(MegaDRs485Handler megaDrs485Handler) {
        String rs485Address = megaDrs485Handler.getThing().getConfiguration().get("address").toString();
        if (megaDRs485HandlerMap != null) {
            megaDRs485HandlerMap.removeIf(
                    handler -> rs485Address.equals(handler.getThing().getConfiguration().get("address").toString()));
        }
    }
}
