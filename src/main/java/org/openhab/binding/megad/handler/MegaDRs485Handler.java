/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.thing.*;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.megad.MegaDBindingConstants;
import org.openhab.binding.megad.internal.sdm120;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link MegaDRs485Handler} is responsible for rs485/modbus feature of megad
 *
 * @author Petr Shatsillo - Initial contribution
 */

@NonNullByDefault
public class MegaDRs485Handler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(MegaDRs485Handler.class);
    private @Nullable ScheduledFuture<?> refreshPollingJob;
    @Nullable
    MegaDBridgeDeviceHandler bridgeDeviceHandler;
    protected long lastRefresh = 0;

    public MegaDRs485Handler(Thing thing) {
        super(thing);

        bridgeDeviceHandler = null;
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
        String address = "";
        if (getThing().getConfiguration().get("address").toString().length() == 1) {
            address = "0" + getThing().getConfiguration().get("address").toString();
        } else {
            address = getThing().getConfiguration().get("address").toString();
        }
        logger.debug("Updating Megadevice thing {}...", getThing().getUID().toString());
        for (Channel channel : getThing().getChannels()) {
            if (isLinked(channel.getUID().getId())) {
                if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_VOLTAGE)) {
                    @Nullable
                    String value = null;

                    if (getThing().getConfiguration().get("type").equals("sdm120")) {
                        value = sdm120.getValueFromSDM120(getBridgeHandler(), address, "0000");
                    }
                    if (value != null) {
                        logger.debug("sdm 120 voltage is : {}", value);
                        updateState(channel.getUID().getId(), DecimalType.valueOf(value));
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_CURRENT)) {

                    @Nullable
                    String value = null;

                    if (getThing().getConfiguration().get("type").equals("sdm120")) {
                        value = sdm120.getValueFromSDM120(getBridgeHandler(), address, "0006");
                    }
                    if (value != null) {
                        logger.debug("sdm 120 current is : {}", value);
                        updateState(channel.getUID().getId(), DecimalType.valueOf(value));
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_ACTIVEPOWER)) {
                    @Nullable
                    String value = null;

                    if (getThing().getConfiguration().get("type").equals("sdm120")) {
                        value = sdm120.getValueFromSDM120(getBridgeHandler(), address, "000C");
                    }
                    if (value != null) {
                        logger.debug("sdm 120 active power is : {}", value);
                        updateState(channel.getUID().getId(), DecimalType.valueOf(value));
                    }

                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_APPARENTPOWER)) {
                    @Nullable
                    String value = null;

                    if (getThing().getConfiguration().get("type").equals("sdm120")) {
                        value = sdm120.getValueFromSDM120(getBridgeHandler(), address, "0012");
                    }
                    if (value != null) {
                        logger.debug("sdm 120 apparent power is : {}", value);
                        updateState(channel.getUID().getId(), DecimalType.valueOf(value));
                    }

                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_REACTIVEPOWER)) {
                    @Nullable
                    String value = null;

                    if (getThing().getConfiguration().get("type").equals("sdm120")) {
                        value = sdm120.getValueFromSDM120(getBridgeHandler(), address, "0018");
                    }
                    if (value != null) {
                        logger.debug("sdm 120 reactive power is : {}", value);
                        updateState(channel.getUID().getId(), DecimalType.valueOf(value));
                    }

                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_POWERFACTOR)) {
                    @Nullable
                    String value = null;

                    if (getThing().getConfiguration().get("type").equals("sdm120")) {
                        value = sdm120.getValueFromSDM120(getBridgeHandler(), address, "001E");
                    }
                    if (value != null) {
                        logger.debug("sdm 120 power factor is : {}", value);
                        updateState(channel.getUID().getId(), DecimalType.valueOf(value));
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_PHASEANGLE)) {
                    @Nullable
                    String value = null;

                    if (getThing().getConfiguration().get("type").equals("sdm120")) {
                        value = sdm120.getValueFromSDM120(getBridgeHandler(), address, "0024");
                    }
                    if (value != null) {
                        logger.debug("sdm 120 Phase angle is : {}", value);
                        updateState(channel.getUID().getId(), DecimalType.valueOf(value));
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_FREQUENCY)) {
                    @Nullable
                    String value = null;

                    if (getThing().getConfiguration().get("type").equals("sdm120")) {
                        value = sdm120.getValueFromSDM120(getBridgeHandler(), address, "0046");
                    }
                    if (value != null) {
                        logger.debug("sdm 120 Frequency is : {}", value);
                        updateState(channel.getUID().getId(), DecimalType.valueOf(value));
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_IMPORTACTNRG)) {
                    @Nullable
                    String value = null;

                    if (getThing().getConfiguration().get("type").equals("sdm120")) {
                        value = sdm120.getValueFromSDM120(getBridgeHandler(), address, "0048");
                    }
                    if (value != null) {
                        logger.debug("sdm 120 power factor is : {}", value);
                        updateState(channel.getUID().getId(), DecimalType.valueOf(value));
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_EXPORTACTNRG)) {
                    @Nullable
                    String value = null;

                    if (getThing().getConfiguration().get("type").equals("sdm120")) {
                        value = sdm120.getValueFromSDM120(getBridgeHandler(), address, "004A");
                    }
                    if (value != null) {
                        logger.debug("sdm 120 power factor is : {}", value);
                        updateState(channel.getUID().getId(), DecimalType.valueOf(value));
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_IMPORTREACTNRG)) {
                    @Nullable
                    String value = null;

                    if (getThing().getConfiguration().get("type").equals("sdm120")) {
                        value = sdm120.getValueFromSDM120(getBridgeHandler(), address, "004C");
                    }
                    if (value != null) {
                        logger.debug("sdm 120 power factor is : {}", value);
                        updateState(channel.getUID().getId(), DecimalType.valueOf(value));
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_EXPORTREACTNRG)) {
                    @Nullable
                    String value = null;

                    if (getThing().getConfiguration().get("type").equals("sdm120")) {
                        value = sdm120.getValueFromSDM120(getBridgeHandler(), address, "004E");
                    }
                    if (value != null) {
                        logger.debug("sdm 120 power factor is : {}", value);
                        updateState(channel.getUID().getId(), DecimalType.valueOf(value));
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_TOTALSYSPWRDMD)) {
                    @Nullable
                    String value = null;

                    if (getThing().getConfiguration().get("type").equals("sdm120")) {
                        value = sdm120.getValueFromSDM120(getBridgeHandler(), address, "0054");
                    }
                    if (value != null) {
                        logger.debug("sdm 120 power factor is : {}", value);
                        updateState(channel.getUID().getId(), DecimalType.valueOf(value));
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_MAXTOTALSYSPWRDMD)) {
                    @Nullable
                    String value = null;

                    if (getThing().getConfiguration().get("type").equals("sdm120")) {
                        value = sdm120.getValueFromSDM120(getBridgeHandler(), address, "0056");
                    }
                    if (value != null) {
                        logger.debug("sdm 120 power factor is : {}", value);
                        updateState(channel.getUID().getId(), DecimalType.valueOf(value));
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_IMPORTSYSPWRDMD)) {
                    @Nullable
                    String value = null;

                    if (getThing().getConfiguration().get("type").equals("sdm120")) {
                        value = sdm120.getValueFromSDM120(getBridgeHandler(), address, "0058");
                    }
                    if (value != null) {
                        logger.debug("sdm 120 power factor is : {}", value);
                        updateState(channel.getUID().getId(), DecimalType.valueOf(value));
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_MAXIMPORTSYSPWRDMD)) {
                    @Nullable
                    String value = null;

                    if (getThing().getConfiguration().get("type").equals("sdm120")) {
                        value = sdm120.getValueFromSDM120(getBridgeHandler(), address, "005A");
                    }
                    if (value != null) {
                        logger.debug("sdm 120 power factor is : {}", value);
                        updateState(channel.getUID().getId(), DecimalType.valueOf(value));
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_EXPORTSYSPWRDMD)) {
                    @Nullable
                    String value = null;

                    if (getThing().getConfiguration().get("type").equals("sdm120")) {
                        value = sdm120.getValueFromSDM120(getBridgeHandler(), address, "005C");
                    }
                    if (value != null) {
                        logger.debug("sdm 120 power factor is : {}", value);
                        updateState(channel.getUID().getId(), DecimalType.valueOf(value));
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_MAXEXPORTSYSPWRDMD)) {
                    @Nullable
                    String value = null;

                    if (getThing().getConfiguration().get("type").equals("sdm120")) {
                        value = sdm120.getValueFromSDM120(getBridgeHandler(), address, "005E");
                    }
                    if (value != null) {
                        logger.debug("sdm 120 power factor is : {}", value);
                        updateState(channel.getUID().getId(), DecimalType.valueOf(value));
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_CURRENTDMD)) {
                    @Nullable
                    String value = null;

                    if (getThing().getConfiguration().get("type").equals("sdm120")) {
                        value = sdm120.getValueFromSDM120(getBridgeHandler(), address, "0102");
                    }
                    if (value != null) {
                        logger.debug("sdm 120 power factor is : {}", value);
                        updateState(channel.getUID().getId(), DecimalType.valueOf(value));
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_MAXCURRENTDMD)) {
                    @Nullable
                    String value = null;

                    if (getThing().getConfiguration().get("type").equals("sdm120")) {
                        value = sdm120.getValueFromSDM120(getBridgeHandler(), address, "0108");
                    }
                    if (value != null) {
                        logger.debug("sdm 120 power factor is : {}", value);
                        updateState(channel.getUID().getId(), DecimalType.valueOf(value));
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_TOTALACTNRG)) {
                    @Nullable
                    String value = null;

                    if (getThing().getConfiguration().get("type").equals("sdm120")) {
                        value = sdm120.getValueFromSDM120(getBridgeHandler(), address, "0156");
                    }
                    if (value != null) {
                        logger.debug("sdm 120 power factor is : {}", value);
                        updateState(channel.getUID().getId(), DecimalType.valueOf(value));
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_TOTALREACTNRG)) {
                    @Nullable
                    String value = null;

                    if (getThing().getConfiguration().get("type").equals("sdm120")) {
                        value = sdm120.getValueFromSDM120(getBridgeHandler(), address, "0158");
                    }
                    if (value != null) {
                        logger.debug("sdm 120 power factor is : {}", value);
                        updateState(channel.getUID().getId(), DecimalType.valueOf(value));
                    }
                }
            }
        }
    }

    // ----------------------------------------------------------
    private synchronized @Nullable MegaDBridgeDeviceHandler getBridgeHandler() {
        Bridge bridge = Objects.requireNonNull(getBridge());
        return getBridgeHandler(bridge);
    }

    private synchronized @Nullable MegaDBridgeDeviceHandler getBridgeHandler(Bridge bridge) {
        ThingHandler handler = Objects.requireNonNull(bridge.getHandler());
        if (handler instanceof MegaDBridgeDeviceHandler) {
            return (MegaDBridgeDeviceHandler) handler;
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
