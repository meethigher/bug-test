package top.meethigher;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.net.NetClientOptions;

import java.util.Scanner;
import java.util.concurrent.locks.LockSupport;

public class App {
    public static void main(String[] args) {


        // Linux时，若epoll可用，则自动启用epoll
        Vertx vertx = Vertx.vertx(new VertxOptions().setPreferNativeTransport(true));

        Scanner scanner = new Scanner(System.in);
        System.out.println("mode lists: ");
        System.out.println("  1. Server");
        System.out.println("  2. Client");
        System.out.print("choose: ");
        int type = scanner.nextInt();
        switch (type) {
            case 2:
                System.out.print("maxConcurrency: ");
                int maxConcurrency = scanner.nextInt();
                System.out.print("port: ");
                int port = scanner.nextInt();
                System.out.print("host: ");
                String host = scanner.next();
                System.out.print("clientHost(Use commas to separate multiple): ");
                String clientHost = scanner.next();
                if (clientHost.contains(",")) {
                    for (String client : clientHost.split(",")) {
                        new Thread(() -> {
                            new C1MTcpClient(vertx.createNetClient(new NetClientOptions().setLocalAddress(client)), maxConcurrency, port, host).start();
                        }).start();
                    }
                } else {
                    new C1MTcpClient(vertx.createNetClient(new NetClientOptions().setLocalAddress(clientHost)), maxConcurrency, port, host).start();
                }
                break;
            case 1:
            default:
                System.out.print("port: ");
                new C1MTcpServer(scanner.nextInt(), vertx.createNetServer()).start();
                break;
        }

        LockSupport.park();
    }
}
