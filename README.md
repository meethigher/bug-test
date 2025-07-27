# bug-test
this is a demo repo

分支说明

close-wait-server: 各种特殊情况复现close-wait

vertx-http-proxy: 复现客户端主动断开时的Response has already been written

vertx-http-dns: vertx httpclient dns解析慢，以及对应的解决办法

vertx-tcp-proxy-closed: tcp反向代理代理短连接时，短连接写回数据就关闭连接。而我作为代理方，数据存储在pipeTo缓冲区，未发送时，就通过closeHandler关闭了连接。

vertx-network-disconnect: 测试tcp的物理网络断开、TCP代理空闲超时断开

vertx-http-alpn: 测试h2及alpn、h2c和prior knowledge