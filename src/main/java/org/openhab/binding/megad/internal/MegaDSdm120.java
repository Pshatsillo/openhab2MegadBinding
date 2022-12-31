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
 * The {@link MegaDSdm120} is responsible for rs485/modbus feature of megad
 *
 * @author Petr Shatsillo - Initial contribution
 */

@NonNullByDefault
public class MegaDSdm120 implements ModbusPowermeterInterface {
    String address;
    MegaDBridgeDeviceHandler bridgeHandler;

    public MegaDSdm120(MegaDBridgeDeviceHandler bridgeHandler, String address) {
        this.address = address;
        this.bridgeHandler = bridgeHandler;
    }

    private String getValueFromSDM120(String valueByte) {
        final Logger logger = LoggerFactory.getLogger(MegaDSdm120.class);
        String result = "http://" + bridgeHandler.getThing().getConfiguration().get("hostname").toString() + "/"
                + bridgeHandler.getThing().getConfiguration().get("password").toString() + "/?uart_tx=" + address + "04"
                + valueByte + "0002&mode=rs485";
        MegaHttpHelpers.sendRequest(result);
        try {
            Thread.sleep(100);
        } catch (InterruptedException ignored) {
        }
        result = "http://" + bridgeHandler.getThing().getConfiguration().get("hostname").toString() + "/"
                + bridgeHandler.getThing().getConfiguration().get("password").toString() + "/?uart_rx=1&mode=rs485";
        String updateRequest = MegaHttpHelpers.sendRequest(result);
        logger.debug("sdm 120 answer: {}", updateRequest);
        try {
            String[] answer = updateRequest.split("[|]");
            String parse = answer[3] + answer[4] + answer[5] + answer[6];
            logger.debug("sdm 120 hex answer: {}", parse);
            int n = (int) Long.parseLong(parse, 16);
            Float convert = Float.intBitsToFloat(n);
            return String.format("%.4f", convert).replace(",", ".");
        } catch (Exception ignored) {
            return "ERROR";
        }
    }

    @Override
    public String getVoltage() {
        return getValueFromSDM120("0000");
    }

    @Override
    public String getCurrent(int line) {
        return getValueFromSDM120("0006");
    }

    @Override
    public String getActivePower(int line) {
        return getValueFromSDM120("000C");
    }

    @Override
    public String getApparentPower(int line) {
        return getValueFromSDM120("0012");
    }

    @Override
    public String getReactivePower(int line) {
        return getValueFromSDM120("0018");
    }

    @Override
    public String getPowerFactor(int line) {
        return getValueFromSDM120("001E");
    }

    @Override
    public String getPhaseAngle(int line) {
        return getValueFromSDM120("0024");
    }

    @Override
    public String getFrequency() {
        return getValueFromSDM120("0046");
    }

    @Override
    public String getImportActiveEnergy() {
        return getValueFromSDM120("0048");
    }

    @Override
    public String getExportActiveEnergy() {
        return getValueFromSDM120("004A");
    }

    @Override
    public String getImportReactiveEnergy() {
        return getValueFromSDM120("004C");
    }

    @Override
    public String getExportReactiveEnergy() {
        return getValueFromSDM120("004E");
    }

    @Override
    public String getTotalSystemPowerDemand() {
        return getValueFromSDM120("0054");
    }

    @Override
    public String getMaxTotalSystemPowerDemand() {
        return getValueFromSDM120("0056");
    }

    @Override
    public String getImportSystemPowerDemand() {
        return getValueFromSDM120("0058");
    }

    @Override
    public String getMaxImportSystemPowerDemand() {
        return getValueFromSDM120("005A");
    }

    @Override
    public String getExportSystemPowerDemand() {
        return getValueFromSDM120("005C");
    }

    @Override
    public String getMaxExportSystemPowerDemand() {
        return getValueFromSDM120("005E");
    }

    @Override
    public String getCurrentDemand() {
        return getValueFromSDM120("0102");
    }

    @Override
    public String getMaxCurrentDemand() {
        return getValueFromSDM120("0108");
    }

    @Override
    public String getTotalActiveEnergy() {
        return getValueFromSDM120("0156");
    }

    @Override
    public String getTotalReactiveActiveEnergy(int line) {
        return getValueFromSDM120("0158");
    }

    @Override
    public List<Channel> getChannelsList(Thing thing) {
        List<Channel> channelList = new ArrayList<>();
        channelList.add(Objects.requireNonNull(thing.getChannel("cmn#" + MegaDBindingConstants.CHANNEL_VOLTAGE)));
        channelList.add(Objects.requireNonNull(thing.getChannel("line1#" + MegaDBindingConstants.CHANNEL_CURRENT)));
        channelList.add(Objects.requireNonNull(thing.getChannel("line1#" + MegaDBindingConstants.CHANNEL_ACTIVEPOWER)));
        channelList
                .add(Objects.requireNonNull(thing.getChannel("line1#" + MegaDBindingConstants.CHANNEL_APPARENTPOWER)));
        channelList
                .add(Objects.requireNonNull(thing.getChannel("line1#" + MegaDBindingConstants.CHANNEL_REACTIVEPOWER)));
        channelList.add(Objects.requireNonNull(thing.getChannel("line1#" + MegaDBindingConstants.CHANNEL_POWERFACTOR)));
        channelList.add(Objects.requireNonNull(thing.getChannel("line1#" + MegaDBindingConstants.CHANNEL_PHASEANGLE)));
        channelList.add(Objects.requireNonNull(thing.getChannel("cmn#" + MegaDBindingConstants.CHANNEL_FREQUENCY)));
        channelList.add(Objects.requireNonNull(thing.getChannel("cmn#" + MegaDBindingConstants.CHANNEL_IMPORTACTNRG)));
        channelList.add(Objects.requireNonNull(thing.getChannel("cmn#" + MegaDBindingConstants.CHANNEL_EXPORTACTNRG)));
        channelList
                .add(Objects.requireNonNull(thing.getChannel("cmn#" + MegaDBindingConstants.CHANNEL_IMPORTREACTNRG)));
        channelList
                .add(Objects.requireNonNull(thing.getChannel("cmn#" + MegaDBindingConstants.CHANNEL_EXPORTREACTNRG)));
        channelList
                .add(Objects.requireNonNull(thing.getChannel("cmn#" + MegaDBindingConstants.CHANNEL_TOTALSYSPWRDMD)));
        channelList.add(
                Objects.requireNonNull(thing.getChannel("cmn#" + MegaDBindingConstants.CHANNEL_MAXTOTALSYSPWRDMD)));
        channelList
                .add(Objects.requireNonNull(thing.getChannel("cmn#" + MegaDBindingConstants.CHANNEL_IMPORTSYSPWRDMD)));
        channelList.add(
                Objects.requireNonNull(thing.getChannel("cmn#" + MegaDBindingConstants.CHANNEL_MAXIMPORTSYSPWRDMD)));
        channelList
                .add(Objects.requireNonNull(thing.getChannel("cmn#" + MegaDBindingConstants.CHANNEL_EXPORTSYSPWRDMD)));
        channelList.add(
                Objects.requireNonNull(thing.getChannel("cmn#" + MegaDBindingConstants.CHANNEL_MAXEXPORTSYSPWRDMD)));
        channelList.add(Objects.requireNonNull(thing.getChannel("cmn#" + MegaDBindingConstants.CHANNEL_CURRENTDMD)));
        channelList.add(Objects.requireNonNull(thing.getChannel("cmn#" + MegaDBindingConstants.CHANNEL_MAXCURRENTDMD)));
        channelList.add(Objects.requireNonNull(thing.getChannel("cmn#" + MegaDBindingConstants.CHANNEL_TOTALACTNRG)));
        channelList.add(Objects.requireNonNull(thing.getChannel("cmn#" + MegaDBindingConstants.CHANNEL_TOTALREACTNRG)));
        return channelList;
    }

    @Override
    public String getActiveEnergy(int line) {
        return " ";
    }

    @Override
    public void updateValues() {
    }
}
