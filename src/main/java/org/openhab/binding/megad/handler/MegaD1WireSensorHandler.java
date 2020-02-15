package org.openhab.binding.megad.handler;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.megad.MegaDBindingConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MegaD1WireSensorHandler extends BaseThingHandler {

    private Logger logger = LoggerFactory.getLogger(MegaD1WireSensorHandler.class);
    private @Nullable ScheduledFuture<?> refreshPollingJob;
    boolean startup = true;
    protected long lastRefresh = 0;

    @Nullable
    MegaDBridge1WireBusHandler bridgeDeviceHandler;

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

    @SuppressWarnings("null")
    @Override
    public void initialize() {
        bridgeDeviceHandler = getBridgeHandler();
        // logger.debug("Thing Handler for {} started", getThing().getUID().getId());
        if (bridgeDeviceHandler != null) {
            registerMegad1WireListener(bridgeDeviceHandler);
        } else {
            logger.debug("Can't register {} at bridge. BridgeHandler is null.", this.getThing().getUID());
        }

        // logger.debug("refresh: {}", rr);

        if (refreshPollingJob == null || refreshPollingJob.isCancelled()) {
            refreshPollingJob = scheduler.scheduleWithFixedDelay(new Runnable() {
                @Override
                public void run() {
                    refresh(30000);
                }
            }, 0, 1000, TimeUnit.MILLISECONDS);
        }
    }

    @SuppressWarnings({ "null" })
    public void refresh(int interval) {
        long now = System.currentTimeMillis();
        if (startup) {

        }
        if (interval != 0) {
            if (now >= (lastRefresh + interval)) {

                for (Channel channel : getThing().getChannels()) {
                    if (isLinked(channel.getUID().getId())) {
                        if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_1WTEMP)) {
                            String address = getThing().getConfiguration().get("address").toString();
                            updateState(channel.getUID().getId(),
                                    DecimalType.valueOf(bridgeDeviceHandler.getOwvalues(address)));
                        }
                    }
                }

                lastRefresh = now;
            }
        }
    }

    private synchronized @Nullable MegaDBridge1WireBusHandler getBridgeHandler() {
        Bridge bridge = getBridge();
        if (bridge == null) {
            logger.error("Required bridge not defined for device {}.");
            // throw new NullPointerException("Required bridge not defined for device");
            return null;
        } else {
            return getBridgeHandler(bridge);
        }
    }

    private synchronized @Nullable MegaDBridge1WireBusHandler getBridgeHandler(Bridge bridge) {
        ThingHandler handler = bridge.getHandler();
        if (handler instanceof MegaDBridge1WireBusHandler) {
            return (MegaDBridge1WireBusHandler) handler;
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

    private void registerMegad1WireListener(@Nullable MegaDBridge1WireBusHandler bridgeHandler) {
        if (bridgeHandler != null) {
            bridgeHandler.registerMegad1WireListener(this);
        }
    }

}
