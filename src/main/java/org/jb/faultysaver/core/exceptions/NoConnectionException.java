package org.jb.faultysaver.core.exceptions;

import java.net.URI;

public class NoConnectionException extends RuntimeException{

    private URI hostURI;

    public NoConnectionException(URI hostURI, Throwable err) {
        super("Connection to " + hostURI + " failed. Server unavailable.", err);
        this.hostURI = hostURI;
    }

    public URI getHostURI() {
        return hostURI;
    }
}
