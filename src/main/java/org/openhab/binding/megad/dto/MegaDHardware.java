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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.megad.internal.MegaDTypesEnum;
import org.openhab.binding.megad.internal.MegaHTTPResponse;
import org.openhab.binding.megad.internal.MegaHttpHelpers;

/**
 * The {@link MegaDHardware} is responsible for creating things and thing
 * handlers.
 *
 * @author Petr Shatsillo - Initial contribution
 */
@NonNullByDefault
public class MegaDHardware {
    // private final Logger logger = LoggerFactory.getLogger(MegaDHardware.class);
    private String firmware = "";
    private String actualFirmware = "";
    private String type = "";
    private String mdid = "";
    private boolean srvloop = false;
    private int portsCount;
    private final Map<Integer, MegaDTypesEnum> portsType = new HashMap<>();

    public MegaDHardware(String hostname, String password) {
        MegaHttpHelpers http = new MegaHttpHelpers();
        MegaHTTPResponse megaHTTPResponse = http.request("http://" + hostname + "/" + password + "/?cf=2");
        mdid = megaHTTPResponse.getResponseResult()
                .substring(megaHTTPResponse.getResponseResult().indexOf("name=mdid"));
        mdid = mdid.substring(mdid.indexOf("value=") + "value=".length(), mdid.indexOf("><br>")).replace("\"", "");

        String srvloop = megaHTTPResponse.getResponseResult()
                .substring(megaHTTPResponse.getResponseResult().indexOf("name=sl"));
        srvloop = srvloop.substring(srvloop.indexOf("name=sl") + "name=sl".length(), srvloop.indexOf("><br>"));
        if (srvloop.contains("checked")) {
            this.srvloop = true;
        }

        megaHTTPResponse = http.request("https://www.ab-log.ru/smart-house/ethernet/megad-2561-firmware");
        actualFirmware = megaHTTPResponse.getResponseResult().substring(
                megaHTTPResponse.getResponseResult().indexOf("<ul><li>") + "<ul><li>".length(),
                megaHTTPResponse.getResponseResult().indexOf("</font><br>"));
        actualFirmware = actualFirmware.split("ver")[1].trim().strip();
    }

    public MegaDHardware() {
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

    // public String getMdid() {
    // return mdid;
    // }
    public boolean isSrvloop() {
        return srvloop;
    }

    public String getActualFirmware() {
        return actualFirmware;
    }

    public void setPortsCount(int ports) {
        this.portsCount = ports;
    }

    public int getPortsCount() {
        return this.portsCount;
    }

    public void setPortType(int portNum, MegaDTypesEnum megaDTypesEnum) {
        portsType.put(portNum, megaDTypesEnum);
    }

    // public Map<Integer, MegaDTypesEnum> getPortsType() {
    // return portsType;
    // }
}
