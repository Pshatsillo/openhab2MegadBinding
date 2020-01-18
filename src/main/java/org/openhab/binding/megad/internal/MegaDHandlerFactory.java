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
package org.openhab.binding.megad.internal;

import static org.openhab.binding.megad.MegaDBindingConstants.*;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerFactory;
import org.openhab.binding.megad.handler.MegaDBridgeDeviceHandler;
import org.openhab.binding.megad.handler.MegaDBridgeIncomingHandler;
import org.openhab.binding.megad.handler.MegaDMegaItoCHandler;
import org.openhab.binding.megad.handler.MegaDMegaPortsHandler;
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
        SUPPORTED_THING_TYPES_UIDS.add(THING_TYPE_I2C);
        SUPPORTED_THING_TYPES_UIDS.add(THING_TYPE_MEGAPORTS);
        SUPPORTED_THING_TYPES_UIDS.add(THING_TYPE_DEVICE_BRIDGE);
        SUPPORTED_THING_TYPES_UIDS.add(THING_TYPE_INCOMING_BRIDGE);
    }

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();
        if (thingTypeUID.equals(THING_TYPE_INCOMING_BRIDGE)) {
            // logger.debug("createHandler Incoming connections");
            return new MegaDBridgeIncomingHandler((Bridge) thing);
        } else if (thingTypeUID.equals(THING_TYPE_DEVICE_BRIDGE)) {
            // logger.debug("createHandler Mega Device hardware");
            return new MegaDBridgeDeviceHandler((Bridge) thing);
        } else if (thingTypeUID.equals(THING_TYPE_MEGAPORTS)) {
            // logger.debug("createHandler Port items");
            return new MegaDMegaPortsHandler(thing);
        } else if (thingTypeUID.equals(THING_TYPE_I2C)) {
            // logger.debug("createHandler Port items");
            return new MegaDMegaItoCHandler(thing);
        }
        logger.error("createHandler for unknown thing type uid {}. Thing label was: {}", thing.getThingTypeUID(),
                thing.getLabel());
        return null;
    }
}
