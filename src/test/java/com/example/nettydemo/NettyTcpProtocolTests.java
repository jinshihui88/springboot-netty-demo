package com.example.nettydemo;

import com.example.nettydemo.config.NettyServerProperties;
import com.example.nettydemo.server.NettyTcpServer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TCP 协议端到端集成测试
 *
 * 使用原生 Socket 按「4 字节大端序长度 + UTF-8 消息体」帧格式与服务端通信，
 * 覆盖：普通回显、心跳命令、中文编解码、粘包（一次写入两帧）。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class NettyTcpProtocolTests {

    @Autowired
    private NettyTcpServer nettyTcpServer;

    @Autowired
    private NettyServerProperties properties;

    @Test
    void serverIsRunning() {
        assertTrue(nettyTcpServer.isRunning(), "Netty TCP 服务应随容器启动");
    }

    @Test
    void echoMessage() throws IOException {
        assertEquals("ECHO: hello netty", roundTrip("hello netty"));
    }

    @Test
    void pingCommand() throws IOException {
        assertEquals("PONG", roundTrip("PING"));
    }

    @Test
    void chineseMessage() throws IOException {
        assertEquals("ECHO: 中文测试", roundTrip("中文测试"));
    }

    @Test
    void stickyFrames() throws IOException {
        // 粘包场景：两帧合并在一次 write 中发出，服务端应解出两条并依次响应
        try (Socket socket = new Socket("127.0.0.1", properties.getPort())) {
            socket.setSoTimeout(5000);
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            byte[] first = "第一条".getBytes(StandardCharsets.UTF_8);
            byte[] second = "第二条".getBytes(StandardCharsets.UTF_8);
            out.writeInt(first.length);
            out.write(first);
            out.writeInt(second.length);
            out.write(second);
            out.flush();

            DataInputStream in = new DataInputStream(socket.getInputStream());
            assertEquals("ECHO: 第一条", readFrame(in));
            assertEquals("ECHO: 第二条", readFrame(in));
        }
    }

    /** 发送一帧并读取一帧响应 */
    private String roundTrip(String message) throws IOException {
        try (Socket socket = new Socket("127.0.0.1", properties.getPort())) {
            socket.setSoTimeout(5000);
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            byte[] body = message.getBytes(StandardCharsets.UTF_8);
            out.writeInt(body.length);
            out.write(body);
            out.flush();
            return readFrame(new DataInputStream(socket.getInputStream()));
        }
    }

    /** 按帧格式读取一条响应 */
    private String readFrame(DataInputStream in) throws IOException {
        int length = in.readInt();
        byte[] body = new byte[length];
        in.readFully(body);
        return new String(body, StandardCharsets.UTF_8);
    }
}
