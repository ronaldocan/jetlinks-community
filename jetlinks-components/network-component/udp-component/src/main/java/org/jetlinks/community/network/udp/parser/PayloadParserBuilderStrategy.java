package org.jetlinks.community.network.udp.parser;

import org.jetlinks.community.ValueObject;

public interface PayloadParserBuilderStrategy {
    PayloadParserType getType();

    PayloadParser build(ValueObject config);
}
