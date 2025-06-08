package top.meethigher;

import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class BugTest {

    private static final Logger log = LoggerFactory.getLogger(BugTest.class);


    private static final Vertx vertx = Vertx.vertx();


    public static void main(String[] args) {
        //step1();
        step2();
    }

    /**
     * 测试TCP空闲超时断开
     */
    public static void step2() {
        /**
         * 复现步骤
         * 1. 开启backendServer
         * 2. 开启proxyServer
         * 3. 手机通过wifi局域网连接到服务器的proxyServer
         * 4. 等待idleTimeout后，查看整条链路连接状态
         * 5. user--[a]--proxyServer proxyClient--[b]--backendServer，即便proxyClient主动超时断开，user也会监听到数据包关闭。其中proxyClient为proxyServer内置的请求client
         *
         *
         * 为什么会出现上述现象？将user、proxyServer、backendServer分别分给三台局域网的不同机器，TCP抓包分析。
         * 发现
         * 当使用pipeTo进行a与b的双向数据绑定时，proxyClient主动断开，user也会监听到断开。
         * 当使用write进行a与b的双向数据绑定时，proxyClient主动断开，user不会监听到。
         *
         * 为啥？
         * 继续跟源码
         *
         * 关键在于io.vertx.core.streams.impl.PipeImpl内部做了太多东西。
         * a.pipeTo(b)
         *
         * 相当于
         * a.resume();
         * a.handler(buf -> {
         *     b.write(buf);
         *     if (b.writeQueueFull()) {
         *         a.pause();
         *         b.drainHandler(t -> a.resume());
         *     }
         * });
         * a.endHandler(v -> {
         *     a.handler(null);
         *     a.endHandler(null);
         *     a.exceptionHandler(null);
         *     b.end().onComplete(completion);
         * });
         * a.exceptionHandler(e -> {
         *     a.handler(null);
         *     a.endHandler(null);
         *     a.exceptionHandler(null);
         *     b.end().onComplete(v -> completion.handle(Future.failedFuture(e)));
         * });
         */


        // 代理
        NetServer proxyServer = vertx.createNetServer();
        NetClient proxyClient = vertx.createNetClient(new NetClientOptions().setIdleTimeoutUnit(TimeUnit.SECONDS)
                .setIdleTimeout(5));
        proxyServer.connectHandler(a -> {
            a.pause();
            a.remoteAddress();
            a.localAddress();
            connectHandler(a);
            closeHandler(a);
            proxyClient.connect(8888, "192.168.1.103").onFailure(e -> System.exit(1))
                    .onSuccess(b -> {
                        b.pause();
                        connectHandler(b);
                        closeHandler(b);
                        b.remoteAddress();
                        b.localAddress();
                        // pipeTo(a, b);
                        // pipeTo(b, a);

                        diyPipeTo(a, b);
                        diyPipeTo(b, a);


                        //a.handler(b::write);
                        //b.handler(a::write);


                        a.resume();
                        b.resume();
                    });
        }).listen(8080).onFailure(e -> System.exit(1));
    }

    /**
     * 测试物理网络断开
     */
    public static void step1() {
        /**
         * 复现步骤：
         * 1. 手机与服务器均连接wifi，获取局域网IP
         * 2. 服务器开启服务
         * 3. 手机通过局域网连接服务
         * 4. 手机断开wifi
         * 5. 服务器的TCP连接仍然处于ESTABLISHED
         */

        NetServer netServer = vertx.createNetServer();
        /**
         *
         * 客户端网络物理断开，Windows与Linux对于连接的处理机制不同。
         *
         * 不开启发送心跳时：
         * Windows/Linux都会一直保持这个假连接。
         *
         * 开启发送心跳时：
         * Windows/Linux在重传超过指定次数后，就会触发RST。Windows的重试次数默认比Linux要小
         */
        final boolean heartbeat = false;

        netServer.connectHandler(socket -> {
            socket.pause();
            log.info("{} -- {} connected", socket.remoteAddress(), socket.localAddress());
            socket.closeHandler(v -> {
                log.info("{} -- {} closed", socket.remoteAddress(), socket.localAddress());
            });
            socket.handler(buf -> {
                log.info("{} -- {} received/sent:\n{}", socket.remoteAddress(), socket.localAddress(), buf.toString());
                write(socket, buf);
            });
            if (heartbeat) {
                vertx.setPeriodic(5000, id -> {
                    write(socket, Buffer.buffer("heartbeat...\n"));
                });
            }
            write(socket, Buffer.buffer("halo wode\n"));
            socket.resume();
        }).listen(8080).onFailure(e -> {
            System.exit(1);
        });
    }

    /**
     * write的结果，并不能判定tcp链路是否正常。
     * 在测试中可发现，即便链路异常，返回仍然是true
     */
    public static void write(NetSocket socket, Buffer buf) {
        socket.write(buf).onComplete(ar -> log.info("{} -- {} write: {}", socket.remoteAddress(), socket.localAddress(), ar.succeeded()));
    }

    public static void connectHandler(NetSocket o) {
        log.info("{} -- {} connected", o.remoteAddress(), o.localAddress());
    }

    public static void closeHandler(NetSocket o) {
        o.closeHandler(t -> {
            log.info("{} -- {} closed", o.remoteAddress(), o.localAddress());
        });
    }

    public static void pipeTo(NetSocket o, NetSocket t) {
        o.pipeTo(t).onComplete(ar -> log.info("{} -- {} pipe to {} -- {} result: {}", o.remoteAddress(), o.localAddress(),
                t.localAddress(), t.remoteAddress(),
                ar.succeeded()));
    }

    public static void diyPipeTo(NetSocket o, NetSocket t) {
        Promise<Void> promise = Promise.promise();
        new DiyPipe<>(o).to(t, promise);
        promise.future().onComplete(ar -> log.info("{} -- {} pipe to {} -- {} result: {}", o.remoteAddress(), o.localAddress(),
                t.localAddress(), t.remoteAddress(),
                ar.succeeded()));
    }
}