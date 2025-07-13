package top.meethigher;

import java.util.Scanner;

public class BugTest {
    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Server lists: ");
        System.out.println("  1 - Not Accept Tcp Server");
        System.out.println("  2 - Not Close Tcp Server");
        System.out.println("  3 - Tomcat Http Server");
        System.out.println("  4 - Undertow Http Server");
        System.out.println("  5 - Vertx Http Server");
        System.out.println("  6 - Netty Native Epoll Http Server");
        System.out.println("  7 - Webflux Http Server");
        System.out.println("  8 - Tcp Client");
        System.out.println("  9 - Http Client");
        System.out.print("Enter the server you want to start: ");
        int type = scanner.nextInt();
        switch (type) {
            case 1:
                CloseWaitServer.notAcceptTcpServer();
                break;
            case 2:
                CloseWaitServer.notCloseTcpServer();
                break;
            case 3:
                CloseWaitServer.tomcatHttpServer();
                break;
            case 4:
                CloseWaitServer.undertowHttpServer();
                break;
            case 5:
                CloseWaitServer.vertxHttpServer();
                break;
            case 6:
                CloseWaitServer.nettyNativeEpoll();
                break;
            case 7:
                CloseWaitServer.webfluxHttpServer();
                break;
            case 8:
                tcpClient(scanner);
                break;
            case 9:
                httpClient(scanner);
                break;
            default:
                System.out.println("Unrecognized type");
        }
    }

    private static void tcpClient(Scanner scanner) throws Exception {
        System.out.print("Enter maxConcurrency: ");
        int maxConcurrency = scanner.nextInt();
        System.out.print("Enter timeout(ms): ");
        long timeout = scanner.nextLong();
        System.out.print("Enter enable tcp so_keepalive(true/false): ");
        boolean soKeepalive = scanner.nextBoolean();
        System.out.print("Enter host: ");
        String host = scanner.next();
        System.out.print("Enter port: ");
        int port = scanner.nextInt();
        CloseWaitServer.tcpClient(maxConcurrency, timeout, soKeepalive, host, port);
    }

    private static void httpClient(Scanner scanner) throws Exception {
        System.out.print("Enter maxConcurrency: ");
        int maxConcurrency = scanner.nextInt();
        System.out.print("Enter timeout(ms): ");
        long timeout = scanner.nextLong();
        System.out.print("Enter enable tcp so_keepalive(true/false): ");
        boolean soKeepalive = scanner.nextBoolean();
        System.out.print("Enter http url: ");
        String url = scanner.next();
        CloseWaitServer.httpClient(maxConcurrency, timeout, soKeepalive, url);
    }
}
