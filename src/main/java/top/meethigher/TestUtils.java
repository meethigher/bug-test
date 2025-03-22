package top.meethigher;

import io.netty.bootstrap.Bootstrap;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.http.RequestOptions;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import javax.net.ssl.*;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

public class TestUtils {

    private static final String url = "https://reqres.in/api/users?page=1";


    /**
     * 与okhttp保持一致，优先使用http2
     */
    public static HttpClientOptions httpClientOptions() {
        return new HttpClientOptions().setVerifyHost(false).setTrustAll(true)
                .setUseAlpn(true)
                .setProtocolVersion(HttpVersion.HTTP_2);
    }

    public static RequestOptions requestOptions() {
        return new RequestOptions()
                .setMethod(HttpMethod.GET)
                .setAbsoluteURI(url);
    }

    public static Request request() {
        return new Request.Builder()
                .url(url)
                .get()
                .build();
    }

    public static OkHttpClient okHttpClient() {
        // 服务器keepalive为60s，客户端keepalive为300s。客户端发起一个请求，然后等待60秒后，再发起一个请求，客户端遇到了java.io.EOFException: \n not found;limit=0 content=
        // 参考https://github.com/square/okhttp/issues/2738
        OkHttpClient.Builder builder = new OkHttpClient().newBuilder()
                .retryOnConnectionFailure(true) //默认值
                .connectionPool(new ConnectionPool(5, 5, TimeUnit.MINUTES)) //默认值
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS);
        builder.followRedirects(false);
        builder.followSslRedirects(false);
        // 忽略HTTPS主机名验证
        builder.hostnameVerifier(new HostnameVerifier() {
            @Override
            public boolean verify(final String s, final SSLSession sslSession) {
                return true;
            }

            @Override
            public final String toString() {
                return "NO_OP";
            }
        });
        // 信任所有HTTPS证书，包括CA证书、自签名证书等
        try {
            X509TrustManager x509TrustManager = new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

                }

                @Override
                public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            };
            TrustManager[] trustManagers = {
                    x509TrustManager
            };
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustManagers, new SecureRandom());
            builder.sslSocketFactory(sslContext.getSocketFactory(), x509TrustManager);
        } catch (Exception ignore) {

        }
        return builder.build();
    }


    public static boolean modifyBootstrap(Bootstrap bootstrap) {
        bootstrap.disableResolver();
        System.out.println("执行啦");
        return false;
    }

    public static boolean modifyBootstrap() {
       return true;
    }

}
