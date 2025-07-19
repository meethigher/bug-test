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
