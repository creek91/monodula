package com.github.monodula.archunit;

import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;

/**
 * Monodula architecture boundary rules (R001-R006).
 *
 * <p>These rules enforce the three-layer module structure (api/core/app) in a modular monolith.
 * Each rule distinguishes between same-module and cross-module dependencies to avoid false
 * positives from internal references and third-party libraries.
 *
 * <p><b>Module structure convention:</b>
 *
 * <pre>
 * com.example.project.{module}.api    - API layer (DTOs, interfaces)
 * com.example.project.{module}.core   - Business implementation
 * com.example.project.{module}.app    - Entry points (Controllers, Jobs)
 * com.example.project.common.{layer}  - Shared common module
 * </pre>
 *
 * <p>Rules identify the module-layer boundary ({@code {module}.{layer}}) from the package
 * structure, then check whether source and target belong to the same project (sharing the same base
 * package) and same/different module to correctly enforce boundary constraints.
 */
public class MonodulaRules {

    private MonodulaRules() {}

    // ── R001: core cannot depend on other module's core ──────────────────────

    /**
     * No class in a {@code core} package may depend on a class in a {@code core} package of a
     * <em>different</em> module. Same-module core-to-core references are allowed. Third-party
     * dependencies (different base package) are ignored.
     */
    public static ArchRule coreNotDependOnOtherCore() {
        return ArchRuleDefinition.classes()
                .that(MonodulaPredicates.resideInLayer("core"))
                .should(ArchUnitConditions.notDependOnDifferentModuleLayer("core"))
                .allowEmptyShould(true)
                .because("R001: core cannot depend on other module's core");
    }

    // ── R002: core cannot depend on other module's app ───────────────────────

    /**
     * No class in a {@code core} package may depend on a class in an {@code app} package of
     * <em>any</em> module within the same project (including its own).
     */
    public static ArchRule coreNotDependOnOtherApp() {
        return ArchRuleDefinition.classes()
                .that(MonodulaPredicates.resideInLayer("core"))
                .should(ArchUnitConditions.notDependOnAnyModuleLayer("app"))
                .allowEmptyShould(true)
                .because("R002: core cannot depend on other module's app");
    }

    // ── R003: api cannot depend on core ──────────────────────────────────────

    /**
     * No class in an {@code api} package may depend on a class in a {@code core} package of
     * <em>any</em> module within the same project (including common-core).
     */
    public static ArchRule apiNotDependOnCore() {
        return ArchRuleDefinition.classes()
                .that(MonodulaPredicates.resideInLayer("api"))
                .should(ArchUnitConditions.notDependOnAnyModuleLayer("core"))
                .allowEmptyShould(true)
                .because("R003: api cannot depend on core");
    }

    // ── R004: api cannot depend on app ───────────────────────────────────────

    /**
     * No class in an {@code api} package may depend on a class in an {@code app} package of
     * <em>any</em> module within the same project.
     */
    public static ArchRule apiNotDependOnApp() {
        return ArchRuleDefinition.classes()
                .that(MonodulaPredicates.resideInLayer("api"))
                .should(ArchUnitConditions.notDependOnAnyModuleLayer("app"))
                .allowEmptyShould(true)
                .because("R004: api cannot depend on app");
    }

    // ── R005: common cannot depend on business modules ───────────────────────

    /**
     * No class in the {@code common} <em>module</em> (not a sub-package named "common" inside a
     * business module) may depend on classes in business modules of the same project. Third-party
     * and standard-library dependencies are allowed.
     */
    public static ArchRule commonNotDependOnBusiness() {
        return ArchRuleDefinition.classes()
                .that(MonodulaPredicates.resideInCommonModule())
                .should(ArchUnitConditions.notDependOnBusinessModules())
                .allowEmptyShould(true)
                .because("R005: common cannot depend on business modules");
    }

    // ── R006: app cannot depend on other module's app ────────────────────────

    /**
     * No class in an {@code app} package may depend on a class in an {@code app} package of a
     * <em>different</em> module. Same-module app-to-app references are allowed. Third-party
     * dependencies are ignored.
     */
    public static ArchRule appNotDependOnOtherApp() {
        return ArchRuleDefinition.classes()
                .that(MonodulaPredicates.resideInLayer("app"))
                .should(ArchUnitConditions.notDependOnDifferentModuleLayer("app"))
                .allowEmptyShould(true)
                .because("R006: app cannot depend on other module's app");
    }
}
