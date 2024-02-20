/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
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
package org.openhab.binding.megad.enums;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link MegaDExtendersEnum} is responsible for http request to megad
 *
 * @author Petr Shatsillo - Initial contribution
 */
@NonNullByDefault
public enum MegaDExtendersEnum {
    NC(0),
    MCP230XX(20),
    PCA9685(21);

    private final int id;

    MegaDExtendersEnum(int id) {
        this.id = id;
    }

    public int getID() {
        return id;
    }
}
