package org.jetlinks.community.network.udp.parser;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.datagram.DatagramPacket;
import reactor.core.publisher.EmitterProcessor;
import reactor.core.publisher.Flux;

import java.util.function.Function;

/**
 * @Description
 * @Date 2021/12/17 9:47
 * @Author zhengguican
 */
public class RecordPayloadParser implements PayloadParser {


    EmitterProcessor<Buffer> processor = EmitterProcessor.create(false);


    @Override
    public void handle(Buffer buffer) {
        processor.onNext(buffer);
    }

    @Override
    public Flux<Buffer> handlePayload() {
        return processor.map(Function.identity());
    }

    @Override
    public void close() {
        processor.onComplete();
    }
}
