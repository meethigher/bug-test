package top.meethigher;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.undertow.Undertow;
import io.undertow.server.RoutingHandler;
import io.undertow.util.Headers;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetClientOptions;
import org.apache.catalina.Context;
import org.apache.catalina.Wrapper;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.apache.coyote.ProtocolHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * 模拟多种情况服务端出现close_wait
 * <p>
 * 测试过程中，建议配合使用抓包工具。实际测试发现，Windows中的curl工具断开连接会直接发送RST，而非发送正常关闭FIN，与Linux相比存在差异
 */
public class CloseWaitServer {

    private final static int port = 6666;
    private static final Logger log = LoggerFactory.getLogger(CloseWaitServer.class);

    /**
     * TCPServer
     * <p>
     * client与server建立连接，server的应用层不进行accept()
     * <p>
     * client断开连接时，会发送FIN，此时server的应用层需要进行close()，但是由于尚未accept()，因此会一直处于close_wait
     *
     * @see <a href="https://www.cnblogs.com/cheyunhua/p/18024802">排查 CLOSE_WAIT 堆积 - 技术颜良 - 博客园</a>
     */
    public static void notAcceptTcpServer() throws Exception {
        ServerSocket serverSocket = new ServerSocket(port);
        log.info("not accept tcp server started on {}", port);
        LockSupport.park();
    }

    /**
     * TCPServer
     * <p>
     * client与server建立连接，server的应用层不进行close()
     * <p>
     * client断开连接时，会发送FIN，此时server的应用层由于一直没有调用close()，因此会一直处于close_wait
     *
     * @see <a href="https://www.cnblogs.com/cheyunhua/p/18024802">排查 CLOSE_WAIT 堆积 - 技术颜良 - 博客园</a>
     */
    public static void notCloseTcpServer() throws Exception {
        ServerSocket serverSocket = new ServerSocket(port);
        log.info("not close tcp server started on {}", port);
        while (true) {
            final Socket accept = serverSocket.accept();
            new Thread(() -> {
                try {
                    InputStream is = accept.getInputStream();
                    int b;
                    while ((b = is.read()) != -1) {

                    }
                    log.info("detected client disconnected");

                    log.info("server closed");
                    accept.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    /**
     * Http1.1 Server
     * <p>
     * client与server建立http连接，server 20秒后响应，client 10秒超时主动断开。
     * <p>
     * client断开时，发送fin，server会等待20秒执行完毕后回复fin，中间这10秒会处于close_wait状态
     *
     * @see <a href="https://www.cnblogs.com/zhaoyongqi/articles/start_tomcat.html">用java代码启动tomcat - 905413993 - 博客园</a>
     */
    public static void tomcatHttpServer() throws Exception {
        // 指定使用的日志管理器和日志配置文件
        System.setProperty("java.util.logging.manager", "java.util.logging.LogManager");
        System.setProperty("java.util.logging.config.file", "logging.properties");

        Tomcat tomcat = new Tomcat();
        tomcat.setPort(port);
        Connector conn = new Connector("HTTP/1.1");
        conn.setPort(port);
        ProtocolHandler protocolHandler = conn.getProtocolHandler();
        // 默认虽然使用的是Http11NioProtocol。由于Servlet本身是一请求一线程的处理方式，所以本质还是阻塞的
        protocolHandler.setExecutor(Executors.newFixedThreadPool(1));
        tomcat.getService().addConnector(conn);


        Context context = tomcat.addContext("", new File(".").getAbsolutePath());
        Wrapper servlet = Tomcat.addServlet(context, "bugTestServlet", new HttpServlet() {
            @Override
            protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
                String requestURI = request.getRequestURI();
                if ("/bug-test".equals(requestURI)) {
                    try {
                        TimeUnit.SECONDS.sleep(20);
                    } catch (Exception e) {
                    }
                } else if ("/bug/bug-test".equals(requestURI)) {
                    try {
                        TimeUnit.SECONDS.sleep(20);
                    } catch (Exception e) {
                    }
                }
                response.setContentType("text/plain;charset=utf-8");
                response.getWriter().write(System.currentTimeMillis() + "");
            }
        });
        // 1表示启动时立即加载Servlet，0表示第一次请求时才会初始化Servlet
        servlet.setLoadOnStartup(1);

        // 注册路径映射至对应的Servlet
        // /*表示匹配任意多级目录的路径
        context.addServletMappingDecoded("/*", servlet.getName());

        tomcat.start();
        tomcat.getServer().await();
    }

    /**
     * Http1.1 Server
     * <p>
     * client与server建立http连接，server 20秒后响应，client 10秒超时主动断开。
     * <p>
     * client断开时，发送fin，server会等待20秒执行完毕后回复fin，中间这10秒会处于close_wait状态
     */
    public static void undertowHttpServer() throws Exception {
        // 指定使用的日志管理器和日志配置文件
        System.setProperty("java.util.logging.manager", "java.util.logging.LogManager");
        System.setProperty("java.util.logging.config.file", "logging.properties");

        RoutingHandler routes = new RoutingHandler()
                .get("/bug-test", exchange -> {
                    exchange.dispatch(() -> {
                        try {
                            TimeUnit.SECONDS.sleep(20);
                        } catch (Exception e) {
                        }
                        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
                        exchange.getResponseSender().send(String.valueOf(System.currentTimeMillis()));
                    });
                });

        Undertow server = Undertow.builder()
                .setIoThreads(1)
                .setWorkerThreads(1)
                .addHttpListener(port, "0.0.0.0") // 监听端口
                .setHandler(routes)
                .build();

        server.start();
    }

    /**
     * Http1.1 Server
     * <p>
     * client与server建立http连接，server 20秒后响应，client 10秒超时主动断开。
     * <p>
     * client断开时，发送fin，server会立即回复fin。不会出现close_wait
     */
    public static void vertxHttpServer() throws Exception {
        Vertx vertx = Vertx.vertx();
        HttpServer httpServer = vertx.createHttpServer();
        httpServer.requestHandler(req -> {
            vertx.executeBlocking(() -> {
                TimeUnit.SECONDS.sleep(20);
                return String.valueOf(System.currentTimeMillis());
            }).onSuccess(result -> {
                req.response().end(result);
            }).onFailure(e -> {
                req.response().setStatusCode(500).end(e.getMessage());
            });
        });
        httpServer.listen(port).onFailure(e -> {
            e.printStackTrace();
            System.exit(1);
        }).onSuccess(res -> {
            log.info("vertx http server started on {}", port);
        });


        LockSupport.park();
    }

    /**
     * 使用Netty的NativeEpoll启动HttpServer1.1。
     */
    public static void nettyNativeEpoll() throws Exception {
        // 使用epoll线程组，若不指定线程，则默认根据当前服务器配置分配线程数
        // boss负责accept(), worker负责处理业务逻辑
        EpollEventLoopGroup boss = new EpollEventLoopGroup(1);
        EpollEventLoopGroup worker = new EpollEventLoopGroup();

        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(boss, worker)
                .channel(EpollServerSocketChannel.class)
                .childHandler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        ch.pipeline()
                                .addLast(new HttpRequestDecoder())
                                .addLast(new HttpResponseEncoder())
                                .addLast(new HttpObjectAggregator(512 * 1024))
                                .addLast(new SimpleChannelInboundHandler<FullHttpRequest>() {
                                    @Override
                                    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req) throws Exception {
                                        if (!"/bug-test".equals(req.uri())) {
                                            DefaultFullHttpResponse resp = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND);
                                            resp.headers().set(HttpHeaderNames.CONTENT_LENGTH, resp.content().readableBytes());
                                            resp.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain;charset=utf-8");
                                            ChannelFuture channelFuture = HttpUtil.isKeepAlive(req) ?
                                                    ctx.writeAndFlush(resp) : ctx.writeAndFlush(resp).addListener(ChannelFutureListener.CLOSE);
                                            return;
                                        }
                                        ctx.executor().parent().next().submit(() -> {
                                            try {
                                                TimeUnit.SECONDS.sleep(20);
                                            } catch (Exception e) {
                                            }
                                            DefaultFullHttpResponse resp = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK,
                                                    Unpooled.copiedBuffer(String.valueOf(System.currentTimeMillis()), StandardCharsets.UTF_8));
                                            resp.headers().set(HttpHeaderNames.CONTENT_LENGTH, resp.content().readableBytes());
                                            resp.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain;charset=utf-8");
                                            ChannelFuture channelFuture = HttpUtil.isKeepAlive(req) ?
                                                    ctx.writeAndFlush(resp) : ctx.writeAndFlush(resp).addListener(ChannelFutureListener.CLOSE);
                                        });
                                    }

                                    @Override
                                    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                                        cause.printStackTrace();
                                        ctx.close();
                                    }
                                });
                    }
                })
                .option(ChannelOption.SO_BACKLOG, 50)
                // so_keepalive的作用，只是控制连接空闲时，内核是否发送数据包探测对端存活，若不存活则关闭。
                /**
                 * <pre>{@code
                 * 服务端开启SO_KEEPALIVE，启动服务端
                 * 客户端开启SO_KEEPALIVE，连接服务端：curl --no-keepalive http://10.0.0.10:6666/bug-test
                 *
                 * 查看连接状态
                 * Every 1.0s: netstat -ano|head -n 2 && netstat -ano|grep 6666                                                                        Sun Jul 13 14:43:20 2025
                 *
                 * Active Internet connections (servers and established)
                 * Proto Recv-Q Send-Q Local Address           Foreign Address         State       Timer
                 * tcp        0      0 10.0.0.10:52970         10.0.0.10:6666          ESTABLISHED off (0.00/0/0)
                 * tcp6       0      0 :::6666                 :::*                    LISTEN      off (0.00/0/0)
                 * tcp6       0      0 10.0.0.10:6666          10.0.0.10:52970         ESTABLISHED keepalive (7215.63/0/0)
                 * }</pre>
                 */
                .childOption(ChannelOption.SO_KEEPALIVE, true);

        bootstrap.bind(port).sync();
        log.info("netty native epoll http server started on {}", port);

    }

    /**
     * Http1.1 Server
     * <p>
     * client与server建立http连接，server 20秒后响应，client 10秒超时主动断开。
     * <p>
     * client断开时，发送fin，server会立即回复fin。不会出现close_wait
     */
    public static void webfluxHttpServer() throws Exception {
        // TODO: 2025/7/11 新建一个springboot项目
        System.out.println("Use Spring Boot to create a WebFlux service yourself or directly start the module webflux-http-server.");
    }


    public static void tcpClient(int maxConcurrency, long timeout,
                                 boolean soKeepalive,
                                 String host, int port) throws Exception {
        Vertx vertx = Vertx.vertx();
        NetClient netClient = vertx.createNetClient(new NetClientOptions().setTcpKeepAlive(soKeepalive));
        CountDownLatch latch = new CountDownLatch(maxConcurrency);
        for (int i = 0; i < maxConcurrency; i++) {
            final int finalI = i;
            netClient.connect(port, host)
                    .onSuccess(socket -> {
                        log.info("{} connected, {}--{}", finalI, socket.remoteAddress(), socket.localAddress());
                        socket.closeHandler(v -> {
                            latch.countDown();
                            log.info("{} closed, {}--{}", finalI, socket.remoteAddress(), socket.localAddress());
                        });
                        vertx.setTimer(timeout, id -> {
                            socket.close();
                        });
                    })
                    .onFailure(e -> {
                        log.error("{} connect error", finalI, e);
                        latch.countDown();
                    });
        }
        latch.countDown();
        vertx.close();
    }

    public static void httpClient(int maxConcurrency, long timeout,
                                  boolean soKeepalive,
                                  String url) throws Exception {
        Vertx vertx = Vertx.vertx();

        CountDownLatch latch = new CountDownLatch(maxConcurrency);

        HttpClient httpClient = vertx.createHttpClient(new HttpClientOptions().setKeepAlive(false).setTcpKeepAlive(soKeepalive).setMaxPoolSize(maxConcurrency));
        RequestOptions requestOptions = new RequestOptions()
                .setAbsoluteURI(url);
        for (int i = 0; i < maxConcurrency; i++) {
            int finalI = i;
            httpClient.request(requestOptions).onSuccess(req -> {
                log.info("{} connected, {}--{}", finalI, req.connection().remoteAddress(), req.connection().localAddress());
                req.connection().closeHandler(v -> {
                    log.info("{} closed. {}--{}", finalI, req.connection().remoteAddress(), req.connection().localAddress());
                    latch.countDown();
                });
                req.send().onSuccess(res -> {
                    log.info("{} send success", finalI);
                }).onFailure(e -> {
                    log.error("{} send error", finalI, e);
                });
            }).onFailure(e -> {
                log.error("{} connect error", finalI, e);
                latch.countDown();
            });
        }


        latch.await();
        vertx.close();
    }


}
