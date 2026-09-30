/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.megad.handler.MegaDPortsHandler;
import org.openhab.core.events.Event;
import org.openhab.core.events.EventSubscriber;
import org.openhab.core.items.events.ItemCommandEvent;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.link.ItemChannelLinkRegistry;
import org.openhab.core.types.Command;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link MegaDEventSubscriber} used for fired item in channel detection
 *
 * @author Petr Shatsillo - Initial contribution
 */
@NonNullByDefault
@Component(service = { MegaDEventSubscriber.class, EventSubscriber.class })
public class MegaDEventSubscriber implements EventSubscriber {
    private final Logger logger = LoggerFactory.getLogger(MegaDEventSubscriber.class);
    private final Map<String, MegaDPortsHandler> handlers = new ConcurrentHashMap<>();
    private final ItemChannelLinkRegistry linkRegistry;

    @Activate
    public MegaDEventSubscriber(@Reference ItemChannelLinkRegistry linkRegistry) {
        this.linkRegistry = linkRegistry;
    }

    @Override
    public Set<String> getSubscribedEventTypes() {
        return Set.of(ItemCommandEvent.TYPE);
    }

    @Override
    public void receive(Event event) {
        logger.debug("MegaDEventSubscriber received event: {}", event.getTopic());
        ItemCommandEvent ise = (ItemCommandEvent) event;
        String itemName = ise.getItemName();
        Command command = ise.getItemCommand();
        for (MegaDPortsHandler handler : handlers.values()) {
            ChannelUID channelUID = findLinkedChannel(handler, itemName);
            if (channelUID != null) {
                logger.debug("Routing command to handler: {}", handler.getThing().getUID());
                handler.processCommand(channelUID, command, itemName);
                return;
            }
        }
    }

    private @Nullable ChannelUID findLinkedChannel(MegaDPortsHandler handler, String itemName) {
        for (Channel channel : handler.getThing().getChannels()) {
            if (linkRegistry.isLinked(itemName, channel.getUID())) {
                return channel.getUID();
            }
        }
        return null;
    }

    public void registerHandler(MegaDPortsHandler handler) {
        String uid = handler.getThing().getUID().toString();
        logger.debug("Registering handler: {}", uid);
        handlers.put(uid, handler);
    }

    public void unregisterHandler(MegaDPortsHandler handler) {
        String uid = handler.getThing().getUID().toString();
        logger.debug("Unregistering handler: {}", uid);
        handlers.remove(uid);
    }
}
