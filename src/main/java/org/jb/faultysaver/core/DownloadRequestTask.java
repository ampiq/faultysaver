package org.jb.faultysaver.core;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.jb.faultysaver.core.exceptions.NoConnectionException;

import java.io.IOException;
import java.net.SocketException;
import java.net.URI;

public class DownloadRequestTask extends AbstractRequestTask {

    private static final int MAX_ATTEMPTS_NUMBER = 20;
    private static final String OCTET_STREAM_TYPE = "application/octet-stream";

    public DownloadRequestTask(CloseableHttpClient client, URI uri) {
        super(client, uri);
    }

    public HttpResponse execute() {
        try {
            HttpUriRequest downloadRequest = RequestBuilder
                    .get()
                    .setUri(getUri())
                    .setHeader(HttpHeaders.CONTENT_TYPE, OCTET_STREAM_TYPE)
                    .build();
            HttpResponse downloadResponse = executeRequestWithAttempts(downloadRequest, MAX_ATTEMPTS_NUMBER);
            return downloadResponse;
        } catch (SocketException ex) {
            throw new NoConnectionException(getUri(), ex);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
