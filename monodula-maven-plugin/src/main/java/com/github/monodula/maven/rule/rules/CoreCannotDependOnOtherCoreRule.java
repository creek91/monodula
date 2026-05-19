package com.github.monodula.maven.rule.rules;

import com.github.monodula.maven.model.MavenModule;
import com.github.monodula.maven.model.ModuleType;
import com.github.monodula.maven.model.Violation;
import com.github.monodula.maven.model.ViolationReport;
import com.github.monodula.maven.rule.DependencyRule;

public class CoreCannotDependOnOtherCoreRule implements DependencyRule {
    @Override
    public void apply(
            MavenModule source, MavenModule target, String location, ViolationReport report) {
        // CORE cannot depend on another business module's CORE
        // common-core is COMMON type, so naturally excluded
        if (source.getType() == ModuleType.CORE && target.getType() == ModuleType.CORE) {
            report.add(
                    new Violation(
                            "R001",
                            "core cannot depend on other module's core",
                            "ERROR",
                            source.getArtifactId(),
                            target.getArtifactId(),
                            location));
        }
    }
}
