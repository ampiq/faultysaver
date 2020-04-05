package org.jb.faultysaver.core;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;

public class GetFilesRequestTask extends AbstractRequestTask {

    private static final Logger LOGGER = LogManager.getLogger(GetFilesRequestTask.class.getName());
    private static final int MAX_ATTEMPTS_NUMBER = 20;
    private static final String JSON_TYPE = "application/json";

    public GetFilesRequestTask(CloseableHttpClient client, URI uri) {
        super(client, uri);
    }

    public HttpResponse execute() {
        HttpResponse getFilesResponse = null;
        try {
            HttpUriRequest getFilesRequest = RequestBuilder
                    .get()
                    .setUri(getUri())
                    .setHeader(HttpHeaders.CONTENT_TYPE, JSON_TYPE)
                    .build();
            getFilesResponse = executeRequestWithAttempts(getFilesRequest, MAX_ATTEMPTS_NUMBER);
            System.out.println("With code " + getFilesResponse.getStatusLine().getStatusCode() + " from get files request");
            return getFilesResponse;
        } catch (Exception ex) {
            LOGGER.error("Getting list of files from {} was aborted ", getUri());
        }
        return getFilesResponse;
    }
}
