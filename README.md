复现步骤

1. 运行单元测试`top.meethigher.ProxyServerTest#start`
2. 运行单元测试`top.meethigher.ProxyServerTest#cancelReq`

会报错

```sh
2025-03-06 18:09:38.495 [vert.x-eventloop-thread-0] INFO backend server started on port 888
2025-03-06 18:09:38.510 [vert.x-eventloop-thread-0] INFO proxy server started on port 8080
2025-03-06 18:09:46.819 [vert.x-eventloop-thread-0] INFO backend server response: /bug-test
2025-03-06 18:09:46.827 [vert.x-eventloop-thread-0] ERROR http://127.0.0.1:888/bug-test pipeto error
io.netty.channel.StacklessClosedChannelException
2025-03-06 18:09:46.827 [vert.x-eventloop-thread-0] ERROR Unhandled exception
java.lang.IllegalStateException: Response has already been written
```



