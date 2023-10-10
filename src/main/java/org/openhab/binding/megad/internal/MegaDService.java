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

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.openhab.core.events.Event;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.events.EventSubscriber;
import org.openhab.core.io.net.http.HttpClientFactory;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.events.ItemStateEvent;
import org.openhab.core.net.HttpServiceUtil;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.link.ItemChannelLinkRegistry;
import org.osgi.framework.Constants;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HTTP Service for Megad
 *
 * @author Petr Shatsillo - Initial contribution
 *
 */
@NonNullByDefault
@Component(service = { MegaDService.class,
        EventSubscriber.class }, configurationPid = "org.openhab.megad", property = Constants.SERVICE_PID
                + "=org.openhab.megad")
public class MegaDService implements EventSubscriber {
    private final Logger logger = LoggerFactory.getLogger(MegaDService.class);
    private final HttpClient httpClient;
    private final HttpService httpService;
    protected static @Nullable EventPublisher eventPublisher;
    public static List<InetAddress> interfacesAddresses = new ArrayList<>();
    public static int port = 0;

    @Activate
    public MegaDService(final @Reference HttpClientFactory httpClientFactory,
            final @Reference ItemRegistry itemRegistry, final @Reference EventPublisher eventPublisher,
            final @Reference HttpService httpService, final @Reference ThingRegistry things,
            final @Reference ItemChannelLinkRegistry link, ComponentContext context) {
        this.httpClient = httpClientFactory.createHttpClient("megad");
        this.httpService = httpService;
        this.httpClient.setStopTimeout(0);
        this.httpClient.setMaxConnectionsPerDestination(200);
        this.httpClient.setConnectTimeout(30000);
        this.httpClient.setFollowRedirects(false);

        MegaDService.eventPublisher = eventPublisher;
        MegaDHTTPCallback megaDHTTPCallback = new MegaDHTTPCallback();
        try {
            this.httpService.registerServlet("/megad", megaDHTTPCallback, null,
                    this.httpService.createDefaultHttpContext());
        } catch (ServletException | NamespaceException ignored) {
        }

        Enumeration<NetworkInterface> networkInterfaces = null;
        try {
            networkInterfaces = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            logger.debug("Exception while getting network interfaces: '{}'", e.getMessage());
        }

        if (networkInterfaces != null) {
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface iface = networkInterfaces.nextElement();
                try {
                    if (iface.isUp() && !iface.isLoopback()) {
                        for (InterfaceAddress ifaceAddr : iface.getInterfaceAddresses()) {
                            if (ifaceAddr.getAddress() instanceof Inet4Address) {
                                interfacesAddresses.add(ifaceAddr.getAddress());
                            }
                        }
                    }
                } catch (SocketException e) {
                    logger.debug("Exception while getting information for network interface '{}': '{}'",
                            iface.getName(), e.getMessage());
                }
            }

            port = HttpServiceUtil.getHttpServicePort(context.getBundleContext());
            logger.warn("ip address is {} port {}", interfacesAddresses.stream().findFirst().get().getHostAddress(),
                    port);
        }
    }

    @Override
    public Set<String> getSubscribedEventTypes() {
        return Set.of(ItemStateEvent.TYPE);
    }

    @Override
    public void receive(Event event) {
    }
}
