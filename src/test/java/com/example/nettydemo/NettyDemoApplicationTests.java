package com.example.nettydemo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 应用上下文测试：验证 Spring 容器与 Netty 服务可正常启动
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class NettyDemoApplicationTests {

    @Test
    void contextLoads() {
    }
}
