package top.meethigher;

import io.vertx.core.Vertx;
import io.vertx.core.net.NetClientOptions;

import java.util.Scanner;
import java.util.concurrent.locks.LockSupport;

public class App {
    public static void main(String[] args) {

        Vertx vertx = Vertx.vertx();

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
                System.out.print("clientHost: ");
                String clientHost = scanner.next();
                new C1MTcpClient(vertx.createNetClient(new NetClientOptions().setLocalAddress(clientHost)), maxConcurrency, port, host).start();
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
