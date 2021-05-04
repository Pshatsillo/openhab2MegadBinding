/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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

import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.megad.MegaDBindingConstants;
import org.openhab.binding.megad.internal.MegaHttpHelpers;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link MegaDItoCSensorHandler} class defines I2C bus feature.
 * You can read I2C sensors connected to one port of MegaD as bus
 *
 * @author Petr Shatsillo - Initial contribution
 */
@NonNullByDefault
public class MegaDItoCSensorHandler extends BaseThingHandler {
    private final Logger logger = LoggerFactory.getLogger(MegaDItoCSensorHandler.class);
    @Nullable
    MegaDBridgeIToCHandler bridgeDeviceHandler;
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

    @SuppressWarnings("null")
    @Override
    public void initialize() {
        bridgeDeviceHandler = getBridgeHandler();
        logger.debug("Thing Handler for {} started", getThing().getUID().getId());

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
        updateStatus(ThingStatus.ONLINE);
    }

    public void refresh(int interval) {
        long now = System.currentTimeMillis();
        if (interval != 0) {
            if (now >= (lastRefresh + interval)) {
                updateData();
                lastRefresh = now;
            }
        }
    }

    @SuppressWarnings("null")
    protected void updateData() {
        logger.debug("Updating Megadevice thing {}...", getThing().getUID().toString());
        for (Channel channel : getThing().getChannels()) {
            if (isLinked(channel.getUID().getId())) {
                if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_PAR0)) {
                    assert bridgeDeviceHandler != null;
                    String result = "http://" + Objects.requireNonNull(getBridgeHandler()).getHostPassword()[0] + "/"
                            + Objects.requireNonNull(getBridgeHandler()).getHostPassword()[1] + "/?pt="
                            + bridgeDeviceHandler.getThing().getConfiguration().get("port").toString() + "&scl="
                            + bridgeDeviceHandler.getThing().getConfiguration().get("scl").toString() + "&i2c_dev="
                            + getThing().getConfiguration().get("sensortype").toString();
                    String updateRequest = MegaHttpHelpers.sendRequest(result);

                    if ("NA".equals(updateRequest)) {
                        logger.debug("Value {} is incorrect for channel {}", updateRequest,
                                MegaDBindingConstants.CHANNEL_PAR0);
                    } else {
                        try {
                            updateState(channel.getUID().getId(), DecimalType.valueOf(updateRequest));
                        } catch (Exception ex) {
                            logger.debug("Value {} is incorrect for channel {}", updateRequest,
                                    MegaDBindingConstants.CHANNEL_PAR0);
                        }
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_PAR1)) {
                    assert bridgeDeviceHandler != null;
                    String result = "http://" + Objects.requireNonNull(getBridgeHandler()).getHostPassword()[0] + "/"
                            + Objects.requireNonNull(getBridgeHandler()).getHostPassword()[1] + "/?pt="
                            + bridgeDeviceHandler.getThing().getConfiguration().get("port").toString() + "&scl="
                            + bridgeDeviceHandler.getThing().getConfiguration().get("scl").toString() + "&i2c_dev="
                            + getThing().getConfiguration().get("sensortype").toString() + "&i2c_par=1";
                    String updateRequest = MegaHttpHelpers.sendRequest(result);

                    if ("NA".equals(updateRequest)) {
                        logger.debug("Value {} is incorrect for channel {}", updateRequest,
                                MegaDBindingConstants.CHANNEL_PAR0);
                    } else {
                        try {
                            updateState(channel.getUID().getId(), DecimalType.valueOf(updateRequest));
                        } catch (Exception ex) {
                            logger.debug("Value {} is incorrect for channel {}", updateRequest,
                                    MegaDBindingConstants.CHANNEL_PAR1);
                        }
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_PAR2)) {
                    assert bridgeDeviceHandler != null;
                    String result = "http://" + Objects.requireNonNull(getBridgeHandler()).getHostPassword()[0] + "/"
                            + Objects.requireNonNull(getBridgeHandler()).getHostPassword()[1] + "/?pt="
                            + bridgeDeviceHandler.getThing().getConfiguration().get("port").toString() + "&scl="
                            + bridgeDeviceHandler.getThing().getConfiguration().get("scl").toString() + "&i2c_dev="
                            + getThing().getConfiguration().get("sensortype").toString() + "&i2c_par=2";
                    String updateRequest = MegaHttpHelpers.sendRequest(result);

                    if ("NA".equals(updateRequest)) {
                        logger.debug("Value {} is incorrect for channel {}", updateRequest,
                                MegaDBindingConstants.CHANNEL_PAR2);
                    } else {
                        try {
                            updateState(channel.getUID().getId(), DecimalType.valueOf(updateRequest));
                        } catch (Exception ex) {
                            logger.debug("Value {} is incorrect for channel {}", updateRequest,
                                    MegaDBindingConstants.CHANNEL_PAR2);
                        }
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_I2CRAW)) {
                    assert bridgeDeviceHandler != null;
                    String result = "";
                    if (getThing().getConfiguration().get("rawparam") != null) {
                        result = "http://" + Objects.requireNonNull(getBridgeHandler()).getHostPassword()[0] + "/"
                                + Objects.requireNonNull(getBridgeHandler()).getHostPassword()[1] + "/?pt="
                                + bridgeDeviceHandler.getThing().getConfiguration().get("port").toString() + "&scl="
                                + bridgeDeviceHandler.getThing().getConfiguration().get("scl").toString() + "&i2c_dev="
                                + getThing().getConfiguration().get("sensortype").toString()
                                + getThing().getConfiguration().get("rawparam").toString();
                    } else {
                        result = "http://" + Objects.requireNonNull(getBridgeHandler()).getHostPassword()[0] + "/"
                                + Objects.requireNonNull(getBridgeHandler()).getHostPassword()[1] + "/?pt="
                                + bridgeDeviceHandler.getThing().getConfiguration().get("port").toString() + "&scl="
                                + bridgeDeviceHandler.getThing().getConfiguration().get("scl").toString() + "&i2c_dev="
                                + getThing().getConfiguration().get("sensortype").toString();
                    }
                    String updateRequest = MegaHttpHelpers.sendRequest(result);

                    try {
                        updateState(channel.getUID().getId(), StringType.valueOf(updateRequest));
                    } catch (Exception ex) {
                        logger.debug("Value {} is incorrect for channel {}", updateRequest,
                                MegaDBindingConstants.CHANNEL_I2CRAW);
                    }
                }
            }
        }
    }

    // @SuppressWarnings("null")
    // -------------------------------------------------------------------
    private synchronized @Nullable MegaDBridgeIToCHandler getBridgeHandler() {
        Bridge bridge = Objects.requireNonNull(getBridge());
        return getBridgeHandler(bridge);
    }

    private synchronized @Nullable MegaDBridgeIToCHandler getBridgeHandler(Bridge bridge) {
        ThingHandler handler = Objects.requireNonNull(bridge.getHandler());
        if (handler instanceof MegaDBridgeIToCHandler) {
            return (MegaDBridgeIToCHandler) handler;
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
