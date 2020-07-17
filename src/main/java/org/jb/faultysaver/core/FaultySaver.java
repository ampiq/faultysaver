package org.jb.faultysaver.core;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.vdurmont.etaprinter.ETAPrinter;
import org.apache.http.HttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jb.faultysaver.core.exceptions.NoConnectionException;

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
import java.util.ListIterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
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
        System.out.println("");
        try {
            List<CompletableFuture<HttpResponse>> tasks = new ArrayList<>();
            List<String> filesFromOldStorage = getFiles(uriFrom);
            List<String> filesFromNewStorage = getFiles(uriTo);
            ETAPrinter printer = ETAPrinter.init("files", filesFromOldStorage.size());

            for (int i = 0; i < filesFromOldStorage.size(); ) {
                while (tasks.size() < BATCH_SIZE && i < filesFromOldStorage.size()) {
                    String fileName = filesFromOldStorage.get(i++);
                    String fullPathToFile = uriFrom.toURL() + URI_SEPARATOR + fileName;
                    URI fileFromUri = new URI(fullPathToFile);

                    if (!filesFromNewStorage.contains(fileName)) {
                        CompletableFuture<HttpResponse> downloadToUploadToDeleteFuture
                                = downloadAsync(fileFromUri).thenComposeAsync(result -> uploadAsync(uriTo, fileName, result))
                                                            .thenComposeAsync(result -> {
                                                                if (isSuccessful(result)) {
                                                                    return deleteAsync(fileFromUri);
                                                                }
                                                                return null;
                                                            });
                        tasks.add(downloadToUploadToDeleteFuture);
                    } else {
                        CompletableFuture<HttpResponse> deleteFuture = deleteAsync(fileFromUri);
                        tasks.add(deleteFuture);
                    }
                }
                removeTasksOnComplete(tasks);
                printer.update(BATCH_SIZE);
            }
        } catch (RuntimeException ex) {
            LOGGER.error("Migration was stopped, request was aborted: {}", ex.getMessage());
        } finally {
            downloadExecutor.shutdownNow();
            uploadExecutor.shutdownNow();
            deleteExecutor.shutdownNow();
        }
    }

    private CompletableFuture<HttpResponse> downloadAsync(URI uri) {
        return CompletableFuture.supplyAsync(() -> new DownloadRequestTask(client, uri).execute(), downloadExecutor);
    }

    private CompletableFuture<HttpResponse> uploadAsync(URI uri, String fileName, HttpResponse previousResponse) {
        return CompletableFuture.supplyAsync(() ->
                new UploadRequestTask(client, uri, fileName, previousResponse).execute(), uploadExecutor);
    }

    private CompletableFuture<HttpResponse> deleteAsync(URI uri) {
        return CompletableFuture.supplyAsync(() ->
                new DeleteRequestTask(client, uri).execute(), deleteExecutor);
    }

    private void removeTasksOnComplete(List<CompletableFuture<HttpResponse>> tasks) throws InterruptedException {
        while (!tasks.isEmpty()) {
            for(ListIterator<CompletableFuture<HttpResponse>> it = tasks.listIterator(); it.hasNext(); ) {
                CompletableFuture<HttpResponse> task = it.next();
                if(task.isDone()) {
                    throwIfExceptionOccurred(task);
                    it.remove();
                }
            }
            if(!tasks.isEmpty()) {
                Thread.sleep(100); //java 9  -> onSpinWait
            }
        }
    }

    private void throwIfExceptionOccurred(CompletableFuture<HttpResponse> task) {
        if (task.isCompletedExceptionally()) {
            try {
                task.join();
            } catch (CompletionException ex) {
                if (ex.getCause() instanceof NoConnectionException) {
                    URI hostURI = ((NoConnectionException) ex.getCause()).getHostURI();
                    throw new NoConnectionException(hostURI, ex);
                } else {
                    throw new RuntimeException(ex.getMessage(), ex.getCause());
                }
            }
        }
    }

    private boolean isSuccessful(HttpResponse response) {
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

    private List<String> getFiles(URI fromUri) throws IOException {
        Gson gson = new Gson();
        AbstractRequestTask getFilesTask = new GetFilesRequestTask(client, fromUri);
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
