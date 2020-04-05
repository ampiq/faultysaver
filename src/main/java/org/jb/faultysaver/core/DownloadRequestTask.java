package org.jb.faultysaver.core;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;

public class DownloadRequestTask extends AbstractRequestTask {

    private static final Logger LOGGER = LogManager.getLogger(DownloadRequestTask.class.getName());
    private static final int MAX_ATTEMPTS_NUMBER = 20;
    private static final String OCTET_STREAM_TYPE = "application/octet-stream";

    public DownloadRequestTask(CloseableHttpClient client, URI uri) {
        super(client, uri);
    }

    public HttpResponse execute() {
        HttpResponse downloadResponse = null;
        try {
            HttpUriRequest downloadRequest = RequestBuilder
                    .get()
                    .setUri(getUri())
                    .setHeader(HttpHeaders.CONTENT_TYPE, OCTET_STREAM_TYPE)
                    .build();
            downloadResponse = executeRequestWithAttempts(downloadRequest, MAX_ATTEMPTS_NUMBER);
            System.out.println("With code " + downloadResponse.getStatusLine().getStatusCode() + " from download request");
            return downloadResponse;
        } catch (Exception ex) {
            LOGGER.error("Download request for {} was aborted ", getUri());
        }
        return downloadResponse;
    }
}
