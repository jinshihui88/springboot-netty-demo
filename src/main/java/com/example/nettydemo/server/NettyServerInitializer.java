package com.example.nettydemo.server;

import com.example.nettydemo.config.NettyServerProperties;
import com.example.nettydemo.handler.NettyMessageHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.CharsetUtil;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 每条客户端连接建立时，为该连接配置独立的 ChannelPipeline
 *
 * 入站方向按添加顺序：帧解码 -> 字符串解码 -> 业务处理
 * 出站方向：字符串编码 -> 长度字段编码 -> 写回客户端
 */
@Component
public class NettyServerInitializer
        extends ChannelInitializer<SocketChannel> {

    private final NettyServerProperties properties;
    private final NettyMessageHandler messageHandler;

    public NettyServerInitializer(
            NettyServerProperties properties,
            NettyMessageHandler messageHandler) {
        this.properties = properties;
        this.messageHandler = messageHandler;
    }

    @Override
    protected void initChannel(SocketChannel ch) {
        ch.pipeline()
            // 入站：按长度字段还原完整帧，超长帧直接拒绝
            .addLast(new LengthFieldBasedFrameDecoder(
                properties.getMaxFrameLength(), 0, 4, 0, 4))
            // 入站：ByteBuf -> UTF-8 String
            .addLast(new StringDecoder(CharsetUtil.UTF_8))
            // 出站：String -> ByteBuf 后，在前面加 4 字节长度
            .addLast(new LengthFieldPrepender(4))
            .addLast(new StringEncoder(CharsetUtil.UTF_8))
            // 读空闲检测
            .addLast(new IdleStateHandler(
                properties.getReaderIdleSeconds(), 0, 0,
                TimeUnit.SECONDS))
            .addLast(messageHandler);
    }
}
