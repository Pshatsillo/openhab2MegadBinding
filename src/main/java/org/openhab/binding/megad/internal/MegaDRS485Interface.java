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

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.megad.handler.MegaDBridgeDeviceHandler;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.Thing;

/**
 * The {@link MegaDRS485Interface} is responsible for Midea modbus protocol feature for megad
 *
 * @author Petr Shatsillo - Initial contribution
 */
@NonNullByDefault
public interface MegaDRS485Interface {
    String[] getValueFromRS485(MegaDBridgeDeviceHandler bridgeHandler);

    void setValuesToRS485(MegaDBridgeDeviceHandler bridgeHandler, String channelUID, String command);

    List<Channel> getChannelsList(Thing thing);
}
