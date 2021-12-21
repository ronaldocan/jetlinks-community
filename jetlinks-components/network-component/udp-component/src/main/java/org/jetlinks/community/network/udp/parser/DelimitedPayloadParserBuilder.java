package org.jetlinks.community.network.udp.parser;

import io.vertx.core.parsetools.RecordParser;
import org.apache.commons.lang.StringEscapeUtils;
import org.jetlinks.community.ValueObject;

public class DelimitedPayloadParserBuilder extends VertxPayloadParserBuilder {
    @Override
    public PayloadParserType getType() {
        return PayloadParserType.DELIMITED;
    }

    @Override
    protected RecordParser createParser(ValueObject config) {
        return RecordParser.newDelimited(StringEscapeUtils.unescapeJava(config.getString("delimited")
                .orElseThrow(() -> new IllegalArgumentException("delimited can not be null"))));
    }


}
