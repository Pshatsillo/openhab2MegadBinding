/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.megad.MegaDConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link MegaDBridgeIncomingHandler} is responsible for creating things and thing
 * handlers.
 *
 * @author Petr Shatsillo - Initial contribution
 */

@NonNullByDefault
public class MegaDBridgeIncomingHandler extends BaseBridgeHandler {

    private Logger logger = LoggerFactory.getLogger(MegaDBridgeIncomingHandler.class);

    private int port;
    @Nullable
    private ScheduledFuture<?> pollingJob;
    private int refreshInterval = 300;
    @Nullable
    private ServerSocket ss;
    private boolean isRunning = true;
    @Nullable
    Socket s = null;
    @Nullable
    private InputStream is;
    @Nullable
    private OutputStream os;
    @Nullable
    MegaDBridgeDeviceHandler deviceHandler;

    private @Nullable Map<String, MegaDBridgeDeviceHandler> devicesHandlerMap = new HashMap<String, MegaDBridgeDeviceHandler>();

    public MegaDBridgeIncomingHandler(Bridge bridge) {
        super(bridge);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
    }

    @Override
    public void initialize() {
        logger.debug("Initializing Megad bridge handler {}", this.toString());

        MegaDConfiguration configuration = getConfigAs(MegaDConfiguration.class);
        port = configuration.port;
        startBackgroundService();
    }

    private void startBackgroundService() {
        logger.debug("Starting background service...");
        if (pollingJob == null || pollingJob.isCancelled()) {
            pollingJob = scheduler.scheduleWithFixedDelay(pollingRunnable, 0, refreshInterval, TimeUnit.SECONDS);
        }
    }

    private Runnable pollingRunnable = new Runnable() {
        @SuppressWarnings("null")
        @Override
        public void run() {
            logger.debug("Polling job called");
            try {
                ss = new ServerSocket(port);

                ss.setPerformancePreferences(1, 0, 0);
                ss.setReuseAddress(true);

                logger.info("MegaD bridge opened port {}", ss.getLocalPort());
                isRunning = true;
                updateStatus(ThingStatus.ONLINE);
            } catch (IOException e) {
                logger.error("ERROR! Cannot open port: {}", e.getMessage());
                updateStatus(ThingStatus.OFFLINE);
            }

            while (isRunning) {
                try {
                    if (ss != null) {
                        s = ss.accept();
                    }

                } catch (IOException e) {
                    logger.error("ERROR in bridge. Incoming server has error: {}", e.getMessage());
                }
                if (!ss.isClosed()) {
                    new Thread(startHttpSocket());
                }
            }
        }

    };

    protected @Nullable Runnable startHttpSocket() {
        try {
            s.setKeepAlive(false);
            s.setReuseAddress(true);
            s.setTcpNoDelay(true);
            this.is = s.getInputStream();
            this.os = s.getOutputStream();
        } catch (IOException e) {
            logger.error("{}", e.getLocalizedMessage());
        }

        String input = readInput();
        writeResponse();
        parseInput(input);
        return null;
    }

    private String readInput() {
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String string = "";
        try {
            string = br.readLine();
            logger.debug("{}", string);
        } catch (IOException e) {
            logger.error("{}", e.getLocalizedMessage());
        }

        return string;
    }

    private void writeResponse() {
        String result = "HTTP/1.1 200 OK\r\n" + "Content-Type: text/html\r\n" + "Content-Length: " + 0 + "\r\n"
                + "Connection: close\r\n\r\n";
        try {
            os.write(result.getBytes());
            s.setSoLinger(true, 0);
            is.close();
            os.close();
        } catch (IOException e) {
            logger.error("{}", e.getLocalizedMessage());
        }

        finally {
            try {
                s.close();
            } catch (IOException e) {
                logger.error("{}", e.getLocalizedMessage());
            }
        }
    }

    @SuppressWarnings("null")
    private void parseInput(@Nullable String s) {
        String[] getCommands;
        String thingID, hostAddress;
        if (!(s == null || s.trim().length() == 0)) {
            if (s.contains("GET") && s.contains("?")) {
                logger.debug("incoming from Megad: {} {}", this.s.getInetAddress().getHostAddress(), s);
                String[] commandParse = s.split("[/ ]");
                String command = commandParse[2];
                getCommands = command.split("[?&>=]");

                for (int i = 0; getCommands.length > i; i++) {
                    logger.debug("{} value {}", i, getCommands[i]);
                }
                if (this.s.getInetAddress().getHostAddress().equals("0:0:0:0:0:0:0:1")) {
                    deviceHandler = devicesHandlerMap.get("localhost");
                } else {
                    deviceHandler = devicesHandlerMap.get(this.s.getInetAddress().getHostAddress());
                }
                if (deviceHandler != null) {
                    deviceHandler.updateValues(command);
                }

            }
        }
    }

    @SuppressWarnings("unused")
    public void registerMegaDeviceListener(MegaDBridgeDeviceHandler megaDBridgeDeviceHandler) {

        String ip = megaDBridgeDeviceHandler.getThing().getConfiguration().get("hostname").toString();
        logger.debug("Register Device with ip {}", ip);
        if (devicesHandlerMap.get(ip) != null) {
            updateThingHandlerStatus(megaDBridgeDeviceHandler, ThingStatus.OFFLINE,
                    ThingStatusDetail.CONFIGURATION_ERROR, "Device already exist");
        } else {
            devicesHandlerMap.put(ip, megaDBridgeDeviceHandler);
            updateThingHandlerStatus(megaDBridgeDeviceHandler, ThingStatus.ONLINE);
            // megaDBridgeDeviceHandler.getAllPortsStatus();
        }

    }

    @SuppressWarnings("null")
    public void unregisterMegaDeviceListener(MegaDBridgeDeviceHandler megaDBridgeDeviceHandler) {
        String ip = megaDBridgeDeviceHandler.getThing().getConfiguration().get("hostname").toString();
        if (devicesHandlerMap.get(ip) != null) {
            devicesHandlerMap.remove(ip);
            updateThingHandlerStatus(megaDBridgeDeviceHandler, ThingStatus.OFFLINE);
        }
    }

    private void updateThingHandlerStatus(MegaDBridgeDeviceHandler megaDBridgeDeviceHandler, ThingStatus status,
            ThingStatusDetail statusDetail, String decript) {
        megaDBridgeDeviceHandler.updateStatus(status, statusDetail, decript);

    }

    private void updateThingHandlerStatus(MegaDBridgeDeviceHandler thingHandler, ThingStatus status) {
        thingHandler.updateStatus(status);
    }

    @Override
    public void dispose() {
        if (pollingJob != null && !pollingJob.isCancelled()) {
            pollingJob.cancel(true);
            pollingJob = null;
        }
        isRunning = false;
        try {
            ss.close();
        } catch (IOException e) {
            logger.error("{}", e.getLocalizedMessage());
        }
        updateStatus(ThingStatus.OFFLINE); // Set all State to offline
        super.dispose();
    }

}
