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
 * The {@link MegaDWBMAP6S} is responsible for rs485/modbus feature of megad
 *
 * @author Petr Shatsillo - Initial contribution
 */

@NonNullByDefault
public class MegaDWBMAP6S implements ModbusPowermeterInterface {
    String address;
    MegaDBridgeDeviceHandler bridgeHandler;

    public MegaDWBMAP6S(MegaDBridgeDeviceHandler bridgeHandler, String address) {
        this.address = address;
        this.bridgeHandler = bridgeHandler;
    }

    private String getValueFromWBMAP6S(String valueByte, int registersCount) {
        final Logger logger = LoggerFactory.getLogger(MegaDWBMAP6S.class);
        String result = "http://" + bridgeHandler.getThing().getConfiguration().get("hostname").toString() + "/"
                + bridgeHandler.getThing().getConfiguration().get("password").toString() + "/?uart_tx="
                + Integer.toHexString(Integer.parseInt(address)) + "04" + valueByte + "000" + registersCount
                + "&mode=rs485";
        MegaHttpHelpers.sendRequest(result);
        try {
            Thread.sleep(100);
        } catch (InterruptedException ignored) {
        }
        result = "http://" + bridgeHandler.getThing().getConfiguration().get("hostname").toString() + "/"
                + bridgeHandler.getThing().getConfiguration().get("password").toString() + "/?uart_rx=1&mode=rs485";
        String updateRequest = MegaHttpHelpers.sendRequest(result);
        logger.debug("WB-MAP6S answer: {}", updateRequest);
        try {
            String[] answer = updateRequest.split("[|]");
            String parse = "";
            int bytesCnt = Integer.parseInt(answer[2]);
            for (int i = 1; i <= bytesCnt; i++) {
                parse = parse + answer[i + 2];
            }
            logger.debug("WB-MAP6S hex answer: {}", parse);

            return parse;
        } catch (Exception ignored) {
            return "ERROR";
        }
    }

    @Override
    public String getVoltage() {
        String value = getValueFromWBMAP6S("10D9", 1);
        int n = (int) Long.parseLong(value, 16);
        float voltage = (float) (n * 0.01);
        return String.format("%.2f", voltage).replace(",", ".");
    }

    @Override
    public void updateValues() {
    }

    @Override
    public String getCurrent() {
        String value = getValueFromWBMAP6S("141A", 2);
        int n = (int) Long.parseLong(value, 16);
        float voltage = (float) (n * 2.44141E-07);
        return String.format("%.2f", voltage).replace(",", ".");
    }

    @Override
    public String getActivePower() {
        String value = getValueFromWBMAP6S("1306", 2);
        int n = (int) Long.parseLong(value, 16);
        float voltage = (float) (n * 2.44141E-07);
        return String.format("%.2f", voltage).replace(",", ".");
    }

    @Override
    public String getApparentPower() {
        return " ";
    }

    @Override
    public String getReactivePower() {
        return " ";
    }

    @Override
    public String getPowerFactor() {
        return " ";
    }

    @Override
    public String getPhaseAngle() {
        return " ";
    }

    @Override
    public String getFrequency() {
        return " ";
    }

    @Override
    public String getImportActiveEnergy() {
        return " ";
    }

    @Override
    public String getExportActiveEnergy() {
        return " ";
    }

    @Override
    public String getImportReactiveEnergy() {
        return " ";
    }

    @Override
    public String getExportReactiveEnergy() {
        return " ";
    }

    @Override
    public String getTotalSystemPowerDemand() {
        return " ";
    }

    @Override
    public String getMaxTotalSystemPowerDemand() {
        return " ";
    }

    @Override
    public String getImportSystemPowerDemand() {
        return " ";
    }

    @Override
    public String getMaxImportSystemPowerDemand() {
        return " ";
    }

    @Override
    public String getExportSystemPowerDemand() {
        return " ";
    }

    @Override
    public String getMaxExportSystemPowerDemand() {
        return " ";
    }

    @Override
    public String getCurrentDemand() {
        return " ";
    }

    @Override
    public String getMaxCurrentDemand() {
        return " ";
    }

    @Override
    public String getTotalActiveEnergy() {
        return " ";
    }

    @Override
    public String getTotalReactiveActiveEnergy() {
        return " ";
    }

    @Override
    public List<Channel> getChannelsList(Thing thing) {
        List<Channel> channelList = new ArrayList<>();
        channelList.add(Objects.requireNonNull(thing.getChannel(MegaDBindingConstants.CHANNEL_VOLTAGE)));
        channelList.add(Objects.requireNonNull(thing.getChannel(MegaDBindingConstants.CHANNEL_CURRENT)));
        channelList.add(Objects.requireNonNull(thing.getChannel(MegaDBindingConstants.CHANNEL_ACTIVEPOWER)));
        channelList.add(Objects.requireNonNull(thing.getChannel(MegaDBindingConstants.CHANNEL_APPARENTPOWER)));
        channelList.add(Objects.requireNonNull(thing.getChannel(MegaDBindingConstants.CHANNEL_REACTIVEPOWER)));
        channelList.add(Objects.requireNonNull(thing.getChannel(MegaDBindingConstants.CHANNEL_POWERFACTOR)));
        channelList.add(Objects.requireNonNull(thing.getChannel(MegaDBindingConstants.CHANNEL_PHASEANGLE)));
        channelList.add(Objects.requireNonNull(thing.getChannel(MegaDBindingConstants.CHANNEL_FREQUENCY)));
        channelList.add(Objects.requireNonNull(thing.getChannel(MegaDBindingConstants.CHANNEL_IMPORTACTNRG)));
        channelList.add(Objects.requireNonNull(thing.getChannel(MegaDBindingConstants.CHANNEL_EXPORTACTNRG)));
        channelList.add(Objects.requireNonNull(thing.getChannel(MegaDBindingConstants.CHANNEL_IMPORTREACTNRG)));
        channelList.add(Objects.requireNonNull(thing.getChannel(MegaDBindingConstants.CHANNEL_EXPORTREACTNRG)));
        channelList.add(Objects.requireNonNull(thing.getChannel(MegaDBindingConstants.CHANNEL_TOTALSYSPWRDMD)));
        channelList.add(Objects.requireNonNull(thing.getChannel(MegaDBindingConstants.CHANNEL_MAXTOTALSYSPWRDMD)));
        channelList.add(Objects.requireNonNull(thing.getChannel(MegaDBindingConstants.CHANNEL_IMPORTSYSPWRDMD)));
        channelList.add(Objects.requireNonNull(thing.getChannel(MegaDBindingConstants.CHANNEL_MAXIMPORTSYSPWRDMD)));
        channelList.add(Objects.requireNonNull(thing.getChannel(MegaDBindingConstants.CHANNEL_EXPORTSYSPWRDMD)));
        channelList.add(Objects.requireNonNull(thing.getChannel(MegaDBindingConstants.CHANNEL_MAXEXPORTSYSPWRDMD)));
        channelList.add(Objects.requireNonNull(thing.getChannel(MegaDBindingConstants.CHANNEL_CURRENTDMD)));
        channelList.add(Objects.requireNonNull(thing.getChannel(MegaDBindingConstants.CHANNEL_MAXCURRENTDMD)));
        channelList.add(Objects.requireNonNull(thing.getChannel(MegaDBindingConstants.CHANNEL_TOTALACTNRG)));
        channelList.add(Objects.requireNonNull(thing.getChannel(MegaDBindingConstants.CHANNEL_TOTALREACTNRG)));
        return channelList;
    }
}
