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
 * The {@link MegadDD238} is responsible for rs485/modbus feature of megad
 *
 * @author Petr Shatsillo - Initial contribution
 */
@NonNullByDefault
public class MegadDD238 {

    @Nullable
    public static String[] getValueFromDD238(@Nullable MegaDBridgeDeviceHandler bridgeHandler, String address) {
        final Logger logger = LoggerFactory.getLogger(MegadDD238.class);
        assert bridgeHandler != null;
        String result = "http://"
                + Objects.requireNonNull(bridgeHandler).getThing().getConfiguration().get("hostname").toString() + "/"
                + Objects.requireNonNull(bridgeHandler).getThing().getConfiguration().get("password").toString()
                + "/?uart_tx=" + address + "0300000012&mode=rs485";
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
        logger.debug("DD238 answer: {}", updateRequest);
        try {
            String[] answer = updateRequest.split("[|]");
            if (answer[0].equals(address)) {
                return answer;
            } else {
                String[] error = { "ERROR" };
                return error;
            }
        } catch (Exception ignored) {
            String[] answer = { "ERROR" };
            return answer;

        }
    }
}
