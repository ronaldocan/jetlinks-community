package org.jetlinks.community.network.udp.executor;

import org.jetlinks.community.network.udp.UdpMessage;
import org.jetlinks.rule.engine.api.RuleData;
import org.jetlinks.rule.engine.api.RuleDataCodec;
import org.jetlinks.rule.engine.api.RuleDataCodecs;
import org.jetlinks.rule.engine.executor.PayloadType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

public class UdpMessageCodec implements RuleDataCodec<UdpMessage> {

    private static final UdpMessageCodec instance = new UdpMessageCodec();

    static {
        RuleDataCodecs.register(UdpMessage.class, instance);
    }

    static void register() {
    }

    @Override
    public Object encode(UdpMessage data, Feature... features) {
        PayloadType payloadType = Feature.find(PayloadType.class, features)
                .orElse(PayloadType.BINARY);

        Map<String, Object> map = new HashMap<>();
        map.put("payload", payloadType.read(data.getPayload()));
        map.put("payloadType", payloadType.name());

        return map;
    }

    @Override
    public Flux<UdpMessage> decode(RuleData data, Feature... features) {
        return data
                .dataToMap()
                .flatMap(map -> {
                    Object payload = map.get("payload");
                    if (payload == null) {
                        return Mono.empty();
                    }
                    PayloadType payloadType = Feature
                            .find(PayloadType.class, features)
                            .orElse(PayloadType.BINARY);

                    UdpMessage message = new UdpMessage();
                    message.setPayload(payloadType.write(payload));
                    //message.setPayloadType(MessagePayloadType.valueOf(payloadType.name()));
                    return Mono.just(message);
                });
    }
}
