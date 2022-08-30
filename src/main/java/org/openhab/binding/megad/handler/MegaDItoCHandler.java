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

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.megad.MegaDBindingConstants;
import org.openhab.binding.megad.internal.MegaHttpHelpers;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Channel;
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
 * The {@link MegaDItoCHandler} is responsible for i2c fatures of megad
 *
 * @author Petr Shatsillo - Initial contribution
 */
@NonNullByDefault
public class MegaDItoCHandler extends BaseThingHandler {

    private Logger logger = LoggerFactory.getLogger(MegaDItoCHandler.class);

    private @Nullable ScheduledFuture<?> refreshPollingJob;

    @Nullable
    MegaDBridgeDeviceHandler bridgeDeviceHandler;
    boolean startup = true;
    protected long lastRefresh = 0;

    public MegaDItoCHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
    }

    @SuppressWarnings("null")
    @Override
    public void initialize() {
        bridgeDeviceHandler = getBridgeHandler();
        // logger.debug("Thing Handler for {} started", getThing().getUID().getId());
        if (bridgeDeviceHandler != null) {
            registerMegadItoCListener(bridgeDeviceHandler);
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

    @SuppressWarnings({ "unused", "null" })
    @Override
    public void dispose() {
        if (refreshPollingJob != null && !refreshPollingJob.isCancelled()) {
            refreshPollingJob.cancel(true);
            refreshPollingJob = null;
        }
        if (bridgeDeviceHandler != null) {
            bridgeDeviceHandler.unregisterItoCListener(this);
        }

        super.dispose();
    }

    private synchronized @Nullable MegaDBridgeDeviceHandler getBridgeHandler() {
        Bridge bridge = getBridge();
        if (bridge == null) {
            logger.error("Required bridge not defined for device.");
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

    @SuppressWarnings("null")
    public void refresh(int interval) {
        long now = System.currentTimeMillis();
        if (startup) {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                logger.warn("{}", e.getLocalizedMessage());
            }
            updateData();
            startup = false;
        }
        if (interval != 0) {
            if (now >= (lastRefresh + interval)) {
                updateData();
                lastRefresh = now;
            }
        }
    }

    @SuppressWarnings("null")
    protected void updateData() {
        logger.debug("Updating i2c things...");

        String result = "http://" + getBridgeHandler().getThing().getConfiguration().get("hostname").toString() + "/"
                + getBridgeHandler().getThing().getConfiguration().get("password").toString() + "/?pt="
                + getThing().getConfiguration().get("port").toString() + "&cmd=get";
        String[] updateRequest = MegaHttpHelpers.sendRequest(result).split("[:/]");

        for (Channel channel : getThing().getChannels()) {
            if (isLinked(channel.getUID().getId())) {
                if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_I2C_TEMP)) {
                    for (int i = 0; i < updateRequest.length; i++) {
                        if (updateRequest.length == 1) {
                            try {
                                updateState(channel.getUID().getId(), DecimalType.valueOf(updateRequest[0]));
                            } catch (Exception ex) {
                                logger.debug("Value {} is incorrect for channel {}", updateRequest[0],
                                        MegaDBindingConstants.CHANNEL_I2C_TEMP);
                            }
                        } else if (updateRequest[i].equals("temp")) {
                            try {
                                updateState(channel.getUID().getId(), DecimalType.valueOf(updateRequest[i + 1]));
                            } catch (Exception ex) {
                                logger.debug("Value {} is incorrect for channel {}", updateRequest[i + 1],
                                        MegaDBindingConstants.CHANNEL_I2C_TEMP);
                            }
                        }
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_I2C_HUM)) {
                    for (int i = 0; i < updateRequest.length; i++) {
                        if (updateRequest[i].equals("hum")) {
                            try {
                                updateState(channel.getUID().getId(), DecimalType.valueOf(updateRequest[i + 1]));
                            } catch (Exception ex) {
                                logger.debug("Value {} is incorrect for channel {}", updateRequest[i + 1],
                                        MegaDBindingConstants.CHANNEL_I2C_HUM);
                            }
                        }
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_I2C_PRESSURE)) {
                    for (int i = 0; i < updateRequest.length; i++) {
                        if (updateRequest[i].equals("press")) {
                            try {
                                updateState(channel.getUID().getId(), DecimalType.valueOf(updateRequest[i + 1]));
                            } catch (Exception ex) {
                                logger.debug("Value {} is incorrect for channel {}", updateRequest[i + 1],
                                        MegaDBindingConstants.CHANNEL_I2C_PRESSURE);
                            }
                        }
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_I2C_GAS)) {
                    for (int i = 0; i < updateRequest.length; i++) {
                        if (updateRequest[i].equals("gas")) {
                            try {
                                updateState(channel.getUID().getId(), DecimalType.valueOf(updateRequest[i + 1]));
                            } catch (Exception ex) {
                                logger.debug("Value {} is incorrect for channel {}", updateRequest[i + 1],
                                        MegaDBindingConstants.CHANNEL_I2C_GAS);
                            }
                        }
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_I2C_OTHER)) {
                    try {
                        updateState(channel.getUID().getId(), DecimalType.valueOf(updateRequest[0]));
                    } catch (Exception ex) {
                        logger.debug("Value {} is incorrect for channel {}", updateRequest[0],
                                MegaDBindingConstants.CHANNEL_I2C_OTHER);
                    }
                }
            }
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

    private void registerMegadItoCListener(@Nullable MegaDBridgeDeviceHandler bridgeHandler) {
        if (bridgeHandler != null) {
            bridgeHandler.registerMegadItoCListener(this);
        }
    }
}
