package org.jetlinks.community.network.udp.parser;

import org.jetlinks.community.ValueObject;

public interface PayloadParserBuilder {

    PayloadParser build(PayloadParserType type, ValueObject configuration);

}
