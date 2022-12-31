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

import java.util.Objects;
import java.util.concurrent.ScheduledFuture;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.megad.MegaDBindingConstants;
import org.openhab.binding.megad.internal.MegaDRS485Interface;
import org.openhab.binding.megad.internal.MegaDSdm120;
import org.openhab.binding.megad.internal.MegaDWBMAP6S;
import org.openhab.binding.megad.internal.MegadDD238;
import org.openhab.binding.megad.internal.MegadMideaProtocol;
import org.openhab.binding.megad.internal.ModbusPowermeterInterface;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.builder.ThingBuilder;
import org.openhab.core.types.Command;
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
    @Nullable
    MegaDRS485Interface rsi;
    @Nullable
    ModbusPowermeterInterface modbus;
    int powerLines;

    public MegaDRs485Handler(Thing thing) {
        super(thing);

        bridgeDeviceHandler = null;
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (getBridgeHandler() != null) {
            if (rsi != null) {
                rsi.setValuesToRS485(getBridgeHandler(), channelUID.getId(), command.toString().split(" ")[0]);
            }
        }
        try {
            Thread.sleep(200);
        } catch (InterruptedException ignored) {
        }
        updateData();
    }

    @Override
    public void initialize() {
        bridgeDeviceHandler = getBridgeHandler();
        logger.debug("Thing Handler for {} started", getThing().getUID().getId());

        if (bridgeDeviceHandler != null) {
            registerMegaRs485Listener(bridgeDeviceHandler);
        } else {
            logger.debug("Can't register {} at bridge. BridgeHandler is null.", this.getThing().getUID());
        }

        String address = "";
        if (getThing().getConfiguration().get("address").toString().length() == 1) {
            address = "0" + getThing().getConfiguration().get("address").toString();
        } else {
            address = getThing().getConfiguration().get("address").toString();
        }

        if (getThing().getConfiguration().get("type").toString().equals("midea")) {
            rsi = new MegadMideaProtocol(address);
            ThingBuilder thingBuilder = editThing();
            thingBuilder.withChannels(rsi.getChannelsList(getThing()));
            updateThing(thingBuilder.build());
        }
        if (getThing().getConfiguration().get("type").toString().equals("dds238")) {
            modbus = new MegadDD238(getBridgeHandler(), address);
            ThingBuilder thingBuilder = editThing();
            thingBuilder.withChannels(modbus.getChannelsList(getThing()));
            updateThing(thingBuilder.build());
        }
        if (getThing().getConfiguration().get("type").toString().equals("sdm120")) {
            modbus = new MegaDSdm120(getBridgeHandler(), address);
            ThingBuilder thingBuilder = editThing();
            thingBuilder.withChannels(modbus.getChannelsList(getThing()));
            updateThing(thingBuilder.build());
        }
        if (getThing().getConfiguration().get("type").toString().equals("wbmap6s")) {
            powerLines = 6;
            modbus = new MegaDWBMAP6S(getBridgeHandler(), address);
            ThingBuilder thingBuilder = editThing();
            thingBuilder.withChannels(modbus.getChannelsList(getThing()));
            updateThing(thingBuilder.build());
        }

        // String[] rr = { getThing().getConfiguration().get("refresh").toString() };// .split("[.]");
        // logger.debug("Thing {}, refresh interval is {} sec", getThing().getUID().toString(), rr[0]);
        // float msec = Float.parseFloat(rr[0]);
        // int pollingPeriod = (int) (msec * 1000);
        // if (refreshPollingJob == null || refreshPollingJob.isCancelled()) {
        // refreshPollingJob = scheduler.scheduleWithFixedDelay(new Runnable() {
        // @Override
        // public void run() {
        // refresh(pollingPeriod);
        // }
        // }, 0, 100, TimeUnit.MILLISECONDS);
        // }

        updateStatus(ThingStatus.ONLINE);
    }

    // public void refresh(int interval) {
    // long now = System.currentTimeMillis();
    // if (interval != 0) {
    // if (now >= (lastRefresh + interval)) {
    // // updateData();
    // lastRefresh = now;
    // }
    // }
    // }

    protected void updateData() {
        String[] values = {};
        if (getThing().getConfiguration().get("type").equals("dds238")) {
            if (modbus != null) {
                modbus.updateValues();
            }
        }
        logger.debug("Updating Megadevice thing {}...", getThing().getUID().toString());
        for (Channel channel : getThing().getChannels()) {
            if (isLinked(channel.getUID().getId())) {
                String grp = channel.getUID().getGroupId();
                if (channel.getUID().getId()
                        .equals(channel.getUID().getGroupId() + "#" + MegaDBindingConstants.CHANNEL_CURRENT)) {
                    @Nullable
                    String value = null;
                    try {
                        if (modbus != null) {
                            value = modbus.getCurrent(Integer.parseInt(channel.getUID().getGroupId().substring(4)));
                            logger.debug("Current is {} A at line {}", value, channel.getUID().getId());
                            updateState(channel.getUID().getId(), DecimalType.valueOf(value));
                        }
                    } catch (Exception ignored) {
                    }
                }
                if (channel.getUID().getId()
                        .equals(channel.getUID().getGroupId() + "#" + MegaDBindingConstants.CHANNEL_VOLTAGE)) {
                    @Nullable
                    String value = null;
                    try {
                        if (modbus != null) {
                            value = modbus.getVoltage();
                            logger.debug("Voltage is : {}", value);
                            updateState(channel.getUID().getId(), DecimalType.valueOf(value));
                        }
                    } catch (Exception ignored) {
                    }
                } else if (channel.getUID().getId()
                        .equals(channel.getUID().getGroupId() + "#" + MegaDBindingConstants.CHANNEL_ACTIVEPOWER)) {
                    @Nullable
                    String value = null;
                    try {
                        if (modbus != null) {
                            value = modbus.getActivePower(Integer.parseInt(channel.getUID().getGroupId().substring(4)));
                            logger.debug("Active power is : {}", value);
                            updateState(channel.getUID().getId(), DecimalType.valueOf(value));
                        }
                    } catch (Exception ignored) {
                    }
                } else if (channel.getUID().getId()
                        .equals(channel.getUID().getGroupId() + "#" + MegaDBindingConstants.CHANNEL_APPARENTPOWER)) {
                    @Nullable
                    String value = null;
                    if (modbus != null) {
                        value = modbus.getApparentPower(Integer.parseInt(channel.getUID().getGroupId().substring(4)));
                        logger.debug("Apparent power is : {}", value);
                        updateState(channel.getUID().getId(), DecimalType.valueOf(value));
                    }
                } else if (channel.getUID().getId()
                        .equals(channel.getUID().getGroupId() + "#" + MegaDBindingConstants.CHANNEL_REACTIVEPOWER)) {
                    @Nullable
                    String value = null;
                    try {
                        if (modbus != null) {
                            value = modbus
                                    .getReactivePower(Integer.parseInt(channel.getUID().getGroupId().substring(4)));
                            logger.debug("Reactive power is : {}", value);
                            updateState(channel.getUID().getId(), DecimalType.valueOf(value));
                        }
                    } catch (Exception ignored) {
                    }
                } else if (channel.getUID().getId()
                        .equals(channel.getUID().getGroupId() + "#" + MegaDBindingConstants.CHANNEL_POWERFACTOR)) {
                    @Nullable
                    String value = null;
                    try {
                        if (modbus != null) {
                            value = modbus.getPowerFactor(Integer.parseInt(channel.getUID().getGroupId().substring(4)));
                            logger.debug("Power factor is : {}", value);
                            updateState(channel.getUID().getId(), DecimalType.valueOf(value));
                        }
                    } catch (Exception ignored) {
                    }
                } else if (channel.getUID().getId()
                        .equals(channel.getUID().getGroupId() + "#" + MegaDBindingConstants.CHANNEL_PHASEANGLE)) {
                    @Nullable
                    String value = null;
                    if (modbus != null) {
                        value = modbus.getPhaseAngle(Integer.parseInt(channel.getUID().getGroupId().substring(4)));
                        logger.debug("Phase angle is : {}", value);
                        updateState(channel.getUID().getId(), DecimalType.valueOf(value));
                    }
                } else if (channel.getUID().getId()
                        .equals(channel.getUID().getGroupId() + "#" + MegaDBindingConstants.CHANNEL_FREQUENCY)) {
                    @Nullable
                    String value = null;
                    try {
                        if (modbus != null) {
                            value = modbus.getFrequency();
                            logger.debug("Frequency is : {}", value);
                            updateState(channel.getUID().getId(), DecimalType.valueOf(value));
                        }
                    } catch (Exception ignored) {
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_IMPORTACTNRG)) {
                    @Nullable
                    String value = null;
                    if (modbus != null) {
                        value = modbus.getImportActiveEnergy();
                        logger.debug("Import active energy: {}", value);
                        updateState(channel.getUID().getId(), DecimalType.valueOf(value));
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_EXPORTACTNRG)) {
                    @Nullable
                    String value = null;
                    if (modbus != null) {
                        value = modbus.getExportActiveEnergy();
                        logger.debug("Export active energy : {}", value);
                        updateState(channel.getUID().getId(), DecimalType.valueOf(value));
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_IMPORTREACTNRG)) {
                    @Nullable
                    String value = null;
                    if (modbus != null) {
                        value = modbus.getImportReactiveEnergy();
                        logger.debug("Import reactive energy : {}", value);
                        updateState(channel.getUID().getId(), DecimalType.valueOf(value));
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_EXPORTREACTNRG)) {
                    @Nullable
                    String value = null;
                    if (modbus != null) {
                        value = modbus.getExportReactiveEnergy();
                        logger.debug("Export reactive energy : {}", value);
                        updateState(channel.getUID().getId(), DecimalType.valueOf(value));
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_TOTALSYSPWRDMD)) {
                    @Nullable
                    String value = null;
                    if (modbus != null) {
                        value = modbus.getTotalSystemPowerDemand();
                        logger.debug("Total system power demand : {}", value);
                        updateState(channel.getUID().getId(), DecimalType.valueOf(value));
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_MAXTOTALSYSPWRDMD)) {
                    @Nullable
                    String value = null;
                    if (modbus != null) {
                        value = modbus.getMaxTotalSystemPowerDemand();
                        logger.debug("Max total system power demand : {}", value);
                        updateState(channel.getUID().getId(), DecimalType.valueOf(value));
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_IMPORTSYSPWRDMD)) {
                    @Nullable
                    String value = null;
                    if (modbus != null) {
                        value = modbus.getImportSystemPowerDemand();
                        logger.debug("Import system power demand : {}", value);
                        updateState(channel.getUID().getId(), DecimalType.valueOf(value));
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_MAXIMPORTSYSPWRDMD)) {
                    @Nullable
                    String value = null;
                    if (modbus != null) {
                        value = modbus.getMaxImportSystemPowerDemand();
                        logger.debug("Max import system power demand : {}", value);
                        updateState(channel.getUID().getId(), DecimalType.valueOf(value));
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_EXPORTSYSPWRDMD)) {
                    @Nullable
                    String value = null;
                    if (modbus != null) {
                        value = modbus.getExportSystemPowerDemand();
                        logger.debug("Export system power demand : {}", value);
                        updateState(channel.getUID().getId(), DecimalType.valueOf(value));
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_MAXEXPORTSYSPWRDMD)) {
                    @Nullable
                    String value = null;
                    if (modbus != null) {
                        value = modbus.getMaxExportSystemPowerDemand();
                        logger.debug("Max export system power demand : {}", value);
                        updateState(channel.getUID().getId(), DecimalType.valueOf(value));
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_CURRENTDMD)) {
                    @Nullable
                    String value = null;
                    if (modbus != null) {
                        value = modbus.getCurrentDemand();
                        logger.debug("Current demand : {}", value);
                        updateState(channel.getUID().getId(), DecimalType.valueOf(value));
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_MAXCURRENTDMD)) {
                    @Nullable
                    String value = null;
                    if (modbus != null) {
                        value = modbus.getMaxCurrentDemand();
                        logger.debug("Max current demand : {}", value);
                        updateState(channel.getUID().getId(), DecimalType.valueOf(value));
                    }
                } else if (channel.getUID().getId()
                        .equals(channel.getUID().getGroupId() + "#" + MegaDBindingConstants.CHANNEL_TOTALACTNRG)) {
                    @Nullable
                    String value = null;
                    try {
                        if (modbus != null) {
                            value = modbus.getTotalActiveEnergy();
                            logger.debug("Total active energy: {}", value);
                            updateState(channel.getUID().getId(), DecimalType.valueOf(value));
                        }
                    } catch (Exception ignored) {
                    }
                } else if (channel.getUID().getId()
                        .equals(channel.getUID().getGroupId() + "#" + MegaDBindingConstants.CHANNEL_ACTIVEENERGY)) {
                    @Nullable
                    String value = null;
                    try {
                        if (modbus != null) {
                            value = modbus
                                    .getActiveEnergy(Integer.parseInt(channel.getUID().getGroupId().substring(4)));
                            logger.debug("Total active energy: {}", value);
                            updateState(channel.getUID().getId(), DecimalType.valueOf(value));
                        }
                    } catch (Exception ignored) {
                    }
                } else if ((channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_TOTALREACTNRG))
                        || (channel.getUID().getId().equals(
                                channel.getUID().getGroupId() + "#" + MegaDBindingConstants.CHANNEL_REACTIVEENERGY))) {
                    @Nullable
                    String value = null;
                    if (modbus != null) {
                        value = modbus.getTotalReactiveActiveEnergy(
                                Integer.parseInt(channel.getUID().getGroupId().substring(4)));
                        logger.debug("Total reactive energy: {}", value);
                        updateState(channel.getUID().getId(), DecimalType.valueOf(value));
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_MIDEAOPERMODE)) {
                    if (rsi != null) {
                        String[] answer = rsi.getValueFromRS485(getBridgeHandler());
                        if (answer.length == 32) {
                            String mode = "";
                            switch (answer[8]) {
                                case "00":
                                    mode = "OFF";
                                    break;
                                case "98":
                                    mode = "AUTO";
                                    break;
                                case "88":
                                    mode = "COOL";
                                    break;
                                case "82":
                                    mode = "DRY";
                                    break;
                                case "84":
                                    mode = "HEAT";
                                    break;
                                case "81":
                                    mode = "FAN";
                                    break;
                            }
                            logger.debug("Midea mode is : {}", mode);
                            updateState(channel.getUID().getId(), StringType.valueOf(mode));
                        } else {
                            logger.debug("Answer != 32 bytes <{}>", (Object) answer);
                        }
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_MIDEAFANMODE)) {
                    if (rsi != null) {
                        String[] answer = rsi.getValueFromRS485(getBridgeHandler());
                        if (answer.length == 32) {
                            String mode = "";
                            switch (answer[9]) {
                                case "00":
                                    mode = "OFF";
                                    break;
                                case "84":
                                    mode = "AUTO";
                                    break;
                                case "01":
                                    mode = "HIGH";
                                    break;
                                case "02":
                                    mode = "MEDIUM";
                                    break;
                                case "04":
                                    mode = "LOW";
                                    break;
                            }
                            logger.debug("Midea fan mode is : {}", mode);
                            updateState(channel.getUID().getId(), StringType.valueOf(mode));
                        } else {
                            logger.debug("Answer != 32 bytes <{}>", (Object) answer);
                        }
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_MIDEATEMP)) {
                    if (rsi != null) {
                        String[] answer = rsi.getValueFromRS485(getBridgeHandler());
                        if (answer.length == 32) {
                            try {
                                int n = (int) Long.parseLong(answer[10], 16);
                                logger.debug("Midea temperature is : {}, hex {}", n, answer[10]);
                                updateState(channel.getUID().getId(), DecimalType.valueOf(String.valueOf(n)));
                            } catch (Exception ignored) {
                            }
                        } else {
                            logger.debug("Answer != 32 bytes <{}>", (Object) answer);
                        }
                    }
                }
            }
        }
    }

    // ----------------------------------------------------------
    private synchronized MegaDBridgeDeviceHandler getBridgeHandler() {
        if (getBridge() != null) {
            @Nullable
            Bridge bridge = getBridge();
            return getBridgeHandler(bridge);
        } else {
            return getBridgeHandler();
        }
    }

    private synchronized MegaDBridgeDeviceHandler getBridgeHandler(@Nullable Bridge bridge) {
        ThingHandler handler = Objects.requireNonNull(bridge.getHandler());
        if (handler instanceof MegaDBridgeDeviceHandler) {
            return (MegaDBridgeDeviceHandler) handler;
        } else {
            logger.debug("No available bridge handler found yet. Bridge: {} .", bridge.getUID());
            return (MegaDBridgeDeviceHandler) handler;
        }
    }

    @Override
    public void dispose() {
        if (refreshPollingJob != null && !refreshPollingJob.isCancelled()) {
            refreshPollingJob.cancel(true);
            refreshPollingJob = null;
        }
        if (bridgeDeviceHandler != null) {
            bridgeDeviceHandler.unregisterMegadRs485Listener(this);
        }
        super.dispose();
    }

    private void registerMegaRs485Listener(@Nullable MegaDBridgeDeviceHandler bridgeHandler) {
        if (bridgeHandler != null) {
            bridgeHandler.registerMegaRs485Listener(this);
        }
    }

    public void lastrefreshAdd(long lastRefresh) {
        this.lastRefresh = lastRefresh;
    }

    public long getLastRefresh() {
        return this.lastRefresh;
    }
}
