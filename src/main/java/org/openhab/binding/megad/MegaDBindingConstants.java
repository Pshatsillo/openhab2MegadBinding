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

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_MEGAPORTS = new ThingTypeUID(BINDING_ID, "megaports");
    public static final ThingTypeUID THING_TYPE_DEVICE_BRIDGE = new ThingTypeUID(BINDING_ID, "device");
    public static final ThingTypeUID THING_TYPE_INCOMING_BRIDGE = new ThingTypeUID(BINDING_ID, "tcp");

    // List of all Channel ids
    public static final String CHANNEL_IN = "in";
    public static final String CHANNEL_RAWIN = "rawin";
    public static final String CHANNEL_INCOUNT = "incount";
    public static final String CHANNEL_OUT = "out";
    public static final String CHANNEL_DIMMER = "dimmer";
    public static final String CHANNEL_DHTTEMP = "temp";
    public static final String CHANNEL_DHTHUM = "humidity";
    public static final String CHANNEL_ONEWIRE = "onewire";
    public static final String CHANNEL_ADC = "adc";
    public static final String CHANNEL_AT = "at";
    public static final String CHANNEL_ST = "st";
    public static final String CHANNEL_IB = "ib";
    public static final String CHANNEL_WIEGAND = "wiegand";
    public static final String CHANNEL_TGET = "tget";
    public static final String CHANNEL_I2C = "i2c";
    public static final String CHANNEL_I2C_DISPLAY = "i2cdisplay";
    public static final String CHANNEL_CONTACT = "contact";
    public static final String CHANNEL_SMS_PHONE = "smsphone";
    public static final String CHANNEL_SMS_TEXT = "smstext";
}
