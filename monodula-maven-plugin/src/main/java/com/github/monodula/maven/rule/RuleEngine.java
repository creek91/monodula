package com.github.monodula.maven.rule;

import com.github.monodula.maven.model.MavenModule;
import com.github.monodula.maven.model.ViolationReport;
import java.util.List;

public class RuleEngine {
    private final List<DependencyRule> rules;

    public RuleEngine(List<DependencyRule> rules) {
        this.rules = rules;
    }

    public ViolationReport check(MavenModule source, MavenModule target, String location) {
        ViolationReport report = new ViolationReport();
        for (DependencyRule rule : rules) {
            rule.apply(source, target, location, report);
        }
        return report;
    }
}
