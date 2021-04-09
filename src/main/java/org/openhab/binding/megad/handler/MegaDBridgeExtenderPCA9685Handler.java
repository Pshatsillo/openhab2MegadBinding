package org.openhab.binding.megad.handler;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
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
 * The {@link MegaDBridgeExtenderPCA9685Handler} is responsible for creating MegaD extenders
 * based on PCA9685
 * this class represent bridge for port where extender is located
 *
 * @author kosh_ - Initial contribution
 */
@NonNullByDefault
public class MegaDBridgeExtenderPCA9685Handler extends BaseBridgeHandler {
    @Nullable
    MegaDBridgeDeviceHandler BridgeDevice;
    private Logger logger = LoggerFactory.getLogger(MegaDBridgeExtenderPCA9685Handler.class);
    private Map<String, MegaDExtenderPCA9685Handler> mapThings = new HashMap<String, MegaDExtenderPCA9685Handler>();
    private boolean startedState = false;
    private @Nullable ScheduledFuture<?> refreshPollingJob;
    protected long lastRefresh = 0;
    @Nullable
    MegaDExtenderPCA9685Handler Thing;

    public MegaDBridgeExtenderPCA9685Handler(Bridge bridge) {
        super(bridge);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
    }

    @SuppressWarnings({ "unused", "null" })
    @Override
    public void initialize() {
        BridgeDevice = getBridgeHandler();
		registerListenerBridge(BridgeDevice);
        String refresh = getThing().getConfiguration().get("refresh").toString();
        logger.debug("Thing {}, refresh interval is {} sec", getThing().getUID().toString(), refresh);
        float msec = Float.parseFloat(refresh);
        int pollingPeriod = (int) (msec * 1000);
        if (refreshPollingJob == null || refreshPollingJob.isCancelled()) {
            refreshPollingJob = scheduler.scheduleWithFixedDelay(
				new Runnable() {
					@Override
					public void run() {
						refresh(pollingPeriod);
					}
				},
				0,
				100,
				TimeUnit.MILLISECONDS
			);
        }
    }

    @SuppressWarnings("null")
    public void refresh(int interval) {
        long now = System.currentTimeMillis();
        if (interval != 0) {
            if (now >= (lastRefresh + interval)) {
                mapThings.forEach((k, v) -> {
                    v.updateData(k);
                });
                setStateStarted(true);
                lastRefresh = now;
            }
        }
    }

    @SuppressWarnings("null")
    public void updateValues(String[] getCommands) {
        String port = getCommands[2].substring(3);
        String action = getCommands[3];
        Thing = mapThings.get(String.valueOf(port));
        Thing.updateValues(action);
        logger.warn("Required bridge not defined for device.");
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

    private void registerListenerBridge(@Nullable MegaDBridgeDeviceHandler bridgeDevice) {
        if (bridgeDevice != null) {
            bridgeDevice.registerMegaDBridgeExtenderPCA9685Listener(this);
        } else {
			logger.debug("Can't register {} at bridge. BridgeHandler is null.", this.getThing().getUID());
		}
    }

    @SuppressWarnings({ "unused", "null" })
    public void registerListenerThing(MegaDExtenderPCA9685Handler thing) {
        String extport = thing.getThing().getConfiguration().get("extport").toString();
        if (mapThings.get(extport) != null) {
            updateThingStatus(
				thing,
				ThingStatus.OFFLINE,
                ThingStatusDetail.CONFIGURATION_ERROR,
				"port already exists"
			);
        } else {
            mapThings.put(extport, thing);
            updateThingStatus(
				thing,
				ThingStatus.ONLINE
			);
        }
    }

    @SuppressWarnings("null")
    public void unregisterListenerThing(@Nullable MegaDExtenderPCA9685Handler thing) {
        String extport = thing.getThing().getConfiguration().get("extport").toString();
        if (mapThings.get(extport) != null) {
            mapThings.remove(extport);
            updateThingStatus(
				thing,
				ThingStatus.OFFLINE
			);
        }
    }

    private void updateThingStatus(MegaDExtenderPCA9685Handler thing, ThingStatus status) {
        thing.updateStatus(status);
    }

    private void updateThingStatus(MegaDExtenderPCA9685Handler thing, ThingStatus status, ThingStatusDetail statusDetail, String decript) {
		thing.updateStatus(status, statusDetail, decript);
    }

    @SuppressWarnings("null")
    public String[] getHostPassword() {
        String[] result = new String[] { BridgeDevice.getThing().getConfiguration().get("hostname").toString(),
			BridgeDevice.getThing().getConfiguration().get("password").toString() };
        return result;
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
        if (BridgeDevice != null) {
            BridgeDevice.unregisterMegaDBridgeExtenderPCA9685Listener(this);
        }
        super.dispose();
    }
}
