package com.github.monodula.maven;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.monodula.maven.analyzer.ModuleClassifier;
import com.github.monodula.maven.model.MavenModule;
import com.github.monodula.maven.model.ModuleType;
import com.github.monodula.maven.model.ViolationReport;
import com.github.monodula.maven.rule.RuleEngine;
import com.github.monodula.maven.rule.rules.*;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class RuleEngineComprehensiveTest {

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

    /** Helper to create a MavenModule using ModuleClassifier for consistent classification. */
    private MavenModule module(String artifactId) {
        return new MavenModule(artifactId, ModuleClassifier.classify(artifactId));
    }

    // ─── R001: CoreCannotDependOnOtherCoreRule ─────────────────────────────

    @Nested
    @DisplayName("R001: CORE cannot depend on other CORE (COMMON type excluded)")
    class R001Tests {

        @Test
        @DisplayName("#1 finance-core -> account-core = VIOLATION")
        void coreDependsOnOtherCore_violation() {
            MavenModule source = module("finance-core");
            MavenModule target = module("account-core");

            ViolationReport report = engine.check(source, target, "pom.xml:10");

            assertThat(report.isClean()).isFalse();
            assertThat(report.getViolations()).anyMatch(v -> "R001".equals(v.getRuleId()));
        }

        @Test
        @DisplayName("#2 finance-core -> common-core = NO violation (COMMON type excluded)")
        void coreDependsOnCommonCore_noViolation() {
            MavenModule source = module("finance-core");
            MavenModule target = module("common-core");

            assertThat(target.getType()).isEqualTo(ModuleType.COMMON);
            assertThat(target.getLayer()).isEqualTo("core");

            ViolationReport report = engine.check(source, target, "pom.xml:10");

            assertThat(report.isClean()).isTrue();
        }
    }

    // ─── R002: CoreCannotDependOnOtherAppRule ──────────────────────────────

    @Nested
    @DisplayName("R002: CORE cannot depend on APP layer (including common-app)")
    class R002Tests {

        @Test
        @DisplayName("#4 finance-core -> account-app = VIOLATION")
        void coreDependsOnApp_violation() {
            MavenModule source = module("finance-core");
            MavenModule target = module("account-app");

            ViolationReport report = engine.check(source, target, "pom.xml:10");

            assertThat(report.isClean()).isFalse();
            assertThat(report.getViolations()).anyMatch(v -> "R002".equals(v.getRuleId()));
        }

        @Test
        @DisplayName("#5 finance-core -> common-app = VIOLATION (common-app has layer=app)")
        void coreDependsOnCommonApp_violation() {
            MavenModule source = module("finance-core");
            MavenModule target = module("common-app");

            assertThat(target.getType()).isEqualTo(ModuleType.COMMON);
            assertThat(target.getLayer()).isEqualTo("app");

            ViolationReport report = engine.check(source, target, "pom.xml:10");

            assertThat(report.isClean()).isFalse();
            assertThat(report.getViolations()).anyMatch(v -> "R002".equals(v.getRuleId()));
        }

        @Test
        @DisplayName("#6 finance-core -> common-core = NO violation (layer=core, not app)")
        void coreDependsOnCommonCore_noViolation() {
            MavenModule source = module("finance-core");
            MavenModule target = module("common-core");

            assertThat(target.getLayer()).isEqualTo("core");

            ViolationReport report = engine.check(source, target, "pom.xml:10");

            assertThat(report.isClean()).isTrue();
        }
    }

    // ─── R003: ApiCannotDependOnCoreRule ───────────────────────────────────

    @Nested
    @DisplayName("R003: API cannot depend on CORE layer (including common-core)")
    class R003Tests {

        @Test
        @DisplayName("#7 finance-api -> finance-core = VIOLATION")
        void apiDependsOnCore_violation() {
            MavenModule source = module("finance-api");
            MavenModule target = module("finance-core");

            ViolationReport report = engine.check(source, target, "pom.xml:10");

            assertThat(report.isClean()).isFalse();
            assertThat(report.getViolations()).anyMatch(v -> "R003".equals(v.getRuleId()));
        }

        @Test
        @DisplayName("#8 finance-api -> common-core = VIOLATION (common-core has layer=core)")
        void apiDependsOnCommonCore_violation() {
            MavenModule source = module("finance-api");
            MavenModule target = module("common-core");

            assertThat(target.getType()).isEqualTo(ModuleType.COMMON);
            assertThat(target.getLayer()).isEqualTo("core");

            ViolationReport report = engine.check(source, target, "pom.xml:10");

            assertThat(report.isClean()).isFalse();
            assertThat(report.getViolations()).anyMatch(v -> "R003".equals(v.getRuleId()));
        }

        @Test
        @DisplayName("#9 finance-api -> common-api = NO violation (layer=api, not core)")
        void apiDependsOnCommonApi_noViolation() {
            MavenModule source = module("finance-api");
            MavenModule target = module("common-api");

            assertThat(target.getLayer()).isEqualTo("api");

            ViolationReport report = engine.check(source, target, "pom.xml:10");

            assertThat(report.isClean()).isTrue();
        }
    }

    // ─── R004: ApiCannotDependOnAppRule ────────────────────────────────────

    @Nested
    @DisplayName("R004: API cannot depend on APP layer (including common-app)")
    class R004Tests {

        @Test
        @DisplayName("#10 finance-api -> finance-app = VIOLATION")
        void apiDependsOnApp_violation() {
            MavenModule source = module("finance-api");
            MavenModule target = module("finance-app");

            ViolationReport report = engine.check(source, target, "pom.xml:10");

            assertThat(report.isClean()).isFalse();
            assertThat(report.getViolations()).anyMatch(v -> "R004".equals(v.getRuleId()));
        }

        @Test
        @DisplayName("#11 finance-api -> common-app = VIOLATION (common-app has layer=app)")
        void apiDependsOnCommonApp_violation() {
            MavenModule source = module("finance-api");
            MavenModule target = module("common-app");

            assertThat(target.getType()).isEqualTo(ModuleType.COMMON);
            assertThat(target.getLayer()).isEqualTo("app");

            ViolationReport report = engine.check(source, target, "pom.xml:10");

            assertThat(report.isClean()).isFalse();
            assertThat(report.getViolations()).anyMatch(v -> "R004".equals(v.getRuleId()));
        }

        @Test
        @DisplayName("#12 finance-api -> common-core = R004 does NOT fire (layer=core, not app)")
        void apiDependsOnCommonCore_noR004Violation() {
            MavenModule source = module("finance-api");
            MavenModule target = module("common-core");

            assertThat(target.getLayer()).isEqualTo("core");

            ViolationReport report = engine.check(source, target, "pom.xml:10");

            // R003 fires because target layer=core, but R004 must not fire (layer is not "app")
            assertThat(report.getViolations()).noneMatch(v -> "R004".equals(v.getRuleId()));
        }
    }

    // ─── R005: CommonCannotDependOnBusinessRule ────────────────────────────

    @Nested
    @DisplayName("R005: COMMON cannot depend on business modules (by type)")
    class R005Tests {

        @Test
        @DisplayName("#13 common-api -> finance-api = VIOLATION")
        void commonApiDependsOnBusinessApi_violation() {
            MavenModule source = module("common-api");
            MavenModule target = module("finance-api");

            ViolationReport report = engine.check(source, target, "pom.xml:10");

            assertThat(report.isClean()).isFalse();
            assertThat(report.getViolations()).anyMatch(v -> "R005".equals(v.getRuleId()));
        }

        @Test
        @DisplayName("#14 common-core -> finance-core = VIOLATION")
        void commonCoreDependsOnBusinessCore_violation() {
            MavenModule source = module("common-core");
            MavenModule target = module("finance-core");

            ViolationReport report = engine.check(source, target, "pom.xml:10");

            assertThat(report.isClean()).isFalse();
            assertThat(report.getViolations()).anyMatch(v -> "R005".equals(v.getRuleId()));
        }

        @Test
        @DisplayName("#15 common-app -> account-app = VIOLATION")
        void commonAppDependsOnBusinessApp_violation() {
            MavenModule source = module("common-app");
            MavenModule target = module("account-app");

            ViolationReport report = engine.check(source, target, "pom.xml:10");

            assertThat(report.isClean()).isFalse();
            assertThat(report.getViolations()).anyMatch(v -> "R005".equals(v.getRuleId()));
        }

        @Test
        @DisplayName("#16 common-api -> common-core = NO violation (both COMMON)")
        void commonApiDependsOnCommonCore_noViolation() {
            MavenModule source = module("common-api");
            MavenModule target = module("common-core");

            assertThat(source.getType()).isEqualTo(ModuleType.COMMON);
            assertThat(target.getType()).isEqualTo(ModuleType.COMMON);

            ViolationReport report = engine.check(source, target, "pom.xml:10");

            assertThat(report.isClean()).isTrue();
        }

        @Test
        @DisplayName("#17 common-api -> common-app = NO violation (both COMMON)")
        void commonApiDependsOnCommonApp_noViolation() {
            MavenModule source = module("common-api");
            MavenModule target = module("common-app");

            assertThat(source.getType()).isEqualTo(ModuleType.COMMON);
            assertThat(target.getType()).isEqualTo(ModuleType.COMMON);

            ViolationReport report = engine.check(source, target, "pom.xml:10");

            assertThat(report.isClean()).isTrue();
        }
    }

    // ─── R006: AppCannotDependOnOtherAppRule ───────────────────────────────

    @Nested
    @DisplayName("R006: APP cannot depend on other APP (COMMON type excluded)")
    class R006Tests {

        @Test
        @DisplayName("#18 finance-app -> account-app = VIOLATION")
        void appDependsOnOtherApp_violation() {
            MavenModule source = module("finance-app");
            MavenModule target = module("account-app");

            ViolationReport report = engine.check(source, target, "pom.xml:10");

            assertThat(report.isClean()).isFalse();
            assertThat(report.getViolations()).anyMatch(v -> "R006".equals(v.getRuleId()));
        }

        @Test
        @DisplayName("#19 finance-app -> common-app = NO violation (COMMON type excluded)")
        void appDependsOnCommonApp_noViolation() {
            MavenModule source = module("finance-app");
            MavenModule target = module("common-app");

            assertThat(target.getType()).isEqualTo(ModuleType.COMMON);

            ViolationReport report = engine.check(source, target, "pom.xml:10");

            assertThat(report.isClean()).isTrue();
        }

        @Test
        @DisplayName("#20 finance-app -> finance-app = NO violation (same module)")
        void appDependsOnSelf_noViolation() {
            MavenModule source = module("finance-app");
            MavenModule target = module("finance-app");

            ViolationReport report = engine.check(source, target, "pom.xml:10");

            assertThat(report.isClean()).isTrue();
        }
    }

    // ─── Cross-domain scenarios ────────────────────────────────────────────

    @Nested
    @DisplayName("Cross-domain scenarios (adapter-core)")
    class CrossDomainTests {

        @Test
        @DisplayName("#21 adapter-core -> account-core = VIOLATION (R001)")
        void adapterCoreDependsOnAccountCore_violationR001() {
            MavenModule source = module("adapter-core");
            MavenModule target = module("account-core");

            ViolationReport report = engine.check(source, target, "pom.xml:10");

            assertThat(report.isClean()).isFalse();
            assertThat(report.getViolations()).anyMatch(v -> "R001".equals(v.getRuleId()));
        }

        @Test
        @DisplayName("#22 adapter-core -> account-app = VIOLATION (R002)")
        void adapterCoreDependsOnAccountApp_violationR002() {
            MavenModule source = module("adapter-core");
            MavenModule target = module("account-app");

            ViolationReport report = engine.check(source, target, "pom.xml:10");

            assertThat(report.isClean()).isFalse();
            assertThat(report.getViolations()).anyMatch(v -> "R002".equals(v.getRuleId()));
        }

        @Test
        @DisplayName("#23 adapter-core -> common-app = VIOLATION (R002)")
        void adapterCoreDependsOnCommonApp_violationR002() {
            MavenModule source = module("adapter-core");
            MavenModule target = module("common-app");

            assertThat(target.getType()).isEqualTo(ModuleType.COMMON);
            assertThat(target.getLayer()).isEqualTo("app");

            ViolationReport report = engine.check(source, target, "pom.xml:10");

            assertThat(report.isClean()).isFalse();
            assertThat(report.getViolations()).anyMatch(v -> "R002".equals(v.getRuleId()));
        }

        @Test
        @DisplayName("#24 adapter-core -> common-core = NO violation (R001 excludes COMMON)")
        void adapterCoreDependsOnCommonCore_noViolation() {
            MavenModule source = module("adapter-core");
            MavenModule target = module("common-core");

            assertThat(target.getType()).isEqualTo(ModuleType.COMMON);

            ViolationReport report = engine.check(source, target, "pom.xml:10");

            assertThat(report.isClean()).isTrue();
        }

        @Test
        @DisplayName("#25 adapter-core -> common-api = NO violation (API layer, no rule violation)")
        void adapterCoreDependsOnCommonApi_noViolation() {
            MavenModule source = module("adapter-core");
            MavenModule target = module("common-api");

            assertThat(target.getType()).isEqualTo(ModuleType.COMMON);
            assertThat(target.getLayer()).isEqualTo("api");

            ViolationReport report = engine.check(source, target, "pom.xml:10");

            assertThat(report.isClean()).isTrue();
        }
    }

    // ─── Dual-type verification ────────────────────────────────────────────

    @Nested
    @DisplayName("Common module dual-type behavior verification")
    class DualTypeVerificationTests {

        @Test
        @DisplayName("common-api: type=COMMON, layer=api")
        void commonApiClassification() {
            MavenModule mod = module("common-api");
            assertThat(mod.getType()).isEqualTo(ModuleType.COMMON);
            assertThat(mod.getLayer()).isEqualTo("api");
        }

        @Test
        @DisplayName("common-core: type=COMMON, layer=core")
        void commonCoreClassification() {
            MavenModule mod = module("common-core");
            assertThat(mod.getType()).isEqualTo(ModuleType.COMMON);
            assertThat(mod.getLayer()).isEqualTo("core");
        }

        @Test
        @DisplayName("common-app: type=COMMON, layer=app")
        void commonAppClassification() {
            MavenModule mod = module("common-app");
            assertThat(mod.getType()).isEqualTo(ModuleType.COMMON);
            assertThat(mod.getLayer()).isEqualTo("app");
        }

        @Test
        @DisplayName("R002 catches common-app via layer even though type=COMMON")
        void r002CatchesCommonAppViaLayer() {
            MavenModule source = module("finance-core");
            MavenModule target = module("common-app");

            ViolationReport report = engine.check(source, target, "pom.xml:5");

            assertThat(report.getViolations()).hasSize(1);
            assertThat(report.getViolations().get(0).getRuleId()).isEqualTo("R002");
        }

        @Test
        @DisplayName("R003 catches common-core via layer even though type=COMMON")
        void r003CatchesCommonCoreViaLayer() {
            MavenModule source = module("finance-api");
            MavenModule target = module("common-core");

            ViolationReport report = engine.check(source, target, "pom.xml:5");

            assertThat(report.getViolations()).hasSize(1);
            assertThat(report.getViolations().get(0).getRuleId()).isEqualTo("R003");
        }

        @Test
        @DisplayName("R004 catches common-app via layer even though type=COMMON")
        void r004CatchesCommonAppViaLayer() {
            MavenModule source = module("finance-api");
            MavenModule target = module("common-app");

            ViolationReport report = engine.check(source, target, "pom.xml:5");

            assertThat(report.getViolations()).hasSize(1);
            assertThat(report.getViolations().get(0).getRuleId()).isEqualTo("R004");
        }

        @Test
        @DisplayName("R001 excludes common-core because type check = COMMON (not CORE)")
        void r001ExcludesCommonCoreByType() {
            MavenModule source = module("finance-core");
            MavenModule target = module("common-core");

            ViolationReport report = engine.check(source, target, "pom.xml:5");

            assertThat(report.isClean()).isTrue();
        }

        @Test
        @DisplayName("R006 excludes common-app because type check = COMMON (not APP)")
        void r006ExcludesCommonAppByType() {
            MavenModule source = module("finance-app");
            MavenModule target = module("common-app");

            ViolationReport report = engine.check(source, target, "pom.xml:5");

            assertThat(report.isClean()).isTrue();
        }
    }

    // ─── Violation metadata tests ──────────────────────────────────────────

    @Nested
    @DisplayName("Violation report metadata")
    class ViolationMetadataTests {

        @Test
        @DisplayName("Violation contains correct source, target, and location")
        void violationMetadata() {
            MavenModule source = module("finance-core");
            MavenModule target = module("account-core");

            ViolationReport report = engine.check(source, target, "finance-core/pom.xml:42");

            assertThat(report.getViolations()).hasSizeGreaterThanOrEqualTo(1);
            var violation = report.getViolations().get(0);
            assertThat(violation.getSourceModule()).isEqualTo("finance-core");
            assertThat(violation.getTargetModule()).isEqualTo("account-core");
            assertThat(violation.getLocation()).isEqualTo("finance-core/pom.xml:42");
            assertThat(violation.getSeverity()).isEqualTo("ERROR");
        }

        @Test
        @DisplayName("getErrorCount returns count of ERROR-severity violations")
        void errorCount() {
            MavenModule source = module("finance-core");
            MavenModule target = module("account-core");

            ViolationReport report = engine.check(source, target, "pom.xml:10");

            assertThat(report.getErrorCount()).isGreaterThanOrEqualTo(1);
        }

        @Test
        @DisplayName("Clean dependency produces zero error count")
        void cleanDependencyZeroErrorCount() {
            MavenModule source = module("finance-core");
            MavenModule target = module("common-core");

            ViolationReport report = engine.check(source, target, "pom.xml:10");

            assertThat(report.getErrorCount()).isZero();
            assertThat(report.isClean()).isTrue();
        }
    }
}
