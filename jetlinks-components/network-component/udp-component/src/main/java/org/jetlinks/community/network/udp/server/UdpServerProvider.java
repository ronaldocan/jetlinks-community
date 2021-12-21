package org.jetlinks.community.network.udp.server;

import io.vertx.core.Vertx;
import io.vertx.core.datagram.DatagramSocket;
import io.vertx.core.datagram.DatagramSocketOptions;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetServerOptions;
import io.vertx.core.net.SocketAddress;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.community.network.*;
import org.jetlinks.community.network.security.CertificateManager;
import org.jetlinks.community.network.security.VertxKeyCertTrustOptions;
import org.jetlinks.community.network.udp.parser.PayloadParserBuilder;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Udp服务提供商
 *
 * @author zhouhao
 */
@Component
@Slf4j
public class UdpServerProvider implements NetworkProvider<UdpServerProperties> {

    private final CertificateManager certificateManager;

    private final Vertx vertx;

    private final PayloadParserBuilder payloadParserBuilder;

    public UdpServerProvider(CertificateManager certificateManager, Vertx vertx, PayloadParserBuilder payloadParserBuilder) {
        this.certificateManager = certificateManager;
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
    public VertxUdpServer createNetwork(@Nonnull UdpServerProperties properties) {
        VertxUdpServer UdpServer = new VertxUdpServer(properties.getId());
        initUdpServer(UdpServer, properties);
        return UdpServer;
    }

    /**
     * Udp服务初始化
     *
     * @param udpServer  Udp服务
     * @param properties Udp配置
     */
    private void initUdpServer(VertxUdpServer udpServer, UdpServerProperties properties) {
        int instance = Math.max(2, properties.getInstance());
        List<DatagramSocket> instances = new ArrayList<>(instance);
        instances.add(vertx.createDatagramSocket(properties.getOptions()));
        // 根据解析类型配置数据解析器
        payloadParserBuilder.build(properties.getParserType(), properties);
        udpServer.setParserSupplier(() -> payloadParserBuilder.build(properties.getParserType(), properties));
        udpServer.setKeepAliveTimeout(properties.getLong("keepAliveTimeout", Duration.ofMinutes(10).toMillis()));
        for (DatagramSocket datagramSocket : instances) {
            datagramSocket.listen(properties.getPort(),properties.getHost(), result -> {
                if (result.succeeded()) {
                    log.info("Udp server startup on {}", result.result().localAddress().port());
                    datagramSocket.handler(packet -> {
                        udpServer.acceptUdpConnection(datagramSocket, packet);
                    });
                } else {
                    log.error("startup Udp server error", result.cause());
                }
            });
        }
        udpServer.setServer(instances);
    }

    @Override
    public void reload(@Nonnull Network network, @Nonnull UdpServerProperties properties) {
        VertxUdpServer UdpServer = ((VertxUdpServer) network);
        UdpServer.shutdown();
        initUdpServer(UdpServer, properties);
    }

    @Nullable
    @Override
    public ConfigMetadata getConfigMetadata() {
        return null;
    }

    @Nonnull
    @Override
    public Mono<UdpServerProperties> createConfig(@Nonnull NetworkProperties properties) {
        return Mono.defer(() -> {
            UdpServerProperties config = FastBeanCopier.copy(properties.getConfigurations(), new UdpServerProperties());
            config.setId(properties.getId());
            if (config.getOptions() == null) {
                config.setOptions(new DatagramSocketOptions());
            }
            return Mono.just(config);
        });
    }
}
