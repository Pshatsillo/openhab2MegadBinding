/**
 * Copyright (c) 2014 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.megadonetry.internal;

import java.util.Hashtable;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.config.discovery.DiscoveryService;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.openhab.binding.megadonetry.MegadOneTryBindingConstants;
import org.openhab.binding.megadonetry.handler.MegadOneTryBridgeHandler;
import org.openhab.binding.megadonetry.handler.MegadOneTryHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link MegadOneTryHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Petr Shatsillo - Initial contribution
 */
public class MegadOneTryHandlerFactory extends BaseThingHandlerFactory {

    private Logger logger = LoggerFactory.getLogger(MegadOneTryHandlerFactory.class);

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return MegadOneTryBindingConstants.SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected ThingHandler createHandler(Thing thing) {

        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (thingTypeUID.equals(MegadOneTryBindingConstants.THING_TYPE_UID_BRIDGE)) {
            MegadOneTryBridgeHandler handler = new MegadOneTryBridgeHandler((Bridge) thing);
            registerThingDiscovery(handler);
            return handler;
        }

        if (supportsThingType(thingTypeUID)) {
            return new MegadOneTryHandler(thing);
        }

        return null;
    }

    private synchronized void registerThingDiscovery(MegadOneTryBridgeHandler bridgeHandler) {
        // VitotronicDiscoveryService discoveryService = new VitotronicDiscoveryService(bridgeHandler);
        logger.trace("Try to register Discovery service on BundleID: {} Service: {}",
                bundleContext.getBundle().getBundleId(), DiscoveryService.class.getName());

        Hashtable<String, String> prop = new Hashtable<String, String>();

        // bundleContext.registerService(DiscoveryService.class.getName(), discoveryService, prop);
        // discoveryService.activate();
    }

    @Override
    public Thing createThing(ThingTypeUID thingTypeUID, Configuration configuration, ThingUID thingUID,
            ThingUID bridgeUID) {
        logger.trace("Create Thing for Type {}", thingUID.toString());
        if (MegadOneTryBindingConstants.THING_TYPE_UID_BRIDGE.equals(thingTypeUID)) {

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
