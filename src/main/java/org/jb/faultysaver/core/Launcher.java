package org.jb.faultysaver.core;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import picocli.CommandLine;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.Callable;

public class Launcher implements Runnable {

    private static final Logger LOGGER = LogManager.getLogger(DeleteRequestTask.class.getName());

    @CommandLine.Option(names={"-from"}, description="Where files migrate from", required=true)
    URI fromStorage;

    @CommandLine.Option(names={"-to"}, description="Where files migrate to", required=true)
    URI toStorage;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Launcher()).execute(args);
    }

    @Override
    public void run(){
        try (CloseableHttpClient client = ClientFactory.createMultithreadedClientWithFixedPool()){
            FaultySaver faultySaver = new FaultySaver(client, fromStorage, toStorage);
            faultySaver.migrateFiles();
            System.out.println("Done!");
        } catch (InterruptedException ex) {
            LOGGER.error("Interrupted");
        } catch (URISyntaxException ex) {
            LOGGER.error("Wrong URI");
        } catch (IOException ex) {
            LOGGER.error("Resource unavailable");
        }
    }
}