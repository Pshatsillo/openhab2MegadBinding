/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.megad.discovery.MegaDDiscoveryService;
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
 * The {@link MegaDBridge1WireBusHandler} class defines 1-wire bus feature.
 * You can read 1-wire sensors connected to one port of MegaD as bus
 *
 * @author Petr Shatsillo - Initial contribution
 */
@NonNullByDefault
public class MegaDBridge1WireBusHandler extends BaseBridgeHandler {
    private final Logger logger = LoggerFactory.getLogger(MegaDBridge1WireBusHandler.class);
    @Nullable
    MegaDBridgeDeviceHandler bridgeDeviceHandler;
    @Nullable
    MegaDDiscoveryService discovery;
    private @Nullable ScheduledFuture<?> refreshPollingJob;
    boolean startup = true;
    protected long lastRefresh = 0;
    @Nullable
    private final Map<String, String> owsensorvalues = new HashMap<>();
    private @Nullable final Map<String, MegaD1WireSensorHandler> addressesHandlerMap = new HashMap<>();

    public MegaDBridge1WireBusHandler(Bridge bridge) {
        super(bridge);
        // bridgeDeviceHandler = Objects.requireNonNull(getBridgeHandler());
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
    }

    @Override
    public void initialize() {
        bridgeDeviceHandler = Objects.requireNonNull(getBridgeHandler());
        logger.debug("Thing Handler for {} started", getThing().getUID().getId());
        MegaDDiscoveryService.oneWireBusList.add(this);
        // if (bridgeDeviceHandler != null) {
        registerMega1WirePortListener(bridgeDeviceHandler);
        // } else {
        // logger.debug("Can't register {} at bridge. BridgeHandler is null.", this.getThing().getUID());
        // }

        String[] rr = { getThing().getConfiguration().get("refresh").toString() };// .split("[.]");
        logger.debug("Thing {}, refresh interval is {} sec", getThing().getUID().toString(), rr[0]);
        float msec = Float.parseFloat(rr[0]);
        int pollingPeriod = (int) (msec * 1000);
        if (refreshPollingJob == null || refreshPollingJob.isCancelled()) {
            refreshPollingJob = scheduler.scheduleWithFixedDelay(() -> refresh(pollingPeriod), 0, 100,
                    TimeUnit.MILLISECONDS);
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
                String conv = "http://" + bridgeDeviceHandler.getThing().getConfiguration().get("hostname").toString()
                        + "/" + bridgeDeviceHandler.getThing().getConfiguration().get("password").toString() + "/?pt="
                        + getThing().getConfiguration().get("port").toString() + "?cmd=conv";
                MegaHttpHelpers.sendRequest(conv);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {
                }
                String request = "http://"
                        + bridgeDeviceHandler.getThing().getConfiguration().get("hostname").toString() + "/"
                        + bridgeDeviceHandler.getThing().getConfiguration().get("password").toString() + "/?pt="
                        + getThing().getConfiguration().get("port").toString() + "?cmd=list";
                String updateRequest = MegaHttpHelpers.sendRequest(request);
                String[] getAddress = updateRequest.split("[;]");
                for (String address : getAddress) {
                    String[] getValues = address.split("[:]");
                    try {
                        setOwvalues(getValues[0], getValues[1]);
                        if (addressesHandlerMap != null) {
                            @Nullable
                            MegaD1WireSensorHandler megaD1WireSensorHandler = addressesHandlerMap.get(getValues[0]);
                            if (megaD1WireSensorHandler != null) {
                                megaD1WireSensorHandler.updateValues(getValues[1]);
                            }
                        }
                    } catch (Exception e) {
                        logger.debug("NOT 1-W BUS");
                    }
                }
                logger.debug("{}", updateRequest);
                lastRefresh = now;
            }
        }
    }

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
        String value;
        assert owsensorvalues != null;
        value = owsensorvalues.get(address);
        if (value != null) {
            return value;
        } else {
            return "NULL";
        }
    }

    public String[] getHostPassword() {
        return new String[] { bridgeDeviceHandler.getThing().getConfiguration().get("hostname").toString(),
                bridgeDeviceHandler.getThing().getConfiguration().get("password").toString() };
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

    /*
     * public void registerMegadPortsListener(MegaD1WireSensorHandler megaDMegaportsHandler) {
     * String ip = megaDMegaportsHandler.getThing().getConfiguration().get("port").toString();
     * logger.debug("Register Device with ip {} and port {}", getThing().getConfiguration().get("hostname").toString(),
     * megaDMegaportsHandler.getThing().getConfiguration().get("port").toString());
     * if (addressesHandlerMap.get(ip) != null) {
     * updateThingHandlerStatus(megaDMegaportsHandler, ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
     * "port already exists");
     * } else {
     * addressesHandlerMap.put(ip, megaDMegaportsHandler);
     * updateThingHandlerStatus(megaDMegaportsHandler, ThingStatus.ONLINE);
     * }
     * }
     */

    private void updateThingHandlerStatus(MegaD1WireSensorHandler megaDMegaportsHandler, ThingStatus status,
            ThingStatusDetail statusDetail, String decript) {
        megaDMegaportsHandler.updateStatus(status, statusDetail, decript);
    }

    private void updateThingHandlerStatus(MegaD1WireSensorHandler thingHandler, ThingStatus status) {
        thingHandler.updateStatus(status);
    }

    public void unregisterMegad1WireListener(MegaD1WireSensorHandler megaD1WireSensorHandler) {
        String ip = megaD1WireSensorHandler.getThing().getConfiguration().get("address").toString();
        if (addressesHandlerMap.get(ip) != null) {
            addressesHandlerMap.remove(ip);
            updateThingHandlerStatus(megaD1WireSensorHandler, ThingStatus.OFFLINE);
        }
    }

    public void registerMegad1WireListener(MegaD1WireSensorHandler megaD1WireSensorHandler) {
        String oneWirePort = megaD1WireSensorHandler.getThing().getConfiguration().get("address").toString();

        if (addressesHandlerMap.get(oneWirePort) != null) {
            updateThingHandlerStatus(megaD1WireSensorHandler, ThingStatus.OFFLINE,
                    ThingStatusDetail.CONFIGURATION_ERROR, "Device already exist");
        } else {
            addressesHandlerMap.put(oneWirePort, megaD1WireSensorHandler);
            updateThingHandlerStatus(megaD1WireSensorHandler, ThingStatus.ONLINE);
        }
    }

    @Override
    public void dispose() {
        if (refreshPollingJob != null && !refreshPollingJob.isCancelled()) {
            refreshPollingJob.cancel(true);
            refreshPollingJob = null;
        }
        bridgeDeviceHandler.unregisterMegad1WireBridgeListener(this);

        super.dispose();
    }
}
