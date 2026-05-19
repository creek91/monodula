package com.github.monodula.maven.rule.rules;

import com.github.monodula.maven.model.MavenModule;
import com.github.monodula.maven.model.ModuleType;
import com.github.monodula.maven.model.Violation;
import com.github.monodula.maven.model.ViolationReport;
import com.github.monodula.maven.rule.DependencyRule;

public class CoreCannotDependOnOtherAppRule implements DependencyRule {
    @Override
    public void apply(
            MavenModule source, MavenModule target, String location, ViolationReport report) {
        // CORE cannot depend on APP, including common-app (layer=app regardless of COMMON type)
        if (source.getType() == ModuleType.CORE && "app".equals(target.getLayer())) {
            report.add(
                    new Violation(
                            "R002",
                            "core cannot depend on app layer",
                            "ERROR",
                            source.getArtifactId(),
                            target.getArtifactId(),
                            location));
        }
    }
}
