package com.github.monodula.maven.strategy;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SplitResult {
    private File pomXml;
    private File dockerfile;
    private File mainClass;
    private List<String> dependencies = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();
    private List<String> generatedFiles = new ArrayList<>();
    private String applicationYml;

    public File getPomXml() {
        return pomXml;
    }

    public void setPomXml(File pomXml) {
        this.pomXml = pomXml;
    }

    public File getDockerfile() {
        return dockerfile;
    }

    public void setDockerfile(File dockerfile) {
        this.dockerfile = dockerfile;
    }

    public File getMainClass() {
        return mainClass;
    }

    public void setMainClass(File mainClass) {
        this.mainClass = mainClass;
    }

    public List<String> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<String> dependencies) {
        this.dependencies = dependencies;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void addWarning(String warning) {
        warnings.add(warning);
    }

    public List<String> getGeneratedFiles() {
        return generatedFiles;
    }

    public void setGeneratedFiles(List<String> generatedFiles) {
        this.generatedFiles = generatedFiles;
    }

    public String getApplicationYml() {
        return applicationYml;
    }

    public void setApplicationYml(String applicationYml) {
        this.applicationYml = applicationYml;
    }

    // String-based path fields for generated output locations
    private String outputDir;
    private String mainClassPath;
    private String dockerfilePath;
    private String applicationYmlPath;

    public String getOutputDir() {
        return outputDir;
    }

    public void setOutputDir(String outputDir) {
        this.outputDir = outputDir;
    }

    public String getMainClassPath() {
        return mainClassPath;
    }

    public void setMainClassPath(String mainClassPath) {
        this.mainClassPath = mainClassPath;
    }

    public String getDockerfilePath() {
        return dockerfilePath;
    }

    public void setDockerfilePath(String dockerfilePath) {
        this.dockerfilePath = dockerfilePath;
    }

    public String getApplicationYmlPath() {
        return applicationYmlPath;
    }

    public void setApplicationYmlPath(String applicationYmlPath) {
        this.applicationYmlPath = applicationYmlPath;
    }
}
