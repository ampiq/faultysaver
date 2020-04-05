package org.jb.faultysaver.core;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;

public class DeleteRequestTask extends AbstractRequestTask{

    private static final Logger LOGGER = LogManager.getLogger(DeleteRequestTask.class.getName());
    private static final int MAX_ATTEMPTS_NUMBER = 20;

    private HttpResponse previousResponse;

    public DeleteRequestTask(CloseableHttpClient client, URI uri, HttpResponse previousResponse) {
        super(client, uri);
        this.previousResponse = previousResponse;
    }

    public HttpResponse execute() {
        HttpResponse deleteResponse = null;
        try {
            if (isSuccessful(previousResponse)) {
                HttpUriRequest deleteRequest = RequestBuilder
                        .delete(getUri())
                        .build();
                deleteResponse = executeRequestWithAttempts(deleteRequest, MAX_ATTEMPTS_NUMBER);
                System.out.println("With code " + deleteResponse.getStatusLine().getStatusCode() + " !!!!!!!!!!!!!from deleteRequest!!!!!!!!!! ");
                return deleteResponse;
            }
        } catch (Exception ex) {
            LOGGER.error("Delete request for {} was aborted ", getUri());
        }
        return deleteResponse;
    }
}

























