package org.jetlinks.community.network.udp.executor;

import lombok.AllArgsConstructor;
import org.hswebframework.web.bean.FastBeanCopier;
import org.jetlinks.community.network.DefaultNetworkType;
import org.jetlinks.community.network.NetworkManager;
import org.jetlinks.community.network.udp.UdpMessage;
import org.jetlinks.community.network.udp.client.UdpClient;
import org.jetlinks.rule.engine.api.RuleDataCodecs;
import org.jetlinks.rule.engine.api.task.ExecutionContext;
import org.jetlinks.rule.engine.api.task.TaskExecutor;
import org.jetlinks.rule.engine.api.task.TaskExecutorProvider;
import org.jetlinks.rule.engine.defaults.AbstractTaskExecutor;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@AllArgsConstructor
@Component
public class UdpClientTaskExecutorProvider implements TaskExecutorProvider {

    private final NetworkManager clientManager;

    static {
        UdpMessageCodec.register();
    }

    @Override
    public String getExecutor() {
        return "udp-client";
    }

    @Override
    public Mono<TaskExecutor> createTask(ExecutionContext context) {
        return Mono.just(new TcpTaskExecutor(context));
    }

    class TcpTaskExecutor extends AbstractTaskExecutor {

        private UdpClientTaskConfiguration config;

        public TcpTaskExecutor(ExecutionContext context) {
            super(context);
            reload();
        }

        @Override
        public String getName() {
            return "Udp Client";
        }

        @Override
        public void reload() {
            config = FastBeanCopier.copy(context.getJob().getConfiguration(), new UdpClientTaskConfiguration());
            config.validate();
        }

        @Override
        public void validate() {
            FastBeanCopier
                .copy(context.getJob().getConfiguration(), new UdpClientTaskConfiguration())
                .validate();
        }

        @Override
        protected Disposable doStart() {
            Disposable.Composite disposable = Disposables.composite();
            disposable.add(context
                .getInput()
                .accept()
                .flatMap(data ->
                    clientManager.<UdpClient>getNetwork(DefaultNetworkType.UDP, config.getClientId())
                        .flatMapMany(client -> RuleDataCodecs
                            .getCodec(UdpMessage.class)
                            .map(codec -> codec.decode(data, config.getPayloadType())
                                .cast(UdpMessage.class)
                                .switchIfEmpty(Mono.fromRunnable(() -> context.getLogger().warn("can not decode rule data to tcp message:{}", data))))
                            .orElseGet(() -> Flux.just(new UdpMessage(config.getPayloadType().write(data.getData()))))
                            .flatMap(client::send)
                            .onErrorContinue((err, r) -> {
                                context.onError(err, data).subscribe();
                            })
                            .then()
                        )).subscribe()
            );
            return disposable;
        }
    }
}
