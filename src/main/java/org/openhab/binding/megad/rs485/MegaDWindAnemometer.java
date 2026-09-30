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
package org.openhab.binding.megad.rs485;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.megad.MegaDBindingConstants;
import org.openhab.binding.megad.MegaDHttpHelpers;
import org.openhab.binding.megad.handler.MegaDDeviceHandler;
import org.openhab.core.io.net.http.HttpClientFactory;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link MegaDWindAnemometer} is responsible for rs485/modbus feature of megad
 *
 * @author Petr Shatsillo - Initial contribution
 */
@NonNullByDefault
public class MegaDWindAnemometer implements MegaDRS485Interface {
    final Logger logger = LoggerFactory.getLogger(MegaDWindAnemometer.class);
    String address;
    MegaDDeviceHandler bridgeHandler;
    private final MegaDHttpHelpers httpHelper = new MegaDHttpHelpers();

    public MegaDWindAnemometer(MegaDDeviceHandler bridgeHandler, String address, HttpClientFactory httpClientFactory) {
        this.address = address;
        this.bridgeHandler = bridgeHandler;
        httpHelper.setHttpClient(httpClientFactory.getCommonHttpClient());
    }

    @Override
    public String[] getValueFromRS485(MegaDDeviceHandler bridgeHandler) {
        String result = "http://"
                + Objects.requireNonNull(bridgeHandler).getThing().getConfiguration().get("hostname").toString() + "/"
                + Objects.requireNonNull(bridgeHandler).getThing().getConfiguration().get("password").toString()
                + "/?uart_tx=" + address + "0300000002&mode=rs485";
        httpHelper.request(result);
        try {
            Thread.sleep(200);
        } catch (InterruptedException ignored) {
        }
        result = "http://"
                + Objects.requireNonNull(bridgeHandler).getThing().getConfiguration().get("hostname").toString() + "/"
                + Objects.requireNonNull(bridgeHandler).getThing().getConfiguration().get("password").toString()
                + "/?uart_rx=1&mode=rs485";
        String updateRequest = httpHelper.request(result).getResponseResult();
        logger.trace("Wind speed answer is: {}", updateRequest);
        String[] request = updateRequest.split("[|]");
        return request;
    }

    @Override
    public void setValuesToRS485(MegaDDeviceHandler bridgeHandler, String channelUID, String command) {
    }

    @Override
    public List<Channel> getChannelsList(Thing thing) {
        List<Channel> channelList = new ArrayList<>();
        ChannelUID windSpeedUID = new ChannelUID(thing.getUID(), MegaDBindingConstants.CHANNEL_WINDSPED);
        Channel windSpeed = ChannelBuilder.create(windSpeedUID)
                .withType(new ChannelTypeUID(MegaDBindingConstants.BINDING_ID, MegaDBindingConstants.CHANNEL_WINDSPED))
                .withLabel("Скорость ветра").withAcceptedItemType("Number").build();
        channelList.add(windSpeed);
        return channelList;
    }
}
