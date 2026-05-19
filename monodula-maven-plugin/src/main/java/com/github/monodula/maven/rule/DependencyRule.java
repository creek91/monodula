package com.github.monodula.maven.rule;

import com.github.monodula.maven.model.MavenModule;
import com.github.monodula.maven.model.ViolationReport;

public interface DependencyRule {
    void apply(MavenModule source, MavenModule target, String location, ViolationReport report);
}
