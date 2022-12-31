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
            if (registersCount != 4) {
                for (int i = 1; i <= bytesCnt; i++) {
                    parse = parse + answer[i + 2];
                }
            } else {
                parse = answer[9] + answer[10] + answer[7] + answer[8] + answer[5] + answer[6] + answer[3] + answer[4];
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
    public String getCurrent(int line) {
        String value;
        switch (line) {
            case 1:
                value = getValueFromWBMAP6S("141A", 2);
                return String.format("%.2f", (float) ((int) Long.parseLong(value, 16) * 2.44141E-07)).replace(",", ".");
            case 2:
                value = getValueFromWBMAP6S("1418", 2);
                return String.format("%.2f", (float) ((int) Long.parseLong(value, 16) * 2.44141E-07)).replace(",", ".");
            case 3:
                value = getValueFromWBMAP6S("1416", 2);
                return String.format("%.2f", (float) ((int) Long.parseLong(value, 16) * 2.44141E-07)).replace(",", ".");
            default:
                return "ERR";
        }
    }

    @Override
    public String getActivePower(int line) {
        String value;
        switch (line) {
            case 1:
                value = getValueFromWBMAP6S("1306", 2);
                return String.format("%.2f", (float) ((int) Long.parseLong(value, 16) * 0.000244141)).replace(",", ".");
            case 2:
                value = getValueFromWBMAP6S("1304", 2);
                return String.format("%.2f", (float) ((int) Long.parseLong(value, 16) * 0.000244141)).replace(",", ".");
            case 3:
                value = getValueFromWBMAP6S("1302", 2);
                return String.format("%.2f", (float) ((int) Long.parseLong(value, 16) * 0.000244141)).replace(",", ".");
            default:
                return "ERR";
        }
    }

    @Override
    public String getApparentPower(int line) {
        String value;
        switch (line) {
            case 1:
                value = getValueFromWBMAP6S("1316", 2);
                return String.format("%.2f", (float) ((int) Long.parseLong(value, 16) * 0.000244141)).replace(",", ".");
            case 2:
                value = getValueFromWBMAP6S("1314", 2);
                return String.format("%.2f", (float) ((int) Long.parseLong(value, 16) * 0.000244141)).replace(",", ".");
            case 3:
                value = getValueFromWBMAP6S("1312", 2);
                return String.format("%.2f", (float) ((int) Long.parseLong(value, 16) * 0.000244141)).replace(",", ".");
            default:
                return "ERR";
        }
    }

    @Override
    public String getReactivePower(int line) {
        String value;
        switch (line) {
            case 1:
                value = getValueFromWBMAP6S("130E", 2);
                return String.format("%.2f", (float) ((int) Long.parseLong(value, 16) * 0.000244141)).replace(",", ".");
            case 2:
                value = getValueFromWBMAP6S("130C", 2);
                return String.format("%.2f", (float) ((int) Long.parseLong(value, 16) * 0.000244141)).replace(",", ".");
            case 3:
                value = getValueFromWBMAP6S("130A", 2);
                return String.format("%.2f", (float) ((int) Long.parseLong(value, 16) * 0.000244141)).replace(",", ".");
            default:
                return "ERR";
        }
    }

    @Override
    public String getPowerFactor(int line) {
        String value;
        switch (line) {
            case 1:
                value = getValueFromWBMAP6S("10BF", 1);
                return String.format("%.2f", (float) ((short) Integer.parseInt(value, 16) * 0.001)).replace(",", ".");
            case 2:
                value = getValueFromWBMAP6S("10BE", 1);
                return String.format("%.2f", (float) ((short) Integer.parseInt(value, 16) * 0.001)).replace(",", ".");
            case 3:
                value = getValueFromWBMAP6S("10BD", 1);
                return String.format("%.2f", (float) ((short) Integer.parseInt(value, 16) * 0.001)).replace(",", ".");
            default:
                return "ERR";
        }
    }

    @Override
    public String getPhaseAngle(int line) {
        String value;
        switch (line) {
            case 1:
                value = getValueFromWBMAP6S("10FB", 1);
                return String.format("%.2f", (float) ((short) Integer.parseInt(value, 16) * 0.1)).replace(",", ".");
            case 2:
                value = getValueFromWBMAP6S("10FA", 1);
                return String.format("%.2f", (float) ((short) Integer.parseInt(value, 16) * 0.1)).replace(",", ".");
            case 3:
                value = getValueFromWBMAP6S("10F9", 1);
                return String.format("%.2f", (float) ((short) Integer.parseInt(value, 16) * 0.1)).replace(",", ".");
            default:
                return "ERR";
        }
    }

    @Override
    public String getFrequency() {
        String value = getValueFromWBMAP6S("10F8", 1);
        return String.format("%.2f", (float) ((int) Long.parseLong(value, 16) * 0.01)).replace(",", ".");
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
    public String getActiveEnergy(int line) {
        String value;
        switch (line) {
            case 1:
                value = getValueFromWBMAP6S("120C", 4);
                return String.format("%.2f", (float) Long.parseLong(value, 16) * 0.00001).replace(",", ".");
            case 2:
                value = getValueFromWBMAP6S("1208", 4);
                return String.format("%.2f", (float) Long.parseLong(value, 16) * 0.00001).replace(",", ".");
            case 3:
                value = getValueFromWBMAP6S("1204", 4);
                return String.format("%.2f", (float) Long.parseLong(value, 16) * 0.00001).replace(",", ".");
            default:
                return "ERR";
        }
    }

    @Override
    public String getTotalReactiveActiveEnergy(int line) {
        String value;
        switch (line) {
            case 1:
                value = getValueFromWBMAP6S("122C", 4);
                return String.format("%.2f", (float) Long.parseLong(value, 16) * 0.00001).replace(",", ".");
            case 2:
                value = getValueFromWBMAP6S("1228", 4);
                return String.format("%.2f", (float) Long.parseLong(value, 16) * 0.00001).replace(",", ".");
            case 3:
                value = getValueFromWBMAP6S("1224", 4);
                return String.format("%.2f", (float) Long.parseLong(value, 16) * 0.00001).replace(",", ".");
            default:
                return "ERR";
        }
    }

    @Override
    public List<Channel> getChannelsList(Thing thing) {
        List<Channel> channelList = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            List<Channel> lineList = thing.getChannelsOfGroup("line" + i);
            channelList.addAll(lineList);
        }
        List<Channel> cmn = new ArrayList<>();
        for (Channel chn : thing.getChannelsOfGroup("cmn")) {
            if (chn.getUID().getId().contains(MegaDBindingConstants.CHANNEL_VOLTAGE)
                    || chn.getUID().getId().contains(MegaDBindingConstants.CHANNEL_FREQUENCY)) {
                cmn.add(chn);
            }
        }
        channelList.addAll(cmn);
        return channelList;
    }
}
