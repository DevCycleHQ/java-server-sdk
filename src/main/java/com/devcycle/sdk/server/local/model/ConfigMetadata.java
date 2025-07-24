package com.devcycle.sdk.server.local.model;

public class ConfigMetadata {

    public final String configETag;
    public final String configLastModified;
    public final ProjectMetadata project;
    public final EnvironmentMetadata environment;

    public ConfigMetadata(String currentETag, String headerLastModified, Project project, Environment environment) {
        this.configETag = currentETag;
        this.configLastModified = headerLastModified;
        this.project = new ProjectMetadata(project._id, project.key);
        this.environment = new EnvironmentMetadata(environment._id, environment.key);
    }
}
