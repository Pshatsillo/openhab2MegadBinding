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

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link MegaDHardware} is responsible for creating things and thing
 * handlers.
 *
 * @author Petr Shatsillo - Initial contribution
 */
@NonNullByDefault
public class MegaDHardware {
    private String firmware = "";
    private String type = "";
    private String mdid = "";
    private boolean srvloop = false;

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
        mdid = megaConfig.substring(megaConfig.indexOf("name=mdid"));
        mdid = mdid.substring(mdid.indexOf("value=") + "value=".length(), mdid.indexOf("><br>")).replace("\"", "");

        String srvloop = megaConfig.substring(megaConfig.indexOf("name=sl"));
        srvloop = srvloop.substring(srvloop.indexOf("name=sl") + "name=sl".length(), srvloop.indexOf("><br>"));
        if (srvloop.contains("checked")) {
            this.srvloop = true;
        }
    }

    public String getMdid() {
        return mdid;
    }

    public boolean isSrvloop() {
        return srvloop;
    }
}
