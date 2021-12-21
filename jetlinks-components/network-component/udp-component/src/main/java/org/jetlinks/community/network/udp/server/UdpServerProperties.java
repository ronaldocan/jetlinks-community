package org.jetlinks.community.network.udp.server;

import io.vertx.core.datagram.DatagramSocketOptions;
import io.vertx.core.net.NetServerOptions;
import io.vertx.core.net.SocketAddress;
import lombok.*;
import org.jetlinks.community.ValueObject;
import org.jetlinks.community.network.udp.parser.PayloadParserType;
import org.jetlinks.rule.engine.executor.PayloadType;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * @author zhengguican
 * @since 1.0
 **/
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UdpServerProperties implements ValueObject {

    private String id;

    private DatagramSocketOptions options;

    private PayloadType payloadType;

    private PayloadParserType parserType;

    private Map<String, Object> parserConfiguration = new HashMap<>();

    private String host;

    private int port;

    private boolean ssl;

    //服务实例数量(线程数)
    private int instance = Runtime.getRuntime().availableProcessors();

    private String certId;

    public SocketAddress createSocketAddress() {
        if (StringUtils.isEmpty(host)) {
            host = "localhost";
        }
        return SocketAddress.inetSocketAddress(port, host);
    }

    @Override
    public Map<String, Object> values() {
        return parserConfiguration;
    }
}
