package org.jb.faultysaver.core;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicStatusLine;
import org.jb.faultysaver.core.exceptions.NoConnectionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UploadRequestTaskTest {

    URI uri;
    String fileName = "1.txt";

    @BeforeEach
    public void setup() throws URISyntaxException {
        uri = new URI("http://example.com");
    }

    @Test
    public void shouldThrowNoConnectionException_if_apiDoNotRespond() throws IOException {
        //given:
        CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        CloseableHttpResponse downloadResponse = mock(CloseableHttpResponse.class);
        HttpEntity entity = mock(HttpEntity.class);
        downloadResponse.setEntity(entity);
        //and:
        when(httpClient.execute(any())).thenThrow(SocketException.class);
        when(downloadResponse.getEntity()).thenReturn(entity);
        when(entity.getContent()).thenReturn(UploadRequestTaskTest.class.getClassLoader().getResourceAsStream("1.txt"));
        when(downloadResponse.getStatusLine())
                .thenReturn(new BasicStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_OK, "Ok!"));
        //and:
        UploadRequestTask uploadRequestTask = new UploadRequestTask(httpClient, uri, fileName, downloadResponse);
        //when:
        Exception exception = assertThrows(NoConnectionException.class, uploadRequestTask::execute);
        //then:
        String expectedMessage = "Server unavailable";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    public void shouldReturn_nullResponse_if_previousDownloadResponse_isNull() throws IOException {
        //given:
        CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        CloseableHttpResponse downloadResponse = null;
        //and:
        when(httpClient.execute(any())).thenThrow(IOException.class);
        //and:
        UploadRequestTask uploadRequestTask = new UploadRequestTask(httpClient, uri, fileName, downloadResponse);
        //when:
        HttpResponse response = uploadRequestTask.execute();
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