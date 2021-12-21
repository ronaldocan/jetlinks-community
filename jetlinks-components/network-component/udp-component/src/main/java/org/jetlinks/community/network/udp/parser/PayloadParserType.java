package org.jetlinks.community.network.udp.parser;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.dict.Dict;
import org.hswebframework.web.dict.EnumDict;

@Getter
@AllArgsConstructor
@Dict("tcp-payload-parser-type")
public enum PayloadParserType implements EnumDict<String> {

    DIRECT("不处理"),

    FIXED_LENGTH("固定长度"),

    DELIMITED("分隔符"),

    /**
     */
    SCRIPT("自定义脚本")
    ;

    private String text;
    @Override
    public String getValue() {
        return name();
    }
}
