package com.github.monodula.maven;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.monodula.maven.model.MavenModule;
import com.github.monodula.maven.model.ModuleType;
import com.github.monodula.maven.model.ViolationReport;
import com.github.monodula.maven.rule.RuleEngine;
import com.github.monodula.maven.rule.rules.*;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RuleEngineTest {

    private RuleEngine engine;

    @BeforeEach
    void setUp() {
        engine =
                new RuleEngine(
                        List.of(
                                new CoreCannotDependOnOtherCoreRule(),
                                new CoreCannotDependOnOtherAppRule(),
                                new ApiCannotDependOnCoreRule(),
                                new ApiCannotDependOnAppRule(),
                                new CommonCannotDependOnBusinessRule(),
                                new AppCannotDependOnOtherAppRule()));
    }

    @Test
    void shouldDetectR001_CoreDependsOnOtherCore() {
        MavenModule source = new MavenModule("module-b-core", ModuleType.CORE);
        MavenModule target = new MavenModule("module-a-core", ModuleType.CORE);
        ViolationReport report = engine.check(source, target, "pom.xml:15");
        assertThat(report.getViolations()).hasSize(1);
        assertThat(report.getViolations().get(0).getRuleId()).isEqualTo("R001");
    }

    @Test
    void shouldDetectR002_CoreDependsOnOtherApp() {
        MavenModule source = new MavenModule("module-b-core", ModuleType.CORE);
        MavenModule target = new MavenModule("module-a-app", ModuleType.APP);
        ViolationReport report = engine.check(source, target, "pom.xml:15");
        assertThat(report.getViolations()).hasSize(1);
        assertThat(report.getViolations().get(0).getRuleId()).isEqualTo("R002");
    }

    @Test
    void shouldDetectR003_ApiDependsOnOwnCore() {
        MavenModule source = new MavenModule("module-a-api", ModuleType.API);
        MavenModule target = new MavenModule("module-a-core", ModuleType.CORE);
        ViolationReport report = engine.check(source, target, "pom.xml:15");
        assertThat(report.getViolations()).hasSize(1);
        assertThat(report.getViolations().get(0).getRuleId()).isEqualTo("R003");
    }

    @Test
    void shouldDetectR003_ApiDependsOnOtherCore() {
        MavenModule source = new MavenModule("module-a-api", ModuleType.API);
        MavenModule target = new MavenModule("module-b-core", ModuleType.CORE);
        ViolationReport report = engine.check(source, target, "pom.xml:15");
        assertThat(report.getViolations()).hasSize(1);
        assertThat(report.getViolations().get(0).getRuleId()).isEqualTo("R003");
    }

    @Test
    void shouldDetectR004_ApiDependsOnApp() {
        MavenModule source = new MavenModule("module-a-api", ModuleType.API);
        MavenModule target = new MavenModule("module-a-app", ModuleType.APP);
        ViolationReport report = engine.check(source, target, "pom.xml:15");
        assertThat(report.getViolations()).hasSize(1);
        assertThat(report.getViolations().get(0).getRuleId()).isEqualTo("R004");
    }

    @Test
    void shouldDetectR005_CommonDependsOnBusiness() {
        MavenModule source = new MavenModule("common-core", ModuleType.COMMON);
        MavenModule target = new MavenModule("module-a-api", ModuleType.API);
        ViolationReport report = engine.check(source, target, "pom.xml:15");
        assertThat(report.getViolations()).hasSize(1);
        assertThat(report.getViolations().get(0).getRuleId()).isEqualTo("R005");
    }

    @Test
    void shouldDetectR006_AppDependsOnOtherApp() {
        MavenModule source = new MavenModule("module-b-app", ModuleType.APP);
        MavenModule target = new MavenModule("module-a-app", ModuleType.APP);
        ViolationReport report = engine.check(source, target, "pom.xml:15");
        assertThat(report.getViolations()).hasSize(1);
        assertThat(report.getViolations().get(0).getRuleId()).isEqualTo("R006");
    }

    @Test
    void shouldAllowCoreDependsOnOtherApi() {
        MavenModule source = new MavenModule("module-b-core", ModuleType.CORE);
        MavenModule target = new MavenModule("module-a-api", ModuleType.API);
        ViolationReport report = engine.check(source, target, "pom.xml:15");
        assertThat(report.isClean()).isTrue();
    }

    @Test
    void shouldAllowAppDependsOnOwnCore() {
        MavenModule source = new MavenModule("module-a-app", ModuleType.APP);
        MavenModule target = new MavenModule("module-a-core", ModuleType.CORE);
        ViolationReport report = engine.check(source, target, "pom.xml:15");
        assertThat(report.isClean()).isTrue();
    }

    @Test
    void shouldAllowAppDependsOnCommon() {
        MavenModule source = new MavenModule("module-a-app", ModuleType.APP);
        MavenModule target = new MavenModule("common-app", ModuleType.COMMON);
        ViolationReport report = engine.check(source, target, "pom.xml:15");
        assertThat(report.isClean()).isTrue();
    }

    @Test
    void shouldReportNoViolationsForCleanProject() {
        MavenModule source = new MavenModule("module-a-core", ModuleType.CORE);
        MavenModule target = new MavenModule("module-b-api", ModuleType.API);
        ViolationReport report = engine.check(source, target, "pom.xml:10");
        assertThat(report.isClean()).isTrue();
        assertThat(report.getErrorCount()).isZero();
    }
}
