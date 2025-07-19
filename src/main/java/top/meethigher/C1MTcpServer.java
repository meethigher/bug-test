package top.meethigher;

import io.vertx.core.net.NetServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class C1MTcpServer {

    private static final Logger log = LoggerFactory.getLogger(C1MTcpServer.class);
    private final int port;
    private final NetServer netServer;

    public C1MTcpServer(int port, NetServer netServer) {
        this.port = port;
        this.netServer = netServer;
    }

    public void start() {
        netServer.connectHandler(socket -> {
        }).listen(port).onFailure(e -> {
            log.error("server started error", e);
        }).onSuccess(v -> {
            log.info("server started on {}", port);
        });
    }


}