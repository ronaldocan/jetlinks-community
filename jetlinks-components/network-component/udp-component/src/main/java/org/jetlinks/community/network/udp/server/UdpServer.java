package org.jetlinks.community.network.udp.server;

import io.vertx.core.datagram.DatagramPacket;
import io.vertx.core.datagram.DatagramSocket;
import org.jetlinks.community.network.Network;
import org.jetlinks.community.network.udp.client.UdpClient;
import reactor.core.publisher.Flux;

/**
 * @Description
 * @Date 2021/12/16 10:37
 * @Author zhengguican
 */
public interface UdpServer extends Network {

    /**
     * 订阅客户端连接
     *
     * @return 客户端流
     * @see UdpClient
     */
    Flux<UdpClient> handleConnection();

    /**
     * 关闭服务端
     */
    @Override
    void shutdown();

    void acceptUdpConnection(DatagramSocket socket, DatagramPacket packet);
}
