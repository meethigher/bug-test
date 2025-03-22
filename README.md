使用main方法运行以下方法

1. 运行`top.meethigher.proxy.http.VertxAndOkhttpResolveDomainTest.testOkhttp`
2. 运行`top.meethigher.proxy.http.VertxAndOkhttpResolveDomainTest.testVertxHttp`

会发现vertx httpclient明显更慢。

此处我需要借助async-profiler分析httpclient慢在哪里



问题明晰：
因为我机器本身有一个虚拟网卡，这个网卡有指定一个局域网的DNS服务器，这个就被netty自动获取到了。我传的是个互联网的域名，他通过这个解析就会等待超时，直到下一个dns服务器解析成功。

解决方式，任选其一
1. 提前预热dns解析结果缓存
2. 超时参数设置的短一点



![image-20250323013209075](README/image-20250323013209075.png)