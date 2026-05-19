package com.github.monodula.maven.rule.rules;

import com.github.monodula.maven.model.MavenModule;
import com.github.monodula.maven.model.ModuleType;
import com.github.monodula.maven.model.Violation;
import com.github.monodula.maven.model.ViolationReport;
import com.github.monodula.maven.rule.DependencyRule;

public class ApiCannotDependOnCoreRule implements DependencyRule {
    @Override
    public void apply(
            MavenModule source, MavenModule target, String location, ViolationReport report) {
        // API cannot depend on CORE, including common-core (layer=core regardless of COMMON type)
        if (source.getType() == ModuleType.API && "core".equals(target.getLayer())) {
            report.add(
                    new Violation(
                            "R003",
                            "api cannot depend on core layer",
                            "ERROR",
                            source.getArtifactId(),
                            target.getArtifactId(),
                            location));
        }
    }
}
