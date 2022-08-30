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
package org.openhab.binding.megad.internal;

import static org.openhab.binding.megad.MegaDBindingConstants.*;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.megad.handler.MegaD1WireSensorHandler;
import org.openhab.binding.megad.handler.MegaDBridge1WireBusHandler;
import org.openhab.binding.megad.handler.MegaDBridgeDeviceHandler;
import org.openhab.binding.megad.handler.MegaDBridgeExtenderPCA9685Handler;
import org.openhab.binding.megad.handler.MegaDBridgeExtenderPortHandler;
import org.openhab.binding.megad.handler.MegaDBridgeIToCHandler;
import org.openhab.binding.megad.handler.MegaDBridgeIncomingHandler;
import org.openhab.binding.megad.handler.MegaDEncoderHandler;
import org.openhab.binding.megad.handler.MegaDExtenderHandler;
import org.openhab.binding.megad.handler.MegaDExtenderPCA9685Handler;
import org.openhab.binding.megad.handler.MegaDGroupHandler;
import org.openhab.binding.megad.handler.MegaDItoCHandler;
import org.openhab.binding.megad.handler.MegaDItoCSensorHandler;
import org.openhab.binding.megad.handler.MegaDLcd1609Handler;
import org.openhab.binding.megad.handler.MegaDPortsHandler;
import org.openhab.binding.megad.handler.MegaDRs485Handler;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.binding.BaseThingHandlerFactory;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link MegaDHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Petr Shatsillo - Initial contribution
 */

@Component(configurationPid = "binding.megad", service = ThingHandlerFactory.class)
@NonNullByDefault
public class MegaDHandlerFactory extends BaseThingHandlerFactory {

    private Logger logger = LoggerFactory.getLogger(MegaDHandlerFactory.class);

    private static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = new HashSet<>();
    static {
        SUPPORTED_THING_TYPES_UIDS.add(THING_TYPE_EXTENDER);
        SUPPORTED_THING_TYPES_UIDS.add(THING_TYPE_EXTENDER_BRIDGE);
        SUPPORTED_THING_TYPES_UIDS.add(THING_TYPE_EXTENDER_PCA9685);
        SUPPORTED_THING_TYPES_UIDS.add(THING_TYPE_EXTENDER_PCA9685_BRIDGE);
        SUPPORTED_THING_TYPES_UIDS.add(THING_TYPE_I2C);
        SUPPORTED_THING_TYPES_UIDS.add(THING_TYPE_1WIREBUS_BRIDGE);
        SUPPORTED_THING_TYPES_UIDS.add(THING_TYPE_1WIREADDRESS);
        SUPPORTED_THING_TYPES_UIDS.add(THING_TYPE_MEGAPORTS);
        SUPPORTED_THING_TYPES_UIDS.add(THING_TYPE_DEVICE_BRIDGE);
        SUPPORTED_THING_TYPES_UIDS.add(THING_TYPE_INCOMING_BRIDGE);
        SUPPORTED_THING_TYPES_UIDS.add(THING_TYPE_ITOC_BRIDGE);
        SUPPORTED_THING_TYPES_UIDS.add(THING_TYPE_I2CBUSSENSOR);
        SUPPORTED_THING_TYPES_UIDS.add(THING_TYPE_MEGAPORTS_STD);
        SUPPORTED_THING_TYPES_UIDS.add(THING_TYPE_RS485);
        SUPPORTED_THING_TYPES_UIDS.add(THING_TYPE_LCD1609);
        SUPPORTED_THING_TYPES_UIDS.add(THING_TYPE_ENCODER);
        SUPPORTED_THING_TYPES_UIDS.add(THING_TYPE_GROUP);
    }

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();
        if (thingTypeUID.equals(THING_TYPE_INCOMING_BRIDGE)) {
            return new MegaDBridgeIncomingHandler((Bridge) thing);
        } else if (thingTypeUID.equals(THING_TYPE_DEVICE_BRIDGE)) {
            return new MegaDBridgeDeviceHandler((Bridge) thing);
        } else if (thingTypeUID.equals(THING_TYPE_MEGAPORTS)) {
            return new MegaDPortsHandler(thing);
        } else if (thingTypeUID.equals(THING_TYPE_MEGAPORTS_STD)) {
            return new MegaDPortsHandler(thing);
        } else if (thingTypeUID.equals(THING_TYPE_I2C)) {
            return new MegaDItoCHandler(thing);
        } else if (thingTypeUID.equals(THING_TYPE_1WIREBUS_BRIDGE)) {
            return new MegaDBridge1WireBusHandler((Bridge) thing);
        } else if (thingTypeUID.equals(THING_TYPE_1WIREADDRESS)) {
            return new MegaD1WireSensorHandler(thing);
        } else if (thingTypeUID.equals(THING_TYPE_EXTENDER_BRIDGE)) {
            return new MegaDBridgeExtenderPortHandler((Bridge) thing);
        } else if (thingTypeUID.equals(THING_TYPE_EXTENDER)) {
            return new MegaDExtenderHandler(thing);
        } else if (thingTypeUID.equals(THING_TYPE_EXTENDER_PCA9685_BRIDGE)) {
            return new MegaDBridgeExtenderPCA9685Handler((Bridge) thing);
        } else if (thingTypeUID.equals(THING_TYPE_EXTENDER_PCA9685)) {
            return new MegaDExtenderPCA9685Handler(thing);
        } else if (thingTypeUID.equals(THING_TYPE_ITOC_BRIDGE)) {
            return new MegaDBridgeIToCHandler((Bridge) thing);
        } else if (thingTypeUID.equals(THING_TYPE_I2CBUSSENSOR)) {
            return new MegaDItoCSensorHandler(thing);
        } else if (thingTypeUID.equals(THING_TYPE_RS485)) {
            return new MegaDRs485Handler(thing);
        } else if (thingTypeUID.equals(THING_TYPE_LCD1609)) {
            return new MegaDLcd1609Handler(thing);
        } else if (thingTypeUID.equals(THING_TYPE_ENCODER)) {
            return new MegaDEncoderHandler(thing);
        } else if (thingTypeUID.equals(THING_TYPE_GROUP)) {
            return new MegaDGroupHandler(thing);
        }
        logger.error("createHandler for unknown thing type uid {}. Thing label was: {}", thing.getThingTypeUID(),
                thing.getLabel());
        return null;
    }
}
