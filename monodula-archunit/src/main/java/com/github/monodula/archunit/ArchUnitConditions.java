package com.github.monodula.archunit;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

/**
 * Custom ArchUnit conditions for monodula boundary checks.
 *
 * <p>These conditions check dependencies by extracting module/layer info from both the source class
 * and each dependency target. Third-party dependencies (different base package) are ignored. Only
 * intra-project cross-module or cross-layer violations are flagged.
 */
final class ArchUnitConditions {

    private ArchUnitConditions() {}

    /**
     * Condition: source class should not depend on classes in the specified layer of a
     * <em>different</em> module within the same project.
     *
     * <p>Same-module dependencies are allowed. Dependencies on the {@code common} module (shared
     * infrastructure) are also allowed. Third-party dependencies are ignored.
     */
    static ArchCondition<JavaClass> notDependOnDifferentModuleLayer(String targetLayer) {
        return new ArchCondition<JavaClass>(
                "not depend on %s layer of a different module".formatted(targetLayer)) {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                ModuleLayerInfo sourceInfo = ModuleLayerInfo.from(item);
                if (sourceInfo == null) {
                    return;
                }
                for (Dependency dep : item.getDirectDependenciesFromSelf()) {
                    JavaClass target = dep.getTargetClass();
                    ModuleLayerInfo targetInfo = ModuleLayerInfo.from(target);
                    if (targetInfo == null) {
                        continue;
                    }
                    // Skip third-party (different base package)
                    if (!sourceInfo.sharesBaseWith(targetInfo)) {
                        continue;
                    }
                    // Allow dependencies on the common module (shared infrastructure)
                    if (targetInfo.isCommonModule()) {
                        continue;
                    }
                    // Flag: different module AND target in specified layer
                    if (sourceInfo.isDifferentModule(targetInfo)
                            && targetInfo.layer.equals(targetLayer)) {
                        events.add(
                                SimpleConditionEvent.violated(
                                        item,
                                        "%s [%s.%s → %s.%s] depends on %s layer of different module %s"
                                                .formatted(
                                                        item.getSimpleName(),
                                                        sourceInfo.module,
                                                        sourceInfo.layer,
                                                        targetInfo.module,
                                                        targetInfo.layer,
                                                        targetLayer,
                                                        targetInfo.module)));
                    }
                }
            }
        };
    }

    /**
     * Condition: source class should not depend on classes in the specified layer of <em>any</em>
     * module within the same project (including its own module and the common module).
     *
     * <p>Third-party dependencies are ignored.
     */
    static ArchCondition<JavaClass> notDependOnAnyModuleLayer(String targetLayer) {
        return new ArchCondition<JavaClass>(
                "not depend on %s layer of any module".formatted(targetLayer)) {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                ModuleLayerInfo sourceInfo = ModuleLayerInfo.from(item);
                if (sourceInfo == null) {
                    return;
                }
                for (Dependency dep : item.getDirectDependenciesFromSelf()) {
                    JavaClass target = dep.getTargetClass();
                    ModuleLayerInfo targetInfo = ModuleLayerInfo.from(target);
                    if (targetInfo == null) {
                        continue;
                    }
                    // Skip third-party (different base package)
                    if (!sourceInfo.sharesBaseWith(targetInfo)) {
                        continue;
                    }
                    // Flag any intra-project dependency on the specified layer
                    if (targetInfo.layer.equals(targetLayer)) {
                        events.add(
                                SimpleConditionEvent.violated(
                                        item,
                                        "%s [%s.%s → %s.%s] depends on %s layer of module %s"
                                                .formatted(
                                                        item.getSimpleName(),
                                                        sourceInfo.module,
                                                        sourceInfo.layer,
                                                        targetInfo.module,
                                                        targetInfo.layer,
                                                        targetLayer,
                                                        targetInfo.module)));
                    }
                }
            }
        };
    }

    /**
     * Condition: classes in the common module should not depend on classes in business modules
     * (non-common modules within the same project).
     *
     * <p>Dependencies on third-party packages, standard libraries, and other common-module classes
     * are allowed.
     */
    static ArchCondition<JavaClass> notDependOnBusinessModules() {
        return new ArchCondition<JavaClass>("not depend on business modules") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                ModuleLayerInfo sourceInfo = ModuleLayerInfo.from(item);
                if (sourceInfo == null) {
                    return;
                }
                for (Dependency dep : item.getDirectDependenciesFromSelf()) {
                    JavaClass target = dep.getTargetClass();
                    ModuleLayerInfo targetInfo = ModuleLayerInfo.from(target);
                    if (targetInfo == null) {
                        // No layer info → standard/third-party class, allow
                        continue;
                    }
                    // Only check intra-project dependencies
                    if (!sourceInfo.sharesBaseWith(targetInfo)) {
                        continue;
                    }
                    // Flag if target belongs to a business (non-common) module
                    if (!targetInfo.isCommonModule()) {
                        events.add(
                                SimpleConditionEvent.violated(
                                        item,
                                        "%s [common.%s → %s.%s] depends on business module %s"
                                                .formatted(
                                                        item.getSimpleName(),
                                                        sourceInfo.layer,
                                                        targetInfo.module,
                                                        targetInfo.layer,
                                                        targetInfo.module)));
                    }
                }
            }
        };
    }
}
