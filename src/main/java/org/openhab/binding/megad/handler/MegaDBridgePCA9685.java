package org.openhab.binding.megad.handler;

import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.megad.internal.MegaHttpHelpers;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.binding.BaseBridgeHandler;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

public class MegaDBridgePCA9685 extends BaseBridgeHandler {
    @Nullable
    MegaDBridgeDeviceHandler bridgeDeviceHandler;
    private Logger logger = LoggerFactory.getLogger(MegaDBridgePCA9685.class);
    private @Nullable ScheduledFuture<?> refreshPollingJob;
    protected long lastRefresh = 0;
    private Map<String, String> portsvalues = new HashMap<>();
    private boolean startedState = false;

    public MegaDBridgePCA9685(Bridge bridge) {
        super(bridge);
    }

    @Override
    public void initialize() {
        bridgeDeviceHandler = getBridgeHandler();
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {

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
}
