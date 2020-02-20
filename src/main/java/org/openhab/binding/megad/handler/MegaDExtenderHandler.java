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
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link MegaDExtenderHandler} is responsible for creating MegaD extenders
 * based on MCP23008/MCP23017
 * this class represent bridge for port where extender is located
 *
 * @author Petr Shatsillo - Initial contribution
 */
@NonNullByDefault
public class MegaDExtenderHandler extends BaseThingHandler {
    @Nullable
    MegaDBridgeExtenderPortHandler extenderPortBridge;
    private Logger logger = LoggerFactory.getLogger(MegaDExtenderHandler.class);

    public MegaDExtenderHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
    }
    @SuppressWarnings("null")
    @Override
    public void initialize() {
        extenderPortBridge = getBridgeHandler();
        //String port = getThing().getConfiguration().get("extport").toString();
        if(extenderPortBridge != null) {
            while (!extenderPortBridge.getStateStarted()) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    logger.error("{}", e.getMessage());
                }
            }
            //String portValue = extenderPortBridge.getPortsvalues(port);
            logger.debug("Extender port value is {}", extenderPortBridge.getPortsvalues(getThing().getConfiguration().get("port").toString()));
        }
    }

    private synchronized @Nullable MegaDBridgeExtenderPortHandler getBridgeHandler() {
        Bridge bridge = getBridge();
        if (bridge == null) {
            logger.error("Required bridge not defined for device.");
            return null;
        } else {
            return getBridgeHandler(bridge);
        }
    }

    private synchronized @Nullable MegaDBridgeExtenderPortHandler getBridgeHandler(Bridge bridge) {
        ThingHandler handler = bridge.getHandler();
        if (handler instanceof MegaDBridgeExtenderPortHandler) {
            return (MegaDBridgeExtenderPortHandler) handler;
        } else {
            logger.debug("No available bridge handler found yet. Bridge: {} .", bridge.getUID());
            return null;
        }
    }
}
