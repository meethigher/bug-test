package top.meethigher;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;
import io.vertx.ext.web.Router;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public class ProxyServerTest {

    private static final Logger log = LoggerFactory.getLogger(ProxyServerTest.class);

    private static final Vertx vertx = Vertx.vertx();

    @Test
    public void start() {
        // 启动后端
        Router router = Router.router(vertx);
        router.route().handler(ctx -> {
            vertx.setTimer(Duration.ofSeconds(5).toMillis(), id -> {
                ctx.response().putHeader("Content-Type", "text/plain;charset=utf-8").end("halo");
                log.info("backend server response: {}", ctx.request().uri());
            });
        });
        int port = 888;
        vertx.createHttpServer().requestHandler(router).listen(port).onSuccess(s -> log.info("backend server started on port {}", port)).onFailure(e -> log.error("backend server started error", e));


        // 启动代理服务
        int proxyPort = 8080;
        ProxyServer proxyServer = new ProxyServer(proxyPort, "/*", "http://127.0.0.1:888", vertx);
        proxyServer.start();

        LockSupport.park();
    }


    @Test
    public void cancelReq() throws InterruptedException {

        HttpClient httpClient = vertx.createHttpClient();
        httpClient.request(new RequestOptions().setMethod(HttpMethod.GET).setAbsoluteURI("http://127.0.0.1:8080/bug-test"), ar -> {
            if (ar.succeeded()) {
                HttpClientRequest req = ar.result();

                // 模拟浏览器断开
                vertx.setTimer(Duration.ofSeconds(2).toMillis(), id -> {
                    req.connection().close();
                });

                req.send().onFailure(e -> {
                    log.error("send error", e);
                }).onSuccess(resp -> {
                    log.info("statusCode: {}", resp.statusCode());
                });
            } else {
                Throwable cause = ar.cause();
                log.error("request error", cause, cause);
            }
        });

        TimeUnit.SECONDS.sleep(10);

    }
}