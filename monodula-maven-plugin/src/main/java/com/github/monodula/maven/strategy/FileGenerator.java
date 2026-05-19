package com.github.monodula.maven.strategy;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

/** Generates files for a standalone Spring Boot application module. */
public class FileGenerator {

    /** Generates a pom.xml for the standalone application. */
    public void generatePom(SplitContext ctx, List<String> dependencies, File outputFile)
            throws IOException {
        String groupId = ctx.getGroupId();
        String rootArtifactId = ctx.getRootArtifactId();
        String version = ctx.getVersion();
        String module = ctx.getModuleName();

        StringBuilder deps = new StringBuilder();
        for (String dep : dependencies) {
            deps.append("        <dependency>\n");
            deps.append("            <groupId>").append(groupId).append("</groupId>\n");
            deps.append("            <artifactId>").append(dep).append("</artifactId>\n");
            deps.append("        </dependency>\n");
        }
        // Add Spring Boot starter
        deps.append("        <dependency>\n");
        deps.append("            <groupId>org.springframework.boot</groupId>\n");
        deps.append("            <artifactId>spring-boot-starter-web</artifactId>\n");
        deps.append("        </dependency>\n");
        // Add ArchUnit rules for architecture tests
        deps.append("        <dependency>\n");
        deps.append("            <groupId>io.github.creek91</groupId>\n");
        deps.append("            <artifactId>monodula-archunit</artifactId>\n");
        deps.append("            <version>0.1.0-SNAPSHOT</version>\n");
        deps.append("            <scope>test</scope>\n");
        deps.append("        </dependency>\n");

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n");
        sb.append("         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n");
        sb.append(
                "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n");
        sb.append("    <modelVersion>4.0.0</modelVersion>\n\n");
        sb.append("    <parent>\n");
        sb.append("        <groupId>").append(groupId).append("</groupId>\n");
        sb.append("        <artifactId>application</artifactId>\n");
        sb.append("        <version>").append(version).append("</version>\n");
        sb.append("    </parent>\n\n");
        sb.append("    <artifactId>").append(module).append("-application</artifactId>\n");
        sb.append("    <packaging>jar</packaging>\n\n");
        sb.append("    <dependencies>\n");
        sb.append(deps);
        sb.append("    </dependencies>\n\n");
        sb.append("    <build>\n");
        sb.append("        <plugins>\n");
        sb.append("            <plugin>\n");
        sb.append("                <groupId>org.springframework.boot</groupId>\n");
        sb.append("                <artifactId>spring-boot-maven-plugin</artifactId>\n");
        sb.append("            </plugin>\n");
        sb.append("        </plugins>\n");
        sb.append("    </build>\n");
        sb.append("</project>\n");

        writeFile(outputFile, sb.toString());
    }

    /** Generates the main application class. */
    public void generateMainClass(SplitContext ctx, File outputFile) throws IOException {
        String basePackage = ctx.getBasePackage();
        String module = ctx.getModuleName();
        String packageSegment = InProcessSplitStrategy.toPackageSegment(module);
        String className = InProcessSplitStrategy.toClassName(module);

        StringBuilder sb = new StringBuilder();
        sb.append("package ")
                .append(basePackage)
                .append(".app.")
                .append(packageSegment)
                .append(";\n\n");
        sb.append("import org.springframework.boot.SpringApplication;\n");
        sb.append("import org.springframework.boot.autoconfigure.SpringBootApplication;\n");
        sb.append("import org.springframework.context.annotation.ComponentScan;\n\n");
        sb.append("@SpringBootApplication\n");
        sb.append("@ComponentScan(\"").append(basePackage).append("\")\n");
        sb.append("public class ").append(className).append("StandaloneApplication {\n\n");
        sb.append("    public static void main(String[] args) {\n");
        sb.append("        SpringApplication.run(")
                .append(className)
                .append("StandaloneApplication.class, args);\n");
        sb.append("    }\n");
        sb.append("}\n");

        writeFile(outputFile, sb.toString());
    }

    /**
     * Generates application.yml. Each module self-registers its config via
     * EnvironmentPostProcessor, so no spring.config.import is needed here.
     */
    public void generateApplicationYml(SplitContext ctx, List<String> dependencies, File outputFile)
            throws IOException {
        String module = ctx.getModuleName();

        StringBuilder sb = new StringBuilder();
        sb.append("server:\n");
        sb.append("  port: 8080\n\n");
        sb.append("spring:\n");
        sb.append("  application:\n");
        sb.append("    name: ").append(module).append("-application\n");
        sb.append("  profiles:\n");
        sb.append("    active: dev\n\n");
        sb.append("apollo:\n");
        sb.append("  meta: ${APOLLO_META:http://localhost:8080}\n");
        sb.append("  bootstrap:\n");
        sb.append("    enabled: true\n");
        sb.append("    namespaces: application\n");

        writeFile(outputFile, sb.toString());
    }

    /** Generates a Dockerfile for the standalone application. */
    public void generateDockerfile(SplitContext ctx, File outputFile) throws IOException {
        String module = ctx.getModuleName();
        String version = ctx.getVersion();

        StringBuilder sb = new StringBuilder();
        sb.append("FROM eclipse-temurin:17-jre\n");
        sb.append("WORKDIR /app\n");
        sb.append("COPY target/")
                .append(module)
                .append("-application-")
                .append(version)
                .append(".jar app.jar\n");
        sb.append("ENTRYPOINT [\"java\", \"-jar\", \"app.jar\"]\n");

        writeFile(outputFile, sb.toString());
    }

    /** Generates an ArchitectureTest.java in the standalone application's test sources. */
    public void generateArchitectureTest(SplitContext ctx, File outputFile) throws IOException {
        String basePackage = ctx.getBasePackage();

        StringBuilder sb = new StringBuilder();
        sb.append("package ").append(basePackage).append(".arch;\n\n");
        sb.append("import com.github.monodula.archunit.MonodulaRules;\n");
        sb.append("import com.tngtech.archunit.core.importer.ImportOption;\n");
        sb.append("import com.tngtech.archunit.junit.AnalyzeClasses;\n");
        sb.append("import com.tngtech.archunit.junit.ArchTest;\n");
        sb.append("import com.tngtech.archunit.lang.ArchRule;\n\n");
        sb.append("@AnalyzeClasses(\n");
        sb.append("        packages = \"").append(basePackage).append("\",\n");
        sb.append("        importOptions = ImportOption.DoNotIncludeTests.class)\n");
        sb.append("class ArchitectureTest {\n\n");
        sb.append("    // R001: core cannot depend on another module's core\n");
        sb.append("    @ArchTest\n");
        sb.append("    ArchRule r001 = MonodulaRules.coreNotDependOnOtherCore();\n\n");
        sb.append("    // R002: core cannot depend on another module's app\n");
        sb.append("    @ArchTest\n");
        sb.append("    ArchRule r002 = MonodulaRules.coreNotDependOnOtherApp();\n\n");
        sb.append("    // R003: api cannot depend on any core\n");
        sb.append("    @ArchTest\n");
        sb.append("    ArchRule r003 = MonodulaRules.apiNotDependOnCore();\n\n");
        sb.append("    // R004: api cannot depend on any app\n");
        sb.append("    @ArchTest\n");
        sb.append("    ArchRule r004 = MonodulaRules.apiNotDependOnApp();\n\n");
        sb.append("    // R005: common cannot depend on business modules\n");
        sb.append("    @ArchTest\n");
        sb.append("    ArchRule r005 = MonodulaRules.commonNotDependOnBusiness();\n\n");
        sb.append("    // R006: app cannot depend on another module's app\n");
        sb.append("    @ArchTest\n");
        sb.append("    ArchRule r006 = MonodulaRules.appNotDependOnOtherApp();\n");
        sb.append("}\n");

        writeFile(outputFile, sb.toString());
    }

    private void writeFile(File file, String content) throws IOException {
        file.getParentFile().mkdirs();
        Files.writeString(file.toPath(), content);
    }
}
