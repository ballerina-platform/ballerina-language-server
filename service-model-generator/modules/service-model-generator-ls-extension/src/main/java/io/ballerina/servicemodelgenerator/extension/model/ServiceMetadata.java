package io.ballerina.servicemodelgenerator.extension.model;

public record ServiceMetadata(String serviceType, String orgName, String packageName, String moduleName) {

    public ServiceMetadata(String serviceType) {
        this(serviceType, null, null, null);
    }
}
