package com.github.monodula.maven.strategy;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PomEditorAddTest {

    @TempDir Path tempDir;

    // ── helpers ──────────────────────────────────────────────────────────────

    private Path writeRootPom(String... modules) throws IOException {
        StringBuilder mods = new StringBuilder("    <modules>\n");
        for (String m : modules) {
            mods.append("        <module>").append(m).append("</module>\n");
        }
        mods.append("    </modules>");
        String pom =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<project>\n"
                        + "    <groupId>com.example</groupId>\n"
                        + "    <artifactId>my-project</artifactId>\n"
                        + "    <version>1.0.0-SNAPSHOT</version>\n"
                        + mods
                        + "\n"
                        + "</project>";
        Path f = tempDir.resolve("root-pom.xml");
        Files.writeString(f, pom);
        return f;
    }

    private Path writeRootPomNoModules() throws IOException {
        String pom =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<project>\n"
                        + "    <groupId>com.example</groupId>\n"
                        + "    <artifactId>my-project</artifactId>\n"
                        + "    <version>1.0.0-SNAPSHOT</version>\n"
                        + "</project>";
        Path f = tempDir.resolve("root-pom-no-modules.xml");
        Files.writeString(f, pom);
        return f;
    }

    private Path writeMonolithPom(String... artifactIds) throws IOException {
        StringBuilder deps = new StringBuilder("    <dependencies>\n");
        deps.append("        <dependency>\n");
        deps.append("            <groupId>com.example</groupId>\n");
        deps.append("            <artifactId>order-app</artifactId>\n");
        deps.append("            <version>1.0.0-SNAPSHOT</version>\n");
        deps.append("        </dependency>\n");
        for (String a : artifactIds) {
            deps.append("        <dependency>\n");
            deps.append("            <groupId>com.example</groupId>\n");
            deps.append("            <artifactId>").append(a).append("</artifactId>\n");
            deps.append("            <version>1.0.0-SNAPSHOT</version>\n");
            deps.append("        </dependency>\n");
        }
        deps.append("    </dependencies>");
        String pom =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<project>\n"
                        + "    <groupId>com.example</groupId>\n"
                        + "    <artifactId>monolith-application</artifactId>\n"
                        + "    <version>1.0.0-SNAPSHOT</version>\n"
                        + deps
                        + "\n"
                        + "</project>";
        Path f = tempDir.resolve("monolith-pom.xml");
        Files.writeString(f, pom);
        return f;
    }

    private Path writeMonolithPomNoDeps() throws IOException {
        String pom =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<project>\n"
                        + "    <groupId>com.example</groupId>\n"
                        + "    <artifactId>monolith-application</artifactId>\n"
                        + "    <version>1.0.0-SNAPSHOT</version>\n"
                        + "</project>";
        Path f = tempDir.resolve("monolith-pom-nodeps.xml");
        Files.writeString(f, pom);
        return f;
    }

    // ── addModuleEntry ────────────────────────────────────────────────────────

    @Test
    void addModuleEntryAppendsNewModule() throws Exception {
        Path pom = writeRootPom("common", "order");
        new PomEditor().addModuleEntry(pom.toFile(), "payment");
        assertThat(Files.readString(pom)).contains("<module>payment</module>");
    }

    @Test
    void addModuleEntryKeepsExistingModules() throws Exception {
        Path pom = writeRootPom("common", "order");
        new PomEditor().addModuleEntry(pom.toFile(), "payment");
        String content = Files.readString(pom);
        assertThat(content).contains("<module>common</module>");
        assertThat(content).contains("<module>order</module>");
    }

    @Test
    void addModuleEntryIdempotent() throws Exception {
        Path pom = writeRootPom("common", "order");
        PomEditor editor = new PomEditor();
        editor.addModuleEntry(pom.toFile(), "payment");
        editor.addModuleEntry(pom.toFile(), "payment");
        long count =
                Files.readString(pom)
                        .lines()
                        .filter(line -> line.contains("<module>payment</module>"))
                        .count();
        assertThat(count).isEqualTo(1);
    }

    @Test
    void addModuleEntryCreatesModulesElementIfAbsent() throws Exception {
        Path pom = writeRootPomNoModules();
        new PomEditor().addModuleEntry(pom.toFile(), "payment");
        String content = Files.readString(pom);
        assertThat(content).contains("<modules>");
        assertThat(content).contains("<module>payment</module>");
    }

    @Test
    void addModuleEntryResultIsValidXml() throws Exception {
        Path pom = writeRootPom("common");
        new PomEditor().addModuleEntry(pom.toFile(), "payment");
        javax.xml.parsers.DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(pom.toFile());
    }

    @Test
    void addModuleEntryHyphenatedName() throws Exception {
        Path pom = writeRootPom("common");
        new PomEditor().addModuleEntry(pom.toFile(), "payment-gateway");
        assertThat(Files.readString(pom)).contains("<module>payment-gateway</module>");
    }

    // ── addDependency ─────────────────────────────────────────────────────────

    @Test
    void addDependencyAppendsNewDep() throws Exception {
        Path pom = writeMonolithPom();
        new PomEditor().addDependency(pom.toFile(), "com.example", "payment-app");
        assertThat(Files.readString(pom)).contains("<artifactId>payment-app</artifactId>");
    }

    @Test
    void addDependencyGroupIdPresent() throws Exception {
        Path pom = writeMonolithPom();
        new PomEditor().addDependency(pom.toFile(), "com.example", "payment-app");
        assertThat(Files.readString(pom)).contains("<groupId>com.example</groupId>");
    }

    @Test
    void addDependencyVersionPresent() throws Exception {
        Path pom = writeMonolithPom();
        new PomEditor().addDependency(pom.toFile(), "com.example", "payment-app");
        assertThat(Files.readString(pom)).doesNotContain("<version>${project.version}</version>");
    }

    @Test
    void addDependencyKeepsExistingDeps() throws Exception {
        Path pom = writeMonolithPom();
        new PomEditor().addDependency(pom.toFile(), "com.example", "payment-app");
        assertThat(Files.readString(pom)).contains("<artifactId>order-app</artifactId>");
    }

    @Test
    void addDependencyIdempotent() throws Exception {
        Path pom = writeMonolithPom();
        PomEditor editor = new PomEditor();
        editor.addDependency(pom.toFile(), "com.example", "payment-app");
        editor.addDependency(pom.toFile(), "com.example", "payment-app");
        long count =
                Files.readString(pom)
                        .lines()
                        .filter(line -> line.contains("<artifactId>payment-app</artifactId>"))
                        .count();
        assertThat(count).isEqualTo(1);
    }

    @Test
    void addDependencyCreatesDependenciesElementIfAbsent() throws Exception {
        Path pom = writeMonolithPomNoDeps();
        new PomEditor().addDependency(pom.toFile(), "com.example", "payment-app");
        String content = Files.readString(pom);
        assertThat(content).contains("<dependencies>");
        assertThat(content).contains("<artifactId>payment-app</artifactId>");
    }

    // ── addDependencyManagementEntries ────────────────────────────────────────

    private Path writeRootPomWithDm(String... existingArtifactIds) throws IOException {
        StringBuilder dm =
                new StringBuilder("    <dependencyManagement>\n        <dependencies>\n");
        for (String a : existingArtifactIds) {
            dm.append("            <dependency>\n");
            dm.append("                <groupId>com.example</groupId>\n");
            dm.append("                <artifactId>").append(a).append("</artifactId>\n");
            dm.append("                <version>${project.version}</version>\n");
            dm.append("            </dependency>\n");
        }
        dm.append("        </dependencies>\n    </dependencyManagement>");
        String pom =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<project>\n"
                        + "    <groupId>com.example</groupId>\n"
                        + "    <artifactId>my-project</artifactId>\n"
                        + "    <version>1.0.0-SNAPSHOT</version>\n"
                        + dm
                        + "\n"
                        + "</project>";
        Path f = tempDir.resolve("root-pom-dm.xml");
        Files.writeString(f, pom);
        return f;
    }

    @Test
    void addDmEntriesAddsThreeArtifacts() throws Exception {
        Path pom = writeRootPomWithDm();
        new PomEditor().addDependencyManagementEntries(pom.toFile(), "com.example", "payment");
        String content = Files.readString(pom);
        assertThat(content).contains("<artifactId>payment-api</artifactId>");
        assertThat(content).contains("<artifactId>payment-core</artifactId>");
        assertThat(content).contains("<artifactId>payment-app</artifactId>");
    }

    @Test
    void addDmEntriesUsesProjectVersion() throws Exception {
        Path pom = writeRootPomWithDm();
        new PomEditor().addDependencyManagementEntries(pom.toFile(), "com.example", "payment");
        String content = Files.readString(pom);
        assertThat(content).contains("<version>${project.version}</version>");
    }

    @Test
    void addDmEntriesGroupIdIsCorrect() throws Exception {
        Path pom = writeRootPomWithDm();
        new PomEditor().addDependencyManagementEntries(pom.toFile(), "com.example", "payment");
        String content = Files.readString(pom);
        assertThat(content).contains("<groupId>com.example</groupId>");
    }

    @Test
    void addDmEntriesIdempotent() throws Exception {
        Path pom = writeRootPomWithDm();
        PomEditor editor = new PomEditor();
        editor.addDependencyManagementEntries(pom.toFile(), "com.example", "payment");
        editor.addDependencyManagementEntries(pom.toFile(), "com.example", "payment");
        long count =
                Files.readString(pom)
                        .lines()
                        .filter(l -> l.contains("<artifactId>payment-api</artifactId>"))
                        .count();
        assertThat(count).isEqualTo(1);
    }

    @Test
    void addDmEntriesSkipsAlreadyPresentArtifacts() throws Exception {
        Path pom = writeRootPomWithDm("payment-api");
        new PomEditor().addDependencyManagementEntries(pom.toFile(), "com.example", "payment");
        long count =
                Files.readString(pom)
                        .lines()
                        .filter(l -> l.contains("<artifactId>payment-api</artifactId>"))
                        .count();
        assertThat(count).isEqualTo(1);
    }

    @Test
    void addDmEntriesCreatesDmBlockIfAbsent() throws Exception {
        Path pom = writeRootPomNoModules();
        new PomEditor().addDependencyManagementEntries(pom.toFile(), "com.example", "payment");
        String content = Files.readString(pom);
        assertThat(content).contains("<dependencyManagement>");
        assertThat(content).contains("<artifactId>payment-api</artifactId>");
    }

    @Test
    void addDmEntriesResultIsValidXml() throws Exception {
        Path pom = writeRootPomWithDm();
        new PomEditor().addDependencyManagementEntries(pom.toFile(), "com.example", "payment");
        javax.xml.parsers.DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(pom.toFile());
    }
}
