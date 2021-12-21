package org.jetlinks.community.network.udp.client;

import lombok.Getter;
import lombok.Setter;

/**
 * @Description
 * @Date 2021/12/16 15:09
 * @Author zhengguican
 */
@Getter
@Setter
public class UdpClientProperties {
    private String id;
    private String clientId;
    private String host;
    private int port;
}
