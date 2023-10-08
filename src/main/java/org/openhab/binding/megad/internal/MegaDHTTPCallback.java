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

import java.io.IOException;
import java.io.Serial;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;

import org.eclipse.jdt.annotation.NonNullByDefault;
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

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        logger.debug("request from {} is: {}", req.getRemoteAddr(), req.getQueryString());
        resp.setContentType(MediaType.TEXT_PLAIN);
        resp.setCharacterEncoding("utf-8");
        resp.setStatus(HttpServletResponse.SC_OK);
    }
}
