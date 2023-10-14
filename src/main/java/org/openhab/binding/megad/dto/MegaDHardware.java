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
package org.openhab.binding.megad.dto;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.megad.internal.MegaHTTPResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link MegaDHardware} is responsible for creating things and thing
 * handlers.
 *
 * @author Petr Shatsillo - Initial contribution
 */
@NonNullByDefault
public class MegaDHardware {
    private final Logger logger = LoggerFactory.getLogger(MegaDHardware.class);
    private String firmware = "";
    private String actualFirmware = "";
    private String type = "";
    private String mdid = "";
    private boolean srvloop = false;

    public MegaDHardware(String hostname, String password) {
        MegaHTTPResponse megaHTTPResponse = request("http://" + hostname + "/" + password + "/?cf=2");
        mdid = megaHTTPResponse.getResponseResult()
                .substring(megaHTTPResponse.getResponseResult().indexOf("name=mdid"));
        mdid = mdid.substring(mdid.indexOf("value=") + "value=".length(), mdid.indexOf("><br>")).replace("\"", "");

        String srvloop = megaHTTPResponse.getResponseResult()
                .substring(megaHTTPResponse.getResponseResult().indexOf("name=sl"));
        srvloop = srvloop.substring(srvloop.indexOf("name=sl") + "name=sl".length(), srvloop.indexOf("><br>"));
        if (srvloop.contains("checked")) {
            this.srvloop = true;
        }

        megaHTTPResponse = request("https://www.ab-log.ru/smart-house/ethernet/megad-2561-firmware");
        actualFirmware = megaHTTPResponse.getResponseResult().substring(
                megaHTTPResponse.getResponseResult().indexOf("<ul><li>") + "<ul><li>".length(),
                megaHTTPResponse.getResponseResult().indexOf("</font><br>"));
        actualFirmware = actualFirmware.split("ver")[1].trim().strip();
    }

    public void parse(String result) {
        type = result.strip().trim().split(" ")[0];
        firmware = result.substring(result.indexOf("fw:") + 3, result.indexOf("<br>") - 1).strip().trim();
    }

    public String getType() {
        return type;
    }

    public String getFirmware() {
        return firmware;
    }

    public void config(String megaConfig) {
    }

    public String getMdid() {
        return mdid;
    }

    public boolean isSrvloop() {
        return srvloop;
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

    public String getActualFirmware() {
        return actualFirmware;
    }
}
