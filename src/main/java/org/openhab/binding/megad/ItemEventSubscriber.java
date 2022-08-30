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

import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.events.Event;
import org.openhab.core.events.EventFilter;
import org.openhab.core.items.events.AbstractItemEventSubscriber;
import org.openhab.core.items.events.ItemCommandEvent;
import org.openhab.core.items.events.ItemStateEvent;

/**
 * The {@link ItemEventSubscriber} is responsible for standart features of megsd
 *
 * @author Petr Shatsillo - Initial contribution
 */
@NonNullByDefault
public class ItemEventSubscriber extends AbstractItemEventSubscriber {

    @Override
    public Set<String> getSubscribedEventTypes() {
        return super.getSubscribedEventTypes();
    }

    @Override
    public @Nullable EventFilter getEventFilter() {
        return super.getEventFilter();
    }

    @Override
    public void receive(Event event) {
        super.receive(event);
    }

    @Override
    protected void receiveCommand(ItemCommandEvent commandEvent) {
        super.receiveCommand(commandEvent);
    }

    @Override
    protected void receiveUpdate(ItemStateEvent updateEvent) {
        super.receiveUpdate(updateEvent);
    }
}
