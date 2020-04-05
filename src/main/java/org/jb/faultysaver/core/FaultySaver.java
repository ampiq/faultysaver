package org.jb.faultysaver.core;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.http.HttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FaultySaver {

    private static final Logger LOGGER = LogManager.getLogger(FaultySaver.class.getName());
    private static final String URI_SEPARATOR = "/";
    private static final int BATCH_SIZE = 500;

    private CloseableHttpClient client;
    private URI uriFrom;
    private URI uriTo;

    private ExecutorService downloadExecutor;
    private ExecutorService uploadExecutor;
    private ExecutorService deleteExecutor;

    public FaultySaver(CloseableHttpClient client, URI uriFrom, URI uriTo) {
        this.client = client;
        this.uriFrom = uriFrom;
        this.uriTo = uriTo;
        downloadExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        uploadExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        deleteExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }

    public void migrateFiles() throws IOException, URISyntaxException, InterruptedException {
        List<String> filesFromOldStorage = getListOfFiles();
        List<CompletableFuture<HttpResponse>> tasks = new ArrayList<>();

        for (int i = 0; i < filesFromOldStorage.size(); ) {
            while (tasks.size() < BATCH_SIZE) {
                String fileName = filesFromOldStorage.get(i++);
                String fullPathToFile = uriFrom.toURL() + URI_SEPARATOR + fileName;
                URI fileFromUri = new URI(fullPathToFile);

                CompletableFuture<HttpResponse> downloadToUploadToDeleteFuture
                        = CompletableFuture.supplyAsync(() -> new DownloadRequestTask(client, fileFromUri).execute(), downloadExecutor)
                                           .thenComposeAsync(result ->
                                                   CompletableFuture.supplyAsync(() ->
                                                           new UploadRequestTask(client, uriTo, fileName, result).execute(), uploadExecutor))
                                           .thenComposeAsync(result ->
                                                   CompletableFuture.supplyAsync(() ->
                                                           new DeleteRequestTask(client, fileFromUri, result).execute(), deleteExecutor));
                tasks.add(downloadToUploadToDeleteFuture);
            }
            removeTasksOnComplete(tasks);
        }

        downloadExecutor.shutdown();
        uploadExecutor.shutdown();
        deleteExecutor.shutdown();
    }

    private void removeTasksOnComplete(List<CompletableFuture<HttpResponse>> tasks) throws InterruptedException {
        while (!tasks.isEmpty()) {
            tasks.removeIf(CompletableFuture::isDone);
            if(!tasks.isEmpty()) {
                Thread.sleep(100); //java 9  -> onSpinWait
            }
        }
    }

    private List<String> getListOfFiles() throws IOException {
        Gson gson = new Gson();
        AbstractRequestTask getFilesTask = new GetFilesRequestTask(client, uriFrom);
        HttpResponse getFilesResponse = getFilesTask.execute();
        if (getFilesResponse == null) {
            return Collections.emptyList();
        }
        InputStream inputStreamJson = getFilesResponse.getEntity()
                                                      .getContent();
        String jsonFiles = extractJsonRepresentation(inputStreamJson);
        if (jsonFiles.isEmpty()) {
            return Collections.emptyList();
        }
        Type listOfStringsType = new TypeToken<List<String>>() {}.getType();
        return gson.fromJson(jsonFiles, listOfStringsType);
    }

    private String extractJsonRepresentation(InputStream is) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line;

        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line)
                  .append("\n");
            }
        } catch (IOException ex) {
            LOGGER.error("I/O error occurred while reading json representation stream", ex);
        } finally {
            try {
                is.close();
            } catch (IOException ex) {
                LOGGER.error("I/O error occurred while closing json representation stream", ex);
            }
        }
        return sb.toString();
    }
}
