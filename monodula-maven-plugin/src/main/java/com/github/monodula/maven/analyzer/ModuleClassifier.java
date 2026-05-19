package com.github.monodula.maven.analyzer;

import com.github.monodula.maven.model.ModuleType;

public class ModuleClassifier {

    public static ModuleType classify(String artifactId) {
        if (artifactId == null || artifactId.isBlank()) {
            return ModuleType.UNKNOWN;
        }
        if (artifactId.startsWith("common-")) {
            return ModuleType.COMMON;
        }
        if (artifactId.endsWith("-api")) {
            return ModuleType.API;
        }
        if (artifactId.endsWith("-core")) {
            return ModuleType.CORE;
        }
        if (artifactId.endsWith("-app")) {
            return ModuleType.APP;
        }
        return ModuleType.UNKNOWN;
    }

    /**
     * Get the layer suffix of a module: api, core, app, or null. Works for both business modules
     * (finance-api) and common modules (common-api).
     */
    public static String layerOf(String artifactId) {
        if (artifactId == null || artifactId.isBlank()) {
            return null;
        }
        if (artifactId.endsWith("-api")) {
            return "api";
        }
        if (artifactId.endsWith("-core")) {
            return "core";
        }
        if (artifactId.endsWith("-app")) {
            return "app";
        }
        return null;
    }
}
