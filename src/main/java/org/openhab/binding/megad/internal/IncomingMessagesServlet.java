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
package org.openhab.binding.megad.internal;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.openhab.binding.megad.handler.MegaDBridgeIncomingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link MegaDBridgeIncomingHandler} is responsible for creating things and thing
 * handlers.
 *
 * @author Petr Shatsillo - Initial contribution
 */
@NonNullByDefault
public class IncomingMessagesServlet extends AbstractHandler {
    Logger logger = LoggerFactory.getLogger(IncomingMessagesServlet.class);
    MegaDBridgeIncomingHandler megaDBridgeIncomingHandler;

    public IncomingMessagesServlet(MegaDBridgeIncomingHandler megaDBridgeIncomingHandler) {
        this.megaDBridgeIncomingHandler = megaDBridgeIncomingHandler;
    }

    // @SuppressWarnings("null")
    @Override
    public void handle(@Nullable String target, @Nullable Request baseRequest, @Nullable HttpServletRequest request,
            @Nullable HttpServletResponse response) throws IOException, ServletException {
        if (response != null) {
            response.setContentType("text/html");
            response.setStatus(HttpServletResponse.SC_OK);
            response.setHeader("Content-Length", "0");
            PrintWriter out = response.getWriter();
            out.println("");
            // out.close();
            response.setHeader("Connection", "close");
            if (baseRequest != null) {
                baseRequest.setHandled(true);
            }
            // baseRequest.logout();
            if (request != null) {
                logger.debug("Incoming {}", request.getParameterMap());

                megaDBridgeIncomingHandler.parseInput(request.getQueryString(), request.getRemoteHost());
            }
        }
    }
}
