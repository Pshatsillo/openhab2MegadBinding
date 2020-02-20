/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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
import org.eclipse.jetty.server.Server;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.megad.MegaDConfiguration;
import org.openhab.binding.megad.internal.IncomingMessagesServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

/**
 * The {@link MegaDBridgeIncomingHandler} is responsible for creating things and thing
 * handlers.
 *
 * @author Petr Shatsillo - Initial contribution
 */
@NonNullByDefault
public class MegaDBridgeIncomingHandler extends BaseBridgeHandler {
    Logger logger = LoggerFactory.getLogger(MegaDBridgeIncomingHandler.class);
    private int port;
    @Nullable
    private ScheduledFuture<?> pollingJob;
    private int refreshInterval = 300;
   /* @Nullable
    private ServerSocket ss;
    private boolean isRunning = true;*/
    @Nullable
   // Socket s = null;
    Server s;
   /* @Nullable
    private InputStream is;
    @Nullable
    private OutputStream os;*/
    @Nullable
    MegaDBridgeDeviceHandler deviceHandler;
    private @Nullable Map<String, MegaDBridgeDeviceHandler> devicesHandlerMap = new HashMap<String, MegaDBridgeDeviceHandler>();
    public MegaDBridgeIncomingHandler(Bridge bridge) {
        super(bridge);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
    }
    @SuppressWarnings("null")
    @Override
    public void initialize() {
        logger.debug("Initializing Megad bridge handler {}", this.toString());

        MegaDConfiguration configuration = getConfigAs(MegaDConfiguration.class);
        port = configuration.port;
     //   startBackgroundService();
        try {
        s = new Server(port);
        s.setHandler(new IncomingMessagesServlet(this));
        //handler.addServletWithMapping(IncomingMessagesServlet.class, "/*");
        s.start();
        updateStatus(ThingStatus.ONLINE);
        s.join();
        } catch (IOException e) {
            logger.error("ERROR! Cannot open port: {}", e.getMessage());
            updateStatus(ThingStatus.OFFLINE);
        } catch (Exception e) {
            //  e.printStackTrace();
        }
    }

/*
    @SuppressWarnings("null")
    private void startBackgroundService() {
        logger.debug("Starting background service...");
        if (pollingJob == null || pollingJob.isCancelled()) {
            pollingJob = scheduler.scheduleWithFixedDelay(pollingRunnable, 0, refreshInterval, TimeUnit.SECONDS);
        }
    }
*/

   /* private Runnable pollingRunnable = new Runnable() {
        @SuppressWarnings("null")
        @Override
        public void run() {
            logger.debug("Polling job called");

                *//*ss = new ServerSocket(port);

                ss.setPerformancePreferences(1, 0, 0);
                ss.setReuseAddress(true);
*//*


              //  logger.info("MegaD bridge opened port {}", ss.getLocalPort());
                isRunning = true;



           *//* while (isRunning) {
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
            }*//*
        }

    };*/

  //  @SuppressWarnings("null")
   /* protected @Nullable Runnable startHttpSocket() {
        if (s != null) {
            try {
                s.setKeepAlive(false);
                s.setReuseAddress(true);
                s.setTcpNoDelay(true);
                this.is = s.getInputStream();
                this.os = s.getOutputStream();
                String input = readInput();
                writeResponse();
                parseInput(input);
            } catch (IOException e) {
                logger.error("{}", e.getLocalizedMessage());
            } finally {
                try {
                    s.close();
                } catch (IOException e) {
                    logger.error("{}", e.getLocalizedMessage());
                }
            }
        }

        return null;
    }*/

   /* private String readInput() {
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String string = "";
        try {
          //  string = br.readLine();
            while ((string = br.readLine()) != null) {
                logger.debug("{}", string);
            }
            logger.debug("{}", string);
        } catch (IOException e) {
            logger.error("{}", e.getLocalizedMessage());
        }

        return string;
    }*/

    /* private void writeResponse() {
        String result = "HTTP/1.1 200 OK\r\n" + "Content-Type: text/html\r\n" + "Content-Length: " + 0 + "\r\n"
                + "Connection: close\r\n\r\n";
        if (os != null) {
            try {
                os.write(result.getBytes());
                s.setSoLinger(true, 0);
            } catch (IOException e) {
                logger.error("{}", e.getLocalizedMessage());
            }
        }
    }*/

    @SuppressWarnings("null")
    public void parseInput(String s, String remoteHost) {
                if (remoteHost.equals("0:0:0:0:0:0:0:1")) {
                    deviceHandler = devicesHandlerMap.get("localhost");
                } else {
                    deviceHandler = devicesHandlerMap.get(remoteHost);
                }
                if (deviceHandler != null) {
                   deviceHandler.updateValues(s);
                }

        logger.debug("incoming from Megad: {} {}", remoteHost, s);
    }
    @SuppressWarnings({"unused","null"})
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

    @SuppressWarnings("unused")
    private void updateThingHandlerStatus(MegaDBridge1WireBusHandler thingHandler, ThingStatus status) {
        thingHandler.updateStatus(status);
    }

    @SuppressWarnings("null")
    @Override
    public void dispose() {
        if (pollingJob != null && !pollingJob.isCancelled()) {
            pollingJob.cancel(true);
            pollingJob = null;
        }
     //   isRunning = false;
        try {
            s.stop();
          //  ss.close();
        } catch (IOException e) {
            logger.error("{}", e.getLocalizedMessage());
        } catch (Exception e) {
            logger.error("{}", e.getMessage());
        }
        updateStatus(ThingStatus.OFFLINE); // Set all State to offline
        super.dispose();
    }
}
