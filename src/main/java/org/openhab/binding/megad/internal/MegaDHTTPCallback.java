/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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

import java.io.Serial;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.megad.handler.MegaDDeviceHandler;
import org.openhab.binding.megad.handler.MegaDPortsHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HTTP callback service for Megad
 * Handles incoming HTTP requests from MegaD devices and routes them to appropriate port handlers.
 *
 * @author Petr Shatsillo - Initial contribution
 *
 */
@NonNullByDefault
public class MegaDHTTPCallback extends HttpServlet {
    private final Logger logger = LoggerFactory.getLogger(MegaDHTTPCallback.class);
    @Serial
    private static final long serialVersionUID = -2725161358635927815L;
    // public static List<MegaDPortsHandler> portListener = new ArrayList<>();

    // Thread-safe collections for OSGi environment
    private static final List<MegaDPortsHandler> PORT_LISTENERS = new CopyOnWriteArrayList<>();

    // Parameter constants
    private static final String PARAM_ALL = "all";
    private static final String PARAM_PT = "pt";
    private static final String PARAM_ST = "st";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        String remoteAddr = req.getRemoteAddr();
        String query = req.getQueryString();
        logger.debug("Request from {} is: {}", remoteAddr, query);

        if (query == null || query.isEmpty()) {
            sendEmptyResponse(resp);
            return;
        }

        try {
            processRequest(remoteAddr, query, resp);
        } catch (Exception e) {
            logger.error("Error processing callback from {}: {}", remoteAddr, e.getMessage());
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }

        sendEmptyResponse(resp);
    }

    /**
     * Process the incoming request
     */
    private void processRequest(String remoteAddr, String query, HttpServletResponse resp) {
        QueryParams params = parseQuery(query);

        // Find all handlers for this device
        List<MegaDPortsHandler> handlers = findHandlersForDevice(remoteAddr);

        if (handlers.isEmpty()) {
            logger.debug("No handlers found for device {}", remoteAddr);
            return;
        }

        // Route to appropriate handler based on query type
        if (params.hasAll()) {
            processAllPorts(handlers, params.getAll());
        } else if (params.hasStart()) {
            processStatusUpdate(handlers);
        } else if (params.hasPort()) {
            processPortUpdate(handlers, params.getPort(), query);
        }
    }

    /**
     * Process port-specific update
     */
    private void processPortUpdate(List<MegaDPortsHandler> handlers, String portNumber, String query) {
        // Try to resolve virtual port mapping
        String resolvedPort = resolvePortNumber(handlers, portNumber);

        for (MegaDPortsHandler handler : handlers) {
            MegaDDeviceHandler bridge = handler.getBridgeDeviceHandler();
            if (bridge != null) {
                String handlerPort = handler.getThing().getConfiguration().get("port").toString();
                if (handlerPort.equals(resolvedPort) || handlerPort.equals(portNumber)) {
                    logger.debug("Port {} update for device {}", handlerPort,
                            bridge.getThing().getConfiguration().get("hostname"));
                    handler.updatePort(query);
                    break;
                }
            }
        }
    }

    /**
     * Resolve virtual port number to actual port
     */
    private String resolvePortNumber(List<MegaDPortsHandler> handlers, String portNumber) {
        for (MegaDPortsHandler handler : handlers) {
            MegaDDeviceHandler bridge = handler.getBridgeDeviceHandler();
            if (bridge != null) {
                int resolved = bridge.megaDHardware.getInt(Integer.parseInt(portNumber));
                if (resolved != -1) {
                    return String.valueOf(resolved);
                }
            }
        }
        return portNumber;
    }

    /**
     * Process "started" status update
     */
    private void processStatusUpdate(List<MegaDPortsHandler> handlers) {
        handlers.stream().findFirst().ifPresent(handler -> {
            MegaDDeviceHandler bridge = handler.getBridgeDeviceHandler();
            if (bridge != null) {
                logger.debug("Processing status update for device {}", bridge.getThing().getUID());
                bridge.started();
            }
        });
    }

    /**
     * Process "all" parameter - update all ports at once
     */
    private void processAllPorts(List<MegaDPortsHandler> handlers, String allValue) {
        if (allValue.isEmpty()) {
            return;
        }

        String[] ports = allValue.split(";");
        Map<String, String> portStatusMap = buildPortStatusMap(ports);

        for (MegaDPortsHandler handler : handlers) {
            String portNumber = handler.getThing().getConfiguration().get("port").toString();
            String status = portStatusMap.get(portNumber);
            if (status != null) {
                logger.debug("Updating port {} with status: {}", portNumber, status);
                handler.updatePort(status);
            }
        }
    }

    /**
     * Build map of port -> status from array
     */
    private Map<String, String> buildPortStatusMap(String[] ports) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < ports.length; i++) {
            if (i < ports.length && ports[i] != null && !ports[i].isEmpty()) {
                map.put(String.valueOf(i), ports[i]);
            }
        }
        return map;
    }

    /**
     * Check if handler belongs to the device with given remote address
     */
    private boolean isHandlerForDevice(MegaDPortsHandler handler, String remoteAddr) {
        MegaDDeviceHandler bridge = handler.getBridgeDeviceHandler();
        if (bridge == null) {
            return false;
        }

        Object hostname = bridge.getThing().getConfiguration().get("hostname");
        return hostname != null && hostname.toString().equals(remoteAddr);
    }

    /**
     * Find handlers for a specific device
     */
    private List<MegaDPortsHandler> findHandlersForDevice(String remoteAddr) {
        return PORT_LISTENERS.stream().filter(handler -> isHandlerForDevice(handler, remoteAddr))
                .collect(Collectors.toList());
    }

    /**
     * Parse query string into structured parameters
     */
    private QueryParams parseQuery(String query) {
        QueryParams params = new QueryParams();

        for (String param : query.split("&")) {
            if (param.isEmpty()) {
                continue;
            }

            String[] kv = param.split("=", 2);
            String key = kv[0];
            String value = kv.length > 1 ? kv[1] : "";

            switch (key) {
                case PARAM_ALL -> params.all = value;
                case PARAM_PT -> params.port = value;
                case PARAM_ST -> params.hasStart = true;
                default -> params.other.put(key, value);
            }
        }

        return params;
    }

    /**
     * Send empty success response
     */
    private void sendEmptyResponse(HttpServletResponse resp) {
        resp.setContentType(MediaType.TEXT_HTML);
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setHeader("Content-Length", "0");
        resp.setHeader("Connection", "close");
        resp.setCharacterEncoding("utf-8");
    }

    /**
     * Register a port handler
     */
    public static void registerPortHandler(MegaDPortsHandler handler) {
        Logger logger = LoggerFactory.getLogger(MegaDHTTPCallback.class);
        if (!PORT_LISTENERS.contains(handler)) {
            PORT_LISTENERS.add(handler);
            logger.debug("Registered port handler for thing {}", handler.getThing().getUID());
        }
    }

    /**
     * Unregister a port handler
     */
    public static void unregisterPortHandler(MegaDPortsHandler handler) {
        Logger logger = LoggerFactory.getLogger(MegaDHTTPCallback.class);
        PORT_LISTENERS.remove(handler);
        logger.debug("Unregistered port handler for thing {}", handler.getThing().getUID());
    }

    /**
     * Query parameters holder
     */
    private static class QueryParams {
        String all = "";
        String port = "";
        boolean hasStart = false;
        Map<String, String> other = new HashMap<>();

        boolean hasAll() {
            return !all.isEmpty();
        }

        boolean hasPort() {
            return !port.isEmpty();
        }

        boolean hasStart() {
            return hasStart;
        }

        String getAll() {
            return all;
        }

        String getPort() {
            return port;
        }
    }
}
