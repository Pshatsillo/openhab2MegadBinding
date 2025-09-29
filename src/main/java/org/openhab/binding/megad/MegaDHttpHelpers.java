/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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
package org.openhab.binding.megad;

import static java.net.http.HttpClient.newHttpClient;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
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

            HttpClient client = newHttpClient();
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(urlString)).GET().build();
            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                megaDHTTPResponse.setResponseCode(response.statusCode());
                @Nullable
                String responseBody = response.body();
                result = responseBody.trim();
            } catch (Exception e) {
                logger.error("Error sending request: {}", e.getLocalizedMessage());
            }
        }
        megaDHTTPResponse.setResponseResult(result.replace("\"", ""));
        logger.debug("Http response from url {} is {}", urlString, megaDHTTPResponse.getResponseResult());
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
            logger.trace("LCD send: {}", data);
        } catch (UnknownHostException ex) {
            logger.error("Server not found: {}", ex.getMessage());
        } catch (IOException ex) {
            logger.error("I/O error: {}", ex.getMessage());
        }
    }
}
