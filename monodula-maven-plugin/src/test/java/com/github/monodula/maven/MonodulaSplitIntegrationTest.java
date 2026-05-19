package com.github.monodula.maven;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Comprehensive integration tests for monodula:split. Tests the full Mojo + Strategy +
 * FileGenerator + PomEditor pipeline end-to-end.
 */
class MonodulaSplitIntegrationTest {

    @TempDir Path tempDir;

    // ============================================================
    // Helper methods
    // ============================================================

    private void writePom(Path dir, String groupId, String artifactId, String version)
            throws IOException {
        Files.createDirectories(dir);
        String pom =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<project>\n"
                        + "    <groupId>"
                        + groupId
                        + "</groupId>\n"
                        + "    <artifactId>"
                        + artifactId
                        + "</artifactId>\n"
                        + "    <version>"
                        + version
                        + "</version>\n"
                        + "</project>";
        Files.writeString(dir.resolve("pom.xml"), pom);
    }

    private Path createMultiModuleProject(String rootArtifactId) throws IOException {
        Path root = tempDir.resolve(rootArtifactId);
        writePom(root, "com.example", rootArtifactId, "1.0.0");
        return root;
    }

    private void addBusinessModule(Path root, String moduleName) throws IOException {
        Path modDir = root.resolve(moduleName);
        writePom(modDir, "com.example", moduleName, "1.0.0");
        writePom(modDir.resolve(moduleName + "-api"), "com.example", moduleName + "-api", "1.0.0");
        writePom(
                modDir.resolve(moduleName + "-core"), "com.example", moduleName + "-core", "1.0.0");
        writePom(modDir.resolve(moduleName + "-app"), "com.example", moduleName + "-app", "1.0.0");
    }

    private void addCommonModules(Path root) throws IOException {
        Path commonDir = root.resolve("common");
        writePom(commonDir, "com.example", "common", "1.0.0");
        writePom(commonDir.resolve("common-api"), "com.example", "common-api", "1.0.0");
        writePom(commonDir.resolve("common-core"), "com.example", "common-core", "1.0.0");
        writePom(commonDir.resolve("common-app"), "com.example", "common-app", "1.0.0");
    }

    private Path createApplicationDir(Path root) throws IOException {
        Path appDir = root.resolve("application");
        Files.createDirectories(appDir);
        // Aggregator pom
        String aggPom =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<project>\n"
                        + "    <groupId>com.example</groupId>\n"
                        + "    <artifactId>application</artifactId>\n"
                        + "    <version>1.0.0</version>\n"
                        + "    <modules>\n"
                        + "        <module>monolith-application</module>\n"
                        + "    </modules>\n"
                        + "</project>";
        Files.writeString(appDir.resolve("pom.xml"), aggPom);
        return appDir;
    }

    private void createMonolithPom(Path appDir, String... deps) throws IOException {
        Path monolithDir = appDir.resolve("monolith-application");
        Files.createDirectories(monolithDir);
        StringBuilder depStr = new StringBuilder();
        depStr.append("    <dependencies>\n");
        for (String dep : deps) {
            depStr.append("        <dependency><groupId>com.example</groupId><artifactId>")
                    .append(dep)
                    .append("</artifactId><version>1.0.0</version></dependency>\n");
        }
        depStr.append("    </dependencies>");
        String pom =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<project>\n"
                        + "    <groupId>com.example</groupId>\n"
                        + "    <artifactId>monolith-application</artifactId>\n"
                        + "    <version>1.0.0</version>\n"
                        + depStr
                        + "\n</project>";
        Files.writeString(monolithDir.resolve("pom.xml"), pom);
    }

    private MonodulaSplitMojo createMojo(Path root, String module) {
        MonodulaSplitMojo mojo = new MonodulaSplitMojo();
        mojo.baseDirectory = root.toFile();
        mojo.module = module;
        mojo.rootArtifactId = root.getFileName().toString();
        mojo.projectGroupId = "com.example";
        mojo.projectVersion = "1.0.0";
        mojo.basePackage = "com.example." + root.getFileName().toString().replace("-", "");
        return mojo;
    }

    // ============================================================
    // 1. Happy path: single module split
    // ============================================================

    @Test
    void split_single_module_generates_all_files() throws Exception {
        Path root = createMultiModuleProject("myproject");
        addBusinessModule(root, "finance");
        addCommonModules(root);
        Path appDir = createApplicationDir(root);
        createMonolithPom(appDir, "finance-app", "common-app");

        createMojo(root, "finance").execute();

        Path financeApp = appDir.resolve("finance-application");
        assertThat(financeApp.resolve("pom.xml")).exists();
        assertThat(financeApp.resolve("Dockerfile")).exists();
        assertThat(financeApp.resolve("src/main/resources/application.yml")).exists();
        assertThat(
                        financeApp.resolve(
                                "src/main/java/com/example/myproject/app/finance/FinanceStandaloneApplication.java"))
                .exists();
        assertThat(financeApp.resolve("src/test/java")).exists();
        assertThat(financeApp.resolve("src/test/resources")).exists();
    }

    // ============================================================
    // 2. Multi-module project: dependency list correctness
    // ============================================================

    @Test
    void split_multi_module_includes_other_business_api_and_core() throws Exception {
        Path root = createMultiModuleProject("myproject");
        addBusinessModule(root, "finance");
        addBusinessModule(root, "account");
        addBusinessModule(root, "order");
        addCommonModules(root);
        Path appDir = createApplicationDir(root);
        createMonolithPom(appDir, "finance-app", "account-app", "order-app", "common-app");

        createMojo(root, "finance").execute();

        Path pomFile = appDir.resolve("finance-application/pom.xml");
        String pomContent = Files.readString(pomFile);
        // Other business modules' api + core are included
        assertThat(pomContent).contains("account-api", "account-core", "order-api", "order-core");
        // Other business modules' app is NOT included
        assertThat(pomContent).doesNotContain("account-app", "order-app");
    }

    @Test
    void split_includes_all_common_modules() throws Exception {
        Path root = createMultiModuleProject("myproject");
        addBusinessModule(root, "finance");
        addCommonModules(root);
        Path appDir = createApplicationDir(root);
        createMonolithPom(appDir, "finance-app", "common-app");

        createMojo(root, "finance").execute();

        String pomContent = Files.readString(appDir.resolve("finance-application/pom.xml"));
        assertThat(pomContent).contains("common-api", "common-core", "common-app");
    }

    @Test
    void split_single_business_module_no_other_business_deps() throws Exception {
        Path root = createMultiModuleProject("myproject");
        addBusinessModule(root, "finance");
        addCommonModules(root);
        Path appDir = createApplicationDir(root);
        createMonolithPom(appDir, "finance-app", "common-app");

        createMojo(root, "finance").execute();

        String pomContent = Files.readString(appDir.resolve("finance-application/pom.xml"));
        // Target module gets app + core, NOT api
        assertThat(pomContent).doesNotContain("finance-api</artifactId>");
        // No other business module deps at all (no account-, order-, etc.)
        assertThat(pomContent).doesNotContain("account-", "order-", "inventory-");
        // Target's own app and core ARE included
        assertThat(pomContent).contains("finance-app", "finance-core");
        // Common modules ARE included (they use common- prefix, classified as COMMON not business)
        assertThat(pomContent).contains("common-api", "common-core", "common-app");
    }

    // ============================================================
    // 3. Generated pom.xml content verification
    // ============================================================

    @Test
    void generated_pom_has_correct_parent_and_artifactId() throws Exception {
        Path root = createMultiModuleProject("myproject");
        addBusinessModule(root, "finance");
        addCommonModules(root);
        Path appDir = createApplicationDir(root);
        createMonolithPom(appDir, "finance-app");

        createMojo(root, "finance").execute();

        String pomContent = Files.readString(appDir.resolve("finance-application/pom.xml"));
        // Parent
        assertThat(pomContent).contains("<groupId>com.example</groupId>");
        assertThat(pomContent).contains("<artifactId>application</artifactId>");
        assertThat(pomContent).contains("<version>1.0.0</version>");
        // Module's own artifactId
        assertThat(pomContent).contains("<artifactId>finance-application</artifactId>");
    }

    @Test
    void generated_pom_contains_target_app_and_core() throws Exception {
        Path root = createMultiModuleProject("myproject");
        addBusinessModule(root, "finance");
        addCommonModules(root);
        Path appDir = createApplicationDir(root);
        createMonolithPom(appDir, "finance-app");

        createMojo(root, "finance").execute();

        String pomContent = Files.readString(appDir.resolve("finance-application/pom.xml"));
        assertThat(pomContent).contains("finance-app", "finance-core");
    }

    @Test
    void generated_pom_contains_spring_boot_starter_and_plugin() throws Exception {
        Path root = createMultiModuleProject("myproject");
        addBusinessModule(root, "finance");
        addCommonModules(root);
        Path appDir = createApplicationDir(root);
        createMonolithPom(appDir, "finance-app");

        createMojo(root, "finance").execute();

        String pomContent = Files.readString(appDir.resolve("finance-application/pom.xml"));
        assertThat(pomContent).contains("spring-boot-starter-web");
        assertThat(pomContent).contains("spring-boot-maven-plugin");
    }

    // ============================================================
    // 4. Generated main class content verification
    // ============================================================

    @Test
    void generated_main_class_has_correct_package_and_className() throws Exception {
        Path root = createMultiModuleProject("myproject");
        addBusinessModule(root, "finance");
        addCommonModules(root);
        Path appDir = createApplicationDir(root);
        createMonolithPom(appDir, "finance-app");

        createMojo(root, "finance").execute();

        Path mainClass =
                appDir.resolve("finance-application")
                        .resolve(
                                "src/main/java/com/example/myproject/app/finance/FinanceStandaloneApplication.java");
        String content = Files.readString(mainClass);
        assertThat(content).contains("package com.example.myproject.app.finance;");
        assertThat(content).contains("public class FinanceStandaloneApplication");
        assertThat(content).contains("@SpringBootApplication");
        assertThat(content).contains("@ComponentScan(\"com.example.myproject\")");
    }

    // ============================================================
    // 5. Generated application.yml content verification
    // ============================================================

    @Test
    void generated_yml_has_server_port_and_application_name() throws Exception {
        Path root = createMultiModuleProject("myproject");
        addBusinessModule(root, "finance");
        addCommonModules(root);
        Path appDir = createApplicationDir(root);
        createMonolithPom(appDir, "finance-app");

        createMojo(root, "finance").execute();

        String yml =
                Files.readString(
                        appDir.resolve("finance-application/src/main/resources/application.yml"));
        assertThat(yml).contains("port: 8080");
        assertThat(yml).contains("name: finance-application");
    }

    @Test
    void generated_yml_has_no_config_import() throws Exception {
        Path root = createMultiModuleProject("myproject");
        addBusinessModule(root, "finance");
        addBusinessModule(root, "account");
        addCommonModules(root);
        Path appDir = createApplicationDir(root);
        createMonolithPom(appDir, "finance-app", "account-app");

        createMojo(root, "finance").execute();

        String yml =
                Files.readString(
                        appDir.resolve("finance-application/src/main/resources/application.yml"));
        assertThat(yml).doesNotContain("config:");
        assertThat(yml).doesNotContain("import:");
        assertThat(yml).doesNotContain("classpath:");
    }

    @Test
    void generated_yml_has_no_profiles_group() throws Exception {
        Path root = createMultiModuleProject("myproject");
        addBusinessModule(root, "finance");
        addBusinessModule(root, "account");
        addCommonModules(root);
        Path appDir = createApplicationDir(root);
        createMonolithPom(appDir, "finance-app", "account-app");

        createMojo(root, "finance").execute();

        String yml =
                Files.readString(
                        appDir.resolve("finance-application/src/main/resources/application.yml"));
        assertThat(yml).doesNotContain("group:");
        assertThat(yml).contains("active: dev");
    }

    @Test
    void generated_yml_has_apollo_config() throws Exception {
        Path root = createMultiModuleProject("myproject");
        addBusinessModule(root, "finance");
        addCommonModules(root);
        Path appDir = createApplicationDir(root);
        createMonolithPom(appDir, "finance-app");

        createMojo(root, "finance").execute();

        String yml =
                Files.readString(
                        appDir.resolve("finance-application/src/main/resources/application.yml"));
        assertThat(yml).contains("apollo:");
        assertThat(yml).contains("APOLLO_META");
        assertThat(yml).contains("bootstrap:");
        assertThat(yml).contains("namespaces: application");
    }

    // ============================================================
    // 6. Generated Dockerfile content verification
    // ============================================================

    @Test
    void generated_dockerfile_has_correct_content() throws Exception {
        Path root = createMultiModuleProject("myproject");
        addBusinessModule(root, "finance");
        addCommonModules(root);
        Path appDir = createApplicationDir(root);
        createMonolithPom(appDir, "finance-app");

        createMojo(root, "finance").execute();

        String dockerfile = Files.readString(appDir.resolve("finance-application/Dockerfile"));
        assertThat(dockerfile).contains("FROM eclipse-temurin:17-jre");
        assertThat(dockerfile).contains("COPY target/finance-application-1.0.0.jar app.jar");
        assertThat(dockerfile).contains("ENTRYPOINT");
    }

    // ============================================================
    // 7. Monolith pom modification
    // ============================================================

    @Test
    void monolith_pom_removes_app_adds_core() throws Exception {
        Path root = createMultiModuleProject("myproject");
        addBusinessModule(root, "finance");
        addCommonModules(root);
        Path appDir = createApplicationDir(root);
        createMonolithPom(appDir, "finance-app", "common-app");

        createMojo(root, "finance").execute();

        String monolith = Files.readString(appDir.resolve("monolith-application/pom.xml"));
        assertThat(monolith).doesNotContain("finance-app");
        assertThat(monolith).contains("finance-core");
    }

    @Test
    void monolith_pom_preserves_api_dependency() throws Exception {
        Path root = createMultiModuleProject("myproject");
        addBusinessModule(root, "finance");
        addCommonModules(root);
        Path appDir = createApplicationDir(root);
        createMonolithPom(appDir, "finance-app", "finance-api", "common-app");

        createMojo(root, "finance").execute();

        String monolith = Files.readString(appDir.resolve("monolith-application/pom.xml"));
        assertThat(monolith).contains("finance-api");
        assertThat(monolith).doesNotContain("finance-app");
    }

    @Test
    void monolith_pom_keeps_other_dependencies_unchanged() throws Exception {
        Path root = createMultiModuleProject("myproject");
        addBusinessModule(root, "finance");
        addBusinessModule(root, "account");
        addCommonModules(root);
        Path appDir = createApplicationDir(root);
        createMonolithPom(appDir, "finance-app", "account-app", "common-api");

        createMojo(root, "finance").execute();

        String monolith = Files.readString(appDir.resolve("monolith-application/pom.xml"));
        assertThat(monolith).contains("account-app");
        assertThat(monolith).contains("common-api");
    }

    @Test
    void monolith_pom_no_duplicate_core_when_already_present() throws Exception {
        Path root = createMultiModuleProject("myproject");
        addBusinessModule(root, "finance");
        addCommonModules(root);
        Path appDir = createApplicationDir(root);
        createMonolithPom(appDir, "finance-app", "finance-core");

        createMojo(root, "finance").execute();

        String monolith = Files.readString(appDir.resolve("monolith-application/pom.xml"));
        long coreCount = monolith.lines().filter(l -> l.contains("finance-core")).count();
        assertThat(coreCount).isEqualTo(1);
    }

    @Test
    void monolith_pom_no_error_when_app_dep_already_removed() throws Exception {
        Path root = createMultiModuleProject("myproject");
        addBusinessModule(root, "finance");
        addCommonModules(root);
        Path appDir = createApplicationDir(root);
        // No finance-app in monolith — already manually removed
        createMonolithPom(appDir, "common-app");

        createMojo(root, "finance").execute();

        // Should not throw, and should still add finance-core
        String monolith = Files.readString(appDir.resolve("monolith-application/pom.xml"));
        assertThat(monolith).contains("finance-core");
    }

    // ============================================================
    // 8. Aggregator pom modification
    // ============================================================

    @Test
    void aggregator_pom_adds_new_module_and_preserves_monolith() throws Exception {
        Path root = createMultiModuleProject("myproject");
        addBusinessModule(root, "finance");
        addCommonModules(root);
        Path appDir = createApplicationDir(root);
        createMonolithPom(appDir, "finance-app");

        createMojo(root, "finance").execute();

        String agg = Files.readString(appDir.resolve("pom.xml"));
        assertThat(agg).contains("<module>finance-application</module>");
        assertThat(agg).contains("<module>monolith-application</module>");
    }

    @Test
    void aggregator_pom_no_duplicate_on_second_split() throws Exception {
        Path root = createMultiModuleProject("myproject");
        addBusinessModule(root, "finance");
        addBusinessModule(root, "account");
        addCommonModules(root);
        Path appDir = createApplicationDir(root);
        createMonolithPom(appDir, "finance-app", "account-app");

        createMojo(root, "finance").execute();

        // Delete finance-application so second split doesn't hit idempotency check
        deleteRecursively(appDir.resolve("finance-application"));

        createMojo(root, "account").execute();

        String agg = Files.readString(appDir.resolve("pom.xml"));
        assertThat(agg).contains("<module>finance-application</module>");
        assertThat(agg).contains("<module>account-application</module>");
        assertThat(agg).contains("<module>monolith-application</module>");
        // Each module appears exactly once
        assertThat(agg.lines().filter(l -> l.contains("finance-application")).count()).isEqualTo(1);
        assertThat(agg.lines().filter(l -> l.contains("account-application")).count()).isEqualTo(1);
    }

    // ============================================================
    // 9. Idempotency
    // ============================================================

    @Test
    void split_fails_when_output_dir_already_exists() throws Exception {
        Path root = createMultiModuleProject("myproject");
        addBusinessModule(root, "finance");
        Path appDir = createApplicationDir(root);
        createMonolithPom(appDir, "finance-app");

        // Pre-create the output directory
        Files.createDirectories(appDir.resolve("finance-application"));

        assertThatThrownBy(() -> createMojo(root, "finance").execute())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already exists");
    }

    // ============================================================
    // 10. Hyphenated module name (payment-gateway)
    // ============================================================

    @Test
    void split_hyphenated_module_generates_correct_paths_and_content() throws Exception {
        Path root = createMultiModuleProject("myproject");
        // Create payment-gateway business module
        Path pgDir = root.resolve("payment-gateway");
        writePom(pgDir, "com.example", "payment-gateway", "1.0.0");
        writePom(
                pgDir.resolve("payment-gateway-api"),
                "com.example",
                "payment-gateway-api",
                "1.0.0");
        writePom(
                pgDir.resolve("payment-gateway-core"),
                "com.example",
                "payment-gateway-core",
                "1.0.0");
        writePom(
                pgDir.resolve("payment-gateway-app"),
                "com.example",
                "payment-gateway-app",
                "1.0.0");
        addCommonModules(root);
        Path appDir = createApplicationDir(root);
        createMonolithPom(appDir, "payment-gateway-app");

        createMojo(root, "payment-gateway").execute();

        Path pgApp = appDir.resolve("payment-gateway-application");
        // Directory name keeps hyphens
        assertThat(pgApp).exists();
        assertThat(pgApp.resolve("pom.xml")).exists();
        // Main class: package segment removes hyphens, class name is camelCase
        Path mainClass =
                pgApp.resolve(
                        "src/main/java/com/example/myproject/app/paymentgateway"
                                + "/PaymentGatewayStandaloneApplication.java");
        assertThat(mainClass).exists();
        String mainContent = Files.readString(mainClass);
        assertThat(mainContent).contains("package com.example.myproject.app.paymentgateway;");
        assertThat(mainContent).contains("public class PaymentGatewayStandaloneApplication");
        // Dockerfile uses hyphenated artifactId
        String dockerfile = Files.readString(pgApp.resolve("Dockerfile"));
        assertThat(dockerfile).contains("payment-gateway-application-1.0.0.jar");
    }

    // ============================================================
    // 11. Module not found — target has no -app or -core
    // ============================================================

    @Test
    void split_fails_when_target_module_has_no_app() throws Exception {
        Path root = createMultiModuleProject("myproject");
        // Only create finance-core, no finance-app
        Path financeDir = root.resolve("finance");
        writePom(financeDir, "com.example", "finance", "1.0.0");
        writePom(financeDir.resolve("finance-core"), "com.example", "finance-core", "1.0.0");
        Path appDir = createApplicationDir(root);
        createMonolithPom(appDir);

        assertThatThrownBy(() -> createMojo(root, "finance").execute())
                .isInstanceOf(Exception.class);
    }

    @Test
    void split_fails_when_target_module_has_no_core() throws Exception {
        Path root = createMultiModuleProject("myproject");
        // Only create finance-app, no finance-core
        Path financeDir = root.resolve("finance");
        writePom(financeDir, "com.example", "finance", "1.0.0");
        writePom(financeDir.resolve("finance-app"), "com.example", "finance-app", "1.0.0");
        Path appDir = createApplicationDir(root);
        createMonolithPom(appDir);

        assertThatThrownBy(() -> createMojo(root, "finance").execute())
                .isInstanceOf(Exception.class);
    }

    @Test
    void split_fails_with_clear_message_when_module_not_found() throws Exception {
        Path root = createMultiModuleProject("myproject");
        addBusinessModule(root, "finance");
        addCommonModules(root);
        Path appDir = createApplicationDir(root);
        createMonolithPom(appDir, "finance-app");

        assertThatThrownBy(() -> createMojo(root, "nonexistent").execute())
                .cause()
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nonexistent");
    }

    // ============================================================
    // 12. rootArtifactId with hyphens (e.g. my-project)
    // ============================================================

    @Test
    void split_with_hyphenated_rootArtifactId_generates_correct_basePackage() throws Exception {
        Path root = tempDir.resolve("my-project");
        writePom(root, "com.example", "my-project", "1.0.0");
        addBusinessModule(root, "finance");
        addCommonModules(root);
        Path appDir = createApplicationDir(root);
        createMonolithPom(appDir, "finance-app");

        MonodulaSplitMojo mojo = new MonodulaSplitMojo();
        mojo.baseDirectory = root.toFile();
        mojo.module = "finance";
        mojo.rootArtifactId = "my-project";
        mojo.projectGroupId = "com.example";
        mojo.projectVersion = "1.0.0";
        mojo.basePackage = "com.example.myproject";

        mojo.execute();

        // basePackage = com.example.myproject (hyphens removed from rootArtifactId)
        Path mainClass =
                appDir.resolve(
                        "finance-application/src/main/java/com/example/myproject/app/finance"
                                + "/FinanceStandaloneApplication.java");
        assertThat(mainClass).exists();
        String content = Files.readString(mainClass);
        assertThat(content).contains("package com.example.myproject.app.finance;");
    }

    // ============================================================
    // Utility
    // ============================================================

    private void deleteRecursively(Path dir) throws IOException {
        if (Files.exists(dir)) {
            Files.walk(dir)
                    .sorted(java.util.Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }
}
