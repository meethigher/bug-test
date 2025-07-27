package top.meethigher;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.dns.AddressResolverOptions;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.RequestOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public class UpdatedDomainResolverTest {


    private static final Logger log = LoggerFactory.getLogger(UpdatedDomainResolverTest.class);

    /**
     * 测试DNS更新域名映射IP前后，Vertx内置DNS解析器与JDK内置解析器的区别
     * <p>
     * 通过修改hosts配置文件来实现
     */
    public static void main(String[] args) {
        vertx(true);

        LockSupport.park();
    }

    /**
     * <pre>{@code
     * OkHttpClient默认的DNS解析，走Java内置的InetAddress
     * }</pre>
     */
    public static void jdk() {
        new Thread(() -> {
            while (true) {
                try {
                    log.info("{}", InetAddress.getByName("meethigher.com"));
                    TimeUnit.SECONDS.sleep(2);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * <pre>{@code
     * -Dvertx.disableDnsResolver=true 只会影响 Vert.x 网络层里所有需要“把主机名解析成 IP 地址”的组件。归纳起来，主要是以下几类工具/场景会随之切换成 JVM 内置的阻塞式 InetAddress 解析：
     * 1. HttpClient / WebClient
     * 2. NetClient（TCP/SSL 客户端）
     * 3. EventBus 桥接 / SockJS / 集群成员发现
     *
     * DnsClient：该对象是“显式 DNS 查询”接口，本身不受 disableDnsResolver 开关控制；它始终使用 Vert.x 自带的异步解析器。如果你想让 DnsClient 也走系统解析，需要改用 InetAddress 自行实现。
     * </pre>
     * <p>
     * <p>
     * 如何验证是否走了netty的dns解析？
     * <p>
     * 打个断点io.netty.resolver.dns.DnsResolveContext#query(io.netty.resolver.dns.DnsServerAddressStream, int, io.netty.handler.codec.dns.DnsQuestion, io.netty.resolver.dns.DnsQueryLifecycleObserver, boolean, io.netty.util.concurrent.Promise, java.lang.Throwable)
     * <p>
     * 若进来，则是启用了netty的dns解析，若不是则是走了jdk的解析
     */
    private static void vertx(boolean enableJdkResolver) {
        System.setProperty("vertx.disableDnsResolver", String.valueOf(enableJdkResolver));
        VertxOptions vertxOptions = new VertxOptions().setAddressResolverOptions(
                new AddressResolverOptions()
                        .setCacheMinTimeToLive(0) // 默认值0
                        .setCacheMaxTimeToLive(Integer.MAX_VALUE) // 默认值int的最大值
                        .setHostsRefreshPeriod(5) // 默认值是0，表示禁用的
        );

        Vertx vertx = Vertx.vertx(vertxOptions);
        HttpClient httpClient = vertx.createHttpClient(new HttpClientOptions().setKeepAlive(false));
        vertx.setPeriodic(2000, id -> {
            httpClient.request(new RequestOptions().setAbsoluteURI(
//                    "http://meethigher.com:8080/api"
                    "http://meethigher.top:8080/api"

                    ))
                    .onSuccess(req -> {
                        req.send().onSuccess(resp -> {
                            resp.bodyHandler(buf -> {
                                log.info("{}", buf.toString());
                            });
                        });
                    });
        });

    }
}
