package com.example.nettydemo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Netty TCP 服务配置属性
 *
 * 对应 application.yml 中 netty.server.* 配置项
 */
@Component
@ConfigurationProperties(prefix = "netty.server")
public class NettyServerProperties {

    /** 监听网卡，0.0.0.0 表示监听所有 IPv4 网卡 */
    private String host = "0.0.0.0";
    /** TCP 监听端口 */
    private int port = 9000;
    /** 待处理连接队列长度的提示值 */
    private int backlog = 1024;
    /** 单帧最大字节数（含 4 字节长度字段） */
    private int maxFrameLength = 1024 * 1024;
    /** 读空闲超时秒数，超时未收到数据则关闭连接 */
    private int readerIdleSeconds = 60;

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public int getBacklog() { return backlog; }
    public void setBacklog(int backlog) { this.backlog = backlog; }

    public int getMaxFrameLength() { return maxFrameLength; }
    public void setMaxFrameLength(int maxFrameLength) { this.maxFrameLength = maxFrameLength; }

    public int getReaderIdleSeconds() { return readerIdleSeconds; }
    public void setReaderIdleSeconds(int seconds) { this.readerIdleSeconds = seconds; }
}
