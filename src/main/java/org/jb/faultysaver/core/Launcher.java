package org.jb.faultysaver.core;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import picocli.CommandLine;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class Launcher implements Runnable {

    private static final Logger LOGGER = LogManager.getLogger(Launcher.class.getName());

    @CommandLine.Option(names={"-from"}, description="Where files migrate from", required=true)
    URI fromStorage;

    @CommandLine.Option(names={"-to"}, description="Where files migrate to", required=true)
    URI toStorage;

    public static void main(String[] args) {
        System.out.println("------------------------");
        System.out.println("------------------------");
        System.out.println("------------------------");
        System.out.println("THERE ARE UPDATES!");
        System.out.println("------------------------");
        System.out.println("------------------------");
        System.out.println("------------------------");
        int exitCode = new CommandLine(new Launcher()).execute(args);
    }

    @Override
    public void run(){
        try (CloseableHttpClient client = ClientFactory.createMultithreadedClientWithFixedPool()){
            FaultySaver faultySaver = new FaultySaver(client, fromStorage, toStorage);
            faultySaver.migrateFiles();
        } catch (InterruptedException ex) {
            LOGGER.error("Interrupted");
        } catch (URISyntaxException ex) {
            LOGGER.error("Wrong URI");
        } catch (IOException ex) {
            LOGGER.error("Resource unavailable");
        }
    }
}
