package org.openhab.binding.megad.handler;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MegaD1WireSensorHandler extends BaseThingHandler {

    private Logger logger = LoggerFactory.getLogger(MegaD1WireSensorHandler.class);
    private @Nullable ScheduledFuture<?> refreshPollingJob;

    @Nullable
    MegaDBridgeDeviceHandler bridgeDeviceHandler;

    public MegaD1WireSensorHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
    }

    @Override
    public void dispose() {
        super.dispose();
    }

    @Override
    public void initialize() {
        bridgeDeviceHandler = getBridgeHandler();
        // logger.debug("Thing Handler for {} started", getThing().getUID().getId());
        if (bridgeDeviceHandler != null) {
            registerMegad1WireListener(bridgeDeviceHandler);
        } else {
            logger.debug("Can't register {} at bridge. BridgeHandler is null.", this.getThing().getUID());
        }

        String[] rr = getThing().getConfiguration().get("refresh").toString().split("[.]");
        logger.debug("refresh: {}", rr[0]);

        if (refreshPollingJob == null || refreshPollingJob.isCancelled()) {
            refreshPollingJob = scheduler.scheduleWithFixedDelay(new Runnable() {
                @Override
                public void run() {
                    int pollingPeriod = Integer.parseInt(rr[0]) * 1000;
                    // refresh(pollingPeriod);
                }
            }, 0, 1000, TimeUnit.MILLISECONDS);
        }
    }

    private synchronized @Nullable MegaDBridgeDeviceHandler getBridgeHandler() {
        Bridge bridge = getBridge();
        if (bridge == null) {
            logger.error("Required bridge not defined for device {}.");
            // throw new NullPointerException("Required bridge not defined for device");
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

    @Override
    public void updateStatus(ThingStatus status) {
        super.updateStatus(status);
    }

    @Override
    protected void updateStatus(ThingStatus status, ThingStatusDetail statusDetail, @Nullable String description) {
        super.updateStatus(status, statusDetail, description);
    }

    private void registerMegad1WireListener(@Nullable MegaDBridgeDeviceHandler bridgeHandler) {
        if (bridgeHandler != null) {
            bridgeHandler.registerMegad1WireListener(this);
        }
    }

}
