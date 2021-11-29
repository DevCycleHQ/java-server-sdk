package com.devcycle.sdk.server.exception;

import com.devcycle.sdk.server.model.ErrorResponse;
import com.devcycle.sdk.server.model.HttpResponseCode;
import lombok.Getter;

@Getter
public class DVCException extends Exception {

    private final HttpResponseCode httpResponseCode;
    private final ErrorResponse errorResponse;

    public DVCException(HttpResponseCode httpResponseCode, ErrorResponse errorResponse) {
        super(errorResponse.getMessage());
        this.httpResponseCode = httpResponseCode;
        this.errorResponse = errorResponse;
    }
}
