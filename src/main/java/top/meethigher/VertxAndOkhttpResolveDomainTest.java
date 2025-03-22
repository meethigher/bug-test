package top.meethigher;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.dns.AddressResolverOptions;
import io.vertx.core.dns.DnsClientOptions;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import okhttp3.OkHttpClient;
import okhttp3.Response;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.concurrent.locks.LockSupport;

/**
 * 针对域名reqres.in的服务进行请求，会发现okhttp明显比vertx http要快
 * 需要搞清楚vertx http为啥解析域名慢
 */
public class VertxAndOkhttpResolveDomainTest {

    public static void main(String[] args) throws Exception {
        new VertxAndOkhttpResolveDomainTest().testVertxHttp();
    }


    public void testInetAddress() throws UnknownHostException {
        long start = System.currentTimeMillis();
        InetAddress byName = InetAddress.getByName("reqres.in");
        System.out.println(byName.getHostAddress());
        System.out.println("耗时：" + (System.currentTimeMillis() - start) + " ms");
        System.exit(0);
    }


    public void testVertxDns() throws Exception {
        Vertx vertx = Vertx.vertx();
        vertx.createDnsClient(new DnsClientOptions().setQueryTimeout(1000)).lookup("reqres.in", ar->{
            if(ar.succeeded()) {
                System.exit(0);
            }else {
                ar.cause().printStackTrace();
            }
        });
        LockSupport.park();
    }


    public void testOkhttp() throws Exception {
        OkHttpClient client = TestUtils.okHttpClient();
        long start = System.currentTimeMillis();
        try (Response response = client.newCall(TestUtils.request()).execute()) {
            System.out.println(response.body().string());
            System.out.println("耗时：" + (System.currentTimeMillis() - start) + " ms");
            System.exit(0);
        }
    }


    /**
     * 断点
     * io.netty.resolver.dns.DnsResolveContext#internalResolve(java.lang.String, io.netty.util.concurrent.Promise)
     * io.netty.resolver.dns.DnsResolveContext#query(io.netty.resolver.dns.DnsServerAddressStream, int, io.netty.handler.codec.dns.DnsQuestion, io.netty.resolver.dns.DnsQueryLifecycleObserver, boolean, io.netty.util.concurrent.Promise, java.lang.Throwable)
     *
     * 问题定位：
     * 因为我机器本身有一个虚拟网卡，这个网卡有指定一个局域网的DNS服务器，这个就被netty自动获取到了。我传的是个互联网的域名，他通过这个解析就会等待超时，直到下一个dns服务器解析成功。
     *
     * 解决方式，任选其一
     * 1. 提前预热dns解析结果缓存
     * 2. 超时参数设置的短一点
     */
    public void testVertxHttp() {

        VertxOptions vertxOptions = new VertxOptions()
                .setAddressResolverOptions(new AddressResolverOptions()
                        .setQueryTimeout(50000)
                        .setServers(new ArrayList<>()));
        Vertx vertx = Vertx.vertx(vertxOptions);

        HttpClient httpClient = vertx.createHttpClient(TestUtils.httpClientOptions());
        long start = System.currentTimeMillis();
        httpClient.request(TestUtils.requestOptions())
                .onComplete(connectHandler(start));


        LockSupport.park();

    }

    public Handler<AsyncResult<HttpClientRequest>> connectHandler(long start) {
        return ar -> {
            if (ar.succeeded()) {
                ar.result().send().onComplete(sendHandler(start));
            } else {
                ar.cause().printStackTrace();
            }
        };
    }

    public Handler<AsyncResult<HttpClientResponse>> sendHandler(long start) {
        return ar1 -> {
            if (ar1.succeeded()) {
                HttpClientResponse resp = ar1.result();
                resp.pause();
                resp.bodyHandler(buffer -> {
                    System.out.println(buffer);
                    System.out.println("耗时：" + (System.currentTimeMillis() - start) + " ms");
                    System.exit(0);
                });
                resp.resume();
            } else {
                ar1.cause().printStackTrace();
            }
        };
    }
}
