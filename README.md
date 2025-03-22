使用main方法运行以下方法

1. 运行`top.meethigher.proxy.http.VertxAndOkhttpResolveDomainTest.testOkhttp`
2. 运行`top.meethigher.proxy.http.VertxAndOkhttpResolveDomainTest.testVertxHttp`

会发现vertx httpclient明显更慢。

此处我需要借助async-profiler分析httpclient慢在哪里





