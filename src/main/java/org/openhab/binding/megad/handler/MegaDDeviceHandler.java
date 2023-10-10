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
import java.net.InetAddress;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.megad.MegaDConfiguration;
import org.openhab.binding.megad.dto.MegaDHardware;
import org.openhab.binding.megad.internal.MegaDService;
import org.openhab.binding.megad.internal.MegaHTTPResponse;
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
        MegaDHardware mega = new MegaDHardware();
        if (config != null) {
            MegaHTTPResponse response = request("http://" + config.hostname + "/" + config.password);
            if (response.getResponseCode() >= 400) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Wrong password");
            } else {
                mega.parse(response.getResponseResult());
                mega.config(getMegaConfig());
                Map<String, String> properties = new HashMap<>();
                properties.put("Type:", mega.getType());
                properties.put("Firmware:", mega.getFirmware());
                // TODO: get actual firmware
                properties.put("Actual Firmware:", mega.getFirmware());
                updateProperties(properties);
                updateStatus(ThingStatus.ONLINE);
                String ip = config.hostname.substring(0, config.hostname.lastIndexOf("."));
                for (InetAddress address : MegaDService.interfacesAddresses) {
                    if (address.getHostAddress().startsWith(ip)) {
                        request("http://" + config.hostname + "/" + config.password + "/?cf=1&sip="
                                + MegaDService.interfacesAddresses.stream().findFirst().get().getHostAddress() + "%3A"
                                + MegaDService.port + "&sct=megad");
                    }

                }
            }
        } else {
            updateStatus(ThingStatus.UNINITIALIZED, ThingStatusDetail.CONFIGURATION_ERROR, "Config is null");
        }
    }

    private String getMegaConfig() {
        final MegaDConfiguration config = this.config;
        if (config != null) {
            MegaHTTPResponse megaHTTPResponse = request("http://" + config.hostname + "/" + config.password + "/?cf=1");
            String cfg = megaHTTPResponse.getResponseResult();
            megaHTTPResponse = request("http://" + config.hostname + "/" + config.password + "/?cf=2");
            cfg += megaHTTPResponse.getResponseResult();
            return cfg;
        } else {
            return "";
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
    }

    private MegaHTTPResponse request(String urlString) {
        MegaHTTPResponse megaHTTPResponse = new MegaHTTPResponse();
        try {
            URL url = new URL(urlString);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setReadTimeout(1500);
            con.setConnectTimeout(1500);
            con.setRequestProperty("User-Agent", "Mozilla/5.0");
            megaHTTPResponse.setResponseCode(con.getResponseCode());
            if (con.getResponseCode() == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                megaHTTPResponse.setResponseResult(response.toString().trim());
                logger.debug("input string from {} -> {}", url, megaHTTPResponse.getResponseResult());
            }
            con.disconnect();
        } catch (IOException e) {
            logger.error("Connect to megadevice error: {}", e.getLocalizedMessage());
        }
        return megaHTTPResponse;
    }
}
