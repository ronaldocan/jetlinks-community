package org.jetlinks.community.network.udp.parser;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.datagram.DatagramPacket;
import reactor.core.publisher.Flux;

/**
 * @Description
 * @Date 2021/12/17 9:46
 * @Author zhengguican
 */
public interface PayloadParser {

    /**
     * 处理一个数据包
     *
     * @param buffer 数据包
     */
    void handle(Buffer buffer);

    /**
     * 订阅完整的数据包流,每一个元素为一个完整的数据包
     *
     * @return 完整数据包流
     */
    Flux<Buffer> handlePayload();

    /**
     * 关闭以释放相关资源
     */
    void close();

    /**
     * 重置规则
     */
    default void reset() {
    }
}
