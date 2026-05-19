package com.github.monodula.maven.strategy;

import com.github.monodula.maven.model.MavenModule;
import java.util.List;

public class SplitContext {
    private final String moduleName;
    private final List<MavenModule> modules;
    private final String rootArtifactId;
    private final String groupId;
    private final String version;
    private final String basePackage;

    public SplitContext(String moduleName, List<MavenModule> modules) {
        this(moduleName, modules, null, null, null, null);
    }

    public SplitContext(
            String moduleName,
            List<MavenModule> modules,
            String rootArtifactId,
            String groupId,
            String version,
            String basePackage) {
        this.moduleName = moduleName;
        this.modules = modules;
        this.rootArtifactId = rootArtifactId;
        this.groupId = groupId;
        this.version = version;
        this.basePackage = basePackage;
    }

    public String getModuleName() {
        return moduleName;
    }

    public List<MavenModule> getModules() {
        return modules;
    }

    public String getRootArtifactId() {
        return rootArtifactId;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getVersion() {
        return version;
    }

    public String getBasePackage() {
        return basePackage;
    }
}
