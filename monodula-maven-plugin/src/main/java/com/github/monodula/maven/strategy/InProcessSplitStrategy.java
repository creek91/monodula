package com.github.monodula.maven.strategy;

import com.github.monodula.maven.model.MavenModule;
import com.github.monodula.maven.model.ModuleType;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class InProcessSplitStrategy implements SplitStrategy {

    @Override
    public String getName() {
        return "in-process";
    }

    @Override
    public SplitResult split(SplitContext ctx) {
        String module = ctx.getModuleName();
        List<MavenModule> modules = ctx.getModules();

        // Validate target module has app and core
        boolean hasApp = modules.stream().anyMatch(m -> m.getArtifactId().equals(module + "-app"));
        boolean hasCore =
                modules.stream().anyMatch(m -> m.getArtifactId().equals(module + "-core"));

        if (!hasApp) {
            throw new IllegalArgumentException(
                    "Target module '" + module + "' has no -app submodule");
        }
        if (!hasCore) {
            throw new IllegalArgumentException(
                    "Target module '" + module + "' has no -core submodule");
        }

        // Build dependency list
        List<String> deps = new ArrayList<>();

        // 1. Target module's app and core
        deps.add(module + "-app");
        deps.add(module + "-core");

        // 2. Other business modules' api and core (not app, not target module)
        modules.stream()
                .filter(m -> m.getType() == ModuleType.API || m.getType() == ModuleType.CORE)
                .filter(m -> !m.getArtifactId().startsWith(module + "-"))
                .map(MavenModule::getArtifactId)
                .forEach(deps::add);

        // 3. All common modules
        modules.stream()
                .filter(m -> m.getType() == ModuleType.COMMON)
                .map(MavenModule::getArtifactId)
                .forEach(deps::add);

        // Compute paths
        String rootArtifactId = ctx.getRootArtifactId();
        String basePackage = ctx.getBasePackage();
        String packageSegment = toPackageSegment(module);
        String className = toClassName(module);

        String appDir = "application";
        String outputDir = appDir + "/" + module + "-application";
        String packagePath = (basePackage != null) ? basePackage.replace('.', '/') : "";
        String mainClassPath =
                outputDir
                        + "/src/main/java/"
                        + (packagePath.isEmpty() ? "" : packagePath + "/")
                        + "app/"
                        + packageSegment
                        + "/"
                        + className
                        + "StandaloneApplication.java";
        String dockerfilePath = outputDir + "/Dockerfile";
        String applicationYmlPath = outputDir + "/src/main/resources/application.yml";

        SplitResult result = new SplitResult();
        result.setDependencies(deps);
        result.setOutputDir(outputDir);
        result.setMainClassPath(mainClassPath);
        result.setDockerfilePath(dockerfilePath);
        result.setApplicationYmlPath(applicationYmlPath);
        // Also populate legacy File-based fields for backward compatibility
        result.setPomXml(new File(outputDir + "/pom.xml"));
        result.setDockerfile(new File(dockerfilePath));
        result.setMainClass(new File(mainClassPath));
        return result;
    }

    /** Converts a hyphenated module name to a Java package segment (no hyphens, lowercase). */
    public static String toPackageSegment(String moduleName) {
        return moduleName.replace("-", "").toLowerCase();
    }

    /** Converts a hyphenated module name to a Java class name prefix (each segment capitalized). */
    public static String toClassName(String moduleName) {
        return Arrays.stream(moduleName.split("-"))
                .map(s -> Character.toUpperCase(s.charAt(0)) + s.substring(1))
                .collect(Collectors.joining());
    }
}
