# springboot-netty-demo

Spring Boot 3.x 整合 Netty 4.2 实战教程项目：在同一 JVM 中运行独立的 TCP 服务端（Netty，端口 9000）与 HTTP 管理接口（Spring MVC，端口 8080），采用自定义长度帧协议（4 字节大端序长度 + UTF-8 消息体），内置心跳检测与粘包拆包处理。

## 项目简介

- Netty TCP 服务随 Spring 容器启停（SmartLifecycle），无残留线程与端口
- LengthFieldBasedFrameDecoder / LengthFieldPrepender 处理粘包与拆包
- IdleStateHandler 实现 60 秒读空闲超时，自动清理失效连接
- 业务协议最小实现：`PING` → `PONG`，其他消息 → `ECHO: <消息>`
- 配套教程文档：`docs/SpringBoot3整合Netty实战教程.md`

## 技术栈及版本

| 组件 | 版本 | 说明 |
|---|---|---|
| JDK | 17 | Spring Boot 3.x 基线 |
| Spring Boot | 3.5.16 | parent BOM |
| Netty | 4.2.21.Final | 由 netty-bom 统一管理，4.1/4.2 不可混用 |
| 协议 | TCP + 长度帧 | 4 字节大端序长度 + UTF-8 消息体 |
| I/O 模型 | NIO | MultiThreadIoEventLoopGroup + NioIoHandler（4.2 推荐 API） |

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.6+

### 启动

```bash
mvn spring-boot:run
```

启动日志应出现：

```text
Tomcat started on port 8080 (http)
Netty TCP 服务已启动，监听 0.0.0.0:9000
Started NettyDemoApplication
```

### 验证

1. HTTP 状态接口：`curl http://localhost:8080/api/netty/status` → `{"service":"netty-tcp","running":true}`
2. 端口检查：`netstat -ano | findstr :9000`（LISTEN）
3. TCP 客户端测试（IDEA 运行 `NettyTestClient`，或命令行）：

```bash
mvn compile exec:java -Dexec.mainClass="com.example.nettydemo.client.NettyTestClient" -Dexec.args="127.0.0.1 9000 PING"
# 服务端响应：PONG
```

4. 自动化测试（含粘包、中文编解码端到端用例）：`mvn test`

## 工程结构

```text
springboot-netty-demo
├── pom.xml                                   # netty-bom 统一版本
├── docs/SpringBoot3整合Netty实战教程.md
└── src
    ├── main/java/com/example/nettydemo
    │   ├── NettyDemoApplication.java         # 启动类
    │   ├── config/NettyServerProperties.java # netty.server.* 配置绑定
    │   ├── server/NettyTcpServer.java        # TCP 服务端（SmartLifecycle）
    │   ├── server/NettyServerInitializer.java# Pipeline 装配
    │   ├── handler/NettyMessageHandler.java  # @Sharable 业务处理器
    │   ├── controller/NettyStatusController.java
    │   └── client/NettyTestClient.java       # 测试客户端
    ├── main/resources/application.yml
    └── test/java/com/example/nettydemo
        ├── NettyDemoApplicationTests.java   # 容器启动测试
        └── NettyTcpProtocolTests.java        # TCP 协议端到端测试
```

## 关键配置说明

- `netty.server.host`：监听网卡，默认 0.0.0.0
- `netty.server.port`：TCP 监听端口，默认 9000（与 HTTP 8080 相互独立）
- `netty.server.backlog`：待处理连接队列提示值，默认 1024
- `netty.server.max-frame-length`：单帧最大字节数（含 4 字节长度字段），默认 1 MiB
- `netty.server.reader-idle-seconds`：读空闲超时秒数，默认 60
- 帧协议：`Length(4B 大端) + Body(UTF-8)`，长度值不含长度字段本身

## 接口 / 功能清单

| 类型 | 路径 / 命令 | 说明 |
|---|---|---|
| HTTP | `GET /api/netty/status` | TCP 服务运行状态 |
| TCP 消息 | `PING` | 服务端响应 `PONG`（心跳命令） |
| TCP 消息 | 任意文本 | 服务端响应 `ECHO: <文本>` |
| 生命周期 | 容器启动 / 关闭 | SmartLifecycle 自动 start/stop，端口随停释放 |
| 空闲检测 | 60 秒无入站数据 | READER_IDLE 触发，服务端主动关闭连接 |

## 踩坑备忘

1. **4.1 与 4.2 不能共存**：通过 netty-bom 统一版本；引入 WebFlux/Reactor Netty 等 Netty 依赖框架后，用 `mvn dependency:tree -Dincludes=io.netty` 复查
2. **长度按字节算，不是 String.length()**：中文 UTF-8 占多字节，帧长度必须用编码后的字节数
3. **NioEventLoopGroup 在 4.2 已弃用**：新代码用 `MultiThreadIoEventLoopGroup + NioIoHandler.newFactory()`
4. **@Sharable 的前提**：Handler 单例被所有 Channel 复用，不得保存连接级可变状态；有状态就每连接 new 或用 Channel Attribute
5. **不要在 I/O 线程做阻塞操作**：慢 SQL/HTTP 调用会拖住同一 EventLoop 上的全部 Channel，迁到有界业务线程池
6. **SslHandler 放帧解码器之前**：先解密再按长度切帧

## 生产化建议

- 设备注册/登录鉴权（设备 ID + 密钥/签名/证书），不要仅凭 IP 认设备
- 跨公网启用 TLS（或双向 mTLS）
- 单 IP/单设备连接数限制、连接速率限制
- deviceId 与 Channel 的连接管理（重复登录踢旧连接、断线重连）
- 有界业务线程池 + WriteBufferWaterMark 背压，`isWritable()` 为 false 时暂缓发送
- 监控活动连接数、吞吐、延迟、写缓冲、直接内存与线程数