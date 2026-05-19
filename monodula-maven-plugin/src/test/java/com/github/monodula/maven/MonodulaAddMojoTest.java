package com.github.monodula.maven;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MonodulaAddMojoTest {

    @TempDir Path tempDir;

    /** Writes root pom.xml with <modules> block and empty dependencyManagement */
    private void writeRootPom() throws IOException {
        String pom =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<project>\n"
                        + "    <groupId>com.example</groupId>\n"
                        + "    <artifactId>my-project</artifactId>\n"
                        + "    <version>1.0.0-SNAPSHOT</version>\n"
                        + "    <packaging>pom</packaging>\n"
                        + "    <modules>\n"
                        + "        <module>common</module>\n"
                        + "    </modules>\n"
                        + "    <dependencyManagement>\n"
                        + "        <dependencies/>\n"
                        + "    </dependencyManagement>\n"
                        + "</project>";
        Files.writeString(tempDir.resolve("pom.xml"), pom);
    }

    /** Writes minimal monolith pom */
    private void writeMonolithPom() throws IOException {
        Path monolithDir = tempDir.resolve("application/monolith-application");
        Files.createDirectories(monolithDir);
        String pom =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<project>\n"
                        + "    <groupId>com.example</groupId>\n"
                        + "    <artifactId>monolith-application</artifactId>\n"
                        + "    <version>1.0.0-SNAPSHOT</version>\n"
                        + "    <dependencies/>\n"
                        + "</project>";
        Files.writeString(monolithDir.resolve("pom.xml"), pom);
    }

    /** Creates a mojo pre-configured with tempDir */
    private MonodulaAddMojo mojo(String module) {
        MonodulaAddMojo m = new MonodulaAddMojo();
        m.baseDirectory = tempDir.toFile();
        m.module = module;
        m.rootArtifactId = "my-project";
        m.projectGroupId = "com.example";
        m.projectVersion = "1.0.0-SNAPSHOT";
        m.basePackage = "com.example.myproject";
        return m;
    }

    // ── 4.1 Happy path ────────────────────────────────────────────────────────

    @Nested
    class HappyPath {

        @BeforeEach
        void setup() throws IOException {
            writeRootPom();
            writeMonolithPom();
        }

        @Test
        void executeCreatesAllModuleFiles() throws Exception {
            mojo("payment").execute();
            Path base = tempDir.resolve("payment");
            assertThat(base.resolve("pom.xml")).isRegularFile();
            assertThat(base.resolve("payment-api/pom.xml")).isRegularFile();
            assertThat(base.resolve("payment-core/pom.xml")).isRegularFile();
            assertThat(base.resolve("payment-app/pom.xml")).isRegularFile();
            assertThat(base.resolve("payment-core/src/main/resources/META-INF/spring.factories"))
                    .isRegularFile();
            assertThat(base.resolve("payment-core/src/main/resources/payment-config.yml"))
                    .isRegularFile();
            assertThat(
                            base.resolve(
                                    "payment-core/src/main/java/com/example/myproject/payment/config"
                                            + "/PaymentConfigLoader.java"))
                    .isRegularFile();
            assertThat(
                            base.resolve(
                                    "payment-app/src/main/java/com/example/myproject/payment/app"
                                            + "/endpoints/controller/PaymentController.java"))
                    .isRegularFile();
        }

        @Test
        void executeUpdatesRootPomModules() throws Exception {
            mojo("payment").execute();
            String content = Files.readString(tempDir.resolve("pom.xml"));
            assertThat(content).contains("<module>payment</module>");
        }

        @Test
        void executeUpdatesMonolithPomDependencies() throws Exception {
            mojo("payment").execute();
            String content =
                    Files.readString(tempDir.resolve("application/monolith-application/pom.xml"));
            assertThat(content).contains("<artifactId>payment-app</artifactId>");
        }

        @Test
        void executeWithHyphenatedModule() throws Exception {
            mojo("payment-gateway").execute();
            assertThat(tempDir.resolve("payment-gateway")).isDirectory();
            String rootContent = Files.readString(tempDir.resolve("pom.xml"));
            assertThat(rootContent).contains("<module>payment-gateway</module>");
        }

        @Test
        void executeRegistersDependencyManagementEntries() throws Exception {
            mojo("payment").execute();
            String content = Files.readString(tempDir.resolve("pom.xml"));
            assertThat(content).contains("<artifactId>payment-api</artifactId>");
            assertThat(content).contains("<artifactId>payment-core</artifactId>");
            assertThat(content).contains("<artifactId>payment-app</artifactId>");
        }

        @Test
        void executeDependencyManagementEntriesUseProjectVersion() throws Exception {
            mojo("payment").execute();
            String content = Files.readString(tempDir.resolve("pom.xml"));
            assertThat(content).contains("<version>${project.version}</version>");
        }
    }

    // ── 4.2 Idempotency ───────────────────────────────────────────────────────

    @Nested
    class Idempotency {

        @BeforeEach
        void setup() throws IOException {
            writeRootPom();
            writeMonolithPom();
            // Pre-create the module directory to simulate already-exists state
            Files.createDirectories(tempDir.resolve("payment"));
        }

        @Test
        void executeThrowsWhenModuleDirAlreadyExists() {
            assertThatThrownBy(() -> mojo("payment").execute())
                    .isInstanceOf(MojoExecutionException.class)
                    .hasMessageContaining("already exists");
        }

        @Test
        void executeDoesNotModifyRootPomWhenModuleDirExists() throws Exception {
            String before = Files.readString(tempDir.resolve("pom.xml"));
            try {
                mojo("payment").execute();
            } catch (MojoExecutionException ignored) {
                // expected
            }
            assertThat(Files.readString(tempDir.resolve("pom.xml"))).isEqualTo(before);
        }

        @Test
        void executeDoesNotModifyMonolithPomWhenModuleDirExists() throws Exception {
            Path monolithPom = tempDir.resolve("application/monolith-application/pom.xml");
            String before = Files.readString(monolithPom);
            try {
                mojo("payment").execute();
            } catch (MojoExecutionException ignored) {
                // expected
            }
            assertThat(Files.readString(monolithPom)).isEqualTo(before);
        }
    }

    // ── 4.3 Root pom missing ─────────────────────────────────────────────────

    @Nested
    class RootPomMissing {

        @BeforeEach
        void setup() throws IOException {
            // Do NOT write root pom
            writeMonolithPom();
        }

        @Test
        void executeThrowsWhenRootPomMissing() {
            assertThatThrownBy(() -> mojo("payment").execute())
                    .isInstanceOf(MojoExecutionException.class);
        }

        @Test
        void executeDoesNotCreateModuleDirWhenRootPomMissing() {
            try {
                mojo("payment").execute();
            } catch (MojoExecutionException ignored) {
                // expected
            }
            assertThat(tempDir.resolve("payment")).doesNotExist();
        }
    }

    // ── 4.4 Monolith pom missing (optional project) ──────────────────────────

    @Test
    void executeSkipsMonolithUpdateWhenMonolithPomMissing() throws Exception {
        writeRootPom();
        // Do NOT create monolith-application directory/pom
        mojo("payment").execute(); // must not throw
        assertThat(tempDir.resolve("payment/pom.xml")).isRegularFile();
    }

    // ── 4.5 Parameter validation ─────────────────────────────────────────────

    @Nested
    class ParameterValidation {

        @BeforeEach
        void setup() throws IOException {
            writeRootPom();
            writeMonolithPom();
        }

        @Test
        void executeThrowsWhenModuleNameIsNull() {
            assertThatThrownBy(() -> mojo(null).execute())
                    .isInstanceOf(MojoExecutionException.class);
        }

        @Test
        void executeThrowsWhenModuleNameIsEmpty() {
            assertThatThrownBy(() -> mojo("").execute()).isInstanceOf(MojoExecutionException.class);
        }

        @Test
        void executeThrowsWhenModuleNameStartsWithDigit() {
            assertThatThrownBy(() -> mojo("1payment").execute())
                    .isInstanceOf(MojoExecutionException.class)
                    .hasMessageContaining("[a-z]");
        }

        @Test
        void executeThrowsWhenModuleNameContainsUpperCase() {
            assertThatThrownBy(() -> mojo("Payment").execute())
                    .isInstanceOf(MojoExecutionException.class);
        }

        @Test
        void executeThrowsWhenModuleNameContainsUnderscore() {
            assertThatThrownBy(() -> mojo("pay_ment").execute())
                    .isInstanceOf(MojoExecutionException.class);
        }

        @Test
        void executeThrowsWhenModuleNameContainsSpace() {
            assertThatThrownBy(() -> mojo("pay ment").execute())
                    .isInstanceOf(MojoExecutionException.class);
        }

        @Test
        void executeAcceptsSingleLetterName() throws Exception {
            mojo("p").execute();
            assertThat(tempDir.resolve("p")).isDirectory();
        }

        @Test
        void executeAcceptsNameWithDigitsAfterFirstChar() throws Exception {
            mojo("pay2ment").execute();
            assertThat(tempDir.resolve("pay2ment")).isDirectory();
        }

        @Test
        void executeAcceptsMultipleHyphens() throws Exception {
            mojo("a-b-c").execute();
            assertThat(tempDir.resolve("a-b-c")).isDirectory();
        }
    }

    // ── 4.6 IO failure ───────────────────────────────────────────────────────

    @Test
    void executeWrapsIOExceptionAsMojoExecutionException() throws IOException {
        writeRootPom();
        writeMonolithPom();
        // Create "payment" as a regular file (not a directory), so mkdir will fail
        Files.writeString(tempDir.resolve("payment"), "not a directory");

        assertThatThrownBy(() -> mojo("payment").execute())
                .isInstanceOf(MojoExecutionException.class);
    }
}
