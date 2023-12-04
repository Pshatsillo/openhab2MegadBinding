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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.megad.internal.MegaDDsenEnum;
import org.openhab.binding.megad.internal.MegaDExtendedType;
import org.openhab.binding.megad.internal.MegaDHTTPResponse;
import org.openhab.binding.megad.internal.MegaDHttpHelpers;
import org.openhab.binding.megad.internal.MegaDI2CSensorsEnum;
import org.openhab.binding.megad.internal.MegaDModesEnum;
import org.openhab.binding.megad.internal.MegaDTypesEnum;

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
    private String sip = "";
    private String sct = "";
    private String emsk = "";
    private String gw = "";
    private String pr = "";
    private String lp = "";
    private boolean gsmf = false;
    private String srvt = "";
    private String gsm = "";
    Cron cron = new Cron();
    IbuttonKeys keys = new IbuttonKeys();
    private boolean sl = false;
    private int portsCount;
    List<Screen> screenList = new ArrayList<>();
    List<Elements> elementsList = new ArrayList<>();
    List<Program> programList = new ArrayList<>();
    List<PID> pidList = new ArrayList<>();
    private final Map<Integer, MegaDTypesEnum> portsType = new HashMap<>();
    private final Map<Integer, MegaDModesEnum> portsMode = new HashMap<>();
    private final Map<Integer, MegaDDsenEnum> dSensorType = new HashMap<>();
    private final Map<Integer, Integer> scl = new HashMap<>();
    private final Map<Integer, MegaDI2CSensorsEnum> dI2cType = new HashMap<>();
    private final Map<Integer, MegaDExtendedType> etyType = new HashMap<>();
    private final Map<Integer, Integer> inta = new HashMap<>();

    public String getSct() {
        return sct;
    }

    public MegaDHardware(String hostname, String password) {
        MegaDHttpHelpers http = new MegaDHttpHelpers();
        MegaDHTTPResponse megaDHTTPResponse = http.request("http://" + hostname + "/" + password + "/?cf=1");
        if (megaDHTTPResponse.getResponseCode() == 200) {
            sip = getValueByHTMLName(megaDHTTPResponse.getResponseResult(), "sip");
            emsk = getValueByHTMLName(megaDHTTPResponse.getResponseResult(), "emsk");
            gw = getValueByHTMLName(megaDHTTPResponse.getResponseResult(), "gw");
            sct = getValueByHTMLName(megaDHTTPResponse.getResponseResult(), "sct");
            pr = getValueByHTMLName(megaDHTTPResponse.getResponseResult(), "pr");
            lp = getValueByHTMLName(megaDHTTPResponse.getResponseResult(), "lp");
            gsmf = getCheckedByHTMLName(megaDHTTPResponse.getResponseResult(), "gsmf");
            srvt = getSelectedByHTMLName(megaDHTTPResponse.getResponseResult(), "srvt");
            gsm = getSelectedByHTMLName(megaDHTTPResponse.getResponseResult(), "gsm");
        }
        megaDHTTPResponse = http.request("http://" + hostname + "/" + password + "/?cf=2");
        if (megaDHTTPResponse.getResponseCode() == 200) {
            mdid = getValueByHTMLName(megaDHTTPResponse.getResponseResult(), "mdid");
            sl = getCheckedByHTMLName(megaDHTTPResponse.getResponseResult(), "sl");
        }
        for (int i = 0; i < 5; i++) {
            megaDHTTPResponse = http.request("http://" + hostname + "/" + password + "/?cf=6&sc=" + i);
            if (megaDHTTPResponse.getResponseCode() == 200) {
                Screen screen = new Screen();
                screen.scrnt = getValueByHTMLName(megaDHTTPResponse.getResponseResult(), "scrnt");
                screen.scrnc = getSelectedByHTMLName(megaDHTTPResponse.getResponseResult(), "scrnc");
                for (int j = 0; j < 15; j++) {
                    screen.e[j] = getCheckedByHTMLName(megaDHTTPResponse.getResponseResult(), "e" + j);
                }
                screenList.add(screen);
            }
        }
        for (int i = 0; i < 16; i++) {
            megaDHTTPResponse = http.request("http://" + hostname + "/" + password + "/?cf=6&el=" + i);
            if (megaDHTTPResponse.getResponseCode() == 200) {
                Elements elements = new Elements();
                elements.elemt = getValueByHTMLName(megaDHTTPResponse.getResponseResult(), "elemt");
                elements.elemy = getSelectedByHTMLName(megaDHTTPResponse.getResponseResult(), "elemy");
                elements.elemi = getValueByHTMLName(megaDHTTPResponse.getResponseResult(), "elemi");
                elements.elemp = getValueByHTMLName(megaDHTTPResponse.getResponseResult(), "elemp");
                elements.elemf = getValueByHTMLName(megaDHTTPResponse.getResponseResult(), "elemf");
                elements.elema = getSelectedByHTMLName(megaDHTTPResponse.getResponseResult(), "elema");
                elements.elemz = getValueByHTMLName(megaDHTTPResponse.getResponseResult(), "elemz");
                elements.elemc = getValueByHTMLName(megaDHTTPResponse.getResponseResult(), "elemc");
                elements.elemr = getValueByHTMLName(megaDHTTPResponse.getResponseResult(), "elemr");
                elementsList.add(elements);
            }
        }
        megaDHTTPResponse = http.request("http://" + hostname + "/" + password + "/?cf=7");
        if (megaDHTTPResponse.getResponseCode() == 200) {
            cron.stime = getValueByHTMLName(megaDHTTPResponse.getResponseResult(), "stime");
            cron.cscl = getValueByHTMLName(megaDHTTPResponse.getResponseResult(), "cscl");
            cron.csda = getValueByHTMLName(megaDHTTPResponse.getResponseResult(), "csda");
            for (int i = 0; i < 5; i++) {
                cron.crnt[i] = getValueByHTMLName(megaDHTTPResponse.getResponseResult(), "crnt" + i);
                cron.crna[i] = getValueByHTMLName(megaDHTTPResponse.getResponseResult(), "crna" + i);
            }

        }
        megaDHTTPResponse = http.request("http://" + hostname + "/" + password + "/?cf=8");
        if (megaDHTTPResponse.getResponseCode() == 200) {
            for (int i = 0; i < 5; i++) {
                keys.key[i] = getValueByHTMLName(megaDHTTPResponse.getResponseResult(), "crnt" + i);
            }
        }
        megaDHTTPResponse = http.request("http://" + hostname + "/" + password + "/?cf=10");
        if (megaDHTTPResponse.getResponseCode() == 200) {
            for (int i = 0; i < 10; i++) {
                Program program = new Program();
                program.prp = getValueByHTMLName(megaDHTTPResponse.getResponseResult(), "prp");
                program.prc = getSelectedByHTMLName(megaDHTTPResponse.getResponseResult(), "prc");
                program.prv = getValueByHTMLName(megaDHTTPResponse.getResponseResult(), "prv");
                program.prd = getValueByHTMLName(megaDHTTPResponse.getResponseResult(), "prd");
                program.prs = getCheckedByHTMLName(megaDHTTPResponse.getResponseResult(), "prs");
                programList.add(program);
            }
        }
        for (int i = 0; i < 5; i++) {
            megaDHTTPResponse = http.request("http://" + hostname + "/" + password + "/?cf=11&pid=" + i);
            if (megaDHTTPResponse.getResponseCode() == 200) {
                PID pid = new PID();
                pid.pidt = getValueByHTMLName(megaDHTTPResponse.getResponseResult(), "pidt");
                pid.pidi = getValueByHTMLName(megaDHTTPResponse.getResponseResult(), "pidi");
                pid.pido = getValueByHTMLName(megaDHTTPResponse.getResponseResult(), "pido");
                pid.pidsp = getValueByHTMLName(megaDHTTPResponse.getResponseResult(), "pidsp");
                pid.pidpf = getValueByHTMLName(megaDHTTPResponse.getResponseResult(), "pidpf");
                pid.pidif = getValueByHTMLName(megaDHTTPResponse.getResponseResult(), "pidif");
                pid.piddf = getValueByHTMLName(megaDHTTPResponse.getResponseResult(), "piddf");
                pid.pidm = getSelectedByHTMLName(megaDHTTPResponse.getResponseResult(), "pidm");
                pid.pidc = getValueByHTMLName(megaDHTTPResponse.getResponseResult(), "pidc");
                pidList.add(pid);
            }
        }
        megaDHTTPResponse = http.request("https://www.ab-log.ru/smart-house/ethernet/megad-2561-firmware");
        if (megaDHTTPResponse.getResponseCode() == 200) {
            actualFirmware = megaDHTTPResponse.getResponseResult().substring(
                    megaDHTTPResponse.getResponseResult().indexOf("<ul><li>") + "<ul><li>".length(),
                    megaDHTTPResponse.getResponseResult().indexOf("</font><br>"));
            actualFirmware = actualFirmware.split("ver")[1].trim().strip();
        }
        megaDHTTPResponse = http.request("http://" + hostname + "/" + password);
        if (megaDHTTPResponse.getResponseCode() == 200) {
            type = megaDHTTPResponse.getResponseResult().strip().trim().split(" ")[0];
            firmware = megaDHTTPResponse.getResponseResult()
                    .substring(megaDHTTPResponse.getResponseResult().indexOf("fw:") + 3,
                            megaDHTTPResponse.getResponseResult().indexOf("<br>") - 1)
                    .strip().trim();
            if (megaDHTTPResponse.getResponseResult().contains("[44,")) {
                setPortsCount(45);
            } else {
                setPortsCount(37);
            }
            for (int i = 0; i <= getPortsCount(); i++) {
                megaDHTTPResponse = http.request("http://" + hostname + "/" + password + "/?pt=" + i);
                if (megaDHTTPResponse.getResponseCode() == 200) {

                }
            }
        }
    }

    public String getSip() {
        return sip;
    }

    public MegaDHardware() {
    }

    // public void parse(String result) {
    // type = result.strip().trim().split(" ")[0];
    // firmware = result.substring(result.indexOf("fw:") + 3, result.indexOf("<br>") - 1).strip().trim();
    // }

    public String getType() {
        return type;
    }

    public String getFirmware() {
        return firmware;
    }

    // public String getMdid() {
    // return mdid;
    // }
    public boolean isSl() {
        return sl;
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

    public @Nullable MegaDTypesEnum getPortsType(int port) {
        return portsType.get(port);
    }

    public void setMode(int portNum, MegaDModesEnum megaDModesEnum) {
        portsMode.put(portNum, megaDModesEnum);
    }

    public @Nullable MegaDModesEnum getMode(Integer port) {
        return portsMode.get(port);
    }

    public void setDSensorType(int portNum, MegaDDsenEnum megaDDsenEnum) {
        dSensorType.put(portNum, megaDDsenEnum);
    }

    public @Nullable MegaDDsenEnum getDSensorType(Integer port) {
        return dSensorType.get(port);
    }

    public @Nullable Integer getScl(Integer port) {
        return scl.get(port);
    }

    public void setScl(int portNum, int scl) {
        this.scl.put(portNum, scl);
    }

    public void setDI2CType(Integer port, MegaDI2CSensorsEnum megaDI2CSensorsEnum) {
        dI2cType.put(port, megaDI2CSensorsEnum);
    }

    public @Nullable MegaDI2CSensorsEnum getDI2cType(Integer port) {
        return dI2cType.get(port);
    }

    public void setEtyType(Integer port, MegaDExtendedType megaDI2CSensorsEnum) {
        etyType.put(port, megaDI2CSensorsEnum);
    }

    public @Nullable MegaDExtendedType getEtyType(Integer port) {
        return etyType.get(port);
    }

    public void setInt(Integer port, int in) {
        inta.put(in, port);
    }

    public Integer getInt(Integer port) {
        int retn = -1;
        Integer num = inta.get(port);
        if (num != null) {
            retn = num;
        }
        return retn;
    }

    private String getValueByHTMLName(String request, String name) {
        String result = "";
        if (request.contains("name=" + name)) {
            result = request.substring(request.indexOf("name=" + name));
            result = result.substring(result.indexOf("name=" + name), result.indexOf(">"));
            if (result.contains("value=")) {
                result = result.substring(result.indexOf("value=") + "value=".length());
            } else {
                result = "";
            }
        }
        return result;
    }

    private String getSelectedByHTMLName(String request, String name) {
        String result = "";
        String tag = "<select name=" + name + ">";
        int dStartIndex = request.indexOf(tag) + tag.length();
        int dEndIndex = request.substring(dStartIndex).indexOf("</select>") + dStartIndex;
        String selectTag = request.substring(dStartIndex, dEndIndex);
        String[] optionList = selectTag.split("<option");
        for (String selectOption : optionList) {
            if (selectOption.contains("selected")) {
                result = selectOption
                        .substring(selectOption.indexOf("value=") + "value=".length(), selectOption.indexOf("s"))
                        .trim();
            }
        }
        return result;
    }

    private boolean getCheckedByHTMLName(String request, String name) {
        boolean result = false;
        String tag = "";
        tag = request.substring(request.indexOf("name=" + name));
        tag = tag.substring(tag.indexOf("name=" + name), tag.indexOf(">"));
        // tag = tag.substring(tag.indexOf("value=") + "value=".length(), tag.indexOf("><br>"));
        if (tag.contains("checked")) {
            result = true;
        }
        return result;
    }

    private class Screen {
        private String scrnt = "";
        private String scrnc = "";
        private boolean[] e = new boolean[15];
    }

    private class Elements {
        private String elemt = "";
        private String elemy = "";
        private String elemi = "";
        private String elemp = "";
        private String elemf = "";
        private String elema = "";
        private String elemz = "";
        private String elemc = "";
        private String elemr = "";
    }

    private class Cron {
        private String stime = "";
        private String cscl = "";
        private String csda = "";
        private String[] crnt = new String[5];
        private String[] crna = new String[5];
    }

    private class IbuttonKeys {
        private String[] key = new String[5];
    }

    private class Program {
        private String prp = "";
        private String prc = "";
        private String prv = "";
        private String prd = "";
        private boolean prs = false;
    }

    private class PID {
        private String pidt = "";
        private String pidi = "";
        private String pido = "";
        private String pidsp = "";
        private String pidpf = "";
        private String pidif = "";
        private String piddf = "";
        private String pidm = "";
        private String pidc = "";
    }

    private class Port{

    }
}
