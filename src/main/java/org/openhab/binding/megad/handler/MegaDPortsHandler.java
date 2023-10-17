/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.megad.internal.MegaDHTTPCallback;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link MegaDPortsHandler} is responsible for standart features of megsd
 *
 * @author Petr Shatsillo - Initial contribution
 */
@NonNullByDefault
public class MegaDPortsHandler extends BaseThingHandler {

    private Logger logger = LoggerFactory.getLogger(MegaDPortsHandler.class);

    private @Nullable ScheduledFuture<?> refreshPollingJob;
    @Nullable
    public MegaDDeviceHandler bridgeDeviceHandler;

    public MegaDPortsHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
    }

    @Override
    public void initialize() {
        // Map<Integer, MegaDPortsHandler> portListener = MegaDHTTPCallback.portListener;
        bridgeDeviceHandler = getBridgeHandler();
        if (bridgeDeviceHandler != null) {
            MegaDHTTPCallback.portListener.add(this);
            String[] rr = { getThing().getConfiguration().get("refresh").toString() };// .split("[.]");
            logger.debug("Thing {}, refresh interval is {} sec", getThing().getUID(), rr[0]);
            int pollingPeriod = Integer.parseInt(rr[0]);
            ScheduledFuture<?> refreshPollingJob = this.refreshPollingJob;
            if (refreshPollingJob == null || refreshPollingJob.isCancelled()) {
                refreshPollingJob = scheduler.scheduleWithFixedDelay(this::refresh, 0, pollingPeriod, TimeUnit.SECONDS);
                this.refreshPollingJob = refreshPollingJob;
            }
            updateStatus(ThingStatus.ONLINE);
        } else {
            updateStatus(ThingStatus.UNINITIALIZED, ThingStatusDetail.BRIDGE_UNINITIALIZED, "Bridge is not defined");
        }
    }

    public void updatePort(String input) {
        logger.debug("Updating port {}", input);
    }

    public void refresh() {
        logger.debug("Refresh port");
        // long now = System.currentTimeMillis();
    }

    @Override
    public void updateStatus(ThingStatus status) {
        super.updateStatus(status);
    }

    @Override
    protected void updateStatus(ThingStatus status, ThingStatusDetail statusDetail, @Nullable String description) {
        super.updateStatus(status, statusDetail, description);
    }

    private synchronized @Nullable MegaDDeviceHandler getBridgeHandler() {
        Bridge bridge = getBridge();
        if (bridge == null) {
            logger.error("Required bridge not defined for device.");
            return null;
        } else {
            return getBridgeHandler(bridge);
        }
    }

    private synchronized @Nullable MegaDDeviceHandler getBridgeHandler(Bridge bridge) {
        ThingHandler handler = bridge.getHandler();
        if (handler instanceof MegaDDeviceHandler) {
            return (MegaDDeviceHandler) handler;
        } else {
            logger.debug("No available bridge handler found yet. Bridge: {} .", bridge.getUID());
            return null;
        }
    }

    @Override
    public void dispose() {
        ScheduledFuture<?> refreshPollingJob = this.refreshPollingJob;
        if (refreshPollingJob != null && !refreshPollingJob.isCancelled()) {
            refreshPollingJob.cancel(true);
        }
        this.refreshPollingJob = null;
        super.dispose();
    }
}
