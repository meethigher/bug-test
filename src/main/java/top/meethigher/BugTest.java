package top.meethigher;

import io.vertx.core.Vertx;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetSocket;
import io.vertx.core.net.SocketAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.locks.LockSupport;

public class BugTest {

    public static void main(String[] args) {
        /**
         * 整体流程模拟
         * 1. 启动一个HTTP后端服务，短连接，返回一个2MB的图片
         * 2. 启动TcpProxyServer，代理这个HTTP后端服务。
         * 3. 启动Jmeter，大批量调用TcpProxyServer。由于后端连接是短连接，返回数据后接着关闭targetSocket。此时由于数据还存储在pipeTo，尚未发送，而sourceSocket已经通过targetSocket.closeHandler关闭，进而导致io.netty.channel.StacklessClosedChannelException
         *
         *
         * 解决办法：
         * closeHandler只记录日志，不操作连接的关闭。因为pipeTo自动会处理正常数据包、异常数据包。
         *
         */
        new BackendServer(8080);
        new TcpProxyServer(808, "192.168.1.107", 8080);
        LockSupport.park();

    }


    public static class TcpProxyServer {

        private static final Logger log = LoggerFactory.getLogger(TcpProxyServer.class);

        private final int port;
        private final String targetHost;
        private final int targetPort;
        private final NetServer netServer;
        private final NetClient netClient;

        public TcpProxyServer(int port, String targetHost, int targetPort) {
            this.port = port;
            this.targetHost = targetHost;
            this.targetPort = targetPort;
            Vertx vertx = Vertx.vertx();
            this.netServer = vertx.createNetServer();
            this.netClient = vertx.createNetClient();
            netServer.connectHandler(this::connectHandle).listen(port).onComplete(ar -> {
                if (ar.succeeded()) {
                    log.info("TcpProxyServer started on port {}", port);
                } else {
                    log.error("TcpProxyServer failed to start on port {}", port, ar.cause());
                }
            });
        }

        private void connectHandle(NetSocket sourceSocket) {
            sourceSocket.pause();
            SocketAddress sourceRemote = sourceSocket.remoteAddress();
            SocketAddress sourceLocal = sourceSocket.localAddress();
            log.info("{} <-- {} connected", sourceLocal, sourceRemote);
            netClient.connect(targetPort, targetHost).onComplete(ar -> {
                if (ar.succeeded()) {
                    NetSocket targetSocket = ar.result();
                    targetSocket.pause();
                    SocketAddress targetRemote = targetSocket.remoteAddress();
                    SocketAddress targetLocal = targetSocket.localAddress();
                    log.info("{} --> {} connected", targetLocal, targetRemote);

                    sourceSocket.pipeTo(targetSocket).onFailure(e -> log.error("pipe failed, {} --> {} --> {} --> {}", sourceRemote, sourceLocal, targetLocal, targetRemote, e));
                    targetSocket.pipeTo(sourceSocket).onFailure(e -> log.error("pipe failed, {} <-- {} <-- {} <-- {}", sourceRemote, sourceLocal, targetLocal, targetRemote, e));

                    sourceSocket.closeHandler(v -> {
                        log.debug("sourceSocket {} <-- {} closed", sourceLocal, sourceRemote);
                        targetSocket.close();
                    });
                    targetSocket.closeHandler(v -> {
                        log.debug("targetSocket {} --> {} closed", targetLocal, targetRemote);
                        sourceSocket.close();
                    });

                    targetSocket.resume();
                    sourceSocket.resume();
                } else {
                    log.error("netClient failed to connect to {}:{}", targetHost, targetPort, ar.cause());
                    sourceSocket.close();
                }
            });
        }
    }


    /**
     * 模拟一个短连接服务
     */
    public static class BackendServer {

        private static final Logger log = LoggerFactory.getLogger(BackendServer.class);
        private final int port;
        private final Vertx vertx;

        public BackendServer(int port) {
            this.port = port;
            this.vertx = Vertx.vertx();
            NetServer netServer = vertx.createNetServer();
            netServer.connectHandler(this::connectHandle).listen(port).onComplete(ar -> {
                if (ar.succeeded()) {
                    log.info("Backend server started on port {}", port);
                } else {
                    log.error("Backend server failed to start on port {}", port);
                }
            });
        }

        private void connectHandle(NetSocket socket) {
            socket.pause();
            // 模拟传输大数据的短连接
            vertx.fileSystem().open("D:/Downloads/debian-12.10.0-arm64-DVD-1.iso", new OpenOptions().setRead(true), result -> {
                if (result.succeeded()) {
                    AsyncFile file = result.result();

                    // 把文件内容通过 pipeTo 写入 socket
                    file.pipeTo(socket, ar -> {
                        if (ar.succeeded()) {
                            socket.close();
                        } else {
                            socket.close();
                        }
                        System.out.println("传输完毕");
                    });
                } else {
                    String err = "打开文件失败: " + result.cause().getMessage();
                    System.err.println(err);
                    socket.write(err).onComplete(v -> socket.close());
                }
            });
            socket.resume();
        }
    }
}