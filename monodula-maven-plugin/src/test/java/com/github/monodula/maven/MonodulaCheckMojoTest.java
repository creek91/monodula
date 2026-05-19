package com.github.monodula.maven;

import static org.assertj.core.api.Assertions.assertThatCode;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MonodulaCheckMojoTest {

    @TempDir Path tempDir;

    private MonodulaCheckMojo mojo() {
        return new MonodulaCheckMojo();
    }

    private void setProject(MonodulaCheckMojo mojo, MavenProject project) throws Exception {
        Field f = MonodulaCheckMojo.class.getDeclaredField("project");
        f.setAccessible(true);
        f.set(mojo, project);
    }

    private MavenProject rootProject() throws Exception {
        Path rootPom = tempDir.resolve("pom.xml");
        Files.writeString(
                rootPom,
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<project>\n"
                        + "  <groupId>com.example</groupId>\n"
                        + "  <artifactId>my-project</artifactId>\n"
                        + "  <version>1.0.0-SNAPSHOT</version>\n"
                        + "  <packaging>pom</packaging>\n"
                        + "</project>");

        MavenProject root = new MavenProject();
        root.setGroupId("com.example");
        root.setArtifactId("my-project");
        root.setVersion("1.0.0-SNAPSHOT");
        root.setPackaging("pom");
        root.setFile(rootPom.toFile());
        return root;
    }

    /**
     * Creates a sub-directory with a pom.xml so scanModules can discover at least one module. The
     * artifactId in the pom must end with -core, -api, or -app for ModuleClassifier to pick it up.
     */
    private void writeSubModuleDir(String artifactId) throws Exception {
        Path moduleDir = tempDir.resolve(artifactId);
        Files.createDirectories(moduleDir);
        Files.writeString(
                moduleDir.resolve("pom.xml"),
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<project>\n"
                        + "  <groupId>com.example</groupId>\n"
                        + "  <artifactId>"
                        + artifactId
                        + "</artifactId>\n"
                        + "  <version>1.0.0-SNAPSHOT</version>\n"
                        + "  <dependencies/>\n"
                        + "</project>");
    }

    private MavenProject reactorModule(String artifactId) {
        MavenProject project = new MavenProject();
        project.setGroupId("com.example");
        project.setArtifactId(artifactId);
        project.setVersion("1.0.0-SNAPSHOT");
        return project;
    }

    /**
     * When a reactor module depends on third-party jars whose artifactIds end with -core or -api
     * (e.g. aliyun-java-sdk-core, slf4j-api), those should NOT be flagged as violations.
     */
    @Test
    void thirdPartyJarsWithLayerSuffixAreIgnored() throws Exception {
        MavenProject root = rootProject();
        writeSubModuleDir("payment-core");

        MavenProject paymentCore = reactorModule("payment-core");

        Dependency thirdPartyCore = new Dependency();
        thirdPartyCore.setGroupId("com.aliyun");
        thirdPartyCore.setArtifactId("aliyun-java-sdk-core");
        thirdPartyCore.setVersion("4.6.0");
        paymentCore.getDependencies().add(thirdPartyCore);

        Dependency thirdPartyApi = new Dependency();
        thirdPartyApi.setGroupId("org.slf4j");
        thirdPartyApi.setArtifactId("slf4j-api");
        thirdPartyApi.setVersion("2.0.9");
        paymentCore.getDependencies().add(thirdPartyApi);

        Dependency thirdPartyServlet = new Dependency();
        thirdPartyServlet.setGroupId("jakarta.servlet");
        thirdPartyServlet.setArtifactId("jakarta.servlet-api");
        thirdPartyServlet.setVersion("6.0.0");
        paymentCore.getDependencies().add(thirdPartyServlet);

        Dependency thirdPartyJob = new Dependency();
        thirdPartyJob.setGroupId("com.xxl-job");
        thirdPartyJob.setArtifactId("xxl-job-core");
        thirdPartyJob.setVersion("2.4.0");
        paymentCore.getDependencies().add(thirdPartyJob);

        List<MavenProject> collected = new ArrayList<>();
        collected.add(paymentCore);
        root.setCollectedProjects(collected);

        MonodulaCheckMojo mojo = mojo();
        setProject(mojo, root);

        assertThatCode(mojo::execute).doesNotThrowAnyException();
    }

    /**
     * When a reactor module depends on another reactor module that violates a rule, it SHOULD still
     * be flagged.
     */
    @Test
    void reactorModuleViolationIsDetected() throws Exception {
        MavenProject root = rootProject();
        writeSubModuleDir("payment-core");
        writeSubModuleDir("order-core");

        MavenProject paymentCore = reactorModule("payment-core");
        MavenProject orderCore = reactorModule("order-core");

        Dependency orderCoreDep = new Dependency();
        orderCoreDep.setGroupId("com.example");
        orderCoreDep.setArtifactId("order-core");
        orderCoreDep.setVersion("1.0.0-SNAPSHOT");
        paymentCore.getDependencies().add(orderCoreDep);

        List<MavenProject> collected = new ArrayList<>();
        collected.add(paymentCore);
        collected.add(orderCore);
        root.setCollectedProjects(collected);

        MonodulaCheckMojo mojo = mojo();
        setProject(mojo, root);

        assertThatCode(mojo::execute).isInstanceOf(MojoFailureException.class);
    }
}
