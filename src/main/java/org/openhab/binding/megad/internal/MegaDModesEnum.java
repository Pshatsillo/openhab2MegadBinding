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

/**
 * The {@link MegaDModesEnum} is responsible for http request to megad
 *
 * @author Petr Shatsillo - Initial contribution
 */
public enum MegaDModesEnum {

    NC(255),
    IN(0),
    OUT(1),
    DSEN(2),
    I2C(4),
    ADC(5);

    private int id;

    MegaDModesEnum(int id) {
        this.id = id;
    }

    public int getID() {
        return id;
    }
}
