package org.jb.faultysaver.core;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public class UploadRequestTask extends AbstractRequestTask {

    private static final Logger LOGGER = LogManager.getLogger(UploadRequestTask.class.getName());
    private static final int MAX_ATTEMPTS_NUMBER = 20;
    private static final String MULTIPART_TYPE = "multipart/form-data";

    private HttpResponse previousResponse;
    private String fileName;

    public UploadRequestTask(CloseableHttpClient client, URI uri, String fileName, HttpResponse previousResponse) {
        super(client, uri);
        this.fileName = fileName;
        this.previousResponse = previousResponse;
    }

    public HttpResponse execute() {
        HttpResponse uploadResponse = null;
        InputStream contentStream = null;
        try {
            if (isSuccessful(previousResponse)) {
                contentStream = previousResponse.getEntity()
                                                      .getContent();
                byte[] uploadBytes = IOUtils.toByteArray(contentStream);
                HttpPost uploadRequest = new HttpPost(getUri());
                uploadResponse = executeRequestWithAttempts(uploadRequest, uploadBytes, MAX_ATTEMPTS_NUMBER);
                System.out.println("With code " + uploadResponse.getStatusLine().getStatusCode() + " from upload request");
                EntityUtils.consume(uploadResponse.getEntity());
                return uploadResponse;
            }
        } catch (Exception e) {
            LOGGER.error("Upload request for {} was aborted ", getUri());
        } finally {
            if (contentStream != null) {
                try {
                    contentStream.close();
                } catch (IOException ex) {
                    LOGGER.error("I/O error occurred while closing upload stream", ex);
                }
            }
        }
        return uploadResponse;
    }

    protected HttpResponse executeRequestWithAttempts(HttpPost request, byte[] uploadBytes, int attemptsNumber) throws IOException {
        HttpResponse response;
        int currentAttempt = 0;
        do {
            currentAttempt++;
            HttpEntity uploadEntity = MultipartEntityBuilder
                    .create()
                    .addBinaryBody("file", new ByteArrayInputStream(uploadBytes), ContentType.create(MULTIPART_TYPE), fileName)
                    .build();
            request.setEntity(uploadEntity);
            response = getClient().execute(request);
            if (!isSuccessful(response)) {
                EntityUtils.consume(response.getEntity());
            }
        } while (!isSuccessful(response) && currentAttempt < attemptsNumber);
        return response;
    }
}
