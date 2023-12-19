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
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link MegaDHttpHelpers} is responsible for http request to megad
 *
 * @author Petr Shatsillo - Initial contribution
 */
@NonNullByDefault
public class MegaDHttpHelpers {
    Logger logger = LoggerFactory.getLogger(MegaDHttpHelpers.class);

    public MegaDHTTPResponse request(String urlString) {
        MegaDHTTPResponse megaDHTTPResponse = new MegaDHTTPResponse();
        String result = "";
        if (!urlString.isEmpty()) {
            try {
                URL url = new URL(urlString);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");
                con.setReadTimeout(1500);
                con.setConnectTimeout(1500);
                con.setRequestProperty("User-Agent", "Mozilla/5.0");
                megaDHTTPResponse.setResponseCode(con.getResponseCode());
                if (con.getResponseCode() == 200) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), "Windows-1251"));
                    String inputLine;
                    StringBuilder response = new StringBuilder();
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();
                    result = response.toString().trim();
                    // logger.debug("input string from {} -> {}", url, megaHTTPResponse.getResponseResult());
                }
                con.disconnect();
            } catch (IOException e) {
                logger.error("Connect to megadevice error: {}", e.getLocalizedMessage());
            }
        }
        megaDHTTPResponse.setResponseResult(result.replace("\"", ""));
        return megaDHTTPResponse;
    }

    public void sendToLCDrawStream(String hostname, String request) {
        String req = "GET " + request + " HTTP/1.1\n\r " + "User-Agent: Mozilla/5.0\n\r " + "Host: " + hostname
                + "\n\r " + "Accept: text/html\n\r " + "Connection: keep-alive";
        int degreeIndex = req.indexOf("°");
        req = req.replace("°", "_");
        int port = 80;
        try (Socket socket = new Socket(hostname, port)) {
            OutputStream output = socket.getOutputStream();
            byte[] data = req.getBytes(StandardCharsets.UTF_8);
            if (degreeIndex != -1) {
                data[degreeIndex] = (byte) 0xdf;
            }
            output.write(data);
            logger.info("LCD send: {}", data);
        } catch (UnknownHostException ex) {
            logger.error("Server not found: {}", ex.getMessage());
        } catch (IOException ex) {
            logger.error("I/O error: {}", ex.getMessage());
        }
    }
}
