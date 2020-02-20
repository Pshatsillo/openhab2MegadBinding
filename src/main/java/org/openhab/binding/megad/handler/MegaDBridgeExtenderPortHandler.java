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
    //private @Nullable Map<String, MegaDBridgeExtenderPortHandler> extenderHandlerMap = new HashMap<>();
    private Map<String, String> portsvalues = new HashMap<>();
    private boolean startedState = false;
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

        String request = "http://" + bridgeDeviceHandler.getThing().getConfiguration().get("hostname").toString() + "/"
                + bridgeDeviceHandler.getThing().getConfiguration().get("password").toString() + "/?pt=" + getThing().getConfiguration().get("port").toString() + "&cmd=get";
        String updateRequest = MegaHttpHelpers.sendRequest(request);
        String[] getValues = updateRequest.split("[;]");
        for (int i = 0; getValues.length > i; i++) {
            setPortsvalues(String.valueOf(i), getValues[i]);
        }
        setStateStarted(true);
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
    @Override
    public void updateStatus(ThingStatus status) {
        super.updateStatus(status);
    }

    @Override
    public void updateStatus(ThingStatus status, ThingStatusDetail statusDetail, @Nullable String description) {
        super.updateStatus(status, statusDetail, description);
    }

    public String getPortsvalues(String port) {
        return portsvalues.get(port).toString();
    }

    public void setPortsvalues(String key, String value) {
        portsvalues.put(key, value);
    }
}
