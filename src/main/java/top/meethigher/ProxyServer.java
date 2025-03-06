package top.meethigher;

import io.vertx.core.Vertx;
import io.vertx.core.http.*;
import io.vertx.ext.web.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProxyServer {
    private static final Logger log = LoggerFactory.getLogger(ProxyServer.class);
    private final Integer port;
    private final Vertx vertx;
    private final String sourceUrl;
    private final String targetUrl;

    public ProxyServer(Integer port, String sourceUrl, String targetUrl, Vertx vertx) {
        this.port = port;
        this.sourceUrl = sourceUrl;
        this.targetUrl = targetUrl;
        this.vertx = vertx;
    }

    public void start() {
        Router router = Router.router(vertx);
        HttpServer httpServer = vertx.createHttpServer();
        HttpClient httpClient = vertx.createHttpClient(new HttpClientOptions().setVerifyHost(false).setTrustAll(true),
                new PoolOptions().setHttp1MaxSize(1000).setHttp2MaxSize(500));
        router.route(sourceUrl).order(Integer.MIN_VALUE).handler(ctx -> {
            HttpServerRequest realServerReq = ctx.request();
            HttpServerResponse realServerResp = ctx.response();

            realServerReq.pause();

            String absoluteURI = targetUrl + realServerReq.uri();
            RequestOptions requestOptions = new RequestOptions()
                    .setAbsoluteURI(absoluteURI)
                    .setMethod(realServerReq.method());

            httpClient.request(requestOptions).onFailure(t -> log.error("{} connect error", absoluteURI)).onSuccess(proxyClientReq -> {
                // 若存在请求体，则将请求体复制。使用流式复制，避免占用大量内存
                if (proxyClientReq.headers().contains("Content-Length") || proxyClientReq.headers().contains("Transfer-Encoding")) {
                    realServerReq.pipeTo(proxyClientReq);
                }
                // 发送请求
                proxyClientReq
                        .send()
                        .onFailure(e -> {
                            if (!realServerResp.ended()) {
                                realServerResp.setStatusCode(502).end("Bad Gateway");
                            }
                            log.error("{} send error", absoluteURI, e);
                        })
                        .onSuccess(proxyClientResp -> {
                            proxyClientResp.pause();

                            if (!realServerResp.headers().contains("Content-Length")) {
                                realServerResp.setChunked(true);
                            }
                            // 设置响应码
                            realServerResp.setStatusCode(proxyClientResp.statusCode());
                            // 流输出
                            proxyClientResp.pipeTo(realServerResp)
                                    .onSuccess(v -> {
                                        log.info("{} pipeto success", absoluteURI);
                                    })
                                    .onFailure(e -> {
                                        if (!realServerResp.ended()) {
                                            realServerResp.setStatusCode(502).end("Bad Gateway");
                                        }
                                        log.error("{} pipeto error", absoluteURI, e);
                                    });

                            realServerReq.resume();
                            proxyClientResp.resume();
                        });
            });

        });

        httpServer.requestHandler(router).listen(port, ar -> {
            if (ar.succeeded()) {
                log.info("proxy server started on port {}", port);
            } else {
                log.error("Failed to start proxy server", ar.cause());
            }
        });
    }
}