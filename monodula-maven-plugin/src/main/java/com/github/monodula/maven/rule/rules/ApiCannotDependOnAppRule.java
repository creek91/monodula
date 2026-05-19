package com.github.monodula.maven.rule.rules;

import com.github.monodula.maven.model.MavenModule;
import com.github.monodula.maven.model.ModuleType;
import com.github.monodula.maven.model.Violation;
import com.github.monodula.maven.model.ViolationReport;
import com.github.monodula.maven.rule.DependencyRule;

public class ApiCannotDependOnAppRule implements DependencyRule {
    @Override
    public void apply(
            MavenModule source, MavenModule target, String location, ViolationReport report) {
        // API cannot depend on APP, including common-app (layer=app regardless of COMMON type)
        if (source.getType() == ModuleType.API && "app".equals(target.getLayer())) {
            report.add(
                    new Violation(
                            "R004",
                            "api cannot depend on app layer",
                            "ERROR",
                            source.getArtifactId(),
                            target.getArtifactId(),
                            location));
        }
    }
}
