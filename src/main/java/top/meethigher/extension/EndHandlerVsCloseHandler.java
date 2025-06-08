package top.meethigher.extension;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.locks.LockSupport;

public class EndHandlerVsCloseHandler {


    private static final Vertx vertx = Vertx.vertx();
    private static final Logger log = LoggerFactory.getLogger(EndHandlerVsCloseHandler.class);

    public static void main(String[] args) {

        NetServer server = vertx.createNetServer();
        server.connectHandler(socket -> {
            socket.handler(buf -> {
                log.info("服务器收到数据: " + buf.toString());
            });

            /**
             * 虽然NetSocket不区分end()和close()
             * 但是endHandler()和closeHandler()还是有本质不同的。
             */

            // 断点 io.vertx.core.net.impl.NetSocketImpl.NetSocketImpl:99
            socket.endHandler(v -> {
                log.info("服务器：触发 endHandler");
            });
            // 断点 io.vertx.core.net.impl.ConnectionBase:390
            socket.closeHandler(v -> {
                log.info("服务器：触发 closeHandler");
            });
        });
        Handler<NetServer> netServerStartedHandler = v -> {
            NetClient client = vertx.createNetClient();
            client.connect(1234, "localhost", res -> {
                if (res.succeeded()) {
                    NetSocket socket = res.result();
                    socket.write("hello");

                    // 对于Vertx的NetSocket来说，是不区分end与close，本质都是close()
//                     socket.end();
                    socket.close();
                }
            });
        };

        server.listen(1234).onFailure(e -> System.exit(1))
                .onSuccess(netServerStartedHandler);


        LockSupport.park();


    }
}
