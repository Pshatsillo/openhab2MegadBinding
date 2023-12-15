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
package org.openhab.binding.megad.enums;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link MegaDTypesEnum} is responsible for http request to megad
 *
 * @author Petr Shatsillo - Initial contribution
 */
@NonNullByDefault
public enum MegaDTypesEnum {
    NC(255),
    IN(0),
    OUT(1),
    DSEN(2),
    I2C(4),
    ADC(5);

    private final int id;

    MegaDTypesEnum(int id) {
        this.id = id;
    }

    public static MegaDTypesEnum setID(int i) {
        MegaDTypesEnum en = NC;
        for (int j = 0; j < MegaDTypesEnum.values().length; j++) {
            if (MegaDTypesEnum.values()[j].id == i) {
                en = MegaDTypesEnum.values()[j];
            }
        }
        return en;
    }

    public int getID() {
        return id;
    }
}
