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
package org.openhab.binding.megad.handler;

import static org.openhab.binding.megad.discovery.MegaDDiscoveryService.megaDDeviceHandlerList;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.megad.MegaDConfiguration;
import org.openhab.binding.megad.dto.MegaDHardware;
import org.openhab.binding.megad.internal.MegaDService;
import org.openhab.binding.megad.internal.MegaHTTPResponse;
import org.openhab.binding.megad.internal.MegaHttpHelpers;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseBridgeHandler;
import org.openhab.core.types.Command;

/**
 * The {@link MegaDDeviceHandler} is responsible for creating things and thing
 * handlers.
 *
 * @author Petr Shatsillo - Initial contribution
 */
@NonNullByDefault
public class MegaDDeviceHandler extends BaseBridgeHandler {
    // private final Logger logger = LoggerFactory.getLogger(MegaDDeviceHandler.class);
    private @Nullable MegaDConfiguration config;
    private final MegaHttpHelpers httpHelper = new MegaHttpHelpers();
    private final ArrayList<MegaDRs485Handler> megaDRs485HandlerMap = new ArrayList<>();
    private @Nullable ScheduledFuture<?> refreshPollingJob;
    protected long lastRefresh = 0;

    public MegaDDeviceHandler(Bridge bridge) {
        super(bridge);
    }

    @Override
    public void initialize() {
        config = getConfigAs(MegaDConfiguration.class);
        final MegaDConfiguration config = this.config;
        if (config != null) {
            MegaDHardware mega = new MegaDHardware(config.hostname, config.password);
            MegaHTTPResponse response = httpHelper.request("http://" + config.hostname + "/" + config.password);
            if (response.getResponseCode() >= 400) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Wrong password");
            } else {
                mega.parse(response.getResponseResult());
                Map<String, String> properties = new HashMap<>();
                properties.put("Type:", mega.getType());
                properties.put("Firmware:", mega.getFirmware());
                properties.put("Actual Firmware:", mega.getActualFirmware());
                updateProperties(properties);
                String ip = config.hostname.substring(0, config.hostname.lastIndexOf("."));
                for (InetAddress address : MegaDService.interfacesAddresses) {
                    if (address.getHostAddress().startsWith(ip)) {
                        httpHelper.request("http://" + config.hostname + "/" + config.password + "/?cf=1&sip="
                                + MegaDService.interfacesAddresses.stream().findFirst().get().getHostAddress() + "%3A"
                                + MegaDService.port + "&sct=megad&srvt=0");
                    }
                }
                updateStatus(ThingStatus.ONLINE);
            }
        } else {
            updateStatus(ThingStatus.UNINITIALIZED, ThingStatusDetail.CONFIGURATION_ERROR, "Config is null");
        }
        Objects.requireNonNull(megaDDeviceHandlerList).add(this);
        final ScheduledFuture<?> refreshPollingJob = this.refreshPollingJob;
        if (refreshPollingJob == null || refreshPollingJob.isCancelled()) {
            this.refreshPollingJob = scheduler.scheduleWithFixedDelay(this::refresh, 0, 1000, TimeUnit.MILLISECONDS);
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE,
                    "Bridge for incoming connections not selected");
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
    }

    private void refresh() {
        try {
            Socket sck = new Socket(getThing().getConfiguration().get("hostname").toString(), 80);
            updateStatus(ThingStatus.ONLINE);
            sck.close();
        } catch (IOException e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, "Device not responding on ping");
            // logger.debug("proc error {}", e.getMessage());

        }

        long now = System.currentTimeMillis();
        ArrayList<MegaDRs485Handler> megaDRs485HandlerMap = this.megaDRs485HandlerMap;
        if (!megaDRs485HandlerMap.isEmpty()) {
            try {
                for (MegaDRs485Handler handler : megaDRs485HandlerMap) {
                    int interval = Integer.parseInt(handler.getThing().getConfiguration().get("refresh").toString());
                    if (interval != 0) {
                        if (now >= (handler.getLastRefresh() + (interval * 1000L))) {
                            handler.updateData();
                            handler.lastrefreshAdd(now);
                            try {
                                Thread.sleep(200);
                            } catch (InterruptedException ignored) {
                            }

                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }
    }

    // RS485
    public void registerMegaRs485Listener(MegaDRs485Handler megaDrs485Handler) {
        String rs485Address = megaDrs485Handler.getThing().getConfiguration().get("address").toString();
        ArrayList<MegaDRs485Handler> megaDRs485HandlerMap = this.megaDRs485HandlerMap;
        if (!megaDRs485HandlerMap.isEmpty()) {
            boolean isexist = false;
            for (MegaDRs485Handler handler : megaDRs485HandlerMap) {
                if (rs485Address.equals(handler.getThing().getConfiguration().get("address").toString())) {
                    isexist = true;
                }
            }
            if (!isexist) {
                this.megaDRs485HandlerMap.add(megaDrs485Handler);
            }
        } else {
            this.megaDRs485HandlerMap.add(megaDrs485Handler);
        }
    }

    public void unregisterMegadRs485Listener(MegaDRs485Handler megaDrs485Handler) {
        String rs485Address = megaDrs485Handler.getThing().getConfiguration().get("address").toString();
        megaDRs485HandlerMap.removeIf(
                handler -> rs485Address.equals(handler.getThing().getConfiguration().get("address").toString()));
    }

    @Override
    public void dispose() {
        ScheduledFuture<?> refreshPollingJob = this.refreshPollingJob;
        if (refreshPollingJob != null && !refreshPollingJob.isCancelled()) {
            refreshPollingJob.cancel(true);
            this.refreshPollingJob = null;
        }
        super.dispose();
    }
}
