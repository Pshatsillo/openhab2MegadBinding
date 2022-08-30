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
package org.openhab.binding.megad.handler;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.megad.MegaDBindingConstants;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link MegaDEncoderHandler} is responsible for encoder feature of megad
 *
 * @author Petr Shatsillo - Initial contribution
 */
@NonNullByDefault
public class MegaDEncoderHandler extends BaseThingHandler {
    private final Logger logger = LoggerFactory.getLogger(MegaDEncoderHandler.class);
    @Nullable
    MegaDBridgeDeviceHandler bridgeDeviceHandler;
    int prevval = 0;

    public MegaDEncoderHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        bridgeDeviceHandler = getBridgeHandler();
        if (bridgeDeviceHandler != null) {
            registerMegadEncoderListener(bridgeDeviceHandler);
        } else {
            logger.debug("Can't register {} at bridge. BridgeHandler is null.", this.getThing().getUID());
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
    }
    // -----------------------------------------------------

    private void registerMegadEncoderListener(@Nullable MegaDBridgeDeviceHandler bridgeHandler) {
        if (bridgeHandler != null) {
            bridgeHandler.registerMegadEncoderListener(this);
        }
    }

    private synchronized @Nullable MegaDBridgeDeviceHandler getBridgeHandler() {
        Bridge bridge = getBridge();
        if (bridge == null) {
            logger.error("Required bridge not defined for device.");
            return null;
        } else {
            return getBridgeHandler(bridge);
        }
    }

    private synchronized @Nullable MegaDBridgeDeviceHandler getBridgeHandler(Bridge bridge) {
        ThingHandler handler = bridge.getHandler();
        if (handler instanceof MegaDBridgeDeviceHandler) {
            return (MegaDBridgeDeviceHandler) handler;
        } else {
            logger.debug("No available bridge handler found yet. Bridge: {} .", bridge.getUID());
            return null;
        }
    }

    @Override
    public void updateStatus(ThingStatus status) {
        super.updateStatus(status);
    }

    @Override
    protected void updateStatus(ThingStatus status, ThingStatusDetail statusDetail, @Nullable String description) {
        super.updateStatus(status, statusDetail, description);
    }

    @SuppressWarnings("null")
    @Override
    public void dispose() {
        if (bridgeDeviceHandler != null) {
            bridgeDeviceHandler.unregisterMegaDEncoderListener(this);
        }
        super.dispose();
    }

    public void updateValues(String getCommand) {
        logger.debug("{}", getCommand);
        for (Channel channel : getThing().getChannels()) {
            if (isLinked(channel.getUID().getId())) {
                if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_DIRECTION)) {
                    int value = Integer.parseInt(getCommand);
                    if (prevval > value) {
                        updateState(channel.getUID().getId(), DecimalType.valueOf("0"));
                    } else {
                        updateState(channel.getUID().getId(), DecimalType.valueOf("100"));
                    }
                    prevval = value;
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_BUTTON)) {
                    updateState(channel.getUID().getId(), OnOffType.ON);
                } else if (channel.getUID().getId().equals(MegaDBindingConstants.CHANNEL_ENCODERDIGITS)) {
                    updateState(channel.getUID().getId(), DecimalType.valueOf(getCommand));
                }
            }
        }
    }
}
