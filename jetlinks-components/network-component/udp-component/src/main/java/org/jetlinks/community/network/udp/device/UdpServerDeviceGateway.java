package org.jetlinks.community.network.udp.device;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.logger.ReactiveLogger;
import org.jetlinks.community.gateway.DeviceGateway;
import org.jetlinks.community.gateway.monitor.DeviceGatewayMonitor;
import org.jetlinks.community.gateway.monitor.GatewayMonitors;
import org.jetlinks.community.network.DefaultNetworkType;
import org.jetlinks.community.network.NetworkType;
import org.jetlinks.community.network.udp.UdpMessage;
import org.jetlinks.community.network.udp.client.UdpClient;
import org.jetlinks.community.network.udp.device.sesssion.UdpDeviceSession;
import org.jetlinks.community.network.udp.device.sesssion.UnknownUdpDeviceSession;
import org.jetlinks.community.network.udp.server.UdpServer;
import org.jetlinks.community.network.utils.DeviceGatewayHelper;
import org.jetlinks.core.ProtocolSupport;
import org.jetlinks.core.ProtocolSupports;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.DeviceProductOperator;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.Message;
import org.jetlinks.core.message.codec.DefaultTransport;
import org.jetlinks.core.message.codec.EncodedMessage;
import org.jetlinks.core.message.codec.FromDeviceMessageContext;
import org.jetlinks.core.message.codec.Transport;
import org.jetlinks.core.server.DeviceGatewayContext;
import org.jetlinks.core.server.session.DeviceSession;
import org.jetlinks.core.server.session.DeviceSessionManager;
import org.jetlinks.supports.server.DecodedClientMessageHandler;
import reactor.core.Disposable;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;

/**
 * @Description
 * @Date 2021/12/16 14:10
 * @Author zhengguican
 */
@Slf4j(topic = "system.udp.gateway")
public class UdpServerDeviceGateway implements DeviceGateway {

    @Getter
    private final String id;

    /**
     * 维护所有创建的udp server
     */
    private final UdpServer udpServer;

    private final String protocol;

    private final ProtocolSupports supports;
    private final DeviceRegistry registry;
    private final DeviceSessionManager sessionManager;
    private final DeviceGatewayMonitor gatewayMonitor;
    private final EmitterProcessor<Message> processor = EmitterProcessor.create(false);
    private final FluxSink<Message> sink = processor.sink(FluxSink.OverflowStrategy.BUFFER);
    private final AtomicBoolean started = new AtomicBoolean();
    private final DeviceGatewayHelper helper;
    /**
     * 连接计数器
     */
    private final LongAdder counter = new LongAdder();
    /**
     * 数据流控开关
     */
    private Disposable disposable;

    public UdpServerDeviceGateway(String id,
                                  String protocol,
                                  ProtocolSupports supports,
                                  DeviceRegistry deviceRegistry,
                                  DecodedClientMessageHandler clientMessageHandler,
                                  DeviceSessionManager sessionManager,
                                  UdpServer udpServer) {
        this.gatewayMonitor = GatewayMonitors.getDeviceGatewayMonitor(id);
        this.id = id;
        this.protocol = protocol;
        this.registry = deviceRegistry;
        this.supports = supports;
        this.udpServer = udpServer;
        this.sessionManager = sessionManager;
        this.helper = new DeviceGatewayHelper(registry, sessionManager, clientMessageHandler);
    }

    @Override
    public String getId() {
        return null;
    }

    @Override
    public Transport getTransport() {
        return DefaultTransport.UDP;
    }

    @Override
    public NetworkType getNetworkType() {
        return DefaultNetworkType.UDP;
    }

    @Override
    public Flux<Message> onMessage() {
        return processor;
    }

    @Override
    public Mono<Void> startup() {
        return Mono.fromRunnable(this::doStart);
    }

    @Override
    public Mono<Void> pause() {
        return Mono.fromRunnable(() -> started.set(false));
    }

    @Override
    public Mono<Void> shutdown() {
        return Mono.fromRunnable(() -> {
            started.set(false);
            disposable.dispose();
            disposable = null;
        });
    }

    public Mono<ProtocolSupport> getProtocol() {
        return supports.getProtocol(protocol);
    }

    /**
     * 启动网关
     */
    private void doStart() {
        if (started.getAndSet(true) || disposable != null) {
            return;
        }
        disposable = udpServer
            .handleConnection()
            .publishOn(Schedulers.parallel())
            .flatMap(client -> new UdpConnection(client).accept(), Integer.MAX_VALUE)
            .onErrorContinue((err, obj) -> log.error(err.getMessage(), err))
            .subscriberContext(ReactiveLogger.start("network", udpServer.getId()))
            .subscribe(
                ignore -> {
                },
                error -> log.error(error.getMessage(), error)
            );
    }

    /**
     * udp 客户端连接
     */
    class UdpConnection implements DeviceGatewayContext {
        final UdpClient client;
        final AtomicReference<Duration> keepaliveTimeout = new AtomicReference<>();
        final AtomicReference<DeviceSession> sessionRef = new AtomicReference<>();
        final InetSocketAddress address;

        UdpConnection(UdpClient client) {
            this.client = client;
            this.address = client.getRemoteAddress();
            gatewayMonitor.totalConnection(counter.sum());
            client.onDisconnect(() -> {
                counter.decrement();
                gatewayMonitor.disconnected();
                gatewayMonitor.totalConnection(counter.sum());
            });
            gatewayMonitor.connected();
            DeviceSession session = sessionManager.getSession(client.getId());
            if (session == null) {
                session = new UnknownUdpDeviceSession(client.getId(), client, getTransport()) {
                    @Override
                    public Mono<Boolean> send(EncodedMessage encodedMessage) {
                        return super.send(encodedMessage).doOnSuccess(r -> gatewayMonitor.sentMessage());
                    }

                    @Override
                    public void setKeepAliveTimeout(Duration timeout) {
                        keepaliveTimeout.set(timeout);
                    }

                    @Override
                    public Optional<InetSocketAddress> getClientAddress() {
                        return Optional.of(address);
                    }
                };
            }
            sessionRef.set(session);
        }

        /**
         * 接收消息
         *
         * @return void
         */
        Mono<Void> accept() {
            return getProtocol()
                .flatMap(protocol -> protocol.onClientConnect(getTransport(), client, this))
                .then(
                    client
                        .subscribe()
                        .filter(udp -> started.get())
                        .publishOn(Schedulers.parallel())
                        .flatMap(this::handleUdpMessage)
                        .onErrorResume((err) -> {
                            log.error(err.getMessage(), err);
                            client.shutdown();
                            return Mono.empty();
                        })
                        .then()
                )
                .doOnCancel(client::shutdown);
        }

        /**
         * 处理udp消息 ==>> 设备消息
         *
         * @param message udp消息
         * @return void
         */
        Mono<Void> handleUdpMessage(UdpMessage message) {
            return getProtocol()
                .flatMap(pt -> pt.getMessageCodec(getTransport()))
                .flatMapMany(codec -> codec.decode(FromDeviceMessageContext.of(sessionRef.get(), message, registry)))
                .cast(DeviceMessage.class)
                .doOnNext(msg -> gatewayMonitor.receivedMessage())
                .flatMap(this::handleDeviceMessage)
                .doOnEach(ReactiveLogger.onError(err -> log.error("处理UDP[{}]消息失败:\n{}",
                    address,
                    message
                    , err)))
                .onErrorResume((err) -> Mono.fromRunnable(client::reset))
                .then();
        }

        /**
         * 处理设备消息
         *
         * @param message 设备消息
         * @return void
         */
        Mono<Void> handleDeviceMessage(DeviceMessage message) {
            if (processor.hasDownstreams()) {
                sink.next(message);
            }
            return helper
                .handleDeviceMessage(message,
                    device -> new UdpDeviceSession(device, client, getTransport(), gatewayMonitor),
                    DeviceGatewayHelper
                        .applySessionKeepaliveTimeout(message, keepaliveTimeout::get)
                        .andThen(session -> {
                            UdpDeviceSession deviceSession = session.unwrap(UdpDeviceSession.class);
                            deviceSession.setClient(client);
                            sessionRef.set(deviceSession);
                        }),
                    () -> log.warn("无法从udp[{}]消息中获取设备信息:{}", address, message)
                )
                .then();
        }

        @Override
        public Mono<DeviceOperator> getDevice(String deviceId) {
            return registry.getDevice(deviceId);
        }

        @Override
        public Mono<DeviceProductOperator> getProduct(String productId) {
            return registry.getProduct(productId);
        }

        @Override
        public Mono<Void> onMessage(DeviceMessage message) {
            return handleDeviceMessage(message);
        }
    }
}
