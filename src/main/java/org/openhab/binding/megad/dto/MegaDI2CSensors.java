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
package org.openhab.binding.megad.dto;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * The {@link MegaDI2CSensors} is responsible for creating things and thing
 * handlers.
 *
 * @author Petr Shatsillo - Initial contribution
 */
@NonNullByDefault
public class MegaDI2CSensors {
    private String sensorLabel = "";
    private String sensorAddress = "";
    private String sensorType = "";
    private final Set<I2CSensorParams> parameters = new HashSet<>();
    boolean sensorInitRequired;
    private Logger logger = LoggerFactory.getLogger(MegaDI2CSensors.class);

    public MegaDI2CSensors(JsonElement sensor) {
        try {
            if (sensor.getAsJsonObject().keySet().stream().findFirst().isPresent()) {
                sensorType = sensor.getAsJsonObject().keySet().stream().findFirst().get();
            }
            sensorLabel = sensor.getAsJsonObject().getAsJsonObject(sensorType).get("Label").getAsString();
            sensorAddress = sensor.getAsJsonObject().getAsJsonObject(sensorType).get("Address").getAsString();
            sensorInitRequired = sensor.getAsJsonObject().getAsJsonObject(sensorType).get("Init").getAsBoolean();
            Set<String> sensorParameters = sensor.getAsJsonObject().getAsJsonObject(sensorType)
                    .getAsJsonObject("Parameters").keySet();
            for (String parameter : sensorParameters) {
                JsonObject paramObj = sensor.getAsJsonObject().getAsJsonObject(sensorType).getAsJsonObject("Parameters")
                        .getAsJsonObject(parameter).getAsJsonObject();
                I2CSensorParams paramClass = new I2CSensorParams();
                paramClass.setId(parameter);
                paramClass.setName(paramObj.get("name").getAsString());
                paramClass.setPath(paramObj.get("path").getAsString());
                paramClass.setOh(paramObj.get("OH").getAsString());
                parameters.add(paramClass);
            }
        } catch (Exception e) {
            logger.error("Json file parsing error, {}", e.getLocalizedMessage());
        }
    }

    public String getSensorLabel() {
        return sensorLabel;
    }

    public String getSensorAddress() {
        return sensorAddress;
    }

    public String getSensorType() {
        return sensorType;
    }

    public Set<I2CSensorParams> getParameters() {
        return parameters;
    }

    public class I2CSensorParams {
        private String id = "";
        private String name = "";
        private String path = "";
        private String oh = "";

        public String getName() {
            return name;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getPath() {
            return path;
        }

        public String getOh() {
            return oh;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public void setOh(String oh) {
            this.oh = oh;
        }
    }

    public boolean isSensorInitRequired() {
        return sensorInitRequired;
    }
}
