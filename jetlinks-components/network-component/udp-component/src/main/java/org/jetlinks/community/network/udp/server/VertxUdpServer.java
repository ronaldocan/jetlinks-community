package org.jetlinks.community.network.udp.server;

import io.vertx.core.datagram.DatagramPacket;
import io.vertx.core.datagram.DatagramSocket;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.community.network.DefaultNetworkType;
import org.jetlinks.community.network.NetworkType;
import org.jetlinks.community.network.udp.client.UdpClient;
import org.jetlinks.community.network.udp.client.VertxUdpClient;
import org.jetlinks.community.network.udp.parser.PayloadParser;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.time.Duration;
import java.util.Collection;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author zhengguican
 * @since 1.0
 **/
@Slf4j
public class VertxUdpServer implements UdpServer {

    @Getter
    private final String id;
    private final EmitterProcessor<UdpClient> processor = EmitterProcessor.create(false);
    private final FluxSink<UdpClient> sink = processor.sink(FluxSink.OverflowStrategy.BUFFER);
    Collection<DatagramSocket> udpSockets;
    private Supplier<PayloadParser> parserSupplier;
    @Setter
    @Getter
    private long keepAliveTimeout = Duration.ofMinutes(10).toMillis();
    private volatile long lastKeepAliveTime = System.currentTimeMillis();

    public VertxUdpServer(String id) {
        this.id = id;
    }

    private void execute(Runnable runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            log.warn("close udp server error", e);
        }
    }

    public void setParserSupplier(Supplier<PayloadParser> parserSupplier) {
        this.parserSupplier = parserSupplier;
    }

    public void setServer(Collection<DatagramSocket> servers) {
        if (this.udpSockets != null && !this.udpSockets.isEmpty()) {
            shutdown();
        }
        this.udpSockets = servers;
    }

    @Override
    public void acceptUdpConnection(DatagramSocket socket, DatagramPacket packet) {
        if (!processor.hasDownstreams()) {
            log.warn("not handler for udp client[{}]", socket.localAddress());
            socket.close();
            return;
        }
        VertxUdpClient client = new VertxUdpClient(id + "_" + packet.sender().host());
        try {
            socket.exceptionHandler(err -> {
                log.error("udp server client [{}] error", packet.sender().host(), err);
            }).close((nil) -> {
                log.debug("udp server client [{}] closed", packet.sender().host());
                client.shutdown();
            });
            socket.exceptionHandler(err -> {
                log.error("udp connect error");
            });
            client.setRecordParser(parserSupplier.get());
            client.setSocket(socket,packet);
            sink.next(client);
            log.debug("accept udp client [{}] connection", packet.sender().host());
        } catch (Exception e) {
            log.error("create udp server client error", e);
            client.shutdown();
        }
    }

    @Override
    public NetworkType getType() {
        return DefaultNetworkType.UDP;
    }

    @Override
    public Flux<UdpClient> handleConnection() {
        return processor
            .map(Function.identity());
    }

    @Override
    public void shutdown() {
        if (null != udpSockets) {
            for (DatagramSocket udpServer : udpSockets) {
                execute(udpServer::close);
            }
            udpSockets = null;
        }
    }

    @Override
    public boolean isAlive() {
        return keepAliveTimeout < 0 || System.currentTimeMillis() - lastKeepAliveTime < keepAliveTimeout;
    }

    @Override
    public boolean isAutoReload() {
        return false;
    }
}
