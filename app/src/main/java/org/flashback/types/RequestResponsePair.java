package org.flashback.types;

import java.io.Closeable;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class RequestResponsePair implements Closeable{
    public HttpServletRequest request;
    public HttpServletResponse response;
    public RequestResponsePair(HttpServletRequest request, HttpServletResponse response) {
        this.request = request;
        this.response = response;
    }

    public HttpServletRequest getRequest() {
        return request;
    }

    public HttpServletResponse getResponse() {
        return response;
    }

    @Override
    public void close() {
        var async = request.getAsyncContext();
        if (async != null) async.complete();
    }
}
