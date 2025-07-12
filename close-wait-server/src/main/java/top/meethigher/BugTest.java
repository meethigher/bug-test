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
        System.out.println("  6 - Webflux Http Server");
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
                CloseWaitServer.webfluxHttpServer();
                break;
            default:
                System.out.println("Unrecognized type");
        }
    }
}
