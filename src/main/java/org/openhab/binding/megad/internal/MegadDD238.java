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
package org.openhab.binding.megad.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.megad.MegaDBindingConstants;
import org.openhab.binding.megad.handler.MegaDBridgeDeviceHandler;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.Thing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link MegadDD238} is responsible for rs485/modbus feature of megad
 *
 * @author Petr Shatsillo - Initial contribution
 */
@NonNullByDefault
public class MegadDD238 implements ModbusPowermeterInterface {
    final Logger logger = LoggerFactory.getLogger(MegadDD238.class);
    String[] answer = {};
    String address;
    MegaDBridgeDeviceHandler bridgeHandler;

    public MegadDD238(MegaDBridgeDeviceHandler bridgeHandler, String address) {
        this.address = address;
        this.bridgeHandler = bridgeHandler;
    }

    @Override
    public String getVoltage() {
        return String.valueOf((double) Integer.parseInt(answer[27] + answer[28], 16) / 10);
    }

    @Override
    public void updateValues() {
        String result = "http://" + bridgeHandler.getThing().getConfiguration().get("hostname").toString() + "/"
                + bridgeHandler.getThing().getConfiguration().get("password").toString() + "/?uart_tx=" + address
                + "0300000012&mode=rs485";
        MegaHttpHelpers.sendRequest(result);
        try {
            Thread.sleep(100);
        } catch (InterruptedException ignored) {
        }
        result = "http://" + bridgeHandler.getThing().getConfiguration().get("hostname").toString() + "/"
                + bridgeHandler.getThing().getConfiguration().get("password").toString() + "/?uart_rx=1&mode=rs485";
        String updateRequest = MegaHttpHelpers.sendRequest(result);
        logger.debug("DD238 answer: {}", updateRequest);
        try {
            answer = updateRequest.split("[|]");
        } catch (Exception ignored) {
            answer = new String[] { "ERROR" };
        }
    }

    @Override
    public String getCurrent(int line) {
        return String.valueOf((double) Integer.parseInt(answer[29] + answer[30], 16) / 100);
    }

    @Override
    public String getActivePower(int line) {
        return String.valueOf(Math.abs((short) Integer.parseInt(answer[31] + answer[32], 16)));
    }

    @Override
    public String getApparentPower(int line) {
        return String.valueOf((double) Integer.parseInt(answer[33] + answer[34], 16));
    }

    @Override
    public String getReactivePower(int line) {
        return "null";
    }

    @Override
    public String getPowerFactor(int line) {
        return String.valueOf((double) Integer.parseInt(answer[35] + answer[36], 16) / 1000);
    }

    @Override
    public String getPhaseAngle(int line) {
        return "null";
    }

    @Override
    public String getFrequency() {
        return String.valueOf((double) Integer.parseInt(answer[37] + answer[38], 16) / 100);
    }

    @Override
    public String getImportActiveEnergy() {
        return "null";
    }

    @Override
    public String getExportActiveEnergy() {
        return "null";
    }

    @Override
    public String getImportReactiveEnergy() {
        return "null";
    }

    @Override
    public String getExportReactiveEnergy() {
        return "null";
    }

    @Override
    public String getTotalSystemPowerDemand() {
        return "null";
    }

    @Override
    public String getMaxTotalSystemPowerDemand() {
        return "null";
    }

    @Override
    public String getImportSystemPowerDemand() {
        return "null";
    }

    @Override
    public String getMaxImportSystemPowerDemand() {
        return "null";
    }

    @Override
    public String getExportSystemPowerDemand() {
        return "null";
    }

    @Override
    public String getMaxExportSystemPowerDemand() {
        return "null";
    }

    @Override
    public String getCurrentDemand() {
        return "null";
    }

    @Override
    public String getMaxCurrentDemand() {
        return "null";
    }

    @Override
    public String getTotalActiveEnergy() {
        return String.valueOf((double) Integer.parseInt(answer[3] + answer[4] + answer[5] + answer[6], 16) / 100);
    }

    @Override
    public String getTotalReactiveActiveEnergy(int line) {
        return "null";
    }

    @Override
    public List<Channel> getChannelsList(Thing thing) {
        List<Channel> channelList = new ArrayList<>();
        channelList.add(Objects.requireNonNull(thing.getChannel("cmn#" + MegaDBindingConstants.CHANNEL_VOLTAGE)));
        channelList.add(Objects.requireNonNull(thing.getChannel("line1#" + MegaDBindingConstants.CHANNEL_CURRENT)));
        channelList.add(Objects.requireNonNull(thing.getChannel("cmn#" + MegaDBindingConstants.CHANNEL_FREQUENCY)));
        channelList.add(Objects.requireNonNull(thing.getChannel("line1#" + MegaDBindingConstants.CHANNEL_ACTIVEPOWER)));
        channelList.add(Objects.requireNonNull(thing.getChannel("line1#" + MegaDBindingConstants.CHANNEL_POWERFACTOR)));
        channelList
                .add(Objects.requireNonNull(thing.getChannel("line1#" + MegaDBindingConstants.CHANNEL_APPARENTPOWER)));
        channelList.add(Objects.requireNonNull(thing.getChannel("cmn#" + MegaDBindingConstants.CHANNEL_TOTALACTNRG)));
        return channelList;
    }

    @Override
    public String getActiveEnergy(int line) {
        return " ";
    }
}
