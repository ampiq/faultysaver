package org.jb.faultysaver.core;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.URI;

public abstract class AbstractRequestTask {

    private CloseableHttpClient client;
    private URI uri;

    public AbstractRequestTask(CloseableHttpClient client, URI uri) {
        this.client = client;
        this.uri = uri;
    }

    public abstract HttpResponse execute();

    protected HttpResponse executeRequestWithAttempts(HttpUriRequest request, int attemptsNumber) throws IOException {
        HttpResponse response;
        int currentAttempt = 0;
        do {
            currentAttempt++;
            response = client.execute(request);
            if (!isSuccessful(response)) {
                EntityUtils.consume(response.getEntity());
            }
        } while (!isSuccessful(response) && currentAttempt < attemptsNumber);
        return response;
    }

    protected boolean isSuccessful(HttpResponse response) {
        boolean successful = false;
        if (response == null) {
            return successful;
        }
        int statusCode = response.getStatusLine()
                                 .getStatusCode();

        if (statusCode == 200 || statusCode == 409) {
            successful = true;
        }
        return successful;
    }

    public URI getUri() {
        return uri;
    }

    public CloseableHttpClient getClient() {
        return client;
    }
}
