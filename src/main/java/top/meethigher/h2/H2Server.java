package top.meethigher.h2;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.net.PemKeyCertOptions;

import java.util.ArrayList;
import java.util.concurrent.locks.LockSupport;

public class H2Server {
    public static void main(String[] args) {
        /**
         *
         * h2 指的是 http/2 over tls
         *
         * 开启一个https server
         * 内置证书为20250622开始的36500天的ssl证书。支持meethigher.com和www.meethigher.com
         *
         * Windows客户端在安装公钥证书时，像我使用的是.pem后缀，只需要改成.crt后缀，安装到根证书即可。
         */
        String dir = System.getProperty("user.dir").replace("\\", "/") + "/ssl";
        String key = "private.key";
        String cert = "public.pem";

        Vertx vertx = Vertx.vertx();

        HttpServerOptions httpServerOptions = new HttpServerOptions()
                .setSsl(true)
                .setUseAlpn(true)
                .setAlpnVersions(new ArrayList<HttpVersion>() {{
                    add(HttpVersion.HTTP_1_1);
                    add(HttpVersion.HTTP_2);
                }})
                .setKeyCertOptions(new PemKeyCertOptions()
                        .addKeyPath(dir + "/" + key)
                        .addCertPath(dir + "/" + cert)
                );

        HttpServer httpServer = vertx.createHttpServer(httpServerOptions);
        httpServer.requestHandler(req -> {
            req.response().end("h2 server " + System.currentTimeMillis() + "\n");
        }).listen(443).onFailure(e -> {
            e.printStackTrace();
            System.exit(1);
        });

        LockSupport.park();
    }
}
