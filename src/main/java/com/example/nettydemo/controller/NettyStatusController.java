package com.example.nettydemo.controller;

import com.example.nettydemo.server.NettyTcpServer;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * HTTP 管理接口：查看 TCP 服务运行状态
 */
@RestController
public class NettyStatusController {

    private final NettyTcpServer nettyTcpServer;

    public NettyStatusController(NettyTcpServer nettyTcpServer) {
        this.nettyTcpServer = nettyTcpServer;
    }

    @GetMapping("/api/netty/status")
    public Map<String, Object> status() {
        return Map.of(
            "running", nettyTcpServer.isRunning(),
            "service", "netty-tcp"
        );
    }
}
