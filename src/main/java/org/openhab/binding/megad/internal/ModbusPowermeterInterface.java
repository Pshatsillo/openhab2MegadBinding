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
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.Thing;

/**
 * The {@link ModbusPowermeterInterface} is responsible for modbus protocol feature for megad
 *
 * @author Petr Shatsillo - Initial contribution
 */
@NonNullByDefault
public interface ModbusPowermeterInterface {
    String getVoltage();

    void updateValues();

    String getCurrent(int line);

    String getActivePower(int line);

    String getApparentPower(int line);

    String getReactivePower(int line);

    String getPowerFactor(int line);

    String getPhaseAngle(int line);

    String getFrequency();

    String getImportActiveEnergy();

    String getExportActiveEnergy();

    String getImportReactiveEnergy();

    String getExportReactiveEnergy();

    String getTotalSystemPowerDemand();

    String getMaxTotalSystemPowerDemand();

    String getImportSystemPowerDemand();

    String getMaxImportSystemPowerDemand();

    String getExportSystemPowerDemand();

    String getMaxExportSystemPowerDemand();

    String getCurrentDemand();

    String getMaxCurrentDemand();

    String getTotalActiveEnergy();

    String getTotalReactiveActiveEnergy(int line);

    List<Channel> getChannelsList(Thing thing);

    String getActiveEnergy(int line);
}
