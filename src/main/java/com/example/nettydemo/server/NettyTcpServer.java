package com.example.nettydemo.server;

import com.example.nettydemo.config.NettyServerProperties;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

/**
 * Netty TCP 服务端
 *
 * 实现 SmartLifecycle：随 Spring 容器启动而启动、随容器关闭而停止，
 * 避免留下非守护线程或未释放的端口。
 */
@Component
public class NettyTcpServer implements SmartLifecycle {

    private static final Logger log =
            LoggerFactory.getLogger(NettyTcpServer.class);

    private final NettyServerProperties properties;
    private final NettyServerInitializer initializer;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;
    private volatile boolean running = false;

    public NettyTcpServer(
            NettyServerProperties properties,
            NettyServerInitializer initializer) {
        this.properties = properties;
        this.initializer = initializer;
    }

    @Override
    public synchronized void start() {
        if (running) {
            return;
        }

        // Netty 4.2 推荐：MultiThreadIoEventLoopGroup + NioIoHandler
        // boss 只负责接收连接，worker 负责已接入连接的 I/O
        bossGroup = new MultiThreadIoEventLoopGroup(
                1, NioIoHandler.newFactory());
        workerGroup = new MultiThreadIoEventLoopGroup(
                NioIoHandler.newFactory());

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG,
                        properties.getBacklog())
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childHandler(initializer);

            serverChannel = bootstrap
                .bind(properties.getHost(), properties.getPort())
                .sync()
                .channel();

            running = true;
            log.info("Netty TCP 服务已启动，监听 {}:{}",
                    properties.getHost(), properties.getPort());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            shutdownGroups();
            throw new IllegalStateException("启动 Netty 服务被中断", e);
        } catch (Exception e) {
            shutdownGroups();
            throw new IllegalStateException("启动 Netty TCP 服务失败", e);
        }
    }

    @Override
    public synchronized void stop() {
        running = false;
        if (serverChannel != null) {
            serverChannel.close().syncUninterruptibly();
            serverChannel = null;
        }
        shutdownGroups();
        log.info("Netty TCP 服务已停止");
    }

    private void shutdownGroups() {
        if (workerGroup != null) {
            workerGroup.shutdownGracefully().syncUninterruptibly();
            workerGroup = null;
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully().syncUninterruptibly();
            bossGroup = null;
        }
    }

    @Override
    public void stop(Runnable callback) {
        try {
            stop();
        } finally {
            callback.run();
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE;
    }
}
