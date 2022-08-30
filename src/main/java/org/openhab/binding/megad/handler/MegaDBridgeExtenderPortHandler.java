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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.megad.internal.MegaHttpHelpers;
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
 * The {@link MegaDBridgeExtenderPortHandler} is responsible for creating MegaD extenders
 * based on MCP23008/MCP23017
 * this class represent bridge for port where extender is located
 *
 * @author Petr Shatsillo - Initial contribution
 */
@NonNullByDefault
public class MegaDBridgeExtenderPortHandler extends BaseBridgeHandler {
    @Nullable
    MegaDBridgeDeviceHandler bridgeDeviceHandler;
    private Logger logger = LoggerFactory.getLogger(MegaDBridgeExtenderPortHandler.class);
    private Map<Integer, MegaDExtenderHandler> extenderHandlerMap = new HashMap<Integer, MegaDExtenderHandler>();
    private Map<String, String> portsvalues = new HashMap<>();
    private boolean startedState = false;
    private @Nullable ScheduledFuture<?> refreshPollingJob;
    protected long lastRefresh = 0;
    @Nullable
    MegaDExtenderHandler megaDExtenderHandler;

    public MegaDBridgeExtenderPortHandler(Bridge bridge) {
        super(bridge);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
    }

    @SuppressWarnings({ "unused", "null" })
    @Override
    public void initialize() {
        bridgeDeviceHandler = getBridgeHandler();
        if (bridgeDeviceHandler != null) {
            registerMegaExtenderPortBridgeListener(bridgeDeviceHandler);
        } else {
            logger.debug("Can't register {} at bridge. BridgeHandler is null.", this.getThing().getUID());
        }
        String[] rr = { getThing().getConfiguration().get("refresh").toString() };// .split("[.]");
        logger.debug("Thing {}, refresh interval is {} sec", getThing().getUID().toString(), rr[0]);
        float msec = Float.parseFloat(rr[0]);
        int pollingPeriod = (int) (msec * 1000);
        if (refreshPollingJob == null || refreshPollingJob.isCancelled()) {
            refreshPollingJob = scheduler.scheduleWithFixedDelay(new Runnable() {
                @Override
                public void run() {
                    refresh(pollingPeriod);
                }
            }, 0, 100, TimeUnit.MILLISECONDS);
        }
    }

    @SuppressWarnings("null")
    public void refresh(int interval) {
        long now = System.currentTimeMillis();
        if (interval != 0) {
            if (now >= (lastRefresh + interval)) {
                String request = "http://"
                        + bridgeDeviceHandler.getThing().getConfiguration().get("hostname").toString() + "/"
                        + bridgeDeviceHandler.getThing().getConfiguration().get("password").toString() + "/?pt="
                        + getThing().getConfiguration().get("port").toString() + "&cmd=get";
                String updateRequest = MegaHttpHelpers.sendRequest(request);
                String[] getValues = updateRequest.split("[;]");
                for (int i = 0; getValues.length > i; i++) {
                    setPortsvalues(String.valueOf(i), getValues[i]);
                    megaDExtenderHandler = extenderHandlerMap.get(i);
                    if (megaDExtenderHandler != null) {
                        megaDExtenderHandler.update();
                    }
                }
                setStateStarted(true);

                lastRefresh = now;
            }
        }
    }

    @SuppressWarnings("null")
    public void updateValues(String[] getCommands) {
        String port = getCommands[2].substring(3);
        String action = getCommands[3];
        megaDExtenderHandler = extenderHandlerMap.get(Integer.parseInt(port));
        megaDExtenderHandler.updateValues(action);
    }

    private void setStateStarted(boolean b) {
        startedState = b;
    }

    public boolean getStateStarted() {
        return startedState;
    }

    private synchronized @Nullable MegaDBridgeDeviceHandler getBridgeHandler() {
        Bridge bridge = getBridge();
        if (bridge == null) {
            logger.warn("Required bridge not defined for device.");
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

    private void registerMegaExtenderPortBridgeListener(@Nullable MegaDBridgeDeviceHandler bridgeDeviceHandler) {
        if (bridgeDeviceHandler != null) {
            bridgeDeviceHandler.registerMegaExtenderPortListener(this);
        }
    }

    @SuppressWarnings({ "unused", "null" })
    public void registerExtenderListener(MegaDExtenderHandler megaDExtenderHandler) {
        String port = megaDExtenderHandler.getThing().getConfiguration().get("extport").toString();
        if (extenderHandlerMap.get(Integer.parseInt(port)) != null) {
            updateThingHandlerStatus(megaDExtenderHandler, ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "port already exists");
        } else {
            extenderHandlerMap.put(Integer.parseInt(port), megaDExtenderHandler);
            updateThingHandlerStatus(megaDExtenderHandler, ThingStatus.ONLINE);
        }
    }

    @SuppressWarnings("null")
    public void unregisterExtenderListener(@Nullable MegaDExtenderHandler megaDExtenderHandler) {
        String port = megaDExtenderHandler.getThing().getConfiguration().get("extport").toString();
        if (extenderHandlerMap.get(Integer.parseInt(port)) != null) {
            extenderHandlerMap.remove(Integer.parseInt(port));
            updateThingHandlerStatus(megaDExtenderHandler, ThingStatus.OFFLINE);
        }
    }

    private void updateThingHandlerStatus(MegaDExtenderHandler thingHandler, ThingStatus status) {
        thingHandler.updateStatus(status);
    }

    private void updateThingHandlerStatus(MegaDExtenderHandler megaDExtenderHandler, ThingStatus status,
            ThingStatusDetail statusDetail, String decript) {
        megaDExtenderHandler.updateStatus(status, statusDetail, decript);
    }

    @SuppressWarnings("null")
    public String[] getHostPassword() {
        String[] result = new String[] { bridgeDeviceHandler.getThing().getConfiguration().get("hostname").toString(),
                bridgeDeviceHandler.getThing().getConfiguration().get("password").toString() };
        return result;
    }

    public String getPortsvalues(String port) {
        return portsvalues.get(port).toString();
    }

    public void setPortsvalues(String key, String value) {
        portsvalues.put(key, value);
    }

    @Override
    public void updateStatus(ThingStatus status) {
        super.updateStatus(status);
    }

    @Override
    public void updateStatus(ThingStatus status, ThingStatusDetail statusDetail, @Nullable String description) {
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
            bridgeDeviceHandler.unregisterMegaDPortsListener(this);
        }
        super.dispose();
    }
}
