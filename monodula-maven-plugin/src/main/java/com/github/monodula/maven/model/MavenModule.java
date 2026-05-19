package com.github.monodula.maven.model;

import com.github.monodula.maven.analyzer.ModuleClassifier;
import java.io.File;
import java.util.Collections;
import java.util.List;

public class MavenModule {
    private final String artifactId;
    private final ModuleType type;
    private final String groupId;
    private final String version;
    private final File basedir;
    private final List<String> dependencies;

    public MavenModule(String artifactId, ModuleType type) {
        this(artifactId, type, null, null, null);
    }

    public MavenModule(
            String artifactId, ModuleType type, String groupId, String version, File basedir) {
        this(artifactId, type, groupId, version, basedir, Collections.emptyList());
    }

    public MavenModule(
            String artifactId,
            ModuleType type,
            String groupId,
            String version,
            File basedir,
            List<String> dependencies) {
        this.artifactId = artifactId;
        this.type = type;
        this.groupId = groupId;
        this.version = version;
        this.basedir = basedir;
        this.dependencies = dependencies != null ? dependencies : Collections.emptyList();
    }

    public String getArtifactId() {
        return artifactId;
    }

    public ModuleType getType() {
        return type;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getVersion() {
        return version;
    }

    public File getBasedir() {
        return basedir;
    }

    public List<String> getDependencies() {
        return dependencies;
    }

    /** Returns the layer: "api", "core", "app", or null */
    public String getLayer() {
        return ModuleClassifier.layerOf(artifactId);
    }
}
