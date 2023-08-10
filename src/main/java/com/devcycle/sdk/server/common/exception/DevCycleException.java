package com.devcycle.sdk.server.common.exception;

import com.devcycle.sdk.server.common.model.ErrorResponse;
import com.devcycle.sdk.server.common.model.HttpResponseCode;
import lombok.Getter;

@Getter
public class DevCycleException extends Exception {

    private final HttpResponseCode httpResponseCode;
    private final ErrorResponse errorResponse;

    public DevCycleException(HttpResponseCode httpResponseCode, ErrorResponse errorResponse) {
        super(errorResponse.getMessage());
        this.httpResponseCode = httpResponseCode;
        this.errorResponse = errorResponse;
    }

    public boolean isRetryable() {
        return httpResponseCode.code() >= HttpResponseCode.SERVER_ERROR.code();
    }
}
