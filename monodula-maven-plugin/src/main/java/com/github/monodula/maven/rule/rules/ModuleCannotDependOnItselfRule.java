package com.github.monodula.maven.rule.rules;

import com.github.monodula.maven.model.MavenModule;
import com.github.monodula.maven.model.Violation;
import com.github.monodula.maven.model.ViolationReport;
import com.github.monodula.maven.rule.DependencyRule;

public class ModuleCannotDependOnItselfRule implements DependencyRule {
    @Override
    public void apply(
            MavenModule source, MavenModule target, String location, ViolationReport report) {
        if (source.getArtifactId().equals(target.getArtifactId())) {
            report.add(
                    new Violation(
                            "R007",
                            "module cannot depend on itself (circular dependency)",
                            "ERROR",
                            source.getArtifactId(),
                            target.getArtifactId(),
                            location));
        }
    }
}
