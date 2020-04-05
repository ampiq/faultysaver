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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DownloadRequestTaskTest {

    URI uri;

    @BeforeEach
    public void setup() throws URISyntaxException {
        uri = new URI("http://example.com");
    }

    @Test
    public void shouldReturn_nullResponse_if_apiDoNotRespond() throws IOException {
        //given:
        CloseableHttpClient httpClient = mock(CloseableHttpClient.class);

        //and:
        when(httpClient.execute(any())).thenThrow(IOException.class);

        //and:
        DownloadRequestTask downloadRequestTask = new DownloadRequestTask(httpClient, uri);

        //when:
        HttpResponse response = downloadRequestTask.execute();

        //then:
        assertNull(response);
    }

    @Test
    public void shouldReturn_okResponse_if_apiDoRespondProperly() throws IOException {
        //given:
        CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        CloseableHttpResponse downloadResponse = mock(CloseableHttpResponse.class);
        HttpEntity downloadEntity = mock(HttpEntity.class);

        //and:
        when(downloadResponse.getStatusLine())
                .thenReturn(new BasicStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_OK, "Ok!"));
        when(downloadResponse.getEntity()).thenReturn(downloadEntity);
        when(downloadEntity.getContent()).thenReturn(getClass().getClassLoader().getResourceAsStream("1.txt"));
        when(httpClient.execute(any())).thenReturn(downloadResponse);

        //and:
        DownloadRequestTask downloadRequestTask = new DownloadRequestTask(httpClient, uri);

        //when:
        HttpResponse response = downloadRequestTask.execute();

        int expectedStatus = 200;
        int actualStatus = response.getStatusLine().getStatusCode();

        //then:
        assertEquals(expectedStatus, actualStatus);
    }

    @Test
    public void shouldRetry_if_apiDoNotRespond() throws IOException {
        //given:
        CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        CloseableHttpResponse downloadResponse = mock(CloseableHttpResponse.class);

        when(downloadResponse.getStatusLine())
                .thenReturn(new BasicStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_INTERNAL_SERVER_ERROR, "Error!"));
        when(httpClient.execute(any())).thenReturn(downloadResponse);

        //and:
        DownloadRequestTask downloadRequestTask = new DownloadRequestTask(httpClient, uri);

        //when:
        downloadRequestTask.execute();

        int expectedSizeOfAttempts = 20;

        verify(httpClient, times(expectedSizeOfAttempts)).execute(any());
    }
}