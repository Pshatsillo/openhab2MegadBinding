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
package org.openhab.binding.megad.internal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link MegaHttpHelpers} is responsible for http request to megad
 *
 * @author Petr Shatsillo - Initial contribution
 */
@NonNullByDefault
public class MegaHttpHelpers {
    public MegaHTTPResponse request(String urlString) {
        Logger logger = LoggerFactory.getLogger(MegaHttpHelpers.class);
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
                // logger.debug("input string from {} -> {}", url, megaHTTPResponse.getResponseResult());
            }
            con.disconnect();
        } catch (IOException e) {
            logger.error("Connect to megadevice error: {}", e.getLocalizedMessage());
        }
        return megaHTTPResponse;
    }
}
