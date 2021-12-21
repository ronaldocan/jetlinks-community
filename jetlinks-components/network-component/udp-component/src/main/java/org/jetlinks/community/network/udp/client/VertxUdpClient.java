package org.jetlinks.community.network.udp.client;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.datagram.DatagramPacket;
import io.vertx.core.datagram.DatagramSocket;
import io.vertx.core.net.SocketAddress;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.community.network.DefaultNetworkType;
import org.jetlinks.community.network.NetworkType;
import org.jetlinks.community.network.udp.UdpMessage;
import org.jetlinks.community.network.udp.parser.PayloadParser;
import org.jetlinks.core.message.codec.EncodedMessage;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.net.SocketException;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

@Slf4j
public class VertxUdpClient implements UdpClient {

    @Getter
    private final String id;
    private final List<Runnable> disconnectListener = new CopyOnWriteArrayList<>();
    private final EmitterProcessor<UdpMessage> processor = EmitterProcessor.create(false);
    private final FluxSink<UdpMessage> sink = processor.sink(FluxSink.OverflowStrategy.BUFFER);
    public volatile DatagramSocket datagramSocket;
    public DatagramSocket socket;
    volatile PayloadParser payloadParser;
    @Setter
    private long keepAliveTimeoutMs = Duration.ofMinutes(10).toMillis();
    private volatile long lastKeepAliveTime = System.currentTimeMillis();
    private String host;

    private int port;


    public VertxUdpClient(String id) {
        this.id = id;
    }

    @Override
    public InetSocketAddress address() {
        return getRemoteAddress();
    }

    @Override
    public Mono<Void> sendMessage(EncodedMessage message) {
        return Mono
            .<Void>create((sink) -> {
                if (socket == null) {
                    sink.error(new SocketException("socket closed"));
                    return;
                }
                Buffer buffer = Buffer.buffer(message.getPayload());
                socket.send(buffer, port, host, r -> {
                    if (r.succeeded()) {
                        sink.success();
                    } else {
                        sink.error(r.cause());
                    }
                });
            });
    }

    @Override
    public Flux<EncodedMessage> receiveMessage() {
        return this
            .subscribe()
            .cast(EncodedMessage.class);
    }

    @Override
    public void disconnect() {
        shutdown();
    }

    @Override
    public boolean isAlive() {
        return socket != null && (keepAliveTimeoutMs < 0 || System.currentTimeMillis() - lastKeepAliveTime < keepAliveTimeoutMs);
    }

    @Override
    public boolean isAutoReload() {
        return true;
    }

    protected void received(UdpMessage message) {
        lastKeepAliveTime = System.currentTimeMillis();
        if (processor.getPending() > processor.getBufferSize() / 2) {
            log.warn("UDP [{}] message pending {} ,drop message:{}", processor.getPending(), getRemoteAddress(), message.toString());
            return;
        }
        sink.next(message);
    }

    private void execute(Runnable runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            log.warn("close UDP client error", e);
        }
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        if (null == socket) {
            return null;
        }
        return new InetSocketAddress(host, port);
    }

    @Override
    public Flux<UdpMessage> subscribe() {
        return processor
            .map(Function.identity());
    }

    @Override
    public NetworkType getType() {
        return DefaultNetworkType.UDP;
    }

    @Override
    public void shutdown() {

    }

    public void setDatagramSocket(DatagramSocket datagramSocket) {
        if (this.datagramSocket != null && this.datagramSocket != datagramSocket) {
            this.datagramSocket.close();
        }
        this.datagramSocket = datagramSocket;
    }

    public void setSocket(DatagramSocket socket, DatagramPacket datagramPacket) {
        synchronized (this) {
            Objects.requireNonNull(payloadParser);
            if (this.socket != null && this.socket != socket) {
                this.socket.close();
            }
            this.host = datagramPacket.sender().host();
            this.port = datagramPacket.sender().port();
            this.socket = socket;
            this.socket
                .handler(clientSocket -> {
                    payloadParser.handle(clientSocket.data());
                    if (this.socket != socket) {
                        socket.close();
                    }
                }).close(v -> shutdown());
        }
    }

    @Override
    public Mono<Boolean> send(UdpMessage message) {
        return sendMessage(message)
            .thenReturn(true);
    }

    @Override
    public void onDisconnect(Runnable disconnected) {
        disconnectListener.add(disconnected);
    }

    @Override
    public void reset() {
        if (null != payloadParser) {
            payloadParser.reset();
        }
    }

    public void setRecordParser(PayloadParser payloadParser) {
        synchronized (this) {
            if (null != this.payloadParser && this.payloadParser != payloadParser) {
                this.payloadParser.close();
            }
            this.payloadParser = payloadParser;
            this.payloadParser
                .handlePayload()
                .onErrorContinue((err, res) -> {
                    log.error(err.getMessage(), err);
                })
                .subscribe(buffer -> received(new UdpMessage(buffer.getByteBuf())));
        }
    }
}
