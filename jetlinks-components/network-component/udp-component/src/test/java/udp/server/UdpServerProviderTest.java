package udp.server;

import io.vertx.core.Vertx;
import io.vertx.core.datagram.DatagramSocketOptions;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.community.network.udp.UdpMessage;
import org.jetlinks.community.network.udp.client.UdpClient;
import org.jetlinks.community.network.udp.parser.DefaultPayloadParserBuilder;
import org.jetlinks.community.network.udp.parser.PayloadParserType;
import org.jetlinks.community.network.udp.server.UdpServer;
import org.jetlinks.community.network.udp.server.UdpServerProperties;
import org.jetlinks.community.network.udp.server.UdpServerProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
/**
 * @Description
 * @Date 2021/12/21 9:40
 * @Author zhengguican
 */
@Slf4j
public class UdpServerProviderTest {
    static UdpServer udpServer;

    @BeforeAll
    static void init() {
        UdpServerProperties properties = UdpServerProperties.builder()
            .id("test")
            .port(8011)
            .options(new DatagramSocketOptions())
            .host("localhost")
            .parserType(PayloadParserType.FIXED_LENGTH)
            .parserConfiguration(Collections.singletonMap("size", 5))
            .build();
        UdpServerProvider provider = new UdpServerProvider((id) -> Mono.empty(), Vertx.vertx(), new DefaultPayloadParserBuilder());
        udpServer = provider.createNetwork(properties);
    }


    @Test
    void test() {
        udpServer.handleConnection()
            .flatMap(UdpClient::subscribe)
            .map(UdpMessage::getPayload)
            .map(payload -> payload.toString(StandardCharsets.UTF_8))
            .take(2)
            .as(StepVerifier::create)
            .verifyComplete();
    }
}
