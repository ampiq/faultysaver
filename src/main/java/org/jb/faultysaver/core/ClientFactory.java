package org.jb.faultysaver.core;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

public class ClientFactory {

    private static final int MAX_POOL_SIZE = 1_000;

    public static CloseableHttpClient createMultithreadedClientWithFixedPool() {
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(MAX_POOL_SIZE);
        cm.setDefaultMaxPerRoute(MAX_POOL_SIZE);
        return HttpClientBuilder.create()
                                .setConnectionManager(cm)
                                .disableAutomaticRetries()
                                .build();
    }
}
