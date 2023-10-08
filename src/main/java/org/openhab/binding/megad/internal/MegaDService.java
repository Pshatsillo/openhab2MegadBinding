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
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.link.ItemChannelLinkRegistry;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;

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
    // private final Logger logger = LoggerFactory.getLogger(MegaDService.class);
    private final HttpClient httpClient;
    private final HttpService httpService;
    protected static @Nullable EventPublisher eventPublisher;

    @Activate
    public MegaDService(final @Reference HttpClientFactory httpClientFactory,
            final @Reference ItemRegistry itemRegistry, final @Reference EventPublisher eventPublisher,
            final @Reference HttpService httpService, final @Reference ThingRegistry things,
            final @Reference ItemChannelLinkRegistry link) {
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
    }

    @Override
    public Set<String> getSubscribedEventTypes() {
        return Set.of(ItemStateEvent.TYPE);
    }

    @Override
    public void receive(Event event) {
    }
}
