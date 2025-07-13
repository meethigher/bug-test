# 一、复现CLOSE_WAIT

复现close_wait步骤

1. 启动close-wait-server
2. 通过telnet或者curl进行测试，配合tcp抓包工具。确保测试的步骤中，都是通过正常发送fin实现。避免如rst等命令

综上可得，出现close_wait的两种情况

* 客户端主动断开时，服务端未调用close()回复FIN，导致连接卡在close_wait
* 客户端断开时，服务端尚未accept()，或者accept()慢了。当客户端断开后，连接卡在close_wait，需要等待服务端accept()->close()才会释放

简而言之，close_wait是因为服务端未调用close_wait导致。



# 二、复现大量CLOSE_WAIT导致程序假死

复现大量close_wait导致程序假死

1. 启动tomcat http server
2. 启动tcpclient或者httpclient


# 三、Windows与Linux的处理差异



在 **Linux** 端启动了一个 **“Not Accept Http Server”**。一旦连接进入 `CLOSE_WAIT`，该服务器**不会主动回收**，`CLOSE_WAIT`是否持续堆积受客户端的不同而不同。 

**Windows 客户端**
 ```sh
 curl -m 1 http://10.0.0.10:6666/bug-test
 ```
1. 服务端把连接置于 `CLOSE_WAIT`；
2. 1 秒超时后，Windows **主动发送 RST** 强制终止；
3. 内核立即回收，**`CLOSE_WAIT` 瞬间消失**。手动 `Ctrl+C` 中断也能复现同样效果。

**Linux 客户端**
 ```sh
 curl -m 1 http://10.0.0.10:6666/bug-test
 ```
虽然同样让服务端进入 `CLOSE_WAIT`，但 **Linux 不会自动发 RST**，  
该状态会**长期保持**，直至应用显式 `close()` 或进程退出。

**结论**
- **Windows** 的 RST 容错机制**在客户端超时/中断时兜底**，避免服务端资源泄露。
- **Linux** 则完全依赖应用层清理；若程序未主动关闭，**`CLOSE_WAIT` 将持续堆积**，最终耗尽文件描述符或内存。