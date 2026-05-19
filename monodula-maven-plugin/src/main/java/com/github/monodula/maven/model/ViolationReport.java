package com.github.monodula.maven.model;

import java.util.ArrayList;
import java.util.List;

public class ViolationReport {
    private final List<Violation> violations = new ArrayList<>();

    public void add(Violation violation) {
        violations.add(violation);
    }

    public List<Violation> getViolations() {
        return new ArrayList<>(violations);
    }

    public boolean isClean() {
        return violations.isEmpty();
    }

    public int getErrorCount() {
        return (int) violations.stream().filter(v -> "ERROR".equals(v.getSeverity())).count();
    }
}
