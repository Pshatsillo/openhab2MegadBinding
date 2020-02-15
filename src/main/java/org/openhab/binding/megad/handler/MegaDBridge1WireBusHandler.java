package org.openhab.binding.megad.handler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MegaDBridge1WireBusHandler extends BaseBridgeHandler {
    private Logger logger = LoggerFactory.getLogger(MegaDBridge1WireBusHandler.class);
    @Nullable
    MegaDBridgeDeviceHandler bridgeDeviceHandler;
    private @Nullable ScheduledFuture<?> refreshPollingJob;
    boolean startup = true;
    protected long lastRefresh = 0;
    private Map<String, String> owsensorvalues = new HashMap<String, String>();
    private @Nullable Map<String, MegaD1WireSensorHandler> addressesHandlerMap = new HashMap<String, MegaD1WireSensorHandler>();

    public MegaDBridge1WireBusHandler(Bridge bridge) {
        super(bridge);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {

    }

    @SuppressWarnings("null")
    @Override
    public void initialize() {
        bridgeDeviceHandler = getBridgeHandler();
        logger.debug("Thing Handler for {} started", getThing().getUID().getId());

        if (bridgeDeviceHandler != null) {
            registerMega1WirePortListener(bridgeDeviceHandler);
        } else {
            logger.debug("Can't register {} at bridge. BridgeHandler is null.", this.getThing().getUID());
        }

        String rr = getThing().getConfiguration().get("refresh").toString();
        logger.debug("refresh: {}", rr);
        int pollingPeriod = Integer.parseInt(rr) * 1000;
        if (refreshPollingJob == null || refreshPollingJob.isCancelled()) {
            refreshPollingJob = scheduler.scheduleWithFixedDelay(new Runnable() {
                @Override
                public void run() {
                    refresh(pollingPeriod);
                }
            }, 0, 1000, TimeUnit.MILLISECONDS);
        }
    }

    @SuppressWarnings("null")
    public void refresh(int interval) {
        long now = System.currentTimeMillis();
        if (startup) {
            startup = false;
        }

        if (interval != 0) {
            if (now >= (lastRefresh + interval)) {
                String request = "http://"
                        + bridgeDeviceHandler.getThing().getConfiguration().get("hostname").toString() + "/"
                        + bridgeDeviceHandler.getThing().getConfiguration().get("password").toString() + "/?pt="
                        + getThing().getConfiguration().get("port").toString() + "?cmd=list";
                String updateRequest = sendRequest(request);
                String[] getAddress = updateRequest.split("[;]");

                for (int i = 0; getAddress.length > i; i++) {
                    String[] getValues = getAddress[i].split("[:]");
                    try {
                        setOwvalues(getValues[0], getValues[1]);
                    } catch (Exception e) {
                        logger.debug("NOT 1-W BUS");
                    }
                }

                logger.debug("{}", updateRequest);

                lastRefresh = now;
            }
        }

    }

    @SuppressWarnings("unused")
    private void registerMega1WirePortListener(@Nullable MegaDBridgeDeviceHandler bridgeHandler) {
        if (bridgeHandler != null) {
            bridgeHandler.registerMega1WireBusListener(this);
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

    public void setOwvalues(String key, String value) {
        owsensorvalues.put(key, value);
    }

    public String getOwvalues(String address) {
        String value = "";
        value = owsensorvalues.get(address);
        return value;
    }

    private synchronized @Nullable MegaDBridgeDeviceHandler getBridgeHandler() {
        Bridge bridge = getBridge();
        if (bridge == null) {
            logger.warn("Required bridge not defined for device {}.");
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

    @SuppressWarnings({ "unused", "null" })
    public void registerMegadPortsListener(MegaD1WireSensorHandler megaDMegaportsHandler) {
        String ip = megaDMegaportsHandler.getThing().getConfiguration().get("port").toString();
        logger.debug("Register Device with ip {} and port {}", getThing().getConfiguration().get("hostname").toString(),
                megaDMegaportsHandler.getThing().getConfiguration().get("port").toString());
        if (addressesHandlerMap.get(ip) != null) {
            updateThingHandlerStatus(megaDMegaportsHandler, ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "port already exists");
        } else {
            addressesHandlerMap.put(ip, megaDMegaportsHandler);
            updateThingHandlerStatus(megaDMegaportsHandler, ThingStatus.ONLINE);
        }
    }

    private void updateThingHandlerStatus(MegaD1WireSensorHandler megaDMegaportsHandler, ThingStatus status,
            ThingStatusDetail statusDetail, String decript) {
        megaDMegaportsHandler.updateStatus(status, statusDetail, decript);

    }

    private void updateThingHandlerStatus(MegaD1WireSensorHandler thingHandler, ThingStatus status) {
        thingHandler.updateStatus(status);
    }

    private String sendRequest(String URL) {
        String result = "";
        if (!URL.equals("")) {
            try {
                URL obj = new URL(URL);
                HttpURLConnection con;

                con = (HttpURLConnection) obj.openConnection();

                logger.debug("URL: {}", URL);

                con.setRequestMethod("GET");
                // con.setReadTimeout(500);
                con.setReadTimeout(1500);
                con.setConnectTimeout(1500);
                // add request header
                con.setRequestProperty("User-Agent", "Mozilla/5.0");

                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                logger.debug("input string-> {}", response.toString());
                result = response.toString().trim();
                con.disconnect();
            } catch (IOException e) {
                logger.error("Connect to megadevice {} error: {}",
                        getThing().getConfiguration().get("hostname").toString(), e.getLocalizedMessage());
            }
        }
        return result;
    }

    @SuppressWarnings("null")
    public void unregisterMegad1WireListener(MegaD1WireSensorHandler megaD1WireSensorHandler) {
        String ip = megaD1WireSensorHandler.getThing().getConfiguration().get("address").toString();
        if (addressesHandlerMap.get(ip) != null) {
            addressesHandlerMap.remove(ip);
            updateThingHandlerStatus(megaD1WireSensorHandler, ThingStatus.OFFLINE);
        }

    }

    @SuppressWarnings("null")
    public void registerMegad1WireListener(MegaD1WireSensorHandler megaD1WireSensorHandler) {
        String oneWirePort = megaD1WireSensorHandler.getThing().getConfiguration().get("address").toString();

        if (addressesHandlerMap.get(oneWirePort) != null) {
            updateThingHandlerStatus(megaD1WireSensorHandler, ThingStatus.OFFLINE,
                    ThingStatusDetail.CONFIGURATION_ERROR, "Device already exist");
        } else {
            addressesHandlerMap.put(oneWirePort, megaD1WireSensorHandler);
            updateThingHandlerStatus(megaD1WireSensorHandler, ThingStatus.ONLINE);
            // megaDBridgeDeviceHandler.getAllPortsStatus();
        }
    }

}
