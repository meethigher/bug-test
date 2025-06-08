package top.meethigher;

import io.vertx.core.Vertx;
import io.vertx.core.net.NetServer;

public class BackendServer {

    private static final Vertx vertx = Vertx.vertx();

    public static void main(String[] args) {
        // 后端
        NetServer backendServer = vertx.createNetServer();
        backendServer.connectHandler(socket -> socket.write(String.valueOf(System.currentTimeMillis())))
                .listen(8888)
                .onFailure(e -> System.exit(1));
    }
}
