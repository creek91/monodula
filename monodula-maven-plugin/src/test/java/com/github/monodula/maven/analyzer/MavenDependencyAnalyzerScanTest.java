package com.github.monodula.maven.analyzer;

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

class MavenDependencyAnalyzerScanTest {

    @TempDir Path tempDir;

    private void writePom(Path dir, String groupId, String artifactId, String version)
            throws IOException {
        Files.createDirectories(dir);
        String pom =
                """
            <?xml version="1.0" encoding="UTF-8"?>
            <project>
                <groupId>%s</groupId>
                <artifactId>%s</artifactId>
                <version>%s</version>
            </project>
            """
                        .formatted(groupId, artifactId, version);
        Files.writeString(dir.resolve("pom.xml"), pom);
    }

    @Test
    void scanModules_reads_artifactId_from_pom_not_dirname() throws IOException {
        // Directory named "fc" but pom says artifactId=finance-core
        Path root = tempDir.resolve("myproject");
        writePom(root, "com.example", "myproject", "1.0.0");

        Path prefixedDir = root.resolve("myproject-finance");
        writePom(prefixedDir, "com.example", "myproject-finance", "1.0.0");

        Path leafDir = prefixedDir.resolve("fc"); // directory name diverges from artifactId
        writePom(leafDir, "com.example", "finance-core", "1.0.0");

        MavenDependencyAnalyzer analyzer = new MavenDependencyAnalyzer();
        List<MavenModule> modules = analyzer.scanModules(root.resolve("pom.xml").toFile());

        assertThat(modules).extracting(MavenModule::getArtifactId).contains("finance-core");
        assertThat(modules).extracting(MavenModule::getArtifactId).doesNotContain("fc");
    }

    @Test
    void scanModules_populates_groupId_and_version() throws IOException {
        Path root = tempDir.resolve("myproject");
        writePom(root, "com.example", "myproject", "1.0.0");

        Path financeDir = root.resolve("myproject-finance");
        writePom(financeDir, "com.example", "myproject-finance", "1.0.0");

        Path coreDir = financeDir.resolve("finance-core");
        writePom(coreDir, "com.example", "finance-core", "1.0.0");

        MavenDependencyAnalyzer analyzer = new MavenDependencyAnalyzer();
        List<MavenModule> modules = analyzer.scanModules(root.resolve("pom.xml").toFile());

        MavenModule financeCore =
                modules.stream()
                        .filter(m -> m.getArtifactId().equals("finance-core"))
                        .findFirst()
                        .orElseThrow();

        assertThat(financeCore.getGroupId()).isEqualTo("com.example");
        assertThat(financeCore.getVersion()).isEqualTo("1.0.0");
    }

    @Test
    void scanModules_skips_all_aggregator_poms() throws IOException {
        Path root = tempDir.resolve("myproject");
        writePom(root, "com.example", "myproject", "1.0.0");

        Path commonDir = root.resolve("myproject-common");
        writePom(commonDir, "com.example", "myproject-common", "1.0.0");

        Path commonApiDir = commonDir.resolve("common-api");
        writePom(commonApiDir, "com.example", "common-api", "1.0.0");

        MavenDependencyAnalyzer analyzer = new MavenDependencyAnalyzer();
        List<MavenModule> modules = analyzer.scanModules(root.resolve("pom.xml").toFile());

        assertThat(modules)
                .extracting(MavenModule::getArtifactId)
                .doesNotContain("myproject", "myproject-common")
                .contains("common-api");
    }

    @Test
    void scanModules_reads_own_artifactId_not_parent_artifactId() throws IOException {
        Path root = tempDir.resolve("myproject");
        writePom(root, "com.example", "myproject", "1.0.0");

        Path financeDir = root.resolve("myproject-finance");
        writePom(financeDir, "com.example", "myproject-finance", "1.0.0");

        // Write a pom with parent block — artifactId is in both parent and module level
        Path coreDir = financeDir.resolve("finance-core");
        Files.createDirectories(coreDir);
        String pomWithParent =
                """
            <?xml version="1.0" encoding="UTF-8"?>
            <project>
                <parent>
                    <groupId>com.example</groupId>
                    <artifactId>myproject-finance</artifactId>
                    <version>1.0.0</version>
                </parent>
                <artifactId>finance-core</artifactId>
            </project>
            """;
        Files.writeString(coreDir.resolve("pom.xml"), pomWithParent);

        MavenDependencyAnalyzer analyzer = new MavenDependencyAnalyzer();
        List<MavenModule> modules = analyzer.scanModules(root.resolve("pom.xml").toFile());

        assertThat(modules).extracting(MavenModule::getArtifactId).contains("finance-core");
        assertThat(modules)
                .extracting(MavenModule::getArtifactId)
                .doesNotContain("myproject-finance");
    }

    @Test
    void scanModules_returns_empty_when_root_pom_absent() {
        MavenDependencyAnalyzer analyzer = new MavenDependencyAnalyzer();
        List<MavenModule> modules = analyzer.scanModules(new File("/nonexistent/pom.xml"));
        assertThat(modules).isEmpty();
    }

    @Test
    void scanModules_classifies_common_modules_correctly() throws IOException {
        Path root = tempDir.resolve("myproject");
        writePom(root, "com.example", "myproject", "1.0.0");

        Path commonDir = root.resolve("myproject-common");
        writePom(commonDir, "com.example", "myproject-common", "1.0.0");

        for (String sub : List.of("common-api", "common-core", "common-app")) {
            Path subDir = commonDir.resolve(sub);
            writePom(subDir, "com.example", sub, "1.0.0");
        }

        MavenDependencyAnalyzer analyzer = new MavenDependencyAnalyzer();
        List<MavenModule> modules = analyzer.scanModules(root.resolve("pom.xml").toFile());

        assertThat(modules).allMatch(m -> m.getType() == ModuleType.COMMON);
        assertThat(modules)
                .extracting(MavenModule::getArtifactId)
                .containsExactlyInAnyOrder("common-api", "common-core", "common-app");
    }

    @Test
    void scanModules_populates_dependencies_from_pom() throws IOException {
        Path root = tempDir.resolve("myproject");
        writePom(root, "com.example", "myproject", "1.0.0");

        Path financeDir = root.resolve("finance");
        Files.createDirectories(financeDir);

        // finance-core depends on finance-api and common-api
        Path coreDir = financeDir.resolve("finance-core");
        Files.createDirectories(coreDir);
        String corePom =
                """
            <?xml version="1.0" encoding="UTF-8"?>
            <project>
                <parent>
                    <groupId>com.example</groupId>
                    <artifactId>finance</artifactId>
                    <version>1.0.0</version>
                </parent>
                <artifactId>finance-core</artifactId>
                <dependencies>
                    <dependency>
                        <groupId>com.example</groupId>
                        <artifactId>finance-api</artifactId>
                    </dependency>
                    <dependency>
                        <groupId>com.example</groupId>
                        <artifactId>common-api</artifactId>
                    </dependency>
                </dependencies>
            </project>
            """;
        Files.writeString(coreDir.resolve("pom.xml"), corePom);

        List<MavenModule> modules =
                new MavenDependencyAnalyzer().scanModules(root.resolve("pom.xml").toFile());

        MavenModule financeCore =
                modules.stream()
                        .filter(m -> "finance-core".equals(m.getArtifactId()))
                        .findFirst()
                        .orElseThrow();
        assertThat(financeCore.getDependencies())
                .containsExactlyInAnyOrder("finance-api", "common-api");
    }

    @Test
    void scanModules_dependencies_empty_when_no_dependencies_block() throws IOException {
        Path root = tempDir.resolve("myproject");
        writePom(root, "com.example", "myproject", "1.0.0");

        Path financeDir = root.resolve("finance");
        Path apiDir = financeDir.resolve("finance-api");
        writePom(apiDir, "com.example", "finance-api", "1.0.0");

        List<MavenModule> modules =
                new MavenDependencyAnalyzer().scanModules(root.resolve("pom.xml").toFile());

        MavenModule financeApi =
                modules.stream()
                        .filter(m -> "finance-api".equals(m.getArtifactId()))
                        .findFirst()
                        .orElseThrow();
        assertThat(financeApi.getDependencies()).isEmpty();
    }

    @Test
    void scanModules_dependencies_does_not_include_parent_artifactId() throws IOException {
        Path root = tempDir.resolve("myproject");
        writePom(root, "com.example", "myproject", "1.0.0");

        Path financeDir = root.resolve("finance");
        Path coreDir = financeDir.resolve("finance-core");
        Files.createDirectories(coreDir);
        // pom has <parent><artifactId>finance</artifactId></parent> — must NOT appear in
        // dependencies
        String corePom =
                """
            <?xml version="1.0" encoding="UTF-8"?>
            <project>
                <parent>
                    <groupId>com.example</groupId>
                    <artifactId>finance</artifactId>
                    <version>1.0.0</version>
                </parent>
                <artifactId>finance-core</artifactId>
                <dependencies>
                    <dependency>
                        <groupId>com.example</groupId>
                        <artifactId>finance-api</artifactId>
                    </dependency>
                </dependencies>
            </project>
            """;
        Files.writeString(coreDir.resolve("pom.xml"), corePom);

        List<MavenModule> modules =
                new MavenDependencyAnalyzer().scanModules(root.resolve("pom.xml").toFile());

        MavenModule financeCore =
                modules.stream()
                        .filter(m -> "finance-core".equals(m.getArtifactId()))
                        .findFirst()
                        .orElseThrow();
        assertThat(financeCore.getDependencies())
                .containsExactly("finance-api")
                .doesNotContain("finance"); // parent artifactId must not leak in
    }
}
