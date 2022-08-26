package com.devcycle.sdk.server.common.model;

import java.util.Arrays;

public enum HttpResponseCode {

    OK(200),
    ACCEPTED(201),
    BAD_REQUEST(400),
    UNAUTHORIZED(401),
    NOT_FOUND(404),
    SERVER_ERROR(500);

    private final int code;

    HttpResponseCode(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }

    public static HttpResponseCode byCode(int code) {
        return Arrays.stream(HttpResponseCode.values())
                .filter(httpResponseCode -> httpResponseCode.code == code)
                .findFirst().orElse(HttpResponseCode.SERVER_ERROR);
    }
}
