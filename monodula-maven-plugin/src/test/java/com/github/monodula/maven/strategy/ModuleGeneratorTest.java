package com.github.monodula.maven.strategy;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ModuleGeneratorTest {

    @TempDir Path tempDir;

    private AddContext ctx;

    @BeforeEach
    void setUp() {
        ctx = new AddContext("payment", "com.example", "my-project", "1.0.0-SNAPSHOT");
    }

    // ── 2.1 Directory structure ───────────────────────────────────────────────

    @Nested
    class DirectoryStructure {

        @BeforeEach
        void generate() throws Exception {
            new ModuleGenerator().generate(ctx, tempDir.toFile());
        }

        @Test
        void aggregatorDirCreated() {
            assertThat(tempDir.resolve("payment")).isDirectory();
        }

        @Test
        void apiDirCreated() {
            assertThat(tempDir.resolve("payment/payment-api")).isDirectory();
        }

        @Test
        void coreDirCreated() {
            assertThat(tempDir.resolve("payment/payment-core")).isDirectory();
        }

        @Test
        void appDirCreated() {
            assertThat(tempDir.resolve("payment/payment-app")).isDirectory();
        }

        @Test
        void coreMainJavaDirCreated() {
            assertThat(
                            tempDir.resolve(
                                    "payment/payment-core/src/main/java/com/example/myproject/payment/config"))
                    .isDirectory();
        }

        @Test
        void coreMainResourcesDirCreated() {
            assertThat(tempDir.resolve("payment/payment-core/src/main/resources/META-INF"))
                    .isDirectory();
        }

        @Test
        void coreTestJavaDirCreated() {
            assertThat(tempDir.resolve("payment/payment-core/src/test/java")).isDirectory();
        }

        @Test
        void coreTestResourcesDirCreated() {
            assertThat(tempDir.resolve("payment/payment-core/src/test/resources")).isDirectory();
        }

        @Test
        void appMainJavaDirCreated() {
            String path =
                    "payment/payment-app/src/main/java"
                            + "/com/example/myproject/payment/app/endpoints/controller";
            assertThat(tempDir.resolve(path)).isDirectory();
        }

        @Test
        void appTestJavaDirCreated() {
            assertThat(tempDir.resolve("payment/payment-app/src/test/java")).isDirectory();
        }
    }

    // ── 2.2 Aggregator pom ───────────────────────────────────────────────────

    @Nested
    class AggregatorPom {

        private String content;

        @BeforeEach
        void generate() throws Exception {
            new ModuleGenerator().generate(ctx, tempDir.toFile());
            content = Files.readString(tempDir.resolve("payment/pom.xml"));
        }

        @Test
        void aggregatorPomExists() {
            assertThat(tempDir.resolve("payment/pom.xml")).isRegularFile();
        }

        @Test
        void aggregatorPomParentGroupId() {
            assertThat(content).contains("<groupId>com.example</groupId>");
        }

        @Test
        void aggregatorPomParentArtifactId() {
            assertThat(content).contains("<artifactId>my-project</artifactId>");
        }

        @Test
        void aggregatorPomParentVersion() {
            assertThat(content).contains("<version>1.0.0-SNAPSHOT</version>");
        }

        @Test
        void aggregatorPomOwnArtifactId() {
            assertThat(content).contains("<artifactId>payment</artifactId>");
        }

        @Test
        void aggregatorPomPackagingIsPom() {
            assertThat(content).contains("<packaging>pom</packaging>");
        }

        @Test
        void aggregatorPomModulesContainsApi() {
            assertThat(content).contains("<module>payment-api</module>");
        }

        @Test
        void aggregatorPomModulesContainsCore() {
            assertThat(content).contains("<module>payment-core</module>");
        }

        @Test
        void aggregatorPomModulesContainsApp() {
            assertThat(content).contains("<module>payment-app</module>");
        }

        @Test
        void aggregatorPomHyphenatedModuleName() throws Exception {
            AddContext hyphenCtx =
                    new AddContext(
                            "payment-gateway", "com.example", "my-project", "1.0.0-SNAPSHOT");
            new ModuleGenerator().generate(hyphenCtx, tempDir.toFile());
            String c = Files.readString(tempDir.resolve("payment-gateway/pom.xml"));
            assertThat(c).contains("<artifactId>payment-gateway</artifactId>");
            assertThat(c).contains("<module>payment-gateway-api</module>");
            assertThat(c).contains("<module>payment-gateway-core</module>");
            assertThat(c).contains("<module>payment-gateway-app</module>");
        }
    }

    // ── 2.3 API pom ──────────────────────────────────────────────────────────

    @Nested
    class ApiPom {

        private String content;

        @BeforeEach
        void generate() throws Exception {
            new ModuleGenerator().generate(ctx, tempDir.toFile());
            content = Files.readString(tempDir.resolve("payment/payment-api/pom.xml"));
        }

        @Test
        void apiPomExists() {
            assertThat(tempDir.resolve("payment/payment-api/pom.xml")).isRegularFile();
        }

        @Test
        void apiPomParentArtifactId() {
            assertThat(content).contains("<artifactId>payment</artifactId>");
        }

        @Test
        void apiPomOwnArtifactId() {
            assertThat(content).contains("<artifactId>payment-api</artifactId>");
        }

        @Test
        void apiPomNoDependencies() {
            assertThat(content).doesNotContain("<dependencies>");
        }
    }

    // ── 2.4 Core pom ─────────────────────────────────────────────────────────

    @Nested
    class CorePom {

        private String content;

        @BeforeEach
        void generate() throws Exception {
            new ModuleGenerator().generate(ctx, tempDir.toFile());
            content = Files.readString(tempDir.resolve("payment/payment-core/pom.xml"));
        }

        @Test
        void corePomExists() {
            assertThat(tempDir.resolve("payment/payment-core/pom.xml")).isRegularFile();
        }

        @Test
        void corePomParentArtifactId() {
            assertThat(content).contains("<artifactId>payment</artifactId>");
        }

        @Test
        void corePomOwnArtifactId() {
            assertThat(content).contains("<artifactId>payment-core</artifactId>");
        }

        @Test
        void corePomDependsOnOwnApi() {
            assertThat(content).contains("<artifactId>payment-api</artifactId>");
        }

        @Test
        void corePomDependsOnCommonApi() {
            assertThat(content).contains("<artifactId>common-api</artifactId>");
        }

        @Test
        void corePomDependsOnCommonCore() {
            assertThat(content).contains("<artifactId>common-core</artifactId>");
        }

        @Test
        void corePomDependencyGroupIdIsProjectGroupId() {
            assertThat(content)
                    .containsPattern(
                            "(?s)<groupId>com\\.example</groupId>.*<artifactId>payment-api</artifactId>");
        }

        @Test
        void corePomIntraModuleDependencyHasVersion() {
            assertThat(content)
                    .containsPattern(
                            "(?s)<artifactId>payment-api</artifactId>"
                                    + "\\s*<version>\\$\\{project\\.version\\}</version>");
        }

        @Test
        void corePomCommonApiDependencyHasVersion() {
            assertThat(content)
                    .containsPattern(
                            "(?s)<artifactId>common-api</artifactId>"
                                    + "\\s*<version>\\$\\{project\\.version\\}</version>");
        }

        @Test
        void corePomCommonCoreDependencyHasVersion() {
            assertThat(content)
                    .containsPattern(
                            "(?s)<artifactId>common-core</artifactId>"
                                    + "\\s*<version>\\$\\{project\\.version\\}</version>");
        }
    }

    // ── 2.5 App pom ──────────────────────────────────────────────────────────

    @Nested
    class AppPom {

        private String content;

        @BeforeEach
        void generate() throws Exception {
            new ModuleGenerator().generate(ctx, tempDir.toFile());
            content = Files.readString(tempDir.resolve("payment/payment-app/pom.xml"));
        }

        @Test
        void appPomExists() {
            assertThat(tempDir.resolve("payment/payment-app/pom.xml")).isRegularFile();
        }

        @Test
        void appPomParentArtifactId() {
            assertThat(content).contains("<artifactId>payment</artifactId>");
        }

        @Test
        void appPomOwnArtifactId() {
            assertThat(content).contains("<artifactId>payment-app</artifactId>");
        }

        @Test
        void appPomDependsOnOwnCore() {
            assertThat(content).contains("<artifactId>payment-core</artifactId>");
        }

        @Test
        void appPomDependsOnSpringBootStarterWeb() {
            assertThat(content).contains("<artifactId>spring-boot-starter-web</artifactId>");
        }

        @Test
        void appPomIntraModuleDependencyHasVersion() {
            assertThat(content)
                    .containsPattern(
                            "(?s)<artifactId>payment-core</artifactId>"
                                    + "\\s*<version>\\$\\{project\\.version\\}</version>");
        }

        @Test
        void appPomSpringBootStarterWebHasGroupId() {
            int idx = content.indexOf("spring-boot-starter-web");
            assertThat(idx).isGreaterThan(0);
            int depStart = content.lastIndexOf("<dependency>", idx);
            int depEnd = content.indexOf("</dependency>", idx) + "</dependency>".length();
            String depBlock = content.substring(depStart, depEnd);
            assertThat(depBlock).contains("<groupId>org.springframework.boot</groupId>");
        }
    }

    // ── 2.6 ConfigLoader ─────────────────────────────────────────────────────

    @Nested
    class ConfigLoader {

        private Path file;
        private String content;

        @BeforeEach
        void generate() throws Exception {
            new ModuleGenerator().generate(ctx, tempDir.toFile());
            file =
                    tempDir.resolve(
                            "payment/payment-core/src/main/java"
                                    + "/com/example/myproject/payment/config/PaymentConfigLoader.java");
            content = Files.readString(file);
        }

        @Test
        void configLoaderFileExists() {
            assertThat(file).isRegularFile();
        }

        @Test
        void configLoaderPackageDeclaration() {
            assertThat(content).contains("package com.example.myproject.payment.config;");
        }

        @Test
        void configLoaderImport() {
            assertThat(content)
                    .contains(
                            "import com.example.myproject.common.core.config.ModuleConfigLoader;");
        }

        @Test
        void configLoaderClassDeclaration() {
            assertThat(content)
                    .contains("public class PaymentConfigLoader extends ModuleConfigLoader");
        }

        @Test
        void configLoaderGetBaseNameReturnsModuleConfig() {
            assertThat(content).contains("return \"payment-config\";");
        }

        @Test
        void configLoaderHyphenatedModule() throws Exception {
            AddContext hyphenCtx =
                    new AddContext(
                            "payment-gateway", "com.example", "my-project", "1.0.0-SNAPSHOT");
            new ModuleGenerator().generate(hyphenCtx, tempDir.toFile());
            Path f =
                    tempDir.resolve(
                            "payment-gateway/payment-gateway-core/src/main/java"
                                    + "/com/example/myproject/paymentgateway/config/PaymentGatewayConfigLoader.java");
            String c = Files.readString(f);
            assertThat(c).contains("package com.example.myproject.paymentgateway.config;");
            assertThat(c)
                    .contains("public class PaymentGatewayConfigLoader extends ModuleConfigLoader");
            assertThat(c).contains("return \"payment-gateway-config\";");
        }

        @Test
        void configLoaderSingleLetterModule() throws Exception {
            AddContext pCtx = new AddContext("p", "com.example", "my-project", "1.0.0-SNAPSHOT");
            new ModuleGenerator().generate(pCtx, tempDir.toFile());
            Path f =
                    tempDir.resolve(
                            "p/p-core/src/main/java/com/example/myproject/p/config/PConfigLoader.java");
            String c = Files.readString(f);
            assertThat(c).contains("public class PConfigLoader extends ModuleConfigLoader");
            assertThat(c).contains("return \"p-config\";");
        }
    }

    // ── 2.7 spring.factories ─────────────────────────────────────────────────

    @Nested
    class SpringFactories {

        private String content;

        @BeforeEach
        void generate() throws Exception {
            new ModuleGenerator().generate(ctx, tempDir.toFile());
            content =
                    Files.readString(
                            tempDir.resolve(
                                    "payment/payment-core/src/main/resources/META-INF/spring.factories"));
        }

        @Test
        void springFactoriesExists() {
            assertThat(
                            tempDir.resolve(
                                    "payment/payment-core/src/main/resources/META-INF/spring.factories"))
                    .isRegularFile();
        }

        @Test
        void springFactoriesKeyIsEnvironmentPostProcessor() {
            assertThat(content).contains("org.springframework.boot.env.EnvironmentPostProcessor=");
        }

        @Test
        void springFactoriesValueIsConfigLoaderFqn() {
            assertThat(content)
                    .contains("com.example.myproject.payment.config.PaymentConfigLoader");
        }

        @Test
        void springFactoriesHyphenatedModule() throws Exception {
            AddContext hyphenCtx =
                    new AddContext(
                            "payment-gateway", "com.example", "my-project", "1.0.0-SNAPSHOT");
            new ModuleGenerator().generate(hyphenCtx, tempDir.toFile());
            String c =
                    Files.readString(
                            tempDir.resolve(
                                    "payment-gateway/payment-gateway-core/src/main/resources"
                                            + "/META-INF/spring.factories"));
            assertThat(c)
                    .contains(
                            "com.example.myproject.paymentgateway.config.PaymentGatewayConfigLoader");
        }
    }

    // ── 2.8 Config YML files ─────────────────────────────────────────────────

    @Nested
    class ConfigYml {

        private Path resourcesDir;

        @BeforeEach
        void generate() throws Exception {
            new ModuleGenerator().generate(ctx, tempDir.toFile());
            resourcesDir = tempDir.resolve("payment/payment-core/src/main/resources");
        }

        @Test
        void configYmlExists() {
            assertThat(resourcesDir.resolve("payment-config.yml")).isRegularFile();
        }

        @Test
        void configYmlDevExists() {
            assertThat(resourcesDir.resolve("payment-config-dev.yml")).isRegularFile();
        }

        @Test
        void configYmlTestExists() {
            assertThat(resourcesDir.resolve("payment-config-test.yml")).isRegularFile();
        }

        @Test
        void configYmlProdExists() {
            assertThat(resourcesDir.resolve("payment-config-prod.yml")).isRegularFile();
        }

        @Test
        void configYmlFilesAreEmpty() throws Exception {
            assertThat(Files.readString(resourcesDir.resolve("payment-config.yml"))).isEmpty();
            assertThat(Files.readString(resourcesDir.resolve("payment-config-dev.yml"))).isEmpty();
            assertThat(Files.readString(resourcesDir.resolve("payment-config-test.yml"))).isEmpty();
            assertThat(Files.readString(resourcesDir.resolve("payment-config-prod.yml"))).isEmpty();
        }

        @Test
        void configYmlHyphenatedModule() throws Exception {
            AddContext hyphenCtx =
                    new AddContext(
                            "payment-gateway", "com.example", "my-project", "1.0.0-SNAPSHOT");
            new ModuleGenerator().generate(hyphenCtx, tempDir.toFile());
            Path res = tempDir.resolve("payment-gateway/payment-gateway-core/src/main/resources");
            assertThat(res.resolve("payment-gateway-config.yml")).isRegularFile();
            assertThat(res.resolve("payment-gateway-config-dev.yml")).isRegularFile();
            assertThat(res.resolve("payment-gateway-config-test.yml")).isRegularFile();
            assertThat(res.resolve("payment-gateway-config-prod.yml")).isRegularFile();
        }
    }

    // ── 2.9 Controller ───────────────────────────────────────────────────────

    @Nested
    class Controller {

        private Path file;
        private String content;

        @BeforeEach
        void generate() throws Exception {
            new ModuleGenerator().generate(ctx, tempDir.toFile());
            file =
                    tempDir.resolve(
                            "payment/payment-app/src/main/java/com/example/myproject"
                                    + "/payment/app/endpoints/controller/PaymentController.java");
            content = Files.readString(file);
        }

        @Test
        void controllerFileExists() {
            assertThat(file).isRegularFile();
        }

        @Test
        void controllerPackageDeclaration() {
            assertThat(content)
                    .contains("package com.example.myproject.payment.app.endpoints.controller;");
        }

        @Test
        void controllerImportRestController() {
            assertThat(content)
                    .contains("import org.springframework.web.bind.annotation.RestController;");
        }

        @Test
        void controllerImportRequestMapping() {
            assertThat(content)
                    .contains("import org.springframework.web.bind.annotation.RequestMapping;");
        }

        @Test
        void controllerClassAnnotationRestController() {
            assertThat(content).contains("@RestController");
        }

        @Test
        void controllerClassAnnotationRequestMapping() {
            assertThat(content).contains("@RequestMapping(\"/payment\")");
        }

        @Test
        void controllerClassDeclaration() {
            assertThat(content).contains("public class PaymentController");
        }

        @Test
        void controllerClassBodyIsEmpty() {
            assertThat(content).doesNotContain("void ");
            assertThat(content).doesNotContain("return ");
        }

        @Test
        void controllerHyphenatedModule() throws Exception {
            AddContext hyphenCtx =
                    new AddContext(
                            "payment-gateway", "com.example", "my-project", "1.0.0-SNAPSHOT");
            new ModuleGenerator().generate(hyphenCtx, tempDir.toFile());
            Path f =
                    tempDir.resolve(
                            "payment-gateway/payment-gateway-app/src/main/java/com/example/myproject"
                                    + "/paymentgateway/app/endpoints/controller/PaymentGatewayController.java");
            String c = Files.readString(f);
            assertThat(c).contains("public class PaymentGatewayController");
            assertThat(c).contains("@RequestMapping(\"/payment-gateway\")");
        }

        @Test
        void controllerSingleLetterModule() throws Exception {
            AddContext pCtx = new AddContext("p", "com.example", "my-project", "1.0.0-SNAPSHOT");
            new ModuleGenerator().generate(pCtx, tempDir.toFile());
            Path f =
                    tempDir.resolve(
                            "p/p-app/src/main/java/com/example/myproject"
                                    + "/p/app/endpoints/controller/PController.java");
            String c = Files.readString(f);
            assertThat(c).contains("public class PController");
            assertThat(c).contains("@RequestMapping(\"/p\")");
        }
    }

    // ── 2.10 Hyphenated module completeness ──────────────────────────────────

    @Test
    void hyphenatedModuleAllFilesGenerated() throws Exception {
        AddContext hyphenCtx =
                new AddContext("payment-gateway", "com.example", "my-project", "1.0.0-SNAPSHOT");
        new ModuleGenerator().generate(hyphenCtx, tempDir.toFile());
        Path base = tempDir.resolve("payment-gateway");
        assertThat(base.resolve("pom.xml")).isRegularFile();
        assertThat(base.resolve("payment-gateway-api/pom.xml")).isRegularFile();
        assertThat(base.resolve("payment-gateway-core/pom.xml")).isRegularFile();
        assertThat(base.resolve("payment-gateway-app/pom.xml")).isRegularFile();
        assertThat(
                        base.resolve(
                                "payment-gateway-core/src/main/resources/META-INF/spring.factories"))
                .isRegularFile();
        assertThat(
                        base.resolve(
                                "payment-gateway-core/src/main/resources/payment-gateway-config.yml"))
                .isRegularFile();
        assertThat(
                        base.resolve(
                                "payment-gateway-core/src/main/resources/payment-gateway-config-dev.yml"))
                .isRegularFile();
        assertThat(
                        base.resolve(
                                "payment-gateway-core/src/main/resources/payment-gateway-config-test.yml"))
                .isRegularFile();
        assertThat(
                        base.resolve(
                                "payment-gateway-core/src/main/resources/payment-gateway-config-prod.yml"))
                .isRegularFile();
        assertThat(
                        base.resolve(
                                "payment-gateway-core/src/main/java/com/example/myproject/paymentgateway/config"
                                        + "/PaymentGatewayConfigLoader.java"))
                .isRegularFile();
        assertThat(
                        base.resolve(
                                "payment-gateway-app/src/main/java/com/example/myproject/paymentgateway/app"
                                        + "/endpoints/controller/PaymentGatewayController.java"))
                .isRegularFile();
    }
}
