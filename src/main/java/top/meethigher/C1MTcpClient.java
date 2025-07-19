package top.meethigher;

import io.vertx.core.net.NetClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class C1MTcpClient {

    private static final Logger log = LoggerFactory.getLogger(C1MTcpClient.class);
    private final NetClient netClient;
    private final int maxConcurrency;
    private final int port;
    private final String host;

    public C1MTcpClient(NetClient netClient, int maxConcurrency, int port, String host) {
        this.netClient = netClient;
        this.maxConcurrency = maxConcurrency;
        this.port = port;
        this.host = host;
    }


    public void start() {
        /**
         * 如果是在 Centos7 系统上部署该程序，即使配置了{@code net.ipv4.ip_local_port_range = 1024   65535 }
         * 实际上只能自动分配到 1024-32279这段范围内的端口，无法分配到32280-65535的端口。并且会报错 {@code java.net.BindException: Cannot assign requested address }
         *
         * 解决办法有两种
         * 1. 升级Linux内核。或者使用Windows
         * 2. 程序上手动bind指定端口
         *
         * {@code
         * Socket socket = new Socket();
         * socket.bind(new InetSocketAddress(65535));
         * socket.connect(new InetSocketAddress(host, port));
         * }
         */
        for (int i = 0; i < maxConcurrency; i++) {
            final int finalI = i;
            netClient.connect(port, host).onComplete(ar -> {
                if (ar.succeeded()) {
                    log.info("{} connected, {}--{}", finalI, ar.result().remoteAddress(), ar.result().localAddress());
                    ar.result().closeHandler(v -> {
                        log.info("{} closed, {}--{}", finalI, ar.result().remoteAddress(),
                                ar.result().localAddress());
                    });
                } else {
                    log.error("{} connect error", finalI, ar.cause());
                }
            });
        }
    }
}
