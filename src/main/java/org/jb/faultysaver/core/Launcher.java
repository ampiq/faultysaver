package org.jb.faultysaver.core;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class Launcher {

    public static final String OLD_STORAGE_URL = "http://localhost:8080/oldStorage/files";
    public static final String NEW_STORAGE_URL = "http://localhost:8080/newStorage/files";

    public static void main(String[] args) throws URISyntaxException, IOException, InterruptedException {

//        URI uriFrom = new URI(args[0]);
//        URI uriTo = new URI(args[1]);
        URI uriFrom = new URI(OLD_STORAGE_URL);
        URI uriTo = new URI(NEW_STORAGE_URL);
        CloseableHttpClient client = createClient();
        FaultySaver faultySaver = new FaultySaver(client, uriFrom, uriTo);
        faultySaver.migrateFiles();
        System.out.println("Done!");
    }

    public static CloseableHttpClient createClient() {
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(1000);
        cm.setDefaultMaxPerRoute(1000);
        return HttpClientBuilder
                .create()
                .setConnectionManager(cm)
                .build();
    }
}
