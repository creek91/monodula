package com.github.monodula.maven.model;

public class Violation {
    private final String ruleId;
    private final String description;
    private final String severity;
    private final String sourceModule;
    private final String targetModule;
    private final String location;

    public Violation(
            String ruleId,
            String description,
            String severity,
            String sourceModule,
            String targetModule,
            String location) {
        this.ruleId = ruleId;
        this.description = description;
        this.severity = severity;
        this.sourceModule = sourceModule;
        this.targetModule = targetModule;
        this.location = location;
    }

    public String getRuleId() {
        return ruleId;
    }

    public String getDescription() {
        return description;
    }

    public String getSeverity() {
        return severity;
    }

    public String getSourceModule() {
        return sourceModule;
    }

    public String getTargetModule() {
        return targetModule;
    }

    public String getLocation() {
        return location;
    }
}
