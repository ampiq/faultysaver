package org.jb.faultysaver.core;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.jb.faultysaver.core.exceptions.NoConnectionException;

import java.io.IOException;
import java.net.SocketException;
import java.net.URI;

public class DeleteRequestTask extends AbstractRequestTask {

    private static final int MAX_ATTEMPTS_NUMBER = 20;

    public DeleteRequestTask(CloseableHttpClient client, URI uri) {
        super(client, uri);
    }

    public HttpResponse execute() {
        try {
            HttpUriRequest deleteRequest = RequestBuilder
                    .delete(getUri())
                    .build();
            HttpResponse deleteResponse = executeRequestWithAttempts(deleteRequest, MAX_ATTEMPTS_NUMBER);
            return deleteResponse;
        } catch (SocketException ex) {
            throw new NoConnectionException(getUri(), ex);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
