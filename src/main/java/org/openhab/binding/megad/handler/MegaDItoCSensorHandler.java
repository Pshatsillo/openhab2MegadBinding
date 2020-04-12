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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * The {@link MegaDItoCSensorHandler} class defines I2C bus feature.
 * You can read I2C sensors connected to one port of MegaD as bus
 *
 * @author Petr Shatsillo - Initial contribution
 */
@NonNullByDefault
public class MegaDItoCSensorHandler extends BaseThingHandler {
    private Logger logger = LoggerFactory.getLogger(MegaDItoCSensorHandler.class);
    @Nullable
    MegaDBridgeIToCBridgeHandler bridgeDeviceHandler;
    private @Nullable ScheduledFuture<?> refreshPollingJob;
    protected long lastRefresh = 0;

    /**
     * Creates a new instance of this class for the {@link Thing}.
     *
     * @param thing the thing that should be handled, not null
     */
    public MegaDItoCSensorHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {

    }

    @Override
    public void initialize() {
        bridgeDeviceHandler = getBridgeHandler();
        logger.debug("Thing Handler for {} started", getThing().getUID().getId());

        String[] rr = getThing().getConfiguration().get("refresh").toString().split("[.]");
        logger.debug("port {}, refresh interval is {} sec",bridgeDeviceHandler.getThing().getConfiguration().get("port").toString(), rr[0]);
        int pollingPeriod = Integer.parseInt(rr[0]) * 1000;
        if (refreshPollingJob == null || refreshPollingJob.isCancelled()) {
            refreshPollingJob = scheduler.scheduleWithFixedDelay(new Runnable() {
                @Override
                public void run() {
                    refresh(pollingPeriod);
                }
            }, 0, 1000, TimeUnit.MILLISECONDS);
        }
        updateStatus(ThingStatus.ONLINE);
    }

    public void refresh(int interval) {
        long now = System.currentTimeMillis();
        if (interval != 0) {
            if (now >= (lastRefresh + interval)) {
                logger.debug("refresh {}", getThing().getUID().toString());
                lastRefresh = now;
            }
        }

    }

    //-------------------------------------------------------------------
    private synchronized @Nullable MegaDBridgeIToCBridgeHandler getBridgeHandler() {
        Bridge bridge = getBridge();
        if (bridge == null) {
            logger.warn("Required bridge not defined for device.");
            return null;
        } else {
            return getBridgeHandler(bridge);
        }
    }

    private synchronized @Nullable MegaDBridgeIToCBridgeHandler getBridgeHandler(Bridge bridge) {
        ThingHandler handler = bridge.getHandler();
        if (handler instanceof MegaDBridgeIToCBridgeHandler) {
            return (MegaDBridgeIToCBridgeHandler) handler;
        } else {
            logger.debug("No available bridge handler found yet. Bridge: {} .", bridge.getUID());
            return null;
        }
    }
    @SuppressWarnings("null")
    @Override
    public void dispose() {
        if (refreshPollingJob != null && !refreshPollingJob.isCancelled()) {
            refreshPollingJob.cancel(true);
            refreshPollingJob = null;
        }
        super.dispose();
    }
}
