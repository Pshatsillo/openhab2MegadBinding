package org.openhab.binding.megad.handler;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

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

public class MegaDBridgePCA9685EPHandler extends BaseBridgeHandler {
    @Nullable
    MegaDBridgeDeviceHandler bridgeDeviceHandler;
    private Logger logger = LoggerFactory.getLogger(MegaDBridgePCA9685EPHandler.class);
    private @Nullable ScheduledFuture<?> refreshPollingJob;
    protected long lastRefresh = 0;
    private Map<String, String> portsvalues = new HashMap<>();
    private boolean startedState = false;

    public MegaDBridgePCA9685EPHandler(Bridge bridge) {
        super(bridge);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {

    }

    @Override
    public void initialize() {
        bridgeDeviceHandler = getBridgeHandler();
        if (bridgeDeviceHandler != null) {
            registerMegaPCA9685EPBridgeListener(bridgeDeviceHandler);
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

    // >>

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
                    megaDExtenderHandler = extenderHandlerMap.get(String.valueOf(i));
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
        megaDExtenderHandler = extenderHandlerMap.get(String.valueOf(port));
        megaDExtenderHandler.updateValues(action);
        logger.warn("Required bridge not defined for device.");
    }

    private void setStateStarted(boolean b) {
        startedState = b;
    }

    public boolean getStateStarted() {
        return startedState;
    }

    // <<

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

    private void registerMegaPCA9685EPBridgeListener(@Nullable MegaDBridgeDeviceHandler bridgeDeviceHandler) {
        if (bridgeDeviceHandler != null) {
            bridgeDeviceHandler.registerMegaPCA9685EPListener(this);
        }
    }

    // >>
    @SuppressWarnings({ "unused", "null" })
    public void registerExtenderListener(MegaDExtenderHandler megaDExtenderHandler) {
        String port = megaDExtenderHandler.getThing().getConfiguration().get("extport").toString();
        if (extenderHandlerMap.get(port) != null) {
            updateThingHandlerStatus(megaDExtenderHandler, ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "port already exists");
        } else {
            extenderHandlerMap.put(port, megaDExtenderHandler);
            updateThingHandlerStatus(megaDExtenderHandler, ThingStatus.ONLINE);
        }
    }

    @SuppressWarnings("null")
    public void unregisterExtenderListener(@Nullable MegaDExtenderHandler megaDExtenderHandler) {
        String port = megaDExtenderHandler.getThing().getConfiguration().get("extport").toString();
        if (extenderHandlerMap.get(port) != null) {
            extenderHandlerMap.remove(port);
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

    // <<
}
