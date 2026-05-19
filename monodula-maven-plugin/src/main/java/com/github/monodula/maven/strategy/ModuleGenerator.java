package com.github.monodula.maven.strategy;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/** Generates all files for a new business module. Does not modify any existing files. */
public class ModuleGenerator {

    /** Generates all directories and files for the given module under baseDirectory. */
    public void generate(AddContext ctx, File baseDirectory) throws IOException {
        String module = ctx.getModuleName();
        String groupId = ctx.getGroupId();
        String version = ctx.getVersion();
        String basePackage = ctx.getBasePackage();
        String packageSegment = ctx.getPackageSegment();
        String className = ctx.getClassName();

        File moduleDir = new File(baseDirectory, module);

        // Aggregator pom
        writeFile(new File(moduleDir, "pom.xml"), buildAggregatorPom(ctx));

        // -api
        File apiDir = new File(moduleDir, module + "-api");
        writeFile(
                new File(apiDir, "pom.xml"),
                buildSubPom(groupId, module, version, module + "-api", null));
        mkdirs(new File(apiDir, "src/test/java"));
        mkdirs(new File(apiDir, "src/test/resources"));

        // -core
        File coreDir = new File(moduleDir, module + "-core");
        writeFile(new File(coreDir, "pom.xml"), buildCorePom(ctx));

        String packagePath = basePackage.replace('.', '/') + "/" + packageSegment;
        File configDir = new File(coreDir, "src/main/java/" + packagePath + "/config");
        mkdirs(configDir);
        writeFile(new File(configDir, className + "ConfigLoader.java"), buildConfigLoader(ctx));

        File metaInfDir = new File(coreDir, "src/main/resources/META-INF");
        mkdirs(metaInfDir);
        writeFile(new File(metaInfDir, "spring.factories"), buildSpringFactories(ctx));

        File resourcesDir = new File(coreDir, "src/main/resources");
        writeFile(new File(resourcesDir, module + "-config.yml"), "");
        writeFile(new File(resourcesDir, module + "-config-dev.yml"), "");
        writeFile(new File(resourcesDir, module + "-config-test.yml"), "");
        writeFile(new File(resourcesDir, module + "-config-prod.yml"), "");

        mkdirs(new File(coreDir, "src/test/java"));
        mkdirs(new File(coreDir, "src/test/resources"));

        // -app
        File appDir = new File(moduleDir, module + "-app");
        writeFile(new File(appDir, "pom.xml"), buildAppPom(ctx));

        String controllerPath =
                basePackage.replace('.', '/') + "/" + packageSegment + "/app/endpoints/controller";
        File controllerDir = new File(appDir, "src/main/java/" + controllerPath);
        mkdirs(controllerDir);
        writeFile(new File(controllerDir, className + "Controller.java"), buildController(ctx));

        mkdirs(new File(appDir, "src/test/java"));
        mkdirs(new File(appDir, "src/test/resources"));
    }

    private String buildAggregatorPom(AddContext ctx) {
        String m = ctx.getModuleName();
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n"
                + "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                + "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0"
                + " http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n"
                + "    <modelVersion>4.0.0</modelVersion>\n\n"
                + "    <parent>\n"
                + "        <groupId>"
                + ctx.getGroupId()
                + "</groupId>\n"
                + "        <artifactId>"
                + ctx.getRootArtifactId()
                + "</artifactId>\n"
                + "        <version>"
                + ctx.getVersion()
                + "</version>\n"
                + "    </parent>\n\n"
                + "    <artifactId>"
                + m
                + "</artifactId>\n"
                + "    <packaging>pom</packaging>\n\n"
                + "    <modules>\n"
                + "        <module>"
                + m
                + "-api</module>\n"
                + "        <module>"
                + m
                + "-core</module>\n"
                + "        <module>"
                + m
                + "-app</module>\n"
                + "    </modules>\n"
                + "</project>\n";
    }

    private String buildSubPom(
            String groupId,
            String parentArtifactId,
            String version,
            String ownArtifactId,
            String extraDeps) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n");
        sb.append("         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n");
        sb.append(
                "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0"
                        + " http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n");
        sb.append("    <modelVersion>4.0.0</modelVersion>\n\n");
        sb.append("    <parent>\n");
        sb.append("        <groupId>").append(groupId).append("</groupId>\n");
        sb.append("        <artifactId>").append(parentArtifactId).append("</artifactId>\n");
        sb.append("        <version>").append(version).append("</version>\n");
        sb.append("    </parent>\n\n");
        sb.append("    <artifactId>").append(ownArtifactId).append("</artifactId>\n");
        if (extraDeps != null) {
            sb.append("\n    <dependencies>\n").append(extraDeps).append("    </dependencies>\n");
        }
        sb.append("</project>\n");
        return sb.toString();
    }

    private String buildCorePom(AddContext ctx) {
        String m = ctx.getModuleName();
        String g = ctx.getGroupId();
        String deps = dep(g, m + "-api") + dep(g, "common-api") + dep(g, "common-core");
        return buildSubPom(g, m, ctx.getVersion(), m + "-core", deps);
    }

    private String buildAppPom(AddContext ctx) {
        String m = ctx.getModuleName();
        String g = ctx.getGroupId();
        String deps =
                dep(g, m + "-core")
                        + "        <dependency>\n"
                        + "            <groupId>org.springframework.boot</groupId>\n"
                        + "            <artifactId>spring-boot-starter-web</artifactId>\n"
                        + "        </dependency>\n";
        return buildSubPom(g, m, ctx.getVersion(), m + "-app", deps);
    }

    private String dep(String groupId, String artifactId) {
        return "        <dependency>\n"
                + "            <groupId>"
                + groupId
                + "</groupId>\n"
                + "            <artifactId>"
                + artifactId
                + "</artifactId>\n"
                + "            <version>${project.version}</version>\n"
                + "        </dependency>\n";
    }

    private String buildConfigLoader(AddContext ctx) {
        String bp = ctx.getBasePackage();
        String ps = ctx.getPackageSegment();
        String cn = ctx.getClassName();
        String m = ctx.getModuleName();
        return "package "
                + bp
                + "."
                + ps
                + ".config;\n\n"
                + "import "
                + bp
                + ".common.core.config.ModuleConfigLoader;\n\n"
                + "public class "
                + cn
                + "ConfigLoader extends ModuleConfigLoader {\n\n"
                + "    @Override\n"
                + "    protected String getBaseName() {\n"
                + "        return \""
                + m
                + "-config\";\n"
                + "    }\n"
                + "}\n";
    }

    private String buildSpringFactories(AddContext ctx) {
        String bp = ctx.getBasePackage();
        String ps = ctx.getPackageSegment();
        String cn = ctx.getClassName();
        return "org.springframework.boot.env.EnvironmentPostProcessor=\\\n"
                + "  "
                + bp
                + "."
                + ps
                + ".config."
                + cn
                + "ConfigLoader\n";
    }

    private String buildController(AddContext ctx) {
        String bp = ctx.getBasePackage();
        String ps = ctx.getPackageSegment();
        String cn = ctx.getClassName();
        String m = ctx.getModuleName();
        return "package "
                + bp
                + "."
                + ps
                + ".app.endpoints.controller;\n\n"
                + "import org.springframework.web.bind.annotation.RequestMapping;\n"
                + "import org.springframework.web.bind.annotation.RestController;\n\n"
                + "@RestController\n"
                + "@RequestMapping(\"/"
                + m
                + "\")\n"
                + "public class "
                + cn
                + "Controller {\n"
                + "}\n";
    }

    private void writeFile(File file, String content) throws IOException {
        file.getParentFile().mkdirs();
        Files.writeString(file.toPath(), content);
    }

    private void mkdirs(File dir) {
        dir.mkdirs();
    }
}
