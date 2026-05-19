package com.github.monodula.archunit;

import com.tngtech.archunit.core.domain.JavaClass;

/**
 * Extracts monodula module/layer info from a class's package name.
 *
 * <p>Package convention: {@code {base}.{module}.{layer}.{rest}}
 *
 * <p>The layer is the <em>first</em> occurrence of "api", "core", or "app" as a standalone
 * dot-separated segment. The segment immediately before it is the module name. Everything before
 * the module is the base package.
 *
 * <p>Examples:
 *
 * <ul>
 *   <li>{@code com.yuanbao.growth.master.account.core.service.impl} →
 *       base="com.yuanbao.growth.master", module="account", layer="core"
 *   <li>{@code com.yuanbao.growth.master.account.core.account.service.api.impl} →
 *       base="com.yuanbao.growth.master", module="account", layer="core" (the "api" at deeper
 *       position is NOT a module-layer boundary)
 *   <li>{@code cn.hutool.core.collection} → base="cn", module="hutool", layer="core"
 * </ul>
 */
final class ModuleLayerInfo {

    final String base;
    final String module;
    final String layer;

    private ModuleLayerInfo(String base, String module, String layer) {
        this.base = base;
        this.module = module;
        this.layer = layer;
    }

    /**
     * Extracts module-layer info from the class's package name. Returns null if no layer segment
     * ("api", "core", "app") is found.
     */
    static ModuleLayerInfo from(JavaClass javaClass) {
        return fromPackageName(javaClass.getPackageName());
    }

    static ModuleLayerInfo fromPackageName(String packageName) {
        String[] parts = packageName.split("\\.");
        for (int i = 1; i < parts.length; i++) {
            if (isLayerName(parts[i])) {
                String module = parts[i - 1];
                String base = join(parts, 0, i - 1);
                return new ModuleLayerInfo(base, module, parts[i]);
            }
        }
        return null;
    }

    /**
     * Whether two classes share the same base package (and thus belong to the same project).
     * Third-party dependencies have a different base.
     */
    boolean sharesBaseWith(ModuleLayerInfo other) {
        return this.base.equals(other.base);
    }

    /** Whether two classes are in different modules within the same project. */
    boolean isDifferentModule(ModuleLayerInfo other) {
        return sharesBaseWith(other) && !this.module.equals(other.module);
    }

    boolean isCommonModule() {
        return "common".equals(module);
    }

    private static boolean isLayerName(String segment) {
        return "api".equals(segment) || "core".equals(segment) || "app".equals(segment);
    }

    private static String join(String[] parts, int start, int end) {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < end; i++) {
            if (i > start) {
                sb.append('.');
            }
            sb.append(parts[i]);
        }
        return sb.toString();
    }
}
