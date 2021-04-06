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
 * The {@link MegaDBridgeExtenderPCA9685Handler} is responsible for creating MegaD extenders
 * based on PCA9685
 * this class represent bridge for port where extender is located
 *
 * @author kosh_ - Initial contribution
 */
@NonNullByDefault
public class MegaDBridgeExtenderPCA9685Handler extends BaseBridgeHandler {
    @Nullable
    MegaDBridgeDeviceHandler bridgeDeviceHandler;
    private Logger logger = LoggerFactory.getLogger(MegaDBridgeExtenderPCA9685Handler.class);
    private Map<String, MegaDExtenderPCA9685Handler> extenderPCA9685HandlerMap = new HashMap<String, MegaDExtenderPCA9685Handler>();
    private Map<String, String> portsvalues = new HashMap<>();
    private boolean startedState = false;
    private @Nullable ScheduledFuture<?> refreshPollingJob;
    protected long lastRefresh = 0;
    @Nullable
    MegaDExtenderPCA9685Handler MegaDExtenderPCA9685Handler;

    public MegaDBridgeExtenderPCA9685Handler(Bridge bridge) {
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
            registerMegaExtenderPCA9685BridgeListener(bridgeDeviceHandler);
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
                String hostname = getHostPassword()[0];
                String password = getHostPassword()[1];
                String port = getThing().getConfiguration().get("port").toString();
                String request = "http://" + hostname + "/" + password + "/?pt=" + port + "&cmd=get";
                String updateRequest = MegaHttpHelpers.sendRequest(request);
                String[] getValues = updateRequest.split("[;]");
                for (int i = 0; getValues.length > i; i++) {
                    setPortsvalues(String.valueOf(i), getValues[i]);
                    MegaDExtenderPCA9685Handler = extenderPCA9685HandlerMap.get(String.valueOf(i));
                    if (MegaDExtenderPCA9685Handler != null) {
                        MegaDExtenderPCA9685Handler.update();
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
        MegaDExtenderPCA9685Handler = extenderPCA9685HandlerMap.get(String.valueOf(port));
        MegaDExtenderPCA9685Handler.updateValues(action);
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

    private void registerMegaExtenderPCA9685BridgeListener(@Nullable MegaDBridgeDeviceHandler bridgeDeviceHandler) {
        if (bridgeDeviceHandler != null) {
            bridgeDeviceHandler.registerMegaExtenderPCA9685Listener(this);
        }
    }

    @SuppressWarnings({ "unused", "null" })
    public void registerExtenderPCA9685Listener(MegaDExtenderPCA9685Handler megaDExtenderPCA9685Handler) {
        String port = megaDExtenderPCA9685Handler.getThing().getConfiguration().get("extport").toString();
        if (extenderPCA9685HandlerMap.get(port) != null) {
            updateThingHandlerStatus(megaDExtenderPCA9685Handler, ThingStatus.OFFLINE,
                    ThingStatusDetail.CONFIGURATION_ERROR, "port already exists");
        } else {
            extenderPCA9685HandlerMap.put(port, megaDExtenderPCA9685Handler);
            updateThingHandlerStatus(megaDExtenderPCA9685Handler, ThingStatus.ONLINE);
        }
    }

    @SuppressWarnings("null")
    public void unregisterExtenderPCA9685Listener(@Nullable MegaDExtenderPCA9685Handler megaDExtenderPCA9685Handler) {
        String port = megaDExtenderPCA9685Handler.getThing().getConfiguration().get("extport").toString();
        if (extenderPCA9685HandlerMap.get(port) != null) {
            extenderPCA9685HandlerMap.remove(port);
            updateThingHandlerStatus(megaDExtenderPCA9685Handler, ThingStatus.OFFLINE);
        }
    }

    private void updateThingHandlerStatus(MegaDExtenderPCA9685Handler thingHandler, ThingStatus status) {
        thingHandler.updateStatus(status);
    }

    private void updateThingHandlerStatus(MegaDExtenderPCA9685Handler megaDExtenderPCA9685Handler, ThingStatus status,
            ThingStatusDetail statusDetail, String decript) {
        megaDExtenderPCA9685Handler.updateStatus(status, statusDetail, decript);
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
            bridgeDeviceHandler.unregisterMegaExtenderPCA9685Listener(this);
        }
        super.dispose();
    }
}
