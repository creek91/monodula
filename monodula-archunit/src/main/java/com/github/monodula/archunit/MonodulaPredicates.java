package com.github.monodula.archunit;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;

/**
 * Predicates that identify monodula module layers from package names.
 *
 * <p>Monodula convention: {@code {base}.{module}.{layer}.{subpackages}}
 *
 * <p>Example: {@code com.yuanbao.growth.master.account.core.service.api.impl} → base =
 * "com.yuanbao.growth.master", module = "account", layer = "core"
 *
 * <p>The layer segment ({@code api}/{@code core}/{@code app}) is identified as the <em>first</em>
 * occurrence of a layer name in the dot-separated package, preceded by the module name segment.
 * This avoids false positives from internal sub-packages that happen to contain a layer name (e.g.
 * "service.api.impl" inside account-core).
 */
final class MonodulaPredicates {

    private MonodulaPredicates() {}

    /**
     * Matches classes whose package structure contains a module-layer boundary for the given layer
     * name. Only matches if the first layer-name segment in the package equals the specified layer.
     */
    static DescribedPredicate<JavaClass> resideInLayer(String layer) {
        return new DescribedPredicate<JavaClass>("reside in a %s layer package".formatted(layer)) {
            @Override
            public boolean test(JavaClass input) {
                ModuleLayerInfo info = ModuleLayerInfo.from(input);
                return info != null && info.layer.equals(layer);
            }
        };
    }

    /**
     * Matches classes in the "common" module (any layer), but NOT classes in sub-packages named
     * "common" inside business modules.
     *
     * <p>e.g. matches {@code com.example.project.common.core.*} but NOT {@code
     * com.example.project.material.core.common.*}
     */
    static DescribedPredicate<JavaClass> resideInCommonModule() {
        return new DescribedPredicate<JavaClass>("reside in the common module") {
            @Override
            public boolean test(JavaClass input) {
                ModuleLayerInfo info = ModuleLayerInfo.from(input);
                return info != null && info.module.equals("common");
            }
        };
    }
}
