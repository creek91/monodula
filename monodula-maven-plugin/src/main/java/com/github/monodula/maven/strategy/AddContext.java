package com.github.monodula.maven.strategy;

/** Carries all data needed to generate a new business module. */
public class AddContext {

    private final String moduleName;
    private final String groupId;
    private final String rootArtifactId;
    private final String version;
    private final String basePackage;
    private final String packageSegment;
    private final String className;

    public AddContext(String moduleName, String groupId, String rootArtifactId, String version) {
        this(
                moduleName,
                groupId,
                rootArtifactId,
                version,
                groupId + "." + rootArtifactId.replace("-", ""));
    }

    public AddContext(
            String moduleName,
            String groupId,
            String rootArtifactId,
            String version,
            String basePackage) {
        this.moduleName = moduleName;
        this.groupId = groupId;
        this.rootArtifactId = rootArtifactId;
        this.version = version;
        this.basePackage = basePackage;
        this.packageSegment = InProcessSplitStrategy.toPackageSegment(moduleName);
        this.className = InProcessSplitStrategy.toClassName(moduleName);
    }

    public String getModuleName() {
        return moduleName;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getRootArtifactId() {
        return rootArtifactId;
    }

    public String getVersion() {
        return version;
    }

    public String getBasePackage() {
        return basePackage;
    }

    public String getPackageSegment() {
        return packageSegment;
    }

    public String getClassName() {
        return className;
    }
}
