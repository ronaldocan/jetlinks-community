package udp.client;

import io.vertx.core.Vertx;
import org.jetlinks.community.network.udp.UdpMessage;
import org.jetlinks.community.network.udp.client.UdpClientProperties;
import org.jetlinks.community.network.udp.client.VertxUdpClientProvider;
import org.jetlinks.community.network.udp.parser.DefaultPayloadParserBuilder;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;

class VertxUdpClientProviderTest {


    @Test
    void test() {
        Vertx vertx = Vertx.vertx();

        vertx.createNetServer()
                .connectHandler(socket -> {
                    socket.write("tes");
                    socket.write("ttest");
                })
                .listen(12311);
        VertxUdpClientProvider provider = new VertxUdpClientProvider(vertx, new DefaultPayloadParserBuilder());
        UdpClientProperties properties = new UdpClientProperties();
        properties.setHost("127.0.0.1");
        properties.setPort(12311);
        provider.createNetwork(properties)
                .subscribe()
                .map(UdpMessage::getPayload)
                .map(buf -> buf.toString(StandardCharsets.UTF_8))
                .take(2)
                .as(StepVerifier::create)
                .expectNext("test", "test")
                .verifyComplete();
    }

}