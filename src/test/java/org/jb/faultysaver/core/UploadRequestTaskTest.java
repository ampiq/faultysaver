package org.jb.faultysaver.core;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicStatusLine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UploadRequestTaskTest {

    URI uri;
    String fileName = "1.txt";

    @BeforeEach
    public void setup() throws URISyntaxException {
        uri = new URI("http://example.com");
    }

    @Test
    public void shouldReturn_nullResponse_if_apiDoNotRespond() throws IOException {
        //given:
        CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        CloseableHttpResponse downloadResponse = mock(CloseableHttpResponse.class);
        //and:
        when(httpClient.execute(any())).thenThrow(IOException.class);
        //and:
        UploadRequestTask UploadRequestTask = new UploadRequestTask(httpClient, uri, fileName, downloadResponse);
        //when:
        HttpResponse response = UploadRequestTask.execute();
        //then:
        assertNull(response);
    }

    @Test
    public void shouldReturn_nullResponse_if_previousDownloadResponse_isNull() throws IOException {
        //given:
        CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        CloseableHttpResponse downloadResponse = null;
        //and:
        when(httpClient.execute(any())).thenThrow(IOException.class);
        //and:
        UploadRequestTask UploadRequestTask = new UploadRequestTask(httpClient, uri, fileName, downloadResponse);
        //when:
        HttpResponse response = UploadRequestTask.execute();
        //then:
        assertNull(response);
    }

    @Test
    public void shouldReturn_nullResponse_if_previousDownloadResponse_isNotSuccessful() throws IOException {
        //given:
        CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        CloseableHttpResponse downloadResponse = mock(CloseableHttpResponse.class);
        CloseableHttpResponse uploadResponse = mock(CloseableHttpResponse.class);

        when(downloadResponse.getStatusLine())
                .thenReturn(new BasicStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_INTERNAL_SERVER_ERROR, "Error!"));
        when(uploadResponse.getStatusLine())
                .thenReturn(new BasicStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_OK, "Ok!"));
        //and:
        when(httpClient.execute(any())).thenReturn(uploadResponse);
        //and:
        UploadRequestTask uploadRequestTask = new UploadRequestTask(httpClient, uri, fileName, downloadResponse);
        //when:
        HttpResponse response = uploadRequestTask.execute();
        //then:
        assertNull(response);
    }

    @Test
    public void shouldReturn_okResponse_if_apiDoRespondProperly() throws IOException {
        //given:
        CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        CloseableHttpResponse downloadResponse = mock(CloseableHttpResponse.class);
        CloseableHttpResponse uploadResponse = mock(CloseableHttpResponse.class);
        HttpEntity entity = mock(HttpEntity.class);
        downloadResponse.setEntity(entity);

        //and:
        when(downloadResponse.getEntity()).thenReturn(entity);
        when(entity.getContent()).thenReturn(UploadRequestTaskTest.class.getClassLoader().getResourceAsStream("1.txt"));

        when(downloadResponse.getStatusLine())
                .thenReturn(new BasicStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_OK, "Ok!"));
        when(uploadResponse.getStatusLine())
                .thenReturn(new BasicStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_OK, "Ok!"));
        when(httpClient.execute(any())).thenReturn(uploadResponse);

        //and:
        UploadRequestTask uploadRequestTaskTest = new UploadRequestTask(httpClient, uri, fileName, downloadResponse);

        //when:
        HttpResponse response = uploadRequestTaskTest.execute();

        int expectedStatus = 200;
        int actualStatus = response.getStatusLine().getStatusCode();

        //then:
        assertEquals(expectedStatus, actualStatus);
    }

    @Test
    public void shouldRetry_if_apiDoNotRespond() throws IOException {
        //given:
        CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        CloseableHttpResponse uploadResponse = mock(CloseableHttpResponse.class);
        CloseableHttpResponse downloadResponse = mock(CloseableHttpResponse.class);
        HttpEntity entity = mock(HttpEntity.class);
        downloadResponse.setEntity(entity);

        when(downloadResponse.getEntity()).thenReturn(entity);
        when(entity.getContent()).thenReturn(UploadRequestTaskTest.class.getClassLoader().getResourceAsStream("1.txt"));

        when(downloadResponse.getStatusLine())
                .thenReturn(new BasicStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_OK, "Ok!"));
        when(uploadResponse.getStatusLine())
                .thenReturn(new BasicStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_INTERNAL_SERVER_ERROR, "Error!"));
        when(httpClient.execute(any())).thenReturn(uploadResponse);

        //and:
        UploadRequestTask uploadRequestTaskTest = new UploadRequestTask(httpClient, uri, fileName, downloadResponse);

        //when:
        uploadRequestTaskTest.execute();
        int expectedSizeOfAttempts = 20;

        verify(httpClient, times(expectedSizeOfAttempts)).execute(any());
    }
}