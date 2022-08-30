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

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.megad.MegaDBindingConstants;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link MegaDLcd1609Handler} is responsible for LCD1609 feature of megad
 *
 * @author Petr Shatsillo - Initial contribution
 */
@NonNullByDefault
public class MegaDLcd1609Handler extends BaseThingHandler {
    private final Logger logger = LoggerFactory.getLogger(MegaDLcd1609Handler.class);
    @Nullable
    MegaDBridgeDeviceHandler bridgeDeviceHandler;

    public MegaDLcd1609Handler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        bridgeDeviceHandler = getBridgeHandler();
        updateStatus(ThingStatus.ONLINE);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.debug("LCD send command: {}", command);
        if (channelUID.getId().equals(MegaDBindingConstants.CHANNEL_LINE1)) {
            if (!command.toString().equals("REFRESH")) {
                // result = "http://" + bridgeDeviceHandler.getThing().getConfiguration().get("hostname").toString() +
                // "/"
                // + bridgeDeviceHandler.getThing().getConfiguration().get("password").toString() + "/?pt="
                // + getThing().getConfiguration().get("port").toString() + "&text=________________";
                // // sendCommand(result);
                sendCommand(bridgeDeviceHandler.getThing().getConfiguration().get("hostname").toString(),
                        "/" + bridgeDeviceHandler.getThing().getConfiguration().get("password").toString() + "/?pt="
                                + getThing().getConfiguration().get("port").toString() + "&text=________________");
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    // e.printStackTrace();
                }
                // result = "http://" + bridgeDeviceHandler.getThing().getConfiguration().get("hostname").toString() +
                // "/"
                // + bridgeDeviceHandler.getThing().getConfiguration().get("password").toString() + "/?pt="
                // + getThing().getConfiguration().get("port").toString() + "&text="
                // + command.toString().replace(" ", "_");
                // // sendCommand(result);
                sendCommand(bridgeDeviceHandler.getThing().getConfiguration().get("hostname").toString(),
                        "/" + bridgeDeviceHandler.getThing().getConfiguration().get("password").toString() + "/?pt="
                                + getThing().getConfiguration().get("port").toString() + "&text="
                                + command.toString().replace(" ", "_"));
            }
        } else if (channelUID.getId().equals(MegaDBindingConstants.CHANNEL_LINE2)) {
            if (!command.toString().equals("REFRESH")) {
                assert bridgeDeviceHandler != null;
                // result = "http://" + bridgeDeviceHandler.getThing().getConfiguration().get("hostname").toString() +
                // "/"
                // + bridgeDeviceHandler.getThing().getConfiguration().get("password").toString() + "/?pt="
                // + getThing().getConfiguration().get("port").toString() + "&text=________________&col=0&row=1";
                // // sendCommand(result);
                sendCommand(bridgeDeviceHandler.getThing().getConfiguration().get("hostname").toString(),
                        "/" + bridgeDeviceHandler.getThing().getConfiguration().get("password").toString() + "/?pt="
                                + getThing().getConfiguration().get("port").toString()
                                + "&text=________________&col=0&row=1");

                try {
                    Thread.sleep(500);
                } catch (InterruptedException ignored) {
                }

                // result = "http://" + bridgeDeviceHandler.getThing().getConfiguration().get("hostname").toString() +
                // "/"
                // + bridgeDeviceHandler.getThing().getConfiguration().get("password").toString() + "/?pt="
                // + getThing().getConfiguration().get("port").toString() + "&text="
                // + command.toString().replace(" ", "_") + "&col=0&row=1";
                sendCommand(bridgeDeviceHandler.getThing().getConfiguration().get("hostname").toString(),
                        "/" + bridgeDeviceHandler.getThing().getConfiguration().get("password").toString() + "/?pt="
                                + getThing().getConfiguration().get("port").toString() + "&text="
                                + command.toString().replace(" ", "_") + "&col=0&row=1");
            }
        }
    }

    @SuppressWarnings("null")
    public void sendCommand(String hostname, String request) {
        String req = "GET " + request + " HTTP/1.1\n\r " + "User-Agent: Mozilla/5.0\n\r " + "Host: 192.168.10.19\n\r "
                + "Accept: text/html\n\r " + "Connection: keep-alive";
        int degreeIndex = req.indexOf("°");
        req = req.replace("°", "_");
        int port = 80;
        try (Socket socket = new Socket(hostname, port)) {
            OutputStream output = socket.getOutputStream();
            byte[] data = req.getBytes(StandardCharsets.UTF_8);
            if (degreeIndex != -1) {
                data[degreeIndex] = (byte) 0xdf;
            }
            output.write(data);
            logger.info("LCD send: {}", data);
        } catch (UnknownHostException ex) {
            logger.error("Server not found: {}", ex.getMessage());
        } catch (IOException ex) {
            logger.error("I/O error: {}", ex.getMessage());
        }
    }

    // ----------------------------------------------------------
    private synchronized @Nullable MegaDBridgeDeviceHandler getBridgeHandler() {
        Bridge bridge = Objects.requireNonNull(getBridge());
        return getBridgeHandler(bridge);
    }

    private synchronized @Nullable MegaDBridgeDeviceHandler getBridgeHandler(Bridge bridge) {
        ThingHandler handler = Objects.requireNonNull(bridge.getHandler());
        if (handler instanceof MegaDBridgeDeviceHandler) {
            return (MegaDBridgeDeviceHandler) handler;
        } else {
            logger.debug("No available bridge handler found yet. Bridge: {} .", bridge.getUID());
            return null;
        }
    }

    @SuppressWarnings("null")
    @Override
    public void dispose() {
        super.dispose();
    }
}
