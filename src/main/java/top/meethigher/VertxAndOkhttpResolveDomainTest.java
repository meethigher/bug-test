package top.meethigher.proxy.http;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.dns.AddressResolverOptions;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientResponse;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import top.meethigher.proxy.utils.TestUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.locks.LockSupport;

/**
 * 针对域名reqres.in的服务进行请求，会发现okhttp明显比vertx http要快
 * 需要搞清楚vertx http为啥解析域名慢
 */
public class VertxAndOkhttpResolveDomainTest {

    public static void main(String[] args) {
        new VertxAndOkhttpResolveDomainTest().testVertxHttp();
    }


    public void testInetAddress() throws UnknownHostException {
        long start = System.currentTimeMillis();
        InetAddress byName = InetAddress.getByName("reqres.in");
        System.out.println(byName.getHostAddress());
        System.out.println("耗时：" + (System.currentTimeMillis() - start) + " ms");
        System.exit(0);
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


    public void testVertxHttp() {

        VertxOptions vertxOptions = new VertxOptions()
                .setAddressResolverOptions(new AddressResolverOptions()
                        .setQueryTimeout(500));
        Vertx vertx = Vertx.vertx(vertxOptions);

        HttpClient httpClient = vertx.createHttpClient(TestUtils.httpClientOptions());
        long start = System.currentTimeMillis();
        httpClient.request(TestUtils.requestOptions())
                .onComplete(ar -> {
                    if (ar.succeeded()) {
                        ar.result().send().onComplete(ar1 -> {
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
                        });
                    } else {
                        ar.cause().printStackTrace();
                    }
                });


        LockSupport.park();

    }
}
