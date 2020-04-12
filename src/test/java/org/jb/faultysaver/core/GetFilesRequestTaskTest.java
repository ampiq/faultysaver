package org.jb.faultysaver.core;

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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GetFilesRequestTaskTest {

    URI uri;

    @BeforeEach
    public void setup() throws URISyntaxException {
        uri = new URI("http://example.com");
    }

    @Test
    public void shouldThrowNoConnectionException_if_apiDoNotRespond() throws IOException {
        //given:
        CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        //and:
        when(httpClient.execute(any())).thenThrow(SocketException.class);
        //and:
        GetFilesRequestTask getFilesRequestTask = new GetFilesRequestTask(httpClient, uri);
        //when:
        Exception exception = assertThrows(NoConnectionException.class, getFilesRequestTask::execute);
        //then:
        String expectedMessage = "Server unavailable";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    public void shouldReturn_okResponse_if_apiDoRespondProperly() throws IOException {
        //given:
        CloseableHttpClient httpClient = mock(CloseableHttpClient.class);
        CloseableHttpResponse downloadResponse = mock(CloseableHttpResponse.class);
        //and:
        when(downloadResponse.getStatusLine())
                .thenReturn(new BasicStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_OK, "Ok!"));
        when(httpClient.execute(any())).thenReturn(downloadResponse);
        //and:
        GetFilesRequestTask downloadRequestTask = new GetFilesRequestTask(httpClient, uri);
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
        GetFilesRequestTask downloadRequestTask = new GetFilesRequestTask(httpClient, uri);
        //when:
        HttpResponse response = downloadRequestTask.execute();
        int expectedSizeOfAttempts = 20;
        verify(httpClient, times(expectedSizeOfAttempts)).execute(any());
    }
}