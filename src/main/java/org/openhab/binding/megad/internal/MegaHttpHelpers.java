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
    public static String sendRequest(String URL) {
        Logger logger = LoggerFactory.getLogger(MegaHttpHelpers.class);
        String result = "";
        if (!"".equals(URL)) {
            try {
                java.net.URL urlreq = new URL(URL);
                HttpURLConnection con;

                con = (HttpURLConnection) urlreq.openConnection();

                logger.debug("URL: {}", URL);

                con.setRequestMethod("GET");
                // con.setReadTimeout(500);
                con.setReadTimeout(1500);
                con.setConnectTimeout(1500);
                // add request header
                con.setRequestProperty("User-Agent", "Mozilla/5.0");

                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                logger.debug("input string from {} -> {}", URL, response.toString());
                result = response.toString().trim();
                con.disconnect();
            } catch (IOException e) {
                logger.error("Connect to megadevice {} error: {}", URL, e.getLocalizedMessage());
            }
        }
        return result;
    }
}
