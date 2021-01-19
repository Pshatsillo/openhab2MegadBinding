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

import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.megad.handler.MegaDBridgeDeviceHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link Sdm120} is responsible for rs485/modbus feature of megad
 *
 * @author Petr Shatsillo - Initial contribution
 */

@NonNullByDefault
public class Sdm120 {

    @Nullable
    public static String getValueFromSDM120(@Nullable MegaDBridgeDeviceHandler bridgeHandler, String address,
            String valueByte) {
        final Logger logger = LoggerFactory.getLogger(Sdm120.class);
        assert bridgeHandler != null;
        String result = "http://"
                + Objects.requireNonNull(bridgeHandler).getThing().getConfiguration().get("hostname").toString() + "/"
                + Objects.requireNonNull(bridgeHandler).getThing().getConfiguration().get("password").toString()
                + "/?uart_tx=" + address + "04" + valueByte + "0002&mode=rs485";
        MegaHttpHelpers.sendRequest(result);
        try {
            Thread.sleep(100);
        } catch (InterruptedException ignored) {
        }
        result = "http://"
                + Objects.requireNonNull(bridgeHandler).getThing().getConfiguration().get("hostname").toString() + "/"
                + Objects.requireNonNull(bridgeHandler).getThing().getConfiguration().get("password").toString()
                + "/?uart_rx=1&mode=rs485";
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
            return null;
        }
    }
}
