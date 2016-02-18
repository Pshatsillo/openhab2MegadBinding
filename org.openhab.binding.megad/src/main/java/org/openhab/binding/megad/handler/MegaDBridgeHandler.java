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

import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.megad.MegaDBindingConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link MegaDBridgeHandler} is responsible for bridge.
 *
 * @author Petr Shatsillo - Initial contribution
 */

public class MegaDBridgeHandler extends BaseBridgeHandler {

    private Logger logger = LoggerFactory.getLogger(MegaDBridgeHandler.class);

    private boolean isConnect = false;
    private int port;
    private ScheduledFuture<?> pollingJob;
    Socket s = null;
    private ServerSocket ss;
    private InputStream is;
    private OutputStream os;
    private boolean isRunning = true;
    private int refreshInterval = 300;
    MegaDHandler MegaDHandler;

    public MegaDBridgeHandler(Bridge bridge) {
        super(bridge);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateStatus(ThingStatus status) {
        super.updateStatus(status);
        updateThingHandlersStatus(status);

    }

    public void updateStatus() {
        if (isConnect) {
            updateStatus(ThingStatus.ONLINE);
        } else {
            updateStatus(ThingStatus.OFFLINE);
        }

    }

    private Map<String, MegaDHandler> thingHandlerMap = new HashMap<String, MegaDHandler>();

    public void registerMegadThingListener(MegaDHandler thingHandler) {
        if (thingHandler == null) {
            throw new IllegalArgumentException("It's not allowed to pass a null ThingHandler.");
        } else {

            String thingID = thingHandler.getThing().getConfiguration().get("hostname").toString() + "."
                    + thingHandler.getThing().getConfiguration().get("port").toString();
            if (thingHandlerMap.get(thingID) != null) {
                thingHandlerMap.remove(thingID);
            }
            logger.debug("thingHandler for thing: '{}'", thingID);
            if (thingHandlerMap.get(thingID) == null) {
                thingHandlerMap.put(thingID, thingHandler);
                logger.debug("register thingHandler for thing: {}", thingHandler);
                updateThingHandlerStatus(thingHandler, this.getStatus());
                if (thingID.equals("localhost.")) {
                    updateThingHandlerStatus(thingHandler, ThingStatus.OFFLINE);
                }
                // sendSocketData("get " + thingID);
            } else {
                logger.debug("thingHandler for thing: '{}' already registerd", thingID);
                updateThingHandlerStatus(thingHandler, this.getStatus());
            }

        }
    }

    public void unregisterThingListener(MegaDHandler thingHandler) {
        if (thingHandler != null) {
            String thingID = thingHandler.getThing().getConfiguration().get("hostname").toString() + "."
                    + thingHandler.getThing().getConfiguration().get("port").toString();
            if (thingHandlerMap.remove(thingID) == null) {
                logger.debug("thingHandler for thing: {} not registered", thingID);
            } else {
                updateThingHandlerStatus(thingHandler, ThingStatus.OFFLINE);
            }
        }

    }

    private void updateThingHandlerStatus(MegaDHandler thingHandler, ThingStatus status) {
        thingHandler.updateStatus(status);
    }

    private void updateThingHandlersStatus(ThingStatus status) {
        for (Map.Entry<String, MegaDHandler> entry : thingHandlerMap.entrySet()) {
            updateThingHandlerStatus(entry.getValue(), status);
        }
    }

    public ThingStatus getStatus() {
        return getThing().getStatus();
    }

    @Override
    public void initialize() {
        logger.debug("Initializing Megad bridge handler {}", this.toString());

        MegaDBindingConfiguration configuration = getConfigAs(MegaDBindingConfiguration.class);
        port = configuration.port;

        // updateStatus(ThingStatus.ONLINE);

        startBackgroundService();
    }

    private void startBackgroundService() {
        logger.debug("Starting background service...");
        if (pollingJob == null || pollingJob.isCancelled()) {
            pollingJob = scheduler.scheduleAtFixedRate(pollingRunnable, 0, refreshInterval, TimeUnit.SECONDS);
        }
    }

    private Runnable pollingRunnable = new Runnable() {
        @Override
        public void run() {
            logger.debug("Polling job called");
            try {
                ss = new ServerSocket(port);
                logger.debug("MegaD Server open port {}", port);
                updateStatus(ThingStatus.ONLINE);
            } catch (IOException e) {
                logger.debug("ERROR -> " + e.getMessage());
                updateStatus(ThingStatus.OFFLINE);
                // e.printStackTrace();
            }

            while (isRunning) {
                try {
                    s = ss != null ? ss.accept() : null;
                } catch (IOException e) {
                    logger.debug("ERROR Cycle --> " + e.getMessage());
                    // e.printStackTrace();
                }
                if (!ss.isClosed()) {
                    new Thread(startHttpSocket());
                }
            }
        }

    };

    protected Runnable startHttpSocket() {
        try {
            this.is = s.getInputStream();
            this.os = s.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }

        readInput();
        writeResponse();
        return null;
    }

    private void readInput() {
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        while (true) {
            String s;
            String[] getCommands;
            String thingID, hostAddress;
            try {
                s = br.readLine();
                if (s == null || s.trim().length() == 0) {
                    break;
                }
                if (s.contains("GET") && s.contains("?")) {
                    logger.debug("incoming from Megad: " + this.s.getInetAddress().getHostAddress() + " " + s);
                    String[] CommandParse = s.split("[/ ]");
                    String command = CommandParse[2];
                    getCommands = command.split("[?&>=]");
                    if (s.contains("m=1")) {
                        hostAddress = this.s.getInetAddress().getHostAddress();
                        if (hostAddress.equals("0:0:0:0:0:0:0:1")) {
                            hostAddress = "localhost";
                        }
                        thingID = hostAddress + "." + getCommands[2];
                        MegaDHandler = thingHandlerMap.get(thingID);
                        // logger.debug("{}", MegaDHandler);
                        if (MegaDHandler != null) {
                            MegaDHandler.updateValues(hostAddress, getCommands, OnOffType.OFF);
                        }
                    } else {
                        hostAddress = this.s.getInetAddress().getHostAddress();
                        if (hostAddress.equals("0:0:0:0:0:0:0:1")) {
                            hostAddress = "localhost";
                        }
                        if (getCommands[1].equals("pt")) {
                            thingID = hostAddress + "." + getCommands[2];
                        } else {
                            thingID = hostAddress + "." + getCommands[1];
                        }

                        MegaDHandler = thingHandlerMap.get(thingID);
                        logger.debug("{}", thingID);
                        if (MegaDHandler != null) {
                            MegaDHandler.updateValues(hostAddress, getCommands, OnOffType.ON);
                        }
                        // for (int i = 0; getCommands.length > i; i++) {
                        // logger.debug(i + " value " + getCommands[i]);
                        // }
                    }
                    break;
                } else {
                    break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void writeResponse() {
        String result = "HTTP/1.1 200 OK\r\n" + "Content-Type: text/html\r\n" + "Content-Length: " + 0 + "\r\n"
                + "Connection: close\r\n\r\n";
        try {
            os.write(result.getBytes());
            os.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void dispose() {
        logger.debug("Dispose Megad bridge handler{}", this.toString());

        if (pollingJob != null && !pollingJob.isCancelled()) {
            pollingJob.cancel(true);
            pollingJob = null;
        }
        updateStatus(ThingStatus.OFFLINE); // Set all State to offline
    }
}
