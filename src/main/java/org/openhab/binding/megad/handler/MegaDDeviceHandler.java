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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.megad.MegaDConfiguration;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseBridgeHandler;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link MegaDDeviceHandler} is responsible for creating things and thing
 * handlers.
 *
 * @author Petr Shatsillo - Initial contribution
 */
@NonNullByDefault
public class MegaDDeviceHandler extends BaseBridgeHandler {
    private final Logger logger = LoggerFactory.getLogger(MegaDDeviceHandler.class);
    private @Nullable MegaDConfiguration config;

    public MegaDDeviceHandler(Bridge bridge) {
        super(bridge);
    }

    @Override
    public void initialize() {
        config = getConfigAs(MegaDConfiguration.class);
        final MegaDConfiguration config = this.config;
        if (config != null) {
            HttpURLConnection con;
            try {
                URL url = new URL("http://" + config.hostname + "/" + config.password);
                con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");
                con.setReadTimeout(1500);
                con.setConnectTimeout(1500);
                con.setRequestProperty("User-Agent", "Mozilla/5.0");
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                String result = response.toString().trim();
                logger.debug("input string from {} -> {}", url, result);
                con.disconnect();
            } catch (IOException e) {
                logger.error("Connect to megadevice error: {}", e.getLocalizedMessage());
            }

            String firmware = "";
            Map<String, String> properties = new HashMap<>();
            properties.put("Firmware:", firmware);
            updateProperties(properties);
            updateStatus(ThingStatus.ONLINE);
        } else {
            updateStatus(ThingStatus.UNINITIALIZED, ThingStatusDetail.CONFIGURATION_ERROR, "Config is null");
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
    }
}
