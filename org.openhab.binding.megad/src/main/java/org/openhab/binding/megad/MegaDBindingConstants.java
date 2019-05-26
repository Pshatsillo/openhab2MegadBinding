/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * The {@link MegaDBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Petr Shatsillo - Initial contribution
 */
@NonNullByDefault
public class MegaDBindingConstants {

    public static final String BINDING_ID = "megad";

    public static final String BRIDGE_MEGAD = "bridge";

    // List of all Thing Type UIDs
    public final static ThingTypeUID THING_TYPE_DEVICE = new ThingTypeUID(BINDING_ID, "device");
    public final static ThingTypeUID THING_TYPE_UID_BRIDGE = new ThingTypeUID(BINDING_ID, BRIDGE_MEGAD);

    // List of all Channel ids
    public final static String CHANNEL_IN = "in";
    public final static String CHANNEL_RAWIN = "rawin";
    public final static String CHANNEL_INCOUNT = "incount";
    public final static String CHANNEL_OUT = "out";
    public final static String CHANNEL_DIMMER = "dimmer";
    public final static String CHANNEL_DHTTEMP = "temp";
    public final static String CHANNEL_DHTHUM = "humidity";
    public final static String CHANNEL_ONEWIRE = "onewire";
    public final static String CHANNEL_ADC = "adc";
    public final static String CHANNEL_AT = "at";
    public final static String CHANNEL_ST = "st";
    public final static String CHANNEL_IB = "ib";
    public final static String CHANNEL_WIEGAND = "wiegand";
    public final static String CHANNEL_TGET = "tget";
    public final static String CHANNEL_I2C = "i2c";
    public final static String CHANNEL_I2C_DISPLAY = "i2cdisplay";
    public final static String CHANNEL_CONTACT = "contact";
    public final static String CHANNEL_SMS_PHONE = "smsphone";
    public final static String CHANNEL_SMS_TEXT = "smstext";
}
