/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
import org.openhab.binding.megad.RS485.MegaDDDs238;
import org.openhab.binding.megad.RS485.MegaDMideaProtocol;
import org.openhab.binding.megad.RS485.MegaDModbusPowermeterInterface;
import org.openhab.binding.megad.RS485.MegaDRS485Interface;
import org.openhab.binding.megad.RS485.MegaDSdm120;
import org.openhab.binding.megad.RS485.MegaDWBMAP6S;
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
    MegaDDeviceHandler bridgeDeviceHandler;
    protected long lastRefresh = 0;
    @Nullable
    MegaDRS485Interface rsi;
    @Nullable
    MegaDModbusPowermeterInterface modbus;
    int powerLines;

    public MegaDRs485Handler(Thing thing) {
        super(thing);
        bridgeDeviceHandler = null;
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        final MegaDRS485Interface megaDRS485Interface = rsi;
        if (megaDRS485Interface != null) {
            final MegaDDeviceHandler bridgeHandler = getBridgeHandler();
            if (bridgeHandler != null) {
                megaDRS485Interface.setValuesToRS485(bridgeHandler, channelUID.getId(),
                        command.toString().split(" ")[0]);
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
        MegaDDeviceHandler bridgeDeviceHandler = this.bridgeDeviceHandler;
        if (bridgeDeviceHandler != null) {
            registerMegaRs485Listener(bridgeDeviceHandler);
        } else {
            logger.debug("Can't register {} at bridge. BridgeHandler is null.", this.getThing().getUID());
        }

        logger.debug("Thing Handler for {} started", getThing().getUID().getId());
        String address;
        if (getThing().getConfiguration().get("address").toString().length() == 1) {
            address = "0" + getThing().getConfiguration().get("address").toString();
        } else {
            address = getThing().getConfiguration().get("address").toString();
        }
        if (getThing().getConfiguration().get("type").toString().equals("midea")) {
            rsi = new MegaDMideaProtocol(address);
            ThingBuilder thingBuilder = editThing();
            final MegaDMideaProtocol megaDRS485Interface = (MegaDMideaProtocol) rsi;
            if (megaDRS485Interface != null) {
                thingBuilder.withChannels(megaDRS485Interface.getChannelsList(getThing()));
                updateThing(thingBuilder.build());
            }
        }
        if (getThing().getConfiguration().get("type").toString().equals("dds238")) {
            final MegaDDeviceHandler bridgeHandler = getBridgeHandler();
            if (bridgeHandler != null) {
                modbus = new MegaDDDs238(bridgeHandler, address);
                ThingBuilder thingBuilder = editThing();
                final MegaDDDs238 modbusPowermeterInterface = (MegaDDDs238) modbus;
                if (modbusPowermeterInterface != null) {
                    thingBuilder.withChannels(modbusPowermeterInterface.getChannelsList(getThing()));
                    updateThing(thingBuilder.build());
                }
            }
        }
        if (getThing().getConfiguration().get("type").toString().equals("sdm120")) {
            final MegaDDeviceHandler bridgeHandler = getBridgeHandler();
            if (bridgeHandler != null) {
                modbus = new MegaDSdm120(bridgeHandler, address);
                ThingBuilder thingBuilder = editThing();
                final MegaDSdm120 modbusPowermeterInterface = (MegaDSdm120) modbus;
                if (modbusPowermeterInterface != null) {
                    thingBuilder.withChannels(modbusPowermeterInterface.getChannelsList(getThing()));
                    updateThing(thingBuilder.build());
                }
            }
        }
        if (getThing().getConfiguration().get("type").toString().equals("wbmap6s")) {
            powerLines = 6;
            final MegaDDeviceHandler bridgeHandler = getBridgeHandler();
            if (bridgeHandler != null) {
                modbus = new MegaDWBMAP6S(bridgeHandler, address);
                ThingBuilder thingBuilder = editThing();
                final MegaDWBMAP6S modbusPowermeterInterface = (MegaDWBMAP6S) modbus;
                if (modbusPowermeterInterface != null) {
                    thingBuilder.withChannels(modbusPowermeterInterface.getChannelsList(getThing()));
                    updateThing(thingBuilder.build());
                }
            }
        }
        updateStatus(ThingStatus.ONLINE);
    }

    protected void updateData() {
        if (getThing().getConfiguration().get("type").equals("dds238")) {
            final MegaDModbusPowermeterInterface megaDModbusPowermeterInterface = modbus;
            if (megaDModbusPowermeterInterface != null) {
                megaDModbusPowermeterInterface.updateValues();
            }
        }
        logger.debug("Updating Megadevice thing {}...", getThing().getUID());
        for (Channel channel : getThing().getChannels()) {
            if (isLinked(channel.getUID().getId())) {
                if (channel.getUID().getId()
                        .equals(channel.getUID().getGroupId() + "#" + MegaDBindingConstants.CHANNEL_CURRENT)) {
                    try {
                        final MegaDModbusPowermeterInterface megaDModbusPowermeterInterface = modbus;
                        if (megaDModbusPowermeterInterface != null) {
                            String value = megaDModbusPowermeterInterface.getCurrent(Integer
                                    .parseInt(Objects.requireNonNull(channel.getUID().getGroupId()).substring(4)));
                            logger.debug("Current is {} A at line {}", value, channel.getUID().getId());
                            updateState(channel.getUID().getId(), DecimalType.valueOf(value));
                        }
                    } catch (Exception ignored) {
                    }
                }
                if (channel.getUID().getId()
                        .equals(channel.getUID().getGroupId() + "#" + MegaDBindingConstants.CHANNEL_VOLTAGE)) {
                    try {
                        final MegaDModbusPowermeterInterface megaDModbusPowermeterInterface = modbus;
                        if (megaDModbusPowermeterInterface != null) {
                            String value = megaDModbusPowermeterInterface.getVoltage();
                            logger.debug("Voltage is : {}", value);
                            updateState(channel.getUID().getId(), DecimalType.valueOf(value));
                        }
                    } catch (Exception ignored) {
                    }
                } else if (channel.getUID().getId()
                        .equals(channel.getUID().getGroupId() + "#" + MegaDBindingConstants.CHANNEL_ACTIVEPOWER)) {
                    try {
                        final MegaDModbusPowermeterInterface megaDModbusPowermeterInterface = modbus;
                        if (megaDModbusPowermeterInterface != null) {
                            String value = megaDModbusPowermeterInterface.getActivePower(Integer
                                    .parseInt(Objects.requireNonNull(channel.getUID().getGroupId()).substring(4)));
                            logger.debug("Active power is : {}", value);
                            updateState(channel.getUID().getId(), DecimalType.valueOf(value));
                        }
                    } catch (Exception ignored) {
                    }
                } else if (channel.getUID().getId()
                        .equals(channel.getUID().getGroupId() + "#" + MegaDBindingConstants.CHANNEL_APPARENTPOWER)) {
                    final MegaDModbusPowermeterInterface megaDModbusPowermeterInterface = modbus;
                    if (megaDModbusPowermeterInterface != null) {
                        String value = megaDModbusPowermeterInterface.getApparentPower(
                                Integer.parseInt(Objects.requireNonNull(channel.getUID().getGroupId()).substring(4)));
                        logger.debug("Apparent power is : {}", value);
                        updateState(channel.getUID().getId(), DecimalType.valueOf(value));
                    }
                } else if (channel.getUID().getId()
                        .equals(channel.getUID().getGroupId() + "#" + MegaDBindingConstants.CHANNEL_REACTIVEPOWER)) {
                    try {
                        final MegaDModbusPowermeterInterface megaDModbusPowermeterInterface = modbus;
                        if (megaDModbusPowermeterInterface != null) {
                            String value = megaDModbusPowermeterInterface.getReactivePower(Integer
                                    .parseInt(Objects.requireNonNull(channel.getUID().getGroupId()).substring(4)));
                            logger.debug("Reactive power is : {}", value);
                            updateState(channel.getUID().getId(), DecimalType.valueOf(value));
                        }
                    } catch (Exception ignored) {
                    }
                } else if (channel.getUID().getId()
                        .equals(channel.getUID().getGroupId() + "#" + MegaDBindingConstants.CHANNEL_POWERFACTOR)) {
                    try {
                        final MegaDModbusPowermeterInterface megaDModbusPowermeterInterface = modbus;
                        if (megaDModbusPowermeterInterface != null) {
                            String value = megaDModbusPowermeterInterface.getPowerFactor(Integer
                                    .parseInt(Objects.requireNonNull(channel.getUID().getGroupId()).substring(4)));
                            logger.debug("Power factor is : {}", value);
                            updateState(channel.getUID().getId(), DecimalType.valueOf(value));
                        }
                    } catch (Exception ignored) {
                    }
                } else if (channel.getUID().getId()
                        .equals(channel.getUID().getGroupId() + "#" + MegaDBindingConstants.CHANNEL_PHASEANGLE)) {
                    final MegaDModbusPowermeterInterface megaDModbusPowermeterInterface = modbus;
                    if (megaDModbusPowermeterInterface != null) {
                        String value = megaDModbusPowermeterInterface.getPhaseAngle(
                                Integer.parseInt(Objects.requireNonNull(channel.getUID().getGroupId()).substring(4)));
                        logger.debug("Phase angle is : {}", value);
                        updateState(channel.getUID().getId(), DecimalType.valueOf(value));
                    }
                } else if (channel.getUID().getId()
                        .equals(channel.getUID().getGroupId() + "#" + MegaDBindingConstants.CHANNEL_FREQUENCY)) {
                    try {
                        final MegaDModbusPowermeterInterface megaDModbusPowermeterInterface = modbus;
                        if (megaDModbusPowermeterInterface != null) {
                            String value = megaDModbusPowermeterInterface.getFrequency();
                            logger.debug("Frequency is : {}", value);
                            updateState(channel.getUID().getId(), DecimalType.valueOf(value));
                        }
                    } catch (Exception ignored) {
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_IMPORTACTNRG)) {
                    final MegaDModbusPowermeterInterface megaDModbusPowermeterInterface = modbus;
                    if (megaDModbusPowermeterInterface != null) {
                        String value = megaDModbusPowermeterInterface.getImportActiveEnergy();
                        logger.debug("Import active energy: {}", value);
                        updateState(channel.getUID().getId(), DecimalType.valueOf(value));
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_EXPORTACTNRG)) {
                    final MegaDModbusPowermeterInterface megaDModbusPowermeterInterface = modbus;
                    if (megaDModbusPowermeterInterface != null) {
                        String value = megaDModbusPowermeterInterface.getExportActiveEnergy();
                        logger.debug("Export active energy : {}", value);
                        updateState(channel.getUID().getId(), DecimalType.valueOf(value));
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_IMPORTREACTNRG)) {
                    final MegaDModbusPowermeterInterface megaDModbusPowermeterInterface = modbus;
                    if (megaDModbusPowermeterInterface != null) {
                        String value = megaDModbusPowermeterInterface.getImportReactiveEnergy();
                        logger.debug("Import reactive energy : {}", value);
                        updateState(channel.getUID().getId(), DecimalType.valueOf(value));
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_EXPORTREACTNRG)) {
                    final MegaDModbusPowermeterInterface megaDModbusPowermeterInterface = modbus;
                    if (megaDModbusPowermeterInterface != null) {
                        String value = megaDModbusPowermeterInterface.getExportReactiveEnergy();
                        logger.debug("Export reactive energy : {}", value);
                        updateState(channel.getUID().getId(), DecimalType.valueOf(value));
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_TOTALSYSPWRDMD)) {
                    final MegaDModbusPowermeterInterface megaDModbusPowermeterInterface = modbus;
                    if (megaDModbusPowermeterInterface != null) {
                        String value = megaDModbusPowermeterInterface.getTotalSystemPowerDemand();
                        logger.debug("Total system power demand : {}", value);
                        updateState(channel.getUID().getId(), DecimalType.valueOf(value));
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_MAXTOTALSYSPWRDMD)) {
                    final MegaDModbusPowermeterInterface megaDModbusPowermeterInterface = modbus;
                    if (megaDModbusPowermeterInterface != null) {
                        String value = megaDModbusPowermeterInterface.getMaxTotalSystemPowerDemand();
                        logger.debug("Max total system power demand : {}", value);
                        updateState(channel.getUID().getId(), DecimalType.valueOf(value));
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_IMPORTSYSPWRDMD)) {
                    final MegaDModbusPowermeterInterface megaDModbusPowermeterInterface = modbus;
                    if (megaDModbusPowermeterInterface != null) {
                        String value = megaDModbusPowermeterInterface.getImportSystemPowerDemand();
                        logger.debug("Import system power demand : {}", value);
                        updateState(channel.getUID().getId(), DecimalType.valueOf(value));
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_MAXIMPORTSYSPWRDMD)) {
                    final MegaDModbusPowermeterInterface megaDModbusPowermeterInterface = modbus;
                    if (megaDModbusPowermeterInterface != null) {
                        String value = megaDModbusPowermeterInterface.getMaxImportSystemPowerDemand();
                        logger.debug("Max import system power demand : {}", value);
                        updateState(channel.getUID().getId(), DecimalType.valueOf(value));
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_EXPORTSYSPWRDMD)) {
                    final MegaDModbusPowermeterInterface megaDModbusPowermeterInterface = modbus;
                    if (megaDModbusPowermeterInterface != null) {
                        String value = megaDModbusPowermeterInterface.getExportSystemPowerDemand();
                        logger.debug("Export system power demand : {}", value);
                        updateState(channel.getUID().getId(), DecimalType.valueOf(value));
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_MAXEXPORTSYSPWRDMD)) {
                    final MegaDModbusPowermeterInterface megaDModbusPowermeterInterface = modbus;
                    if (megaDModbusPowermeterInterface != null) {
                        String value = megaDModbusPowermeterInterface.getMaxExportSystemPowerDemand();
                        logger.debug("Max export system power demand : {}", value);
                        updateState(channel.getUID().getId(), DecimalType.valueOf(value));
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_CURRENTDMD)) {
                    final MegaDModbusPowermeterInterface megaDModbusPowermeterInterface = modbus;
                    if (megaDModbusPowermeterInterface != null) {
                        String value = megaDModbusPowermeterInterface.getCurrentDemand();
                        logger.debug("Current demand : {}", value);
                        updateState(channel.getUID().getId(), DecimalType.valueOf(value));
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_MAXCURRENTDMD)) {
                    final MegaDModbusPowermeterInterface megaDModbusPowermeterInterface = modbus;
                    if (megaDModbusPowermeterInterface != null) {
                        String value = megaDModbusPowermeterInterface.getMaxCurrentDemand();
                        logger.debug("Max current demand : {}", value);
                        updateState(channel.getUID().getId(), DecimalType.valueOf(value));
                    }
                } else if (channel.getUID().getId()
                        .equals(channel.getUID().getGroupId() + "#" + MegaDBindingConstants.CHANNEL_TOTALACTNRG)) {
                    try {
                        final MegaDModbusPowermeterInterface megaDModbusPowermeterInterface = modbus;
                        if (megaDModbusPowermeterInterface != null) {
                            String value = megaDModbusPowermeterInterface.getTotalActiveEnergy();
                            logger.debug("Total active energy: {}", value);
                            updateState(channel.getUID().getId(), DecimalType.valueOf(value));
                        }
                    } catch (Exception ignored) {
                    }
                } else if (channel.getUID().getId()
                        .equals(channel.getUID().getGroupId() + "#" + MegaDBindingConstants.CHANNEL_ACTIVEENERGY)) {
                    try {
                        final MegaDModbusPowermeterInterface megaDModbusPowermeterInterface = modbus;
                        if (megaDModbusPowermeterInterface != null) {
                            String value = megaDModbusPowermeterInterface.getActiveEnergy(Integer
                                    .parseInt(Objects.requireNonNull(channel.getUID().getGroupId()).substring(4)));
                            logger.debug("Total active energy: {}", value);
                            updateState(channel.getUID().getId(), DecimalType.valueOf(value));
                        }
                    } catch (Exception ignored) {
                    }
                } else if ((channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_TOTALREACTNRG))
                        || (channel.getUID().getId().equals(
                                channel.getUID().getGroupId() + "#" + MegaDBindingConstants.CHANNEL_REACTIVEENERGY))) {
                    final MegaDModbusPowermeterInterface megaDModbusPowermeterInterface = modbus;
                    if (megaDModbusPowermeterInterface != null) {
                        String value = megaDModbusPowermeterInterface.getTotalReactiveActiveEnergy(
                                Integer.parseInt(Objects.requireNonNull(channel.getUID().getGroupId()).substring(4)));
                        logger.debug("Total reactive energy: {}", value);
                        updateState(channel.getUID().getId(), DecimalType.valueOf(value));
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_MIDEAOPERMODE)) {
                    final MegaDRS485Interface megaDRS485Interface = rsi;
                    if (megaDRS485Interface != null) {
                        final MegaDDeviceHandler bridgeHandler = getBridgeHandler();
                        if (bridgeHandler != null) {
                            String[] answer = megaDRS485Interface.getValueFromRS485(bridgeHandler);
                            if (answer.length == 32) {
                                String mode = switch (answer[8]) {
                                    case "00" -> "OFF";
                                    case "98" -> "AUTO";
                                    case "88" -> "COOL";
                                    case "82" -> "DRY";
                                    case "84" -> "HEAT";
                                    case "81" -> "FAN";
                                    default -> "";
                                };
                                logger.debug("Midea mode is : {}", mode);
                                updateState(channel.getUID().getId(), StringType.valueOf(mode));
                            } else {
                                logger.debug("Answer != 32 bytes <{}>", (Object) answer);
                            }
                        }
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_MIDEAFANMODE)) {
                    final MegaDRS485Interface megaDRS485Interface = rsi;
                    if (megaDRS485Interface != null) {
                        final MegaDDeviceHandler bridgeHandler = getBridgeHandler();
                        if (bridgeHandler != null) {
                            String[] answer = megaDRS485Interface.getValueFromRS485(bridgeHandler);
                            if (answer.length == 32) {
                                String mode = switch (answer[9]) {
                                    case "00" -> "OFF";
                                    case "84" -> "AUTO";
                                    case "01" -> "HIGH";
                                    case "02" -> "MEDIUM";
                                    case "04" -> "LOW";
                                    default -> "";
                                };
                                logger.debug("Midea fan mode is : {}", mode);
                                updateState(channel.getUID().getId(), StringType.valueOf(mode));
                            } else {
                                logger.debug("Answer != 32 bytes <{}>", (Object) answer);
                            }
                        }
                    }
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_MIDEATEMP)) {
                    final MegaDRS485Interface megaDRS485Interface = rsi;
                    if (megaDRS485Interface != null) {
                        final MegaDDeviceHandler bridgeHandler = getBridgeHandler();
                        if (bridgeHandler != null) {
                            String[] answer = megaDRS485Interface.getValueFromRS485(bridgeHandler);
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
    }

    // ----------------------------------------------------------
    private synchronized @Nullable MegaDDeviceHandler getBridgeHandler() {
        if (getBridge() != null) {
            @Nullable
            Bridge bridge = getBridge();
            if (bridge != null) {
                return getBridgeHandler(bridge);
            } else {
                return null;
            }
        } else {
            return getBridgeHandler();
        }
    }

    private synchronized @Nullable MegaDDeviceHandler getBridgeHandler(@Nullable Bridge bridge) {
        if (bridge != null) {
            ThingHandler handler = bridge.getHandler();
            if (handler instanceof MegaDDeviceHandler) {
                return (MegaDDeviceHandler) handler;
            } else {
                logger.debug("No available bridge handler found yet. Bridge: {} .", bridge.getUID());
                return null;
            }
        } else {
            return null;
        }
    }

    @Override
    public void dispose() {
        ScheduledFuture<?> refreshPollingJob = this.refreshPollingJob;
        if (refreshPollingJob != null && !refreshPollingJob.isCancelled()) {
            refreshPollingJob.cancel(true);
            this.refreshPollingJob = null;
        }
        MegaDDeviceHandler bridgeDeviceHandler = this.bridgeDeviceHandler;
        if (bridgeDeviceHandler != null) {
            bridgeDeviceHandler.unregisterMegadRs485Listener(this);
            this.bridgeDeviceHandler = bridgeDeviceHandler;
        }
        super.dispose();
    }

    public void lastrefreshAdd(long lastRefresh) {
        this.lastRefresh = lastRefresh;
    }

    public long getLastRefresh() {
        return this.lastRefresh;
    }

    private void registerMegaRs485Listener(@Nullable MegaDDeviceHandler bridgeHandler) {
        if (bridgeHandler != null) {
            bridgeHandler.registerMegaRs485Listener(this);
        }
    }
}
