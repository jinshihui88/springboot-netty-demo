package com.example.nettydemo.handler;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * TCP 业务处理器
 *
 * 单例 Bean 被所有 Channel 复用，标注 @Sharable 的前提：
 * Handler 不保存任何连接级可变状态。
 */
@ChannelHandler.Sharable
@Component
public class NettyMessageHandler
        extends SimpleChannelInboundHandler<String> {

    private static final Logger log =
            LoggerFactory.getLogger(NettyMessageHandler.class);

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        log.info("客户端连接：{}", ctx.channel().remoteAddress());
    }

    @Override
    protected void channelRead0(
            ChannelHandlerContext ctx, String message) {
        String content = message.trim();
        log.info("收到客户端消息：{}", content);

        if ("PING".equalsIgnoreCase(content)) {
            ctx.writeAndFlush("PONG");
            return;
        }
        ctx.writeAndFlush("ECHO: " + content);
    }

    @Override
    public void userEventTriggered(
            ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent idleEvent
                && idleEvent.state() == IdleState.READER_IDLE) {
            log.warn("客户端读空闲超时，关闭连接：{}",
                    ctx.channel().remoteAddress());
            ctx.close();
            return;
        }
        super.userEventTriggered(ctx, evt);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        log.info("客户端断开：{}", ctx.channel().remoteAddress());
    }

    @Override
    public void exceptionCaught(
            ChannelHandlerContext ctx, Throwable cause) {
        log.warn("连接处理异常：{}", cause.toString());
        ctx.close();
    }
}
