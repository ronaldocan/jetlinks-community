package org.jetlinks.community.network.udp.device.sesssion;

import lombok.Getter;
import org.jetlinks.community.network.udp.UdpMessage;
import org.jetlinks.community.network.udp.client.UdpClient;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.message.codec.EncodedMessage;
import org.jetlinks.core.message.codec.Transport;
import org.jetlinks.core.server.session.DeviceSession;
import reactor.core.publisher.Mono;

public class UnknownUdpDeviceSession implements DeviceSession {

    @Getter
    private final String id;

    private final UdpClient client;

    @Getter
    private final Transport transport;
    private final long connectTime = System.currentTimeMillis();
    private long lastPingTime = System.currentTimeMillis();

    public UnknownUdpDeviceSession(String id, UdpClient client, Transport transport) {
        this.id = id;
        this.client = client;
        this.transport = transport;
    }

    @Override
    public String getDeviceId() {
        return "unknown";
    }

    @Override
    public DeviceOperator getOperator() {
        return null;
    }

    @Override
    public long lastPingTime() {
        return lastPingTime;
    }

    @Override
    public long connectTime() {
        return connectTime;
    }

    @Override
    public Mono<Boolean> send(EncodedMessage encodedMessage) {
        return client.send(new UdpMessage(encodedMessage.getPayload()));
    }

    @Override
    public void close() {
        client.shutdown();
    }

    @Override
    public void ping() {
        lastPingTime = System.currentTimeMillis();
    }

    @Override
    public boolean isAlive() {
        return client.isAlive();
    }

    @Override
    public void onClose(Runnable call) {
        client.onDisconnect(call);
    }
}
