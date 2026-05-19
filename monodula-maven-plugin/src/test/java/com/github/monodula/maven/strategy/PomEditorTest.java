package com.github.monodula.maven.strategy;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PomEditorTest {

    @TempDir Path tempDir;

    private Path writeMonolithPom(String... extraDeps) throws IOException {
        StringBuilder deps = new StringBuilder();
        deps.append("    <dependencies>\n");
        deps.append(
                "        <dependency><groupId>com.example</groupId>"
                        + "<artifactId>finance-app</artifactId>"
                        + "<version>1.0.0</version></dependency>\n");
        for (String dep : extraDeps) {
            deps.append("        <dependency><groupId>com.example</groupId><artifactId>")
                    .append(dep)
                    .append("</artifactId><version>1.0.0</version></dependency>\n");
        }
        deps.append("    </dependencies>");

        String pom =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<project>\n"
                        + "    <groupId>com.example</groupId>\n"
                        + "    <artifactId>monolith-application</artifactId>\n"
                        + "    <version>1.0.0</version>\n"
                        + deps
                        + "\n</project>";

        Path pomFile = tempDir.resolve("monolith-pom.xml");
        Files.writeString(pomFile, pom);
        return pomFile;
    }

    private Path writeAggregatorPom(String... modules) throws IOException {
        StringBuilder mods = new StringBuilder();
        mods.append("    <modules>\n");
        for (String m : modules) {
            mods.append("        <module>").append(m).append("</module>\n");
        }
        mods.append("    </modules>");

        String pom =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<project>\n"
                        + "    <groupId>com.example</groupId>\n"
                        + "    <artifactId>myproject-application</artifactId>\n"
                        + "    <version>1.0.0</version>\n"
                        + mods
                        + "\n</project>";

        Path pomFile = tempDir.resolve("aggregator-pom.xml");
        Files.writeString(pomFile, pom);
        return pomFile;
    }

    @Test
    void monolith_pom_removes_module_app_dependency() throws Exception {
        Path pomFile = writeMonolithPom();
        new PomEditor().removeAppAddCore(pomFile.toFile(), "finance", "com.example");
        String content = Files.readString(pomFile);
        assertThat(content).doesNotContain("finance-app");
    }

    @Test
    void monolith_pom_adds_module_core_when_not_present() throws Exception {
        Path pomFile = writeMonolithPom();
        new PomEditor().removeAppAddCore(pomFile.toFile(), "finance", "com.example");
        String content = Files.readString(pomFile);
        assertThat(content).contains("finance-core");
        assertThat(content)
                .containsPattern("(?s)<artifactId>finance-core</artifactId>\\s*</dependency>");
    }

    @Test
    void monolith_pom_keeps_module_core_when_already_present() throws Exception {
        Path pomFile = writeMonolithPom("finance-core");
        new PomEditor().removeAppAddCore(pomFile.toFile(), "finance", "com.example");
        String content = Files.readString(pomFile);
        long count = content.lines().filter(line -> line.contains("finance-core")).count();
        assertThat(count).isEqualTo(1);
    }

    @Test
    void monolith_pom_keeps_other_dependencies_unchanged() throws Exception {
        Path pomFile = writeMonolithPom("account-app", "common-api");
        new PomEditor().removeAppAddCore(pomFile.toFile(), "finance", "com.example");
        String content = Files.readString(pomFile);
        assertThat(content).contains("account-app");
        assertThat(content).contains("common-api");
    }

    @Test
    void monolith_pom_no_error_when_app_dep_absent() throws Exception {
        // pom without finance-app at all
        String pom =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<project>\n"
                        + "    <dependencies>\n"
                        + "        <dependency><groupId>com.example</groupId>"
                        + "<artifactId>account-app</artifactId>"
                        + "<version>1.0.0</version></dependency>\n"
                        + "    </dependencies>\n"
                        + "</project>";
        Path pomFile = tempDir.resolve("no-app-pom.xml");
        Files.writeString(pomFile, pom);

        // Should not throw
        new PomEditor().removeAppAddCore(pomFile.toFile(), "finance", "com.example");
        String content = Files.readString(pomFile);
        assertThat(content).doesNotContain("finance-app");
        assertThat(content).contains("finance-core");
    }

    @Test
    void application_aggregator_pom_adds_new_module() throws Exception {
        Path pomFile = writeAggregatorPom("monolith-application");
        new PomEditor().addModuleToAggregator(pomFile.toFile(), "finance");
        String content = Files.readString(pomFile);
        assertThat(content).contains("<module>finance-application</module>");
    }

    @Test
    void application_aggregator_pom_keeps_monolith_module() throws Exception {
        Path pomFile = writeAggregatorPom("monolith-application");
        new PomEditor().addModuleToAggregator(pomFile.toFile(), "finance");
        String content = Files.readString(pomFile);
        assertThat(content).contains("<module>monolith-application</module>");
    }

    @Test
    void application_aggregator_pom_idempotent() throws Exception {
        Path pomFile = writeAggregatorPom("monolith-application", "finance-application");
        new PomEditor().addModuleToAggregator(pomFile.toFile(), "finance");
        String content = Files.readString(pomFile);
        long count = content.lines().filter(line -> line.contains("finance-application")).count();
        assertThat(count).isEqualTo(1);
    }

    @Test
    void monolith_pom_keeps_module_api_when_present() throws Exception {
        Path pomFile = writeMonolithPom("finance-api");
        new PomEditor().removeAppAddCore(pomFile.toFile(), "finance", "com.example");
        String content = Files.readString(pomFile);
        assertThat(content).contains("finance-api");
    }

    // ── removeModuleEntry ────────────────────────────────────────────────────

    @Test
    void root_pom_removes_module_entry() throws Exception {
        Path pomFile = writeAggregatorPom("common", "finance", "order");
        new PomEditor().removeModuleEntry(pomFile.toFile(), "finance");
        String content = Files.readString(pomFile);
        assertThat(content).doesNotContain("<module>finance</module>");
        assertThat(content).contains("<module>common</module>");
        assertThat(content).contains("<module>order</module>");
    }

    @Test
    void root_pom_remove_module_entry_idempotent() throws Exception {
        Path pomFile = writeAggregatorPom("common", "order");
        // "finance" does not exist — should not throw
        new PomEditor().removeModuleEntry(pomFile.toFile(), "finance");
        String content = Files.readString(pomFile);
        assertThat(content).contains("<module>common</module>");
        assertThat(content).contains("<module>order</module>");
    }

    // ── removeDependency ─────────────────────────────────────────────────────

    @Test
    void monolith_pom_removes_dependency() throws Exception {
        Path pom = writeMonolithPom();
        // First add a dep, then remove it
        new PomEditor().addDependency(pom.toFile(), "com.example", "finance-app");
        new PomEditor().addDependency(pom.toFile(), "com.example", "order-app");
        new PomEditor().removeDependency(pom.toFile(), "finance-app");
        String content = Files.readString(pom);
        assertThat(content).doesNotContain("<artifactId>finance-app</artifactId>");
        assertThat(content).contains("<artifactId>order-app</artifactId>");
    }

    @Test
    void monolith_pom_remove_dependency_idempotent() throws Exception {
        Path pom = writeMonolithPom();
        // "account-app" was never added — should not throw
        new PomEditor().removeDependency(pom.toFile(), "account-app");
        String content = Files.readString(pom);
        // File should still be valid XML
        assertThat(content).contains("<dependencies>");
    }

    // ── removeDependencyManagementEntries ────────────────────────────────────

    private Path writeRootPomWithDm(String... artifactIds) throws IOException {
        StringBuilder dm = new StringBuilder();
        dm.append("    <dependencyManagement>\n");
        dm.append("        <dependencies>\n");
        for (String aid : artifactIds) {
            dm.append("            <dependency>\n");
            dm.append("                <groupId>com.example</groupId>\n");
            dm.append("                <artifactId>").append(aid).append("</artifactId>\n");
            dm.append("                <version>${project.version}</version>\n");
            dm.append("            </dependency>\n");
        }
        dm.append("        </dependencies>\n");
        dm.append("    </dependencyManagement>");
        String pom =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<project>\n"
                        + "    <groupId>com.example</groupId>\n"
                        + "    <artifactId>my-project</artifactId>\n"
                        + "    <version>1.0.0-SNAPSHOT</version>\n"
                        + dm
                        + "\n</project>";
        Path f = tempDir.resolve("root-pom-dm.xml");
        Files.writeString(f, pom);
        return f;
    }

    @Test
    void removeDmEntriesRemovesAllThree() throws Exception {
        Path pom = writeRootPomWithDm("payment-api", "payment-core", "payment-app");
        new PomEditor().removeDependencyManagementEntries(pom.toFile(), "payment");
        String content = Files.readString(pom);
        assertThat(content).doesNotContain("payment-api");
        assertThat(content).doesNotContain("payment-core");
        assertThat(content).doesNotContain("payment-app");
    }

    @Test
    void removeDmEntriesKeepsUnrelatedEntries() throws Exception {
        Path pom = writeRootPomWithDm("payment-api", "payment-core", "payment-app", "order-api");
        new PomEditor().removeDependencyManagementEntries(pom.toFile(), "payment");
        String content = Files.readString(pom);
        assertThat(content).contains("order-api");
    }

    @Test
    void removeDmEntriesIdempotentWhenEntriesAbsent() throws Exception {
        Path pom = writeRootPomWithDm("order-api");
        // "payment-*" entries not present — should not throw
        new PomEditor().removeDependencyManagementEntries(pom.toFile(), "payment");
        String content = Files.readString(pom);
        assertThat(content).contains("order-api");
    }

    @Test
    void removeDmEntriesNoOpWhenDmBlockAbsent() throws Exception {
        Path pom = writeAggregatorPom("common");
        // no <dependencyManagement> block at all — should not throw
        new PomEditor().removeDependencyManagementEntries(pom.toFile(), "payment");
        String content = Files.readString(pom);
        assertThat(content).doesNotContain("dependencyManagement");
    }

    @Test
    void removeDmEntriesPartialRemoval() throws Exception {
        // Only -api and -core present, -app missing — should remove what exists
        Path pom = writeRootPomWithDm("payment-api", "payment-core");
        new PomEditor().removeDependencyManagementEntries(pom.toFile(), "payment");
        String content = Files.readString(pom);
        assertThat(content).doesNotContain("payment-api");
        assertThat(content).doesNotContain("payment-core");
    }

    @Test
    void removeDmEntriesResultIsValidXml() throws Exception {
        Path pom = writeRootPomWithDm("payment-api", "payment-core", "payment-app", "order-api");
        new PomEditor().removeDependencyManagementEntries(pom.toFile(), "payment");
        javax.xml.parsers.DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(pom.toFile());
    }
}
