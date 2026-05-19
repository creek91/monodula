package com.github.monodula.maven.strategy;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.monodula.maven.model.MavenModule;
import com.github.monodula.maven.model.ModuleType;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileGeneratorTest {

    @TempDir Path tempDir;

    private SplitContext ctx(String module) {
        List<MavenModule> modules =
                List.of(
                        new MavenModule("finance-app", ModuleType.APP),
                        new MavenModule("finance-core", ModuleType.CORE),
                        new MavenModule("common-api", ModuleType.COMMON));
        return new SplitContext(
                module, modules, "myproject", "com.example", "1.0.0", "com.example.myproject");
    }

    @Test
    void generated_pom_has_correct_parent() throws IOException {
        File outDir = tempDir.toFile();
        List<String> deps = List.of("finance-app", "finance-core", "common-api");
        File pomFile = new File(outDir, "pom.xml");

        new FileGenerator().generatePom(ctx("finance"), deps, pomFile);

        String content = Files.readString(pomFile.toPath());
        assertThat(content).contains("<artifactId>application</artifactId>");
        assertThat(content).contains("<groupId>com.example</groupId>");
        assertThat(content).contains("<version>1.0.0</version>");
    }

    @Test
    void generated_pom_contains_all_dependencies() throws IOException {
        File outDir = tempDir.toFile();
        List<String> deps = List.of("finance-app", "finance-core", "account-api", "common-api");
        File pomFile = new File(outDir, "pom.xml");

        new FileGenerator().generatePom(ctx("finance"), deps, pomFile);

        String content = Files.readString(pomFile.toPath());
        assertThat(content).contains("finance-app");
        assertThat(content).contains("finance-core");
        assertThat(content).contains("account-api");
        assertThat(content).contains("common-api");
    }

    @Test
    void generated_pom_has_spring_boot_plugin() throws IOException {
        File outDir = tempDir.toFile();
        List<String> deps = List.of("finance-app", "finance-core");
        File pomFile = new File(outDir, "pom.xml");

        new FileGenerator().generatePom(ctx("finance"), deps, pomFile);

        String content = Files.readString(pomFile.toPath());
        assertThat(content).contains("spring-boot-maven-plugin");
    }

    @Test
    void generated_main_class_has_correct_package_declaration() throws IOException {
        File mainClassFile = tempDir.resolve("FinanceStandaloneApplication.java").toFile();

        new FileGenerator().generateMainClass(ctx("finance"), mainClassFile);

        String content = Files.readString(mainClassFile.toPath());
        assertThat(content).contains("package com.example.myproject.app.finance;");
    }

    @Test
    void generated_main_class_has_component_scan() throws IOException {
        File mainClassFile = tempDir.resolve("FinanceStandaloneApplication.java").toFile();

        new FileGenerator().generateMainClass(ctx("finance"), mainClassFile);

        String content = Files.readString(mainClassFile.toPath());
        assertThat(content).contains("@ComponentScan(\"com.example.myproject\")");
    }

    @Test
    void generated_yml_has_no_config_import() throws IOException {
        File ymlFile = tempDir.resolve("application.yml").toFile();
        List<String> deps =
                List.of("finance-app", "finance-core", "account-api", "account-core", "common-api");

        new FileGenerator().generateApplicationYml(ctx("finance"), deps, ymlFile);

        String content = Files.readString(ymlFile.toPath());
        assertThat(content).doesNotContain("config:");
        assertThat(content).doesNotContain("import:");
    }

    @Test
    void generated_yml_has_no_classpath_references() throws IOException {
        File ymlFile = tempDir.resolve("application.yml").toFile();
        List<String> deps = List.of("finance-app", "finance-core", "common-api");

        new FileGenerator().generateApplicationYml(ctx("finance"), deps, ymlFile);

        String content = Files.readString(ymlFile.toPath());
        assertThat(content).doesNotContain("classpath:");
    }

    @Test
    void generated_yml_profiles_active_is_dev_only() throws IOException {
        File ymlFile = tempDir.resolve("application.yml").toFile();
        List<String> deps = List.of("finance-app", "finance-core");

        new FileGenerator().generateApplicationYml(ctx("finance"), deps, ymlFile);

        String content = Files.readString(ymlFile.toPath());
        assertThat(content).contains("active: dev");
        assertThat(content).doesNotContain("active: common,");
    }

    @Test
    void generated_yml_has_no_profiles_group() throws IOException {
        File ymlFile = tempDir.resolve("application.yml").toFile();
        List<String> deps = List.of("finance-app", "finance-core", "account-api");

        new FileGenerator().generateApplicationYml(ctx("finance"), deps, ymlFile);

        String content = Files.readString(ymlFile.toPath());
        assertThat(content).doesNotContain("group:");
    }

    @Test
    void generated_dockerfile_copies_correct_jar() throws IOException {
        File dockerFile = tempDir.resolve("Dockerfile").toFile();

        new FileGenerator().generateDockerfile(ctx("finance"), dockerFile);

        String content = Files.readString(dockerFile.toPath());
        assertThat(content).contains("finance-application-1.0.0.jar");
    }
}
