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

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
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
    @Nullable
    HttpClient httpClient = null;

    public MegaDHTTPResponse request(String urlString, int timeout) {
        MegaDHTTPResponse megaDHTTPResponse = new MegaDHTTPResponse();
        if (!urlString.isEmpty()) {
            HttpClient httpClient = this.httpClient;
            if (httpClient != null) {
                Request request = httpClient.newRequest(urlString).method(HttpMethod.GET)
                        .timeout(timeout, TimeUnit.MILLISECONDS).header("User-Agent",
                                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36");
                try {
                    ContentResponse response = request.send();
                    if (response.getStatus() == HttpStatus.OK_200) {
                        megaDHTTPResponse.setResponseCode(HttpStatus.OK_200);
                        megaDHTTPResponse
                                .setResponseResult(new String(response.getContent(), Charset.forName("windows-1251"))
                                        .trim().replace("\"", ""));
                        logger.trace("Http response from url {} is {}", urlString,
                                megaDHTTPResponse.getResponseResult());
                    } else {
                        logger.error("Megad request resulted in HTTP {} with message: {}", response.getStatus(),
                                response.getReason());
                    }
                } catch (InterruptedException | TimeoutException | ExecutionException e) {
                    logger.error("Request to {} failed: {}", urlString, e.getLocalizedMessage());
                }
            }
        }
        return megaDHTTPResponse;
    }

    public MegaDHTTPResponse request(String urlString) {
        return request(urlString, 300);
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

    public void setHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }
}
