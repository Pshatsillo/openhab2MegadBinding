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
 * The {@link MegadMideaProtocol} is responsible for Midea modbus protocol feature for megad
 *
 * @author Petr Shatsillo - Initial contribution
 */
@NonNullByDefault
public class MegadMideaProtocol implements MegaDRS485Interface {
    String[] answer = { "" };
    final Logger logger = LoggerFactory.getLogger(MegadMideaProtocol.class);
    String address;

    public MegadMideaProtocol(String address) {
        this.address = address;
    }

    private void request(MegaDBridgeDeviceHandler bridgeHandler) {
        logger.debug("Requesting...");
        int crc = (int) Long.parseLong("C0", 16);
        crc += (int) Long.parseLong(address, 16);
        crc += (int) Long.parseLong("80", 16);
        crc += (int) Long.parseLong("3F", 16);
        int crcRq = 255 - crc % 256 + 1;
        String result = "http://"
                + Objects.requireNonNull(bridgeHandler).getThing().getConfiguration().get("hostname").toString() + "/"
                + Objects.requireNonNull(bridgeHandler).getThing().getConfiguration().get("password").toString()
                + "/?uart_tx=AAC0" + address + "008000000000000000003F" + String.format("%02X", crcRq) + "55";
        MegaHttpHelpers.sendRequest(result);
        try {
            Thread.sleep(200);
        } catch (InterruptedException ignored) {
        }
        result = "http://"
                + Objects.requireNonNull(bridgeHandler).getThing().getConfiguration().get("hostname").toString() + "/"
                + Objects.requireNonNull(bridgeHandler).getThing().getConfiguration().get("password").toString()
                + "/?uart_rx=1";
        String updateRequest = MegaHttpHelpers.sendRequest(result);
        logger.info("Midea answer is: {}", updateRequest);
        try {
            answer = updateRequest.split("[|]");
            String parse = "";
            int crcSum = 0;
            for (int i = 1; i < 29; i++) {
                parse += answer[i];
                int n = (int) Long.parseLong(answer[i], 16);
                crcSum += n;
            }
            int crcFin = 255 - crcSum % 256 + 1;
            if (answer[30].equals(String.format("%02X", crcFin))) {
                logger.debug("CRC: {}", String.format("%02X", crcFin));
            } else {
                logger.debug("Bad CRC: {}", String.format("%02X", crcFin));
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public String[] getValueFromRS485(MegaDBridgeDeviceHandler bridgeHandler) {
        answer = new String[] { "" };
        request(bridgeHandler);
        return answer;
    }

    @Override
    public void setValuesToRS485(MegaDBridgeDeviceHandler bridgeHandler, String channelUID, String command) {
        answer = new String[] { "" };
        request(bridgeHandler);
        try {
            Thread.sleep(200);
        } catch (InterruptedException ignored) {
        }
        String cmd = "AAC3" + String.format("%02X", Integer.parseInt(address)) + "008000";
        int crc = (int) Long.parseLong("C3", 16);
        crc += (int) Long.parseLong(String.format("%02X", Integer.parseInt(address)), 16);
        crc += (int) Long.parseLong("80", 16);
        if ("opermode".equals(channelUID)) {
            switch (command) {
                case "OFF":
                    cmd += "00";
                    crc += (int) Long.parseLong("00", 16);
                    break;
                case "AUTO":
                    cmd += "98";
                    crc += (int) Long.parseLong("98", 16);
                    break;
                case "COOL":
                    cmd += "88";
                    crc += (int) Long.parseLong("88", 16);
                    break;
                case "DRY":
                    cmd += "82";
                    crc += (int) Long.parseLong("82", 16);
                    break;
                case "HEAT":
                    cmd += "84";
                    crc += (int) Long.parseLong("84", 16);
                    break;
                case "FAN":
                    cmd += "81";
                    crc += (int) Long.parseLong("81", 16);
                    break;
            }
        } else {
            if (answer.length == 32) {
                cmd += answer[8];
                crc += (int) Long.parseLong(answer[8], 16);
            } else {
                logger.error("Response from rs485 contains errors: <{}>", (Object) answer);
            }
        }

        if ("fanmode".equals(channelUID)) {
            switch (command) {
                case "OFF":
                    cmd += "00";
                    crc += (int) Long.parseLong("00", 16);
                    break;
                case "AUTO":
                    cmd += "84";
                    crc += (int) Long.parseLong("84", 16);
                    break;
                case "HIGH":
                    cmd += "01";
                    crc += (int) Long.parseLong("01", 16);
                    break;
                case "MEDIUM":
                    cmd += "02";
                    crc += (int) Long.parseLong("02", 16);
                    break;
                case "LOW":
                    cmd += "04";
                    crc += (int) Long.parseLong("04", 16);
                    break;
            }
        } else {
            if (answer.length == 32) {
                cmd += answer[9];
                crc += (int) Long.parseLong(answer[9], 16);
            } else {
                logger.error("Response from rs485 contains errors: <{}>", (Object) answer);
            }

        }
        if ("mideatemperature".equals(channelUID)) {
            cmd += String.format("%02X", Integer.parseInt(command));
            crc += Integer.parseInt(command);
        } else {
            if (answer.length == 32) {
                cmd += answer[10];
                crc += (int) Long.parseLong(answer[10], 16);
            } else {
                logger.error("Response from rs485 contains errors: <{}>", (Object) answer);
            }

        }
        if (answer.length == 32) {
            crc += (int) Long.parseLong(answer[20] + answer[17] + answer[18] + "003C", 16);
            int crcFin = 255 - crc % 256 + 1;
            cmd += answer[20] + answer[17] + answer[18] + "003C" + String.format("%02X", crcFin) + "55";
            String result = "http://"
                    + Objects.requireNonNull(bridgeHandler).getThing().getConfiguration().get("hostname").toString()
                    + "/"
                    + Objects.requireNonNull(bridgeHandler).getThing().getConfiguration().get("password").toString()
                    + "/?uart_tx=" + cmd;
            MegaHttpHelpers.sendRequest(result);
            logger.debug("Sending command: {}", result);
            try {
                Thread.sleep(200);
            } catch (InterruptedException ignored) {
            }
            result = "http://"
                    + Objects.requireNonNull(bridgeHandler).getThing().getConfiguration().get("hostname").toString()
                    + "/"
                    + Objects.requireNonNull(bridgeHandler).getThing().getConfiguration().get("password").toString()
                    + "/?uart_rx=1";
            String updateRequest = MegaHttpHelpers.sendRequest(result);
            logger.info("Receive: {}", updateRequest);
        } else {
            logger.error("Response from rs485 contains errors: <{}>", (Object) answer);
        }
    }

    @Override
    public List<Channel> getChannelsList(Thing thing) {
        List<Channel> channelList = new ArrayList<>();
        channelList.add(Objects.requireNonNull(thing.getChannel(MegaDBindingConstants.CHANNEL_MIDEAFANMODE)));
        channelList.add(Objects.requireNonNull(thing.getChannel(MegaDBindingConstants.CHANNEL_MIDEAOPERMODE)));
        channelList.add(Objects.requireNonNull(thing.getChannel(MegaDBindingConstants.CHANNEL_MIDEATEMP)));
        return channelList;
    }
}
