package org.jetlinks.community.network.udp.client;

import org.jetlinks.community.network.Network;
import org.jetlinks.community.network.udp.UdpMessage;
import org.jetlinks.core.server.ClientConnection;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

/**
 * @Description Udp客户端
 * @Date 2021/12/16 10:47
 * @Author zhengguican
 */
public interface UdpClient extends Network, ClientConnection {

    /**
     * 获取客户端远程地址
     *
     * @return 客户端远程地址
     */
    InetSocketAddress getRemoteAddress();

    /**
     * 订阅UDP消息,此消息是已经处理过粘拆包的完整消息
     *
     * @return UDP消息
     */
    Flux<UdpMessage> subscribe();

    /**
     * 向客户端发送数据
     *
     * @param message 数据对象
     * @return 发送结果
     */
    Mono<Boolean> send(UdpMessage message);

    /**
     * 断开连接
     *
     * @param disconnected
     */
    void onDisconnect(Runnable disconnected);

    /**
     * 重置
     */
    void reset();
}
