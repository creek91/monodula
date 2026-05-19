package com.github.monodula.maven.rule.rules;

import com.github.monodula.maven.model.MavenModule;
import com.github.monodula.maven.model.ModuleType;
import com.github.monodula.maven.model.Violation;
import com.github.monodula.maven.model.ViolationReport;
import com.github.monodula.maven.rule.DependencyRule;

public class AppCannotDependOnOtherAppRule implements DependencyRule {
    @Override
    public void apply(
            MavenModule source, MavenModule target, String location, ViolationReport report) {
        // APP cannot depend on another business module's APP
        // common-app is COMMON type, so naturally excluded
        // Self-dependency is handled by R007
        if (source.getType() == ModuleType.APP
                && target.getType() == ModuleType.APP
                && !source.getArtifactId().equals(target.getArtifactId())) {
            report.add(
                    new Violation(
                            "R006",
                            "app cannot depend on other module's app",
                            "ERROR",
                            source.getArtifactId(),
                            target.getArtifactId(),
                            location));
        }
    }
}
