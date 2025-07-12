package top.meethigher;

import io.undertow.Undertow;
import io.undertow.server.RoutingHandler;
import io.undertow.util.Headers;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
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


}
