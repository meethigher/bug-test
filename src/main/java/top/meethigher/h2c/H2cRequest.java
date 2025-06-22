package top.meethigher.h2c;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.http.RequestOptions;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

public class H2cRequest {
    private static final Logger log = LoggerFactory.getLogger(H2cRequest.class);

    /**
     * h2c 指的 http/2 over cleartext
     */
    public static void main(String[] args) {
        h2cUpgrade();
    }


    /**
     * 使用Vertx实现通过upgrade发起h2c请求
     */
    private static void h2cUpgrade() {

        CountDownLatch latch = new CountDownLatch(1);

        Vertx vertx = Vertx.vertx();
        HttpClientOptions httpClientOptions = new HttpClientOptions()
                .setProtocolVersion(HttpVersion.HTTP_2)
                .setUseAlpn(false);
        HttpClient httpClient = vertx.createHttpClient(httpClientOptions);
        RequestOptions requestOptions = new RequestOptions()
                // 方便wireshark抓包，需要H2cServer放到局域网另外一台机器上面
                .setAbsoluteURI("http://meethigher.com:80/test");
        httpClient.request(requestOptions).onSuccess(req -> {
            req.send().onSuccess(resp -> {
                resp.bodyHandler(buf -> {
                    log.info("{} received:\n{}", resp.version(), buf);
                    latch.countDown();
                });
            }).onFailure(e -> {
                e.printStackTrace();
                System.exit(1);
            });
        }).onFailure(e -> {
            e.printStackTrace();
            System.exit(1);
        });

        try {
            latch.await();
        } catch (Exception ignore) {
        }
    }

    /**
     * Vertx不支持prior knowledge
     * 因此使用okhttpclient实现
     */
    private static void h2cPriorKnowledge() {
        CountDownLatch latch = new CountDownLatch(1);

        OkHttpClient client = new OkHttpClient.Builder()
                .protocols(new ArrayList<Protocol>() {{
                    add(Protocol.H2_PRIOR_KNOWLEDGE);
                }})
                .build();
        Request request = new Request.Builder()
                .url("http://meethigher.com:80/test")
                .build();

        try (Response resp = client.newCall(request).execute()) {
            log.info("{} received:\n{}", resp.protocol(), resp.body().string());
            latch.countDown();
        } catch (Exception e) {
            e.printStackTrace();
            try {
                latch.await();
            } catch (Exception ignore) {
            }
        }
    }
}
