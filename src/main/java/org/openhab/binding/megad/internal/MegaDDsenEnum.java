/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link MegaDDsenEnum} is responsible for http request to megad
 *
 * @author Petr Shatsillo - Initial contribution
 */
@NonNullByDefault
public enum MegaDDsenEnum {
    DHT11(1),
    DHT22(2),
    ONEWIRE(3),
    IB(4),
    ONEWIREBUS(5),
    W26(6);

    private final int id;

    MegaDDsenEnum(int id) {
        this.id = id;
    }

    public int getID() {
        return id;
    }
}
