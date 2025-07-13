package top.meethigher;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.PoolOptions;
import io.vertx.core.http.RequestOptions;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.locks.LockSupport;

public class CloseWaitServerTest {

    private static final Logger log = LoggerFactory.getLogger(CloseWaitServerTest.class);

    @Test
    public void tomcatHttpServer() {
        Vertx vertx = Vertx.vertx();
        HttpClient httpClient = vertx.createHttpClient(new HttpClientOptions().setIdleTimeout(2000).setMaxPoolSize(100),
                new PoolOptions().setHttp1MaxSize(2000).setHttp2MaxSize(20));
        RequestOptions requestOptions = new RequestOptions().setAbsoluteURI("http://10.0.0.10:6666/bug-test");
        for (int i = 0; i < 10000; i++) {
            final int finalI = i;
            httpClient.request(requestOptions).onSuccess(req -> {
                log.info("{} connected", finalI);
                vertx.setTimer(2000, id -> {
                    req.connection().close();
                });
                req.connection().closeHandler(v -> {
                    log.info("{} closed", finalI);
                });
                req.send().onSuccess(res -> {
                    log.info("{} res success", finalI);
                });
            }).onFailure(e -> {
                log.error("{} error", finalI,e);
            });
        }

        LockSupport.park();
    }
}