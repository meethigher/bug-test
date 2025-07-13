package top.meethigher.acceptqueue;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.concurrent.locks.LockSupport;

public class AcceptQueueTest {
    public static void main(String[] args) throws Exception {
        /**
         * Windows与Linux的机制不同，使用Linux进行复习
         */
        ServerSocket serverSocket = new ServerSocket();
        // net.core.somaxconn=128，这时候全连接队列为min(5,net.core.somaxconn)=5，也就是说，同时只允许5个连接ESTAB
        serverSocket.bind(new InetSocketAddress("0.0.0.0", 6666), 5);


        LockSupport.park();
    }
}
