package com.github.monodula.maven;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MonodulaSplitMojoTest {

    @TempDir Path tempDir;

    @Test
    void mojo_fails_when_output_dir_exists() throws Exception {
        // Create the output directory to simulate idempotency check
        Path outputDir = tempDir.resolve("application").resolve("finance-application");
        Files.createDirectories(outputDir);

        MonodulaSplitMojo mojo = new MonodulaSplitMojo();
        mojo.baseDirectory = tempDir.toFile();
        mojo.module = "finance";
        mojo.rootArtifactId = "myproject";
        mojo.projectGroupId = "com.example";
        mojo.projectVersion = "1.0.0";
        mojo.basePackage = "com.example.myproject";

        assertThatThrownBy(() -> mojo.execute())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void mojo_generates_all_files() throws Exception {
        // Create a realistic project structure
        Path root = tempDir.resolve("myproject");
        Files.createDirectories(root);

        // Create pom.xml files for module structure
        writePom(root, "com.example", "myproject", "1.0.0");
        Path financeDir = root.resolve("finance");
        writePom(financeDir, "com.example", "finance", "1.0.0");
        writePom(financeDir.resolve("finance-core"), "com.example", "finance-core", "1.0.0");
        writePom(financeDir.resolve("finance-app"), "com.example", "finance-app", "1.0.0");

        // Create monolith and aggregator poms
        Path appDir = root.resolve("application");
        Files.createDirectories(appDir);
        writePom(appDir, "com.example", "application", "1.0.0");

        Path monolithDir = appDir.resolve("monolith-application");
        Files.createDirectories(monolithDir);
        String monolithPom =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<project>\n"
                        + "    <groupId>com.example</groupId>\n"
                        + "    <artifactId>monolith-application</artifactId>\n"
                        + "    <version>1.0.0</version>\n"
                        + "    <dependencies>\n"
                        + "        <dependency><groupId>com.example</groupId>"
                        + "<artifactId>finance-app</artifactId>"
                        + "<version>1.0.0</version></dependency>\n"
                        + "    </dependencies>\n"
                        + "</project>";
        Files.writeString(monolithDir.resolve("pom.xml"), monolithPom);

        // Aggregator pom with modules
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

        MonodulaSplitMojo mojo = new MonodulaSplitMojo();
        mojo.baseDirectory = root.toFile();
        mojo.module = "finance";
        mojo.rootArtifactId = "myproject";
        mojo.projectGroupId = "com.example";
        mojo.projectVersion = "1.0.0";
        mojo.basePackage = "com.example.myproject";

        mojo.execute();

        // Verify generated files exist
        Path financeApp = appDir.resolve("finance-application");
        assertThat(financeApp.resolve("pom.xml")).exists();
        assertThat(financeApp.resolve("Dockerfile")).exists();
        assertThat(financeApp.resolve("src/main/resources/application.yml")).exists();

        // Verify main class exists
        assertThat(
                        financeApp.resolve(
                                "src/main/java/com/example/myproject/app/finance/FinanceStandaloneApplication.java"))
                .exists();

        // Verify test directories exist
        assertThat(financeApp.resolve("src/test/java")).exists();
        assertThat(financeApp.resolve("src/test/resources")).exists();
    }

    @Test
    void mojo_updates_monolith_pom() throws Exception {
        Path root = tempDir.resolve("myproject");
        Files.createDirectories(root);
        writePom(root, "com.example", "myproject", "1.0.0");

        Path financeDir = root.resolve("finance");
        writePom(financeDir, "com.example", "finance", "1.0.0");
        writePom(financeDir.resolve("finance-core"), "com.example", "finance-core", "1.0.0");
        writePom(financeDir.resolve("finance-app"), "com.example", "finance-app", "1.0.0");

        Path appDir = root.resolve("application");
        Files.createDirectories(appDir);
        writePom(appDir, "com.example", "application", "1.0.0");

        Path monolithDir = appDir.resolve("monolith-application");
        Files.createDirectories(monolithDir);
        String monolithPom =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<project>\n"
                        + "    <groupId>com.example</groupId>\n"
                        + "    <artifactId>monolith-application</artifactId>\n"
                        + "    <version>1.0.0</version>\n"
                        + "    <dependencies>\n"
                        + "        <dependency><groupId>com.example</groupId>"
                        + "<artifactId>finance-app</artifactId>"
                        + "<version>1.0.0</version></dependency>\n"
                        + "    </dependencies>\n"
                        + "</project>";
        Files.writeString(monolithDir.resolve("pom.xml"), monolithPom);

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

        MonodulaSplitMojo mojo = new MonodulaSplitMojo();
        mojo.baseDirectory = root.toFile();
        mojo.module = "finance";
        mojo.rootArtifactId = "myproject";
        mojo.projectGroupId = "com.example";
        mojo.projectVersion = "1.0.0";
        mojo.basePackage = "com.example.myproject";

        mojo.execute();

        // Verify monolith pom was updated
        String monolithContent = Files.readString(monolithDir.resolve("pom.xml"));
        assertThat(monolithContent).doesNotContain("finance-app");
        assertThat(monolithContent).contains("finance-core");
    }

    @Test
    void mojo_updates_aggregator_pom() throws Exception {
        Path root = tempDir.resolve("myproject");
        Files.createDirectories(root);
        writePom(root, "com.example", "myproject", "1.0.0");

        Path financeDir = root.resolve("finance");
        writePom(financeDir, "com.example", "finance", "1.0.0");
        writePom(financeDir.resolve("finance-core"), "com.example", "finance-core", "1.0.0");
        writePom(financeDir.resolve("finance-app"), "com.example", "finance-app", "1.0.0");

        Path appDir = root.resolve("application");
        Files.createDirectories(appDir);

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

        Path monolithDir = appDir.resolve("monolith-application");
        Files.createDirectories(monolithDir);
        String monolithPom =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<project>\n"
                        + "    <groupId>com.example</groupId>\n"
                        + "    <artifactId>monolith-application</artifactId>\n"
                        + "    <version>1.0.0</version>\n"
                        + "    <dependencies>\n"
                        + "        <dependency><groupId>com.example</groupId>"
                        + "<artifactId>finance-app</artifactId>"
                        + "<version>1.0.0</version></dependency>\n"
                        + "    </dependencies>\n"
                        + "</project>";
        Files.writeString(monolithDir.resolve("pom.xml"), monolithPom);

        MonodulaSplitMojo mojo = new MonodulaSplitMojo();
        mojo.baseDirectory = root.toFile();
        mojo.module = "finance";
        mojo.rootArtifactId = "myproject";
        mojo.projectGroupId = "com.example";
        mojo.projectVersion = "1.0.0";
        mojo.basePackage = "com.example.myproject";

        mojo.execute();

        String aggContent = Files.readString(appDir.resolve("pom.xml"));
        assertThat(aggContent).contains("<module>finance-application</module>");
        assertThat(aggContent).contains("<module>monolith-application</module>");
    }

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
}
