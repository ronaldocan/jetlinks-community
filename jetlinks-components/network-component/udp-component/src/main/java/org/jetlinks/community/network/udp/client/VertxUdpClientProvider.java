package org.jetlinks.community.network.udp.client;

import io.vertx.core.Vertx;
import io.vertx.core.datagram.DatagramSocket;
import io.vertx.core.datagram.DatagramSocketOptions;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.community.network.*;
import org.jetlinks.community.network.security.CertificateManager;
import org.jetlinks.community.network.udp.parser.PayloadParserBuilder;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

@Component
@Slf4j
public class VertxUdpClientProvider implements NetworkProvider<UdpClientProperties> {

    private final PayloadParserBuilder payloadParserBuilder;

    private final Vertx vertx;

    public VertxUdpClientProvider(Vertx vertx, PayloadParserBuilder payloadParserBuilder) {
        this.vertx = vertx;
        this.payloadParserBuilder = payloadParserBuilder;
    }

    @Nonnull
    @Override
    public NetworkType getType() {
        return DefaultNetworkType.UDP;
    }

    @Nonnull
    @Override
    public VertxUdpClient createNetwork(@Nonnull UdpClientProperties properties) {
        VertxUdpClient client = new VertxUdpClient(properties.getId());
        initClient(client, properties);
        return client;
    }

    @Override
    public void reload(@Nonnull Network network, @Nonnull UdpClientProperties properties) {
        initClient(((VertxUdpClient) network), properties);
    }

    public void initClient(VertxUdpClient client, UdpClientProperties properties) {
        DatagramSocket socket = vertx.createDatagramSocket(new DatagramSocketOptions());
        client.setDatagramSocket(socket);
        socket.listen(properties.getPort(), properties.getHost(), asyncResult -> {
            if (asyncResult.succeeded()) {
                socket.handler(packet -> {
                    // Do something with the packet
                    client.setSocket(socket, packet);
                });
            } else {
                log.error("Listen failed" + asyncResult.cause());
            }
        });
    }

    @Nullable
    @Override
    public ConfigMetadata getConfigMetadata() {
        // TODO: 2019/12/19
        return null;
    }

    @Nonnull
    @Override
    public Mono<UdpClientProperties> createConfig(@Nonnull NetworkProperties properties) {
        return Mono.defer(() -> {
            UdpClientProperties config = FastBeanCopier.copy(properties.getConfigurations(), new UdpClientProperties());
            config.setId(properties.getId());
            return Mono.just(config);
        });
    }
}
