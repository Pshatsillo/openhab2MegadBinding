/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.megad.handler.MegaDPortsHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HTTP callback service for Megad
 *
 * @author Petr Shatsillo - Initial contribution
 *
 */
@NonNullByDefault
public class MegaDHTTPCallback extends HttpServlet {
    private final Logger logger = LoggerFactory.getLogger(MegaDHTTPCallback.class);
    @Serial
    private static final long serialVersionUID = -2725161358635927815L;
    public static List<MegaDPortsHandler> portListener = new ArrayList<>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        logger.debug("request from {} is: {}", req.getRemoteAddr(), req.getQueryString());
        String query = req.getQueryString();
        resp.setContentType(MediaType.TEXT_PLAIN);
        resp.setCharacterEncoding("utf-8");
        resp.setStatus(HttpServletResponse.SC_OK);
        if (query != null) {
            if (query.contains("all=")) {
                // logger.debug("loop incoming");
                String[] prm = query.split("[&]");
                List<MegaDPortsHandler> portListener = MegaDHTTPCallback.portListener;
                for (MegaDPortsHandler port : portListener) {
                    if (Objects.requireNonNull(port.bridgeDeviceHandler).getThing().getConfiguration().get("hostname")
                            .toString().equals(req.getRemoteAddr())) {
                        for (String parameters : prm) {
                            if (parameters.contains("all")) {
                                String portsStatus = parameters.split("=")[1];
                                String[] ports = portsStatus.split(";");
                                // logger.debug("split is {}", (Object) ports);
                                for (int i = 0; i < ports.length; i++) {
                                    if (port.getThing().getConfiguration().get("port").toString()
                                            .equals(String.valueOf(i))) {
                                        port.updatePort(ports[i]);
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                List<MegaDPortsHandler> portListener = MegaDHTTPCallback.portListener;
                boolean started = false;
                for (MegaDPortsHandler port : portListener) {
                    if (Objects.requireNonNull(port.bridgeDeviceHandler).getThing().getConfiguration().get("hostname")
                            .toString().equals(req.getRemoteAddr())) {
                        if (query.contains("st=1")) {
                            if (!started) {
                                Objects.requireNonNull(port.bridgeDeviceHandler).started();
                                started = true;
                            }
                        } else {
                            String[] prm = query.split("[&]");
                            for (String parameters : prm) {
                                if (parameters.contains("pt")) {
                                    String portNumber = parameters.split("=")[1];
                                    if (port.getThing().getConfiguration().get("port").toString().equals(portNumber)) {
                                        logger.debug("port is {} at device {}",
                                                port.getThing().getConfiguration().get("port"),
                                                Objects.requireNonNull(port.bridgeDeviceHandler).getThing()
                                                        .getConfiguration().get("hostname").toString());
                                        port.updatePort(query);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
