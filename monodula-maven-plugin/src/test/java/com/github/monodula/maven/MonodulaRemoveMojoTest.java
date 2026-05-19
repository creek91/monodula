package com.github.monodula.maven;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MonodulaRemoveMojoTest {

    @TempDir Path tempDir;

    // ── helpers ──────────────────────────────────────────────────────────────

    /** Writes a minimal root aggregator pom listing the given modules. */
    private Path writeRootPom(String... modules) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<project>\n");
        sb.append("    <groupId>com.example</groupId>\n");
        sb.append("    <artifactId>myproject</artifactId>\n");
        sb.append("    <version>1.0.0</version>\n");
        sb.append("    <packaging>pom</packaging>\n");
        sb.append("    <modules>\n");
        for (String m : modules) {
            sb.append("        <module>").append(m).append("</module>\n");
        }
        sb.append("    </modules>\n");
        sb.append("</project>\n");
        Path pom = tempDir.resolve("pom.xml");
        Files.writeString(pom, sb.toString());
        return pom;
    }

    /** Writes a monolith pom with the given module deps already declared. */
    private Path writeMonolithPom(String... artifactIds) throws Exception {
        Path appDir = tempDir.resolve("application").resolve("monolith-application");
        Files.createDirectories(appDir);
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<project>\n");
        sb.append("    <artifactId>monolith-application</artifactId>\n");
        sb.append("    <dependencies>\n");
        for (String aid : artifactIds) {
            sb.append("        <dependency>\n");
            sb.append("            <groupId>com.example</groupId>\n");
            sb.append("            <artifactId>").append(aid).append("</artifactId>\n");
            sb.append("        </dependency>\n");
        }
        sb.append("    </dependencies>\n");
        sb.append("</project>\n");
        Path pom = appDir.resolve("pom.xml");
        Files.writeString(pom, sb.toString());
        return pom;
    }

    /** Creates the module directory structure with sub-module poms. */
    private void createModuleDir(String moduleName) throws Exception {
        Path moduleDir = tempDir.resolve(moduleName);
        for (String layer : new String[] {"api", "core", "app"}) {
            Path layerDir = moduleDir.resolve(moduleName + "-" + layer);
            Files.createDirectories(layerDir);
            String pom =
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                            + "<project>\n"
                            + "    <parent>\n"
                            + "        <groupId>com.example</groupId>\n"
                            + "        <artifactId>"
                            + moduleName
                            + "</artifactId>\n"
                            + "        <version>1.0.0</version>\n"
                            + "    </parent>\n"
                            + "    <artifactId>"
                            + moduleName
                            + "-"
                            + layer
                            + "</artifactId>\n"
                            + "</project>\n";
            Files.writeString(layerDir.resolve("pom.xml"), pom);
        }
    }

    /** Creates a module that depends on the given dependency artifactId. */
    private void createDependentModule(String dependentName, String dependencyArtifactId)
            throws Exception {
        Path moduleDir = tempDir.resolve(dependentName);
        Path coreDir = moduleDir.resolve(dependentName + "-core");
        Files.createDirectories(coreDir);
        String pom =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<project>\n"
                        + "    <parent>\n"
                        + "        <groupId>com.example</groupId>\n"
                        + "        <artifactId>"
                        + dependentName
                        + "</artifactId>\n"
                        + "        <version>1.0.0</version>\n"
                        + "    </parent>\n"
                        + "    <artifactId>"
                        + dependentName
                        + "-core</artifactId>\n"
                        + "    <dependencies>\n"
                        + "        <dependency>\n"
                        + "            <groupId>com.example</groupId>\n"
                        + "            <artifactId>"
                        + dependencyArtifactId
                        + "</artifactId>\n"
                        + "        </dependency>\n"
                        + "    </dependencies>\n"
                        + "</project>\n";
        Files.writeString(coreDir.resolve("pom.xml"), pom);
    }

    private MonodulaRemoveMojo newMojo(String module, boolean purge) {
        MonodulaRemoveMojo mojo = new MonodulaRemoveMojo();
        mojo.module = module;
        mojo.purge = purge;
        mojo.baseDirectory = tempDir.toFile();
        mojo.projectGroupId = "com.example";
        return mojo;
    }

    // ── tests ────────────────────────────────────────────────────────────────

    @Test
    void happy_path_removes_pom_references() throws Exception {
        writeRootPom("finance", "common");
        writeMonolithPom("finance-app", "common-app");
        createModuleDir("finance");

        newMojo("finance", false).execute();

        String rootContent = Files.readString(tempDir.resolve("pom.xml"));
        assertThat(rootContent).doesNotContain("<module>finance</module>");
        assertThat(rootContent).contains("<module>common</module>");

        String monolithContent =
                Files.readString(tempDir.resolve("application/monolith-application/pom.xml"));
        assertThat(monolithContent).doesNotContain("<artifactId>finance-app</artifactId>");
        assertThat(monolithContent).contains("<artifactId>common-app</artifactId>");

        // Source dir preserved
        assertThat(tempDir.resolve("finance").toFile()).exists();
    }

    @Test
    void purge_true_deletes_source_directory() throws Exception {
        writeRootPom("finance");
        writeMonolithPom("finance-app");
        createModuleDir("finance");

        newMojo("finance", true).execute();

        assertThat(tempDir.resolve("finance").toFile()).doesNotExist();
    }

    @Test
    void fails_when_module_directory_not_exist() throws Exception {
        writeRootPom("finance");
        writeMonolithPom("finance-app");
        // Do NOT create the finance module directory

        assertThatThrownBy(() -> newMojo("finance", false).execute())
                .isInstanceOf(MojoFailureException.class)
                .hasMessageContaining("finance");
    }

    @Test
    void fails_when_module_is_depended_on() throws Exception {
        writeRootPom("finance", "order");
        writeMonolithPom("finance-app", "order-app");
        createModuleDir("finance");
        createDependentModule("order", "finance-api");

        assertThatThrownBy(() -> newMojo("finance", false).execute())
                .isInstanceOf(MojoFailureException.class)
                .hasMessageContaining("finance")
                .hasMessageContaining("order-core");
    }

    @Test
    void succeeds_when_monolith_pom_absent() throws Exception {
        writeRootPom("finance");
        // No monolith pom created
        createModuleDir("finance");

        // Should not throw
        newMojo("finance", false).execute();

        String rootContent = Files.readString(tempDir.resolve("pom.xml"));
        assertThat(rootContent).doesNotContain("<module>finance</module>");
    }

    @Test
    void remove_module_entry_idempotent() throws Exception {
        writeRootPom("common"); // finance not listed
        writeMonolithPom();
        createModuleDir("finance");

        // Should not throw even though finance is not in root pom
        newMojo("finance", false).execute();
    }

    // ── additional helpers ────────────────────────────────────────────────────

    private Path writeRootPom() throws Exception {
        return writeRootPom(new String[0]);
    }

    private void createModuleDirNamed(String moduleName) throws Exception {
        createModuleDir(moduleName);
    }

    private MonodulaRemoveMojo newMojoNamed(String module, boolean purge) {
        return newMojo(module, purge);
    }

    // ── BUG-01: self-reference within module ─────────────────────────────────

    @Test
    void self_reference_within_module_does_not_block_remove() throws Exception {
        // finance-core depends on finance-api — intra-module, must NOT block remove
        writeRootPom("finance");
        writeMonolithPom("finance-app");
        createModuleDir("finance");
        // Overwrite finance-core pom to include finance-api dependency
        Path coreDir = tempDir.resolve("finance").resolve("finance-core");
        String corePom =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<project>\n"
                        + "    <parent><groupId>com.example</groupId>"
                        + "<artifactId>finance</artifactId><version>1.0.0</version></parent>\n"
                        + "    <artifactId>finance-core</artifactId>\n"
                        + "    <dependencies>\n"
                        + "        <dependency>\n"
                        + "            <groupId>com.example</groupId>\n"
                        + "            <artifactId>finance-api</artifactId>\n"
                        + "        </dependency>\n"
                        + "    </dependencies>\n"
                        + "</project>\n";
        Files.writeString(coreDir.resolve("pom.xml"), corePom);

        // Should NOT throw — finance-core's dependency on finance-api is intra-module
        newMojo("finance", false).execute();

        String rootContent = Files.readString(tempDir.resolve("pom.xml"));
        assertThat(rootContent).doesNotContain("<module>finance</module>");
    }

    // ── BUG-02: plugin dependencies must not pollute dependency scan ──────────

    @Test
    void plugin_dependencies_in_pom_do_not_pollute_dependency_scan() throws Exception {
        // order-core pom has <build><plugins><plugin><dependencies> before <dependencies>
        // Those plugin deps must not appear in getDependencies()
        writeRootPom("finance", "order");
        writeMonolithPom("finance-app", "order-app");
        createModuleDir("finance");

        Path orderCoreDir = tempDir.resolve("order").resolve("order-core");
        Files.createDirectories(orderCoreDir);
        String pomWithBuildDeps =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<project>\n"
                        + "    <parent><groupId>com.example</groupId>"
                        + "<artifactId>order</artifactId><version>1.0.0</version></parent>\n"
                        + "    <artifactId>order-core</artifactId>\n"
                        + "    <build>\n"
                        + "        <plugins>\n"
                        + "            <plugin>\n"
                        + "                <artifactId>maven-compiler-plugin</artifactId>\n"
                        + "                <dependencies>\n"
                        + "                    <dependency>\n"
                        + "                        <groupId>com.example</groupId>\n"
                        + "                        <artifactId>finance-api</artifactId>\n"
                        + "                    </dependency>\n"
                        + "                </dependencies>\n"
                        + "            </plugin>\n"
                        + "        </plugins>\n"
                        + "    </build>\n"
                        + "    <dependencies>\n"
                        + "        <dependency>\n"
                        + "            <groupId>com.example</groupId>\n"
                        + "            <artifactId>common-api</artifactId>\n"
                        + "        </dependency>\n"
                        + "    </dependencies>\n"
                        + "</project>\n";
        Files.writeString(orderCoreDir.resolve("pom.xml"), pomWithBuildDeps);

        // order-core has finance-api only in plugin deps (not module deps)
        // Remove finance should succeed — plugin dep must not block remove
        newMojo("finance", false).execute();

        String rootContent = Files.readString(tempDir.resolve("pom.xml"));
        assertThat(rootContent).doesNotContain("<module>finance</module>");
    }

    // ── parameter validation ──────────────────────────────────────────────────

    @Test
    void fails_when_module_is_null() throws Exception {
        writeRootPom("finance");
        MonodulaRemoveMojo mojo = new MonodulaRemoveMojo();
        mojo.module = null;
        mojo.purge = false;
        mojo.baseDirectory = tempDir.toFile();
        mojo.projectGroupId = "com.example";

        assertThatThrownBy(mojo::execute).isInstanceOf(MojoFailureException.class);
    }

    @Test
    void fails_when_module_is_empty_string() throws Exception {
        writeRootPom("finance");
        MonodulaRemoveMojo mojo = new MonodulaRemoveMojo();
        mojo.module = "";
        mojo.purge = false;
        mojo.baseDirectory = tempDir.toFile();
        mojo.projectGroupId = "com.example";

        assertThatThrownBy(mojo::execute).isInstanceOf(MojoFailureException.class);
    }

    @Test
    void fails_when_module_starts_with_digit() throws Exception {
        writeRootPom();
        MonodulaRemoveMojo mojo = new MonodulaRemoveMojo();
        mojo.module = "1finance";
        mojo.purge = false;
        mojo.baseDirectory = tempDir.toFile();
        mojo.projectGroupId = "com.example";

        assertThatThrownBy(mojo::execute)
                .isInstanceOf(MojoFailureException.class)
                .hasMessageContaining("1finance");
    }

    @Test
    void fails_when_module_contains_uppercase() throws Exception {
        writeRootPom();
        MonodulaRemoveMojo mojo = new MonodulaRemoveMojo();
        mojo.module = "Finance";
        mojo.purge = false;
        mojo.baseDirectory = tempDir.toFile();
        mojo.projectGroupId = "com.example";

        assertThatThrownBy(mojo::execute).isInstanceOf(MojoFailureException.class);
    }

    @Test
    void fails_when_module_contains_underscore() throws Exception {
        writeRootPom();
        MonodulaRemoveMojo mojo = new MonodulaRemoveMojo();
        mojo.module = "fin_ance";
        mojo.purge = false;
        mojo.baseDirectory = tempDir.toFile();
        mojo.projectGroupId = "com.example";

        assertThatThrownBy(mojo::execute).isInstanceOf(MojoFailureException.class);
    }

    @Test
    void accepts_module_with_hyphens() throws Exception {
        writeRootPom("payment-gateway");
        writeMonolithPom("payment-gateway-app");
        createModuleDirNamed("payment-gateway");

        newMojoNamed("payment-gateway", false).execute();

        assertThat(Files.readString(tempDir.resolve("pom.xml")))
                .doesNotContain("<module>payment-gateway</module>");
    }

    @Test
    void accepts_single_char_module_name() throws Exception {
        writeRootPom("a");
        writeMonolithPom("a-app");
        createModuleDirNamed("a");

        newMojoNamed("a", false).execute();

        assertThat(Files.readString(tempDir.resolve("pom.xml")))
                .doesNotContain("<module>a</module>");
    }

    // ── precise matching: no substring false positive ─────────────────────────

    @Test
    void does_not_confuse_pay_with_payment_module() throws Exception {
        // Removing 'pay' must not block when 'payment-core' depends on 'payment-api'
        // (payment-api != pay-api)
        writeRootPom("pay", "payment");
        writeMonolithPom("pay-app", "payment-app");
        createModuleDir("pay");

        // payment-core depends on payment-api — intra-module for 'payment', must not block removing
        // 'pay'
        Path paymentCoreDir = tempDir.resolve("payment").resolve("payment-core");
        Files.createDirectories(paymentCoreDir);
        String pom =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<project>\n"
                        + "    <artifactId>payment-core</artifactId>\n"
                        + "    <dependencies>\n"
                        + "        <dependency>\n"
                        + "            <groupId>com.example</groupId>\n"
                        + "            <artifactId>payment-api</artifactId>\n"
                        + "        </dependency>\n"
                        + "    </dependencies>\n"
                        + "</project>\n";
        Files.writeString(paymentCoreDir.resolve("pom.xml"), pom);

        // Removing 'pay' should succeed — payment-api != pay-api
        newMojo("pay", false).execute();

        assertThat(Files.readString(tempDir.resolve("pom.xml")))
                .doesNotContain("<module>pay</module>")
                .contains("<module>payment</module>");
    }

    @Test
    void error_message_lists_all_dependents_when_multiple() throws Exception {
        writeRootPom("finance", "order", "report");
        writeMonolithPom("finance-app", "order-app", "report-app");
        createModuleDir("finance");
        createDependentModule("order", "finance-api");
        createDependentModule("report", "finance-core");

        assertThatThrownBy(() -> newMojo("finance", false).execute())
                .isInstanceOf(MojoFailureException.class)
                .hasMessageContaining("order-core")
                .hasMessageContaining("report-core");
    }

    // ── pom edge cases ────────────────────────────────────────────────────────

    @Test
    void succeeds_when_root_pom_has_no_modules_block() throws Exception {
        // Root pom without <modules> — removeModuleEntry should not throw
        Path pom = tempDir.resolve("pom.xml");
        Files.writeString(
                pom,
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<project>\n"
                        + "    <groupId>com.example</groupId>\n"
                        + "    <artifactId>myproject</artifactId>\n"
                        + "    <version>1.0.0</version>\n"
                        + "</project>\n");
        writeMonolithPom("finance-app");
        createModuleDir("finance");

        newMojo("finance", false).execute(); // must not throw
    }

    @Test
    void succeeds_when_monolith_pom_has_no_dependencies_block() throws Exception {
        writeRootPom("finance");
        // Monolith pom without <dependencies> block
        Path appDir = tempDir.resolve("application").resolve("monolith-application");
        Files.createDirectories(appDir);
        Files.writeString(
                appDir.resolve("pom.xml"),
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<project>\n"
                        + "    <artifactId>monolith-application</artifactId>\n"
                        + "</project>\n");
        createModuleDir("finance");

        newMojo("finance", false).execute(); // must not throw
    }

    // ── purge atomicity ───────────────────────────────────────────────────────

    @Test
    void purge_does_not_delete_when_dependency_check_fails() throws Exception {
        // purge=true but another module depends on finance — module dir must NOT be deleted
        writeRootPom("finance", "order");
        writeMonolithPom("finance-app", "order-app");
        createModuleDir("finance");
        createDependentModule("order", "finance-api");

        assertThatThrownBy(() -> newMojo("finance", true).execute())
                .isInstanceOf(MojoFailureException.class);

        // Source directory must still exist — dependency check failed before purge
        assertThat(tempDir.resolve("finance").toFile()).exists();
    }

    // ── dependencyManagement cleanup ─────────────────────────────────────────

    private Path writeRootPomWithDm(String... modules) throws Exception {
        StringBuilder dm = new StringBuilder();
        dm.append("    <dependencyManagement>\n");
        dm.append("        <dependencies>\n");
        for (String m : modules) {
            for (String suffix : new String[] {"-api", "-core", "-app"}) {
                dm.append("            <dependency>\n");
                dm.append("                <groupId>com.example</groupId>\n");
                dm.append("                <artifactId>")
                        .append(m)
                        .append(suffix)
                        .append("</artifactId>\n");
                dm.append("                <version>${project.version}</version>\n");
                dm.append("            </dependency>\n");
            }
        }
        dm.append("        </dependencies>\n");
        dm.append("    </dependencyManagement>");
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<project>\n");
        sb.append("    <groupId>com.example</groupId>\n");
        sb.append("    <artifactId>myproject</artifactId>\n");
        sb.append("    <version>1.0.0-SNAPSHOT</version>\n");
        sb.append("    <packaging>pom</packaging>\n");
        sb.append("    <modules>\n");
        for (String m : modules) {
            sb.append("        <module>").append(m).append("</module>\n");
        }
        sb.append("    </modules>\n");
        sb.append(dm).append("\n");
        sb.append("</project>\n");
        Path pom = tempDir.resolve("pom.xml");
        Files.writeString(pom, sb.toString());
        return pom;
    }

    @Test
    void remove_cleans_up_dependency_management_entries() throws Exception {
        writeRootPomWithDm("finance", "common");
        writeMonolithPom("finance-app", "common-app");
        createModuleDir("finance");

        newMojo("finance", false).execute();

        String rootContent = Files.readString(tempDir.resolve("pom.xml"));
        assertThat(rootContent).doesNotContain("finance-api");
        assertThat(rootContent).doesNotContain("finance-core");
        assertThat(rootContent).doesNotContain("finance-app");
        // common entries must remain
        assertThat(rootContent).contains("common-api");
        assertThat(rootContent).contains("common-core");
        assertThat(rootContent).contains("common-app");
    }

    @Test
    void remove_succeeds_when_sub_module_dirs_partially_missing() throws Exception {
        // Simulate hand-deletion of -app sub-module: only -api and -core dirs exist
        writeRootPom("finance");
        writeMonolithPom("finance-app");
        Path moduleDir = tempDir.resolve("finance");
        for (String layer : new String[] {"api", "core"}) {
            Path layerDir = moduleDir.resolve("finance-" + layer);
            Files.createDirectories(layerDir);
            Files.writeString(
                    layerDir.resolve("pom.xml"),
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                            + "<project><artifactId>finance-"
                            + layer
                            + "</artifactId></project>\n");
        }
        // finance-app directory intentionally absent

        // Must not throw — missing sub-module pom is not an error
        newMojo("finance", false).execute();

        String rootContent = Files.readString(tempDir.resolve("pom.xml"));
        assertThat(rootContent).doesNotContain("<module>finance</module>");
    }
}
