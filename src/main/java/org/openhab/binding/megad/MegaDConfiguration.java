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

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link MegaDConfiguration} class contains fields mapping thing configuration parameters.
 *
 * @author Petr Shatsillo - Initial contribution
 */
@NonNullByDefault
public class MegaDConfiguration {

    /**
     * Sample configuration parameter. Replace with your own.
     */
    public Integer port = 0;
    public boolean discovery = true;
}
