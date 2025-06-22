package top.meethigher.h2;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.http.RequestOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

public class H2Request {
    private static final Logger log = LoggerFactory.getLogger(H2Request.class);

    public static void main(String[] args) {
        alpn();
    }

    private static void alpn() {
        CountDownLatch latch = new CountDownLatch(1);
        Vertx vertx = Vertx.vertx();
        HttpClientOptions httpClientOptions = new HttpClientOptions()
                .setProtocolVersion(HttpVersion.HTTP_1_1)
                .setUseAlpn(true)
                .setAlpnVersions(new ArrayList<HttpVersion>() {{
                    add(HttpVersion.HTTP_1_1);
                    add(HttpVersion.HTTP_2);
                }});
        HttpClient httpClient = vertx.createHttpClient(httpClientOptions);
        RequestOptions requestOptions = new RequestOptions()
                .setAbsoluteURI("https://meethigher.com/test");
        httpClient.request(requestOptions)
                .onSuccess(req -> {
                    req.send().onSuccess(resp -> {
                        resp.bodyHandler(buf -> {
                            log.info("{} received:\n{}", resp.version(), buf.toString());
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

    private static void noAlpn() {
        CountDownLatch latch = new CountDownLatch(1);
        Vertx vertx = Vertx.vertx();
        HttpClientOptions httpClientOptions = new HttpClientOptions()
                .setProtocolVersion(HttpVersion.HTTP_1_1)
                .setUseAlpn(false)
                .setAlpnVersions(new ArrayList<HttpVersion>() {{
                    add(HttpVersion.HTTP_1_1);
                    add(HttpVersion.HTTP_2);
                }});
        HttpClient httpClient = vertx.createHttpClient(httpClientOptions);
        RequestOptions requestOptions = new RequestOptions()
                .setAbsoluteURI("https://meethigher.com/test");
        httpClient.request(requestOptions)
                .onSuccess(req -> {
                    req.send()
                            .onSuccess(resp -> {
                                resp.bodyHandler(buf -> {
                                    log.info("{} received:\n{}", resp.version(), buf.toString());
                                    latch.countDown();
                                });
                            })
                            .onFailure(e -> {
                                e.printStackTrace();
                                System.exit(1);
                            });
                })
                .onFailure(e -> {
                    e.printStackTrace();
                    System.exit(1);
                });
        try {
            latch.countDown();
        } catch (Exception ignore) {
        }
    }
}
