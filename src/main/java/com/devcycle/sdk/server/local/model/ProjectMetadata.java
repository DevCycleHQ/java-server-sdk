package com.devcycle.sdk.server.local.model;

public class ProjectMetadata {
    public final String id;
    public final String key;

    public ProjectMetadata(String id, String key) {
        this.id = id;
        this.key = key;
    }

    public String getId() {
        return id;
    }

    public String getKey() {
        return key;
    }
}
