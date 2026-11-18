package com.example.nettydemo.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;

/**
 * Netty 测试客户端
 *
 * 与服务端使用完全相同的长度帧协议：
 * 4 字节大端序长度字段 + UTF-8 消息体
 *
 * 运行方式（IDEA 直接运行，或命令行传参）：
 *   NettyTestClient                       -> 发送 hello netty
 *   NettyTestClient 127.0.0.1 9000 PING   -> 发送心跳命令
 */
public class NettyTestClient {

    public static void main(String[] args) throws Exception {
        String host = args.length > 0 ? args[0] : "127.0.0.1";
        int port = args.length > 1
                ? Integer.parseInt(args[1]) : 9000;
        String message = args.length > 2 ? args[2] : "hello netty";

        EventLoopGroup group = new MultiThreadIoEventLoopGroup(
                NioIoHandler.newFactory());
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline()
                            .addLast(new LengthFieldBasedFrameDecoder(
                                1024 * 1024, 0, 4, 0, 4))
                            .addLast(new StringDecoder(CharsetUtil.UTF_8))
                            .addLast(new LengthFieldPrepender(4))
                            .addLast(new StringEncoder(CharsetUtil.UTF_8))
                            .addLast(new SimpleChannelInboundHandler<String>() {
                                @Override
                                protected void channelRead0(
                                        ChannelHandlerContext ctx, String msg) {
                                    System.out.println("[客户端] 收到服务端响应：" + msg);
                                    ctx.close();
                                }
                            });
                    }
                });

            Channel channel = bootstrap.connect(host, port).sync().channel();
            System.out.println("[客户端] 已连接服务端 " + host + ":" + port);
            System.out.println("[客户端] 发送：" + message);
            channel.writeAndFlush(message).sync();
            channel.closeFuture().sync();
        } finally {
            group.shutdownGracefully().sync();
        }
    }
}
