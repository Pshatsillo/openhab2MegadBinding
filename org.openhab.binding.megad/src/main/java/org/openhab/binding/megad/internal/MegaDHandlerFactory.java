/**
 * Copyright (c) 2014 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.megad.internal;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.openhab.binding.megad.MegaDBindingConstants;
import org.openhab.binding.megad.handler.MegaDBridgeHandler;
import org.openhab.binding.megad.handler.MegaDHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link MegadHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Petr Shatsillo - Initial contribution
 */
public class MegaDHandlerFactory extends BaseThingHandlerFactory {

    private Logger logger = LoggerFactory.getLogger(MegaDHandlerFactory.class);

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return MegaDBindingConstants.SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected ThingHandler createHandler(Thing thing) {

        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (thingTypeUID.equals(MegaDBindingConstants.THING_TYPE_UID_BRIDGE)) {
            MegaDBridgeHandler handler = new MegaDBridgeHandler((Bridge) thing);
            return handler;
        }

        if (supportsThingType(thingTypeUID)) {
            return new MegaDHandler(thing);
        }

        return null;
    }

    @Override
    public Thing createThing(ThingTypeUID thingTypeUID, Configuration configuration, ThingUID thingUID,
            ThingUID bridgeUID) {
        logger.trace("Create Thing for Type {}", thingUID.toString());
        if (MegaDBindingConstants.THING_TYPE_UID_BRIDGE.equals(thingTypeUID)) {

            logger.trace("Create Bride: {}", thingTypeUID);
            return super.createThing(thingTypeUID, configuration, thingUID, null);
        } else {
            if (supportsThingType(thingTypeUID)) {
                logger.trace("Create Thing: {}", thingTypeUID);
                return super.createThing(thingTypeUID, configuration, thingUID, bridgeUID);
            }
        }

        throw new IllegalArgumentException("The thing type " + thingTypeUID + " is not supported by the binding.");
    }
}
