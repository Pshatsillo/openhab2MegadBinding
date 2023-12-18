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
import org.openhab.binding.megad.enums.MegaDDsenEnum;
import org.openhab.binding.megad.enums.MegaDExtendedTypeEnum;
import org.openhab.binding.megad.enums.MegaDExtendersEnum;
import org.openhab.binding.megad.enums.MegaDModesEnum;
import org.openhab.binding.megad.enums.MegaDTypesEnum;
import org.openhab.binding.megad.internal.MegaDHTTPResponse;
import org.openhab.binding.megad.internal.MegaDHttpHelpers;
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
    private Logger logger = LoggerFactory.getLogger(MegaDHardware.class);
    // private final Logger logger = LoggerFactory.getLogger(MegaDHardware.class);
    private String firmware = "";
    private String actualFirmware = "";
    private String type = "";
    private String mdid = "";
    private String sip = "";
    private String sct = "";
    private String emsk = "";

    public String getMdid() {
        return mdid;
    }

    public String getEmsk() {
        return emsk;
    }

    public String getGw() {
        return gw;
    }

    public String getPr() {
        return pr;
    }

    public String getLp() {
        return lp;
    }

    public String isGsmf() {
        if (gsmf) {
            return "1";
        } else {
            return "";
        }
    }

    public String getSrvt() {
        return srvt;
    }

    public String getGsm() {
        return gsm;
    }

    public Cron getCron() {
        return cron;
    }

    public IbuttonKeys getKeys() {
        return keys;
    }

    public List<Screen> getScreenList() {
        return screenList;
    }

    public List<Elements> getElementsList() {
        return elementsList;
    }

    public List<Program> getProgramList() {
        return programList;
    }

    public List<PID> getPidList() {
        return pidList;
    }

    public Map<Integer, Port> getPortList() {
        return portList;
    }

    public Map<Integer, MegaDTypesEnum> getPortsType() {
        return portsType;
    }

    public Map<Integer, MegaDModesEnum> getPortsMode() {
        return portsMode;
    }

    public Map<Integer, MegaDDsenEnum> getdSensorType() {
        return dSensorType;
    }

    // public Map<Integer, Integer> getScl() {
    // return scl;
    // }

    public Map<Integer, MegaDExtendersEnum> getdI2cType() {
        return dI2cType;
    }

    public Map<Integer, MegaDExtendedTypeEnum> getEtyType() {
        return etyType;
    }

    public Map<Integer, Integer> getInta() {
        return inta;
    }

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
    Map<Integer, Port> portList = new HashMap<>();
    private final Map<Integer, MegaDTypesEnum> portsType = new HashMap<>();
    private final Map<Integer, MegaDModesEnum> portsMode = new HashMap<>();
    private final Map<Integer, MegaDDsenEnum> dSensorType = new HashMap<>();
    // private final Map<Integer, Integer> scl = new HashMap<>();
    private final Map<Integer, MegaDExtendersEnum> dI2cType = new HashMap<>();
    private final Map<Integer, MegaDExtendedTypeEnum> etyType = new HashMap<>();
    private final Map<Integer, Integer> inta = new HashMap<>();

    public String getSct() {
        return sct;
    }

    public MegaDHardware(String hostname, String password) {
        MegaDHttpHelpers http = new MegaDHttpHelpers();
        readConfigPage1(hostname, password, http);
        readConfigPage2(hostname, password, http);
        // readScreens(hostname, password, http);
        // readElements(hostname, password, http);
        // readCron(hostname, password, http);
        // readKeys(hostname, password, http);
        // readProgram(hostname, password, http);
        // readPID(hostname, password, http);
        getActualFirmware(http);
        getMegaPortsAndType(hostname, password, http);
        getPortsStatus(hostname, password, http);
    }

    public void getPortsStatus(String hostname, String password, MegaDHttpHelpers http) {
        MegaDHTTPResponse megaDHTTPResponse;
        for (int i = 0; i < getPortsCount(); i++) {
            megaDHTTPResponse = http.request("http://" + hostname + "/" + password + "/?pt=" + i);
            if (megaDHTTPResponse.getResponseCode() == 200) {
                Port port = new Port();
                String pty = getSelectedByHTMLName(megaDHTTPResponse.getResponseResult(), "pty");
                if (pty.isBlank()) {
                    port.pty = MegaDTypesEnum.NC;
                } else {
                    port.pty = MegaDTypesEnum.setID(Integer.parseInt(pty));
                }
                port.ecmd = getValueByHTMLName(megaDHTTPResponse.getResponseResult(), "ecmd");
                port.af = getCheckedByHTMLName(megaDHTTPResponse.getResponseResult(), "af");
                port.eth = getValueByHTMLName(megaDHTTPResponse.getResponseResult(), "eth");
                port.naf = getCheckedByHTMLName(megaDHTTPResponse.getResponseResult(), "naf");
                port.setM(getSelectedByHTMLName(megaDHTTPResponse.getResponseResult(), "m"));
                port.miscChecked = getCheckedByHTMLName(megaDHTTPResponse.getResponseResult(), "misc");
                port.setMisc(getValueByHTMLName(megaDHTTPResponse.getResponseResult(), "misc"));
                port.dCheckbox = getCheckedByHTMLName(megaDHTTPResponse.getResponseResult(), "d");
                port.mt = getCheckedByHTMLName(megaDHTTPResponse.getResponseResult(), "mt");
                port.emt = getValueByHTMLName(megaDHTTPResponse.getResponseResult(), "emt");
                port.dSelect = getSelectedByHTMLName(megaDHTTPResponse.getResponseResult(), "d");
                port.setD(getSelectedByHTMLName(megaDHTTPResponse.getResponseResult(), "d"));
                port.grp = getValueByHTMLName(megaDHTTPResponse.getResponseResult(), "grp");
                port.hst = getValueByHTMLName(megaDHTTPResponse.getResponseResult(), "hst");
                port.gr = getSelectedByHTMLName(megaDHTTPResponse.getResponseResult(), "gr");
                port.clock = getValueByHTMLName(megaDHTTPResponse.getResponseResult(), "clock");
                setInt(i, getValueByHTMLName(megaDHTTPResponse.getResponseResult(), "inta"));
                if (port.pty == MegaDTypesEnum.I2C && (port.d.equals("20") || port.d.equals("21"))) {
                    for (int j = 0; j < 16; j++) {
                        megaDHTTPResponse = http
                                .request("http://" + hostname + "/" + password + "/?pt=" + i + "&ext=" + j);
                        if (megaDHTTPResponse.getResponseCode() == 200) {
                            ExtPort extPort = new ExtPort(port.getExtenders());
                            extPort.setEty(getSelectedByHTMLName(megaDHTTPResponse.getResponseResult(), "ety"));
                            extPort.ept = getValueByHTMLName(megaDHTTPResponse.getResponseResult(), "ept");
                            extPort.eact = getValueByHTMLName(megaDHTTPResponse.getResponseResult(), "eact");
                            extPort.epf = getCheckedByHTMLName(megaDHTTPResponse.getResponseResult(), "epf");
                            extPort.emode = getSelectedByHTMLName(megaDHTTPResponse.getResponseResult(), "emode");
                            extPort.emin = getValueByHTMLName(megaDHTTPResponse.getResponseResult(), "emin");
                            extPort.emax = getValueByHTMLName(megaDHTTPResponse.getResponseResult(), "emax");
                            extPort.espd = getValueByHTMLName(megaDHTTPResponse.getResponseResult(), "espd");
                            extPort.setEpwm(getValueByHTMLName(megaDHTTPResponse.getResponseResult(), "epwm"));
                            port.extPorts.put(j, extPort);
                        }
                    }
                }
                portList.put(i, port);
            }
        }
    }

    public void getMegaPortsAndType(String hostname, String password, MegaDHttpHelpers http) {
        MegaDHTTPResponse megaDHTTPResponse;
        megaDHTTPResponse = http.request("http://" + hostname + "/" + password);
        if (megaDHTTPResponse.getResponseCode() == 200) {
            type = megaDHTTPResponse.getResponseResult().strip().trim().split(" ")[0];
            firmware = megaDHTTPResponse.getResponseResult()
                    .substring(megaDHTTPResponse.getResponseResult().indexOf("fw:") + 3,
                            megaDHTTPResponse.getResponseResult().indexOf("<br>") - 1)
                    .strip().trim();
            if (megaDHTTPResponse.getResponseResult().contains("[44,")) {
                setPortsCount(46);
            } else {
                setPortsCount(37);
            }
        }
    }

    private void getActualFirmware(MegaDHttpHelpers http) {
        MegaDHTTPResponse megaDHTTPResponse;
        megaDHTTPResponse = http.request("https://www.ab-log.ru/smart-house/ethernet/megad-2561-firmware");
        if (megaDHTTPResponse.getResponseCode() == 200) {
            actualFirmware = megaDHTTPResponse.getResponseResult().substring(
                    megaDHTTPResponse.getResponseResult().indexOf("<ul><li>") + "<ul><li>".length(),
                    megaDHTTPResponse.getResponseResult().indexOf("</font><br>"));
            actualFirmware = actualFirmware.split("ver")[1].trim().strip();
        }
    }

    public void readPID(String hostname, String password, MegaDHttpHelpers http) {
        MegaDHTTPResponse megaDHTTPResponse;
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
    }

    public void readProgram(String hostname, String password, MegaDHttpHelpers http) {
        MegaDHTTPResponse megaDHTTPResponse;
        programList.clear();
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
    }

    public void readKeys(String hostname, String password, MegaDHttpHelpers http) {
        MegaDHTTPResponse megaDHTTPResponse;
        megaDHTTPResponse = http.request("http://" + hostname + "/" + password + "/?cf=8");
        if (megaDHTTPResponse.getResponseCode() == 200) {
            for (int i = 0; i < 5; i++) {
                keys.key[i] = getValueByHTMLName(megaDHTTPResponse.getResponseResult(), "crnt" + i);
            }
        }
    }

    public void readCron(String hostname, String password, MegaDHttpHelpers http) {
        MegaDHTTPResponse megaDHTTPResponse;
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
    }

    public void readElements(String hostname, String password, MegaDHttpHelpers http) {
        MegaDHTTPResponse megaDHTTPResponse;
        elementsList.clear();
        for (int i = 0; i < 16; i++) {
            megaDHTTPResponse = http.request("http://" + hostname + "/" + password + "/?cf=6&el=" + i);
            if (megaDHTTPResponse.getResponseCode() == 200) {
                Elements elements = new Elements();
                elements.elemt = getValueByHTMLName(megaDHTTPResponse.getResponseResult(), "elemt");
                elements.elemy = getSelectedByHTMLName(megaDHTTPResponse.getResponseResult(), "elemy");
                elements.elemi = getValueByHTMLName(megaDHTTPResponse.getResponseResult().replace("&amp;", "&"),
                        "elemi");
                elements.elemp = getValueByHTMLName(megaDHTTPResponse.getResponseResult(), "elemp");
                elements.elemf = getValueByHTMLName(megaDHTTPResponse.getResponseResult(), "elemf");
                elements.elema = getSelectedByHTMLName(megaDHTTPResponse.getResponseResult(), "elema");
                elements.elemz = getValueByHTMLName(megaDHTTPResponse.getResponseResult(), "elemz");
                elements.elemc = getValueByHTMLName(megaDHTTPResponse.getResponseResult(), "elemc");
                elements.elemu = getValueByHTMLName(megaDHTTPResponse.getResponseResult(), "elemu");
                elements.elemr = getValueByHTMLName(megaDHTTPResponse.getResponseResult(), "elemr");
                elementsList.add(elements);
            }
        }
    }

    public void readScreens(String hostname, String password, MegaDHttpHelpers http) {
        MegaDHTTPResponse megaDHTTPResponse;
        for (int i = 0; i < 5; i++) {
            megaDHTTPResponse = http.request("http://" + hostname + "/" + password + "/?cf=6&sc=" + i);
            if (megaDHTTPResponse.getResponseCode() == 200) {
                Screen screen = new Screen();
                screen.scrnt = getValueByHTMLName(megaDHTTPResponse.getResponseResult(), "scrnt");
                screen.scrnc = getSelectedByHTMLName(megaDHTTPResponse.getResponseResult(), "scrnc");
                for (int j = 0; j < 16; j++) {
                    screen.e[j] = getCheckedByHTMLName(megaDHTTPResponse.getResponseResult(), "e" + j);
                }
                screenList.add(screen);
            }
        }
    }

    public void readConfigPage2(String hostname, String password, MegaDHttpHelpers http) {
        MegaDHTTPResponse megaDHTTPResponse;
        megaDHTTPResponse = http.request("http://" + hostname + "/" + password + "/?cf=2");
        if (megaDHTTPResponse.getResponseCode() == 200) {
            mdid = getValueByHTMLName(megaDHTTPResponse.getResponseResult(), "mdid");
            sl = getCheckedByHTMLName(megaDHTTPResponse.getResponseResult(), "sl");
        }
    }

    public void readConfigPage1(String hostname, String password, MegaDHttpHelpers http) {
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
    }

    public String getSip() {
        return sip;
    }

    public MegaDHardware() {
    }

    public String getType() {
        return type;
    }

    public String getFirmware() {
        return firmware;
    }

    public String isSl() {
        if (sl) {
            return "1";
        } else {
            return "";
        }
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

    // public @Nullable MegaDTypesEnum getPortsType(int portNum) {
    // Map<Integer, Port> portList = this.portList;
    // Port port = portList.get(portNum);
    // if (port != null) {
    // return port.getPty();
    // } else {
    // return null;
    // }
    // }
    //
    // public void setMode(int portNum, MegaDModesEnum megaDModesEnum) {
    // portsMode.put(portNum, megaDModesEnum);
    // }
    //
    // public @Nullable MegaDModesEnum getMode(Integer port) {
    // return portsMode.get(port);
    // }
    //
    // public void setDSensorType(int portNum, MegaDDsenEnum megaDDsenEnum) {
    // dSensorType.put(portNum, megaDDsenEnum);
    // }

    // public @Nullable MegaDDsenEnum getDSensorType(Integer port) {
    // return dSensorType.get(port);
    // }

    // public @Nullable Integer getScl(Integer port) {
    // return scl.get(port);
    // }

    // public void setScl(int portNum, int scl) {
    // this.scl.put(portNum, scl);
    // }

    // public void setDI2CType(Integer port, MegaDExtendersEnum megaDExtendersEnum) {
    // dI2cType.put(port, megaDExtendersEnum);
    // }

    // public @Nullable MegaDExtendersEnum getDI2cType(Integer port) {
    // return dI2cType.get(port);
    // }

    // public void setEtyType(Integer port, MegaDExtendedTypeEnum megaDI2CSensorsEnum) {
    // etyType.put(port, megaDI2CSensorsEnum);
    // }
    //
    // public @Nullable MegaDExtendedTypeEnum getEtyType(Integer port) {
    // return etyType.get(port);
    // }
    //
    public void setInt(Integer port, String in) {
        if (!in.isEmpty()) {
            inta.put(Integer.parseInt(in), port);
        }
    }

    public Integer getInt(Integer port) {
        int retn = -1;
        Integer num = inta.get(port);
        if (num != null) {
            retn = num;
        }
        return retn;
    }

    public String getIntAsString(Integer port) {
        var ref = new Object() {
            String num = "";
        };
        inta.forEach((k, v) -> {
            if (v.equals(port)) {
                ref.num = k.toString();
            }
        });
        return ref.num;
    }

    private String getValueByHTMLName(String request, String name) {
        String result = "";
        if (request.contains("name=" + name)) {
            result = request.substring(request.indexOf("name=" + name));
            result = result.substring(result.indexOf("name=" + name), result.indexOf(">"));
            if (result.contains("value=")) {
                result = result.substring(result.indexOf("value=") + "value=".length()).replace("''", "");
            } else {
                result = "";
            }
        }
        return result;
    }

    private String getSelectedByHTMLName(String request, String name) {
        String result = "";
        String tag = "<select name=" + name + ">";
        if (request.contains(tag)) {
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
        } else {
            return "";
        }
    }

    private boolean getCheckedByHTMLName(String request, String name) {
        boolean result = false;
        String tag = "";
        if (request.contains("name=" + name)) {
            tag = request.substring(request.indexOf("name=" + name));
            tag = tag.substring(tag.indexOf("name=" + name), tag.indexOf(">"));
            // tag = tag.substring(tag.indexOf("value=") + "value=".length(), tag.indexOf("><br>"));
            if (tag.contains("checked")) {
                result = true;
            }
        }
        return result;
    }

    public @Nullable Port getPort(int i) {
        Map<Integer, Port> portList = this.portList;
        return portList.get(i);
    }

    public class Screen {
        public String getScrnt() {
            return scrnt;
        }

        public String getScrnc() {
            return scrnc;
        }

        public boolean[] getE() {
            return e;
        }

        private String scrnt = "";
        private String scrnc = "";
        private boolean[] e = new boolean[16];
    }

    public class Elements {
        public String getElemt() {
            return elemt;
        }

        public String getElemy() {
            return elemy;
        }

        public String getElemi() {
            return elemi;
        }

        public String getElemp() {
            return elemp;
        }

        public String getElemf() {
            return elemf;
        }

        public String getElema() {
            return elema;
        }

        public String getElemz() {
            return elemz;
        }

        public String getElemc() {
            return elemc;
        }

        public String getElemr() {
            return elemr;
        }

        private String elemt = "";
        private String elemy = "";
        private String elemi = "";
        private String elemp = "";
        private String elemf = "";
        private String elema = "";
        private String elemz = "";
        private String elemc = "";
        private String elemr = "";
        private String elemu = "";

        public String getElemu() {
            return elemu;
        }
    }

    public class Cron {
        public String getStime() {
            return stime;
        }

        public String getCscl() {
            return cscl;
        }

        public String getCsda() {
            return csda;
        }

        public String[] getCrnt() {
            return crnt;
        }

        public String[] getCrna() {
            return crna;
        }

        private String stime = "";
        private String cscl = "";
        private String csda = "";
        private String[] crnt = new String[5];
        private String[] crna = new String[5];
    }

    public class IbuttonKeys {
        public String[] getKey() {
            return key;
        }

        private String[] key = new String[5];
    }

    public class Program {
        public String getPrp() {
            return prp;
        }

        public String getPrc() {
            return prc;
        }

        public String getPrv() {
            return prv;
        }

        public String getPrd() {
            return prd;
        }

        public String isPrs() {
            if (prs) {
                return "1";
            } else {
                return "";
            }
        }

        private String prp = "";
        private String prc = "";
        private String prv = "";
        private String prd = "";
        private boolean prs = false;
    }

    public class PID {
        public String getPidt() {
            return pidt;
        }

        public String getPidi() {
            return pidi;
        }

        public String getPido() {
            return pido;
        }

        public String getPidsp() {
            return pidsp;
        }

        public String getPidpf() {
            return pidpf;
        }

        public String getPidif() {
            return pidif;
        }

        public String getPiddf() {
            return piddf;
        }

        public String getPidm() {
            return pidm;
        }

        public String getPidc() {
            return pidc;
        }

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

    public class Port {
        private Map<Integer, ExtPort> extPorts = new HashMap<>();
        private MegaDTypesEnum pty = MegaDTypesEnum.NC;
        private String ecmd = "";
        private boolean af = false;
        private String eth = "";
        private boolean naf = false;
        private MegaDModesEnum m = MegaDModesEnum.NONE;
        private String mAsString = "";
        private boolean miscChecked = false;
        private String misc = "";
        private boolean dCheckbox = false;
        private String d = "";
        private MegaDExtendersEnum extenders = MegaDExtendersEnum.NC;
        private String dSelect = "";
        private boolean mt = false;
        private String emt = "";
        private String grp = "";
        private String hst = "";
        private String gr = "";
        private String clock = "";
        private MegaDDsenEnum senType = MegaDDsenEnum.NC;
        private int scl = -1;

        public Map<Integer, ExtPort> getExtPorts() {
            return extPorts;
        }

        public MegaDTypesEnum getPty() {
            return pty;
        }

        public String getEcmd() {
            return ecmd;
        }

        public String isAf() {
            if (af) {
                return "1";
            } else {
                return "";
            }
        }

        public String getEth() {
            return eth;
        }

        public String isNaf() {
            if (naf) {
                return "1";
            } else {
                return "";
            }
        }

        public MegaDModesEnum getM() {
            return m;
        }

        public void setM(String m) {
            this.mAsString = m;
            if (pty == MegaDTypesEnum.I2C) {
                switch (m) {
                    case "0" -> this.m = MegaDModesEnum.NC;
                    case "1" -> this.m = MegaDModesEnum.SDA;
                    case "2" -> this.m = MegaDModesEnum.SCL;
                }
            } else if (pty == MegaDTypesEnum.IN) {
                switch (m) {
                    case "0" -> this.m = MegaDModesEnum.P;
                    case "1" -> this.m = MegaDModesEnum.PR;
                    case "2" -> this.m = MegaDModesEnum.R;
                    case "3" -> this.m = MegaDModesEnum.C;
                }
            } else if (pty == MegaDTypesEnum.OUT) {
                switch (m) {
                    case "0" -> this.m = MegaDModesEnum.SW;
                    case "1" -> this.m = MegaDModesEnum.PWM;
                    case "2" -> this.m = MegaDModesEnum.DS2413;
                    case "3" -> this.m = MegaDModesEnum.SWLINK;
                    case "4" -> this.m = MegaDModesEnum.WS281X;
                }
            }
        }

        public String isMiscChecked() {
            if (miscChecked) {
                return "1";
            } else {
                return "";
            }
        }

        public String getMisc() {
            return misc;
        }

        public String isdCheckbox() {
            if (dCheckbox) {
                return "1";
            } else {
                return "";
            }
        }

        public String getD() {
            return d;
        }

        public MegaDExtendersEnum getExtenders() {
            return extenders;
        }

        public void setD(String d) {
            this.d = d;
            if (pty == MegaDTypesEnum.I2C) {
                switch (d) {
                    case "0" -> this.extenders = MegaDExtendersEnum.NC;
                    case "21" -> this.extenders = MegaDExtendersEnum.PCA9685;
                    case "20" -> this.extenders = MegaDExtendersEnum.MCP230XX;
                }
            } else if (pty == MegaDTypesEnum.DSEN) {
                switch (d) {
                    case "1" -> this.senType = MegaDDsenEnum.DHT11;
                    case "2" -> this.senType = MegaDDsenEnum.DHT22;
                    case "3" -> this.senType = MegaDDsenEnum.ONEWIRE;
                    case "4" -> this.senType = MegaDDsenEnum.IB;
                    case "5" -> this.senType = MegaDDsenEnum.ONEWIREBUS;
                    case "6" -> this.senType = MegaDDsenEnum.W26;
                }
            }
        }

        public String getdSelect() {
            return dSelect;
        }

        public String isMt() {
            if (mt) {
                return "1";
            } else {
                return "";
            }
        }

        public String getEmt() {
            return emt;
        }

        public String getGrp() {
            return grp;
        }

        public String getHst() {
            return hst;
        }

        public String getGr() {
            return gr;
        }

        public String getClock() {
            return clock;
        }

        public String getmAsString() {
            return mAsString;
        }

        public MegaDDsenEnum getSenType() {
            return senType;
        }

        public void setMisc(String misc) {
            this.misc = misc;
            if (getPty().equals(MegaDTypesEnum.I2C)) {
                if (!misc.isEmpty()) {
                    this.scl = Integer.parseInt(misc);
                }
            }
        }

        public int getScl() {
            return scl;
        }
    }

    public class ExtPort {
        private MegaDExtendedTypeEnum ety = MegaDExtendedTypeEnum.NA;
        private String ept = "";
        private String eact = "";
        private boolean epf = false;
        private String emode = "";
        private String emin = "";
        private String emax = "";
        private String espd = "";
        private String epwm = "";
        MegaDExtendersEnum extType;

        public ExtPort(MegaDExtendersEnum extenders) {
            extType = extenders;
        }

        public MegaDExtendedTypeEnum getEty() {
            return ety;
        }

        public String getEpt() {
            return ept;
        }

        public String getEact() {
            return eact;
        }

        public String isEpf() {
            if (epf) {
                return "1";
            } else {
                return "";
            }
        }

        public String getEmode() {
            return emode;
        }

        public String getEmin() {
            return emin;
        }

        public String getEmax() {
            return emax;
        }

        public String getEspd() {
            return espd;
        }

        public String getEpwm() {
            return epwm;
        }

        public void setEpwm(String epwm) {
            this.epwm = epwm.split(" ")[0];
        }

        public void setEty(String ety) {
            if (extType.equals(MegaDExtendersEnum.MCP230XX)) {
                switch (ety) {
                    case "0" -> this.ety = MegaDExtendedTypeEnum.IN;
                    case "1" -> this.ety = MegaDExtendedTypeEnum.OUT;
                }
            } else if (extType.equals(MegaDExtendersEnum.PCA9685)) {
                switch (ety) {
                    case "0" -> this.ety = MegaDExtendedTypeEnum.PWM;
                    case "1" -> this.ety = MegaDExtendedTypeEnum.SW;
                }
            }
        }
    }
}
