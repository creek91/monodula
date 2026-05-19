package com.github.monodula.maven.rule.rules;

import com.github.monodula.maven.model.MavenModule;
import com.github.monodula.maven.model.ModuleType;
import com.github.monodula.maven.model.Violation;
import com.github.monodula.maven.model.ViolationReport;
import com.github.monodula.maven.rule.DependencyRule;

public class CommonCannotDependOnBusinessRule implements DependencyRule {
    @Override
    public void apply(
            MavenModule source, MavenModule target, String location, ViolationReport report) {
        if (source.getType() == ModuleType.COMMON
                && target.getType() != ModuleType.COMMON
                && target.getType() != ModuleType.UNKNOWN) {
            report.add(
                    new Violation(
                            "R005",
                            "common cannot depend on business modules",
                            "ERROR",
                            source.getArtifactId(),
                            target.getArtifactId(),
                            location));
        }
    }
}
