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
package org.openhab.binding.megad;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.type.ChannelTypeUID;

/**
 * The {@link MegaDBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Petr Shatsillo - Initial contribution
 */
@NonNullByDefault
public class MegaDBindingConstants {

    public static final String BINDING_ID = "megad";

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_ITOC_BRIDGE = new ThingTypeUID(BINDING_ID, "itoc");
    public static final ThingTypeUID THING_TYPE_I2C = new ThingTypeUID(BINDING_ID, "i2c");
    public static final ThingTypeUID THING_TYPE_1WIREBUS_BRIDGE = new ThingTypeUID(BINDING_ID, "1wirebus");
    public static final ThingTypeUID THING_TYPE_1WIREADDRESS = new ThingTypeUID(BINDING_ID, "1wireaddress");
    public static final ThingTypeUID THING_TYPE_MEGAPORTS = new ThingTypeUID(BINDING_ID, "standart");
    public static final ThingTypeUID THING_TYPE_MEGAPORTS_STD = new ThingTypeUID(BINDING_ID, "standard");
    public static final ThingTypeUID THING_TYPE_DEVICE_BRIDGE = new ThingTypeUID(BINDING_ID, "device");
    public static final ThingTypeUID THING_TYPE_INCOMING_BRIDGE = new ThingTypeUID(BINDING_ID, "tcp");
    public static final ThingTypeUID THING_TYPE_I2CBUSSENSOR = new ThingTypeUID(BINDING_ID, "i2cbussensor");
    public static final ThingTypeUID THING_TYPE_RS485 = new ThingTypeUID(BINDING_ID, "rs485");
    public static final ThingTypeUID THING_TYPE_LCD1609 = new ThingTypeUID(BINDING_ID, "lcd1609");
    public static final ThingTypeUID THING_TYPE_ENCODER = new ThingTypeUID(BINDING_ID, "encoder");
    public static final ThingTypeUID THING_TYPE_GROUP = new ThingTypeUID(BINDING_ID, "group");
    // Extender MCP230XX
    public static final ThingTypeUID THING_TYPE_EXTENDER_BRIDGE = new ThingTypeUID(BINDING_ID, "extenderport");
    public static final ThingTypeUID THING_TYPE_EXTENDER = new ThingTypeUID(BINDING_ID, "extender");
    // Extender PCA9685
    public static final ThingTypeUID THING_TYPE_EXTENDER_PCA9685_BRIDGE = new ThingTypeUID(BINDING_ID,
            "extenderPCA9685Bridge");
    public static final ThingTypeUID THING_TYPE_EXTENDER_PCA9685 = new ThingTypeUID(BINDING_ID, "extenderPCA9685");

    // List of all Channel ids
    public static final String CHANNEL_IN = "in";
    public static final String CHANNEL_INCOUNT = "incount";
    public static final String CHANNEL_OUT = "out";
    public static final String CHANNEL_DS2413 = "ds2413";
    public static final String CHANNEL_DIMMER = "dimmer";
    public static final String CHANNEL_PWM = "pwm";
    public static final String CHANNEL_M2 = "m2signal";
    public static final String CHANNEL_CLICK = "click";
    public static final String CHANNEL_1WTEMP = "1wtemp";
    public static final String CHANNEL_ONEWIRE = "onewire";
    public static final String CHANNEL_ADC = "adc";
    public static final String CHANNEL_AT = "at";
    public static final String CHANNEL_ST = "st";
    public static final String CHANNEL_IB = "ib";
    public static final String CHANNEL_WIEGAND = "wiegand";
    public static final String CHANNEL_TGET = "tget";
    public static final String CHANNEL_CONTACT = "contact";
    public static final String CHANNEL_SMS_PHONE = "smsphone";
    public static final String CHANNEL_SMS_TEXT = "smstext";
    public static final String CHANNEL_SMOOTH = "smoothtime";

    // i2c
    public static final String CHANNEL_I2C_TEMP = "temp";
    public static final String CHANNEL_I2C_HUM = "humidity";
    public static final String CHANNEL_I2C_PRESSURE = "pressure";
    public static final String CHANNEL_I2C_GAS = "gas";
    public static final String CHANNEL_I2C_OTHER = "other";

    // extender
    public static final String CHANNEL_EXTENDER_IN = "extin";
    public static final String CHANNEL_EXTENDER_OUT = "extout";

    // megad2w
    public static final String CHANNEL_MEGAD2W_A = "a";
    public static final String CHANNEL_MEGAD2W_B = "b";

    public static final String CHANNEL_PAR0 = "par0";
    public static final String CHANNEL_PAR1 = "par1";
    public static final String CHANNEL_PAR2 = "par2";
    public static final String CHANNEL_I2CRAW = "i2craw";

    // rs485 sdm120
    public static final String CHANNEL_VOLTAGE = "voltage";
    public static final String CHANNEL_CURRENT = "current";
    public static final String CHANNEL_ACTIVEPOWER = "activepower";
    public static final String CHANNEL_ACTIVEENERGY = "actnrg";
    public static final String CHANNEL_APPARENTPOWER = "apparentpower";
    public static final String CHANNEL_REACTIVEPOWER = "reactivepower";
    public static final String CHANNEL_REACTIVEENERGY = "reactnrg";
    public static final String CHANNEL_POWERFACTOR = "powerfactor";
    public static final String CHANNEL_PHASEANGLE = "phaseangle";
    public static final String CHANNEL_FREQUENCY = "frequency";
    public static final String CHANNEL_IMPORTACTNRG = "importactnrg";
    public static final String CHANNEL_EXPORTACTNRG = "exportactnrg";
    public static final String CHANNEL_IMPORTREACTNRG = "importreactnrg";
    public static final String CHANNEL_EXPORTREACTNRG = "exportreactnrg";
    public static final String CHANNEL_TOTALSYSPWRDMD = "totalsyspwrdmd";
    public static final String CHANNEL_MAXTOTALSYSPWRDMD = "maxtotalsyspwrdmd";
    public static final String CHANNEL_IMPORTSYSPWRDMD = "importsyspwrdmd";
    public static final String CHANNEL_MAXIMPORTSYSPWRDMD = "maximportsyspwrdmd";
    public static final String CHANNEL_EXPORTSYSPWRDMD = "exportsyspwrdmd";
    public static final String CHANNEL_MAXEXPORTSYSPWRDMD = "maxexportsyspwrdmd";
    public static final String CHANNEL_CURRENTDMD = "currentdmd";
    public static final String CHANNEL_MAXCURRENTDMD = "maxcurrentdmd";
    public static final String CHANNEL_TOTALACTNRG = "totalactnrg";
    public static final String CHANNEL_TOTALREACTNRG = "totalreactnrg";
    // lcd 1609
    public static final String CHANNEL_LINE1 = "line1";
    public static final String CHANNEL_LINE2 = "line2";
    // encoder
    public static final String CHANNEL_DIRECTION = "direction";
    public static final String CHANNEL_BUTTON = "button";
    public static final String CHANNEL_ENCODERDIGITS = "encoderdigits";
    // group
    public static final String CHANNEL_GROUP = "groupswitch";
    // Dynamic channels
    public static final ChannelTypeUID CHANNEL_DYNAMIC_I2C = new ChannelTypeUID(BINDING_ID, "i2cpar");
    public static final String CHANNEL_MIDEAOPERMODE = "opermode";
    public static final String CHANNEL_MIDEAFANMODE = "fanmode";
    public static final String CHANNEL_MIDEATEMP = "mideatemperature";
}
