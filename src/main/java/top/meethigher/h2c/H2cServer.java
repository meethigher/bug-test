package top.meethigher.h2c;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;

import java.util.concurrent.locks.LockSupport;

public class H2cServer {

    /**
     * HTTP/2 over cleartext TCP，即 基于明文 TCP 的 HTTP/2 协议。
     * 也就是不开启 https 也可以使用 http2
     */
    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        HttpServerOptions httpServerOptions = new HttpServerOptions()
                .setUseAlpn(false)
                .setHttp2ClearTextEnabled(true);
        HttpServer httpServer = vertx.createHttpServer(httpServerOptions);
        httpServer.requestHandler(req -> {
            req.response().end("h2c server " + System.currentTimeMillis());
        }).listen(80).onFailure(e -> {
            e.printStackTrace();
            System.exit(1);
        });

        LockSupport.park();
    }
}
