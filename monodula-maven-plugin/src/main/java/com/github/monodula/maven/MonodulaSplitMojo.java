package com.github.monodula.maven;

import com.github.monodula.maven.analyzer.MavenDependencyAnalyzer;
import com.github.monodula.maven.model.MavenModule;
import com.github.monodula.maven.strategy.FileGenerator;
import com.github.monodula.maven.strategy.InProcessSplitStrategy;
import com.github.monodula.maven.strategy.PomEditor;
import com.github.monodula.maven.strategy.SplitContext;
import com.github.monodula.maven.strategy.SplitResult;
import java.io.File;
import java.util.List;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "split", aggregator = true)
public class MonodulaSplitMojo extends AbstractMojo {

    @Parameter(property = "module", required = true)
    String module;

    @Parameter(defaultValue = "${project.basedir}")
    File baseDirectory;

    @Parameter(defaultValue = "${project.artifactId}")
    String rootArtifactId;

    @Parameter(defaultValue = "${project.groupId}")
    String projectGroupId;

    @Parameter(defaultValue = "${project.version}")
    String projectVersion;

    @Parameter(
            property = "monodula.basePackage",
            defaultValue = "${project.groupId}.${project.artifactId}")
    String basePackage;

    @Override
    public void execute() throws MojoExecutionException {
        try {
            getLog().info("monodula:split - Splitting module: " + module);

            // Sanitize basePackage: remove hyphens from each segment
            basePackage = MonodulaAddMojo.sanitizePackage(basePackage);

            // Idempotency check: fail if output directory already exists
            // Compute path directly without needing strategy result
            File outputDir = new File(baseDirectory, "application/" + module + "-application");
            if (outputDir.exists()) {
                throw new IllegalStateException(
                        "Output directory already exists: "
                                + outputDir.getAbsolutePath()
                                + ". Remove it manually if you want to regenerate.");
            }

            // Scan modules
            File rootPom = new File(baseDirectory, "pom.xml");
            MavenDependencyAnalyzer analyzer = new MavenDependencyAnalyzer();
            List<MavenModule> modules = analyzer.scanModules(rootPom);

            // Build context with full project coordinates
            SplitContext ctx =
                    new SplitContext(
                            module,
                            modules,
                            rootArtifactId,
                            projectGroupId,
                            projectVersion,
                            basePackage);

            // Run strategy
            InProcessSplitStrategy strategy = new InProcessSplitStrategy();
            SplitResult result = strategy.split(ctx);

            getLog().info("Dependencies included:");
            for (String dep : result.getDependencies()) {
                getLog().info("  - " + dep);
            }

            // Generate files
            FileGenerator generator = new FileGenerator();
            List<String> deps = result.getDependencies();

            // Generate pom.xml
            generator.generatePom(ctx, deps, new File(outputDir, "pom.xml"));
            getLog().info("Generated pom.xml");

            // Generate main class
            String packageSegment = InProcessSplitStrategy.toPackageSegment(module);
            String className = InProcessSplitStrategy.toClassName(module);
            String packagePath = basePackage.replace('.', '/');
            File mainClassFile =
                    new File(
                            outputDir,
                            "src/main/java/"
                                    + packagePath
                                    + "/app/"
                                    + packageSegment
                                    + "/"
                                    + className
                                    + "StandaloneApplication.java");
            generator.generateMainClass(ctx, mainClassFile);
            getLog().info("Generated main class: " + mainClassFile.getPath());

            // Generate application.yml
            generator.generateApplicationYml(
                    ctx, deps, new File(outputDir, "src/main/resources/application.yml"));
            getLog().info("Generated application.yml");

            // Generate Dockerfile
            generator.generateDockerfile(ctx, new File(outputDir, "Dockerfile"));
            getLog().info("Generated Dockerfile");

            // Create test source directories
            new File(outputDir, "src/test/java").mkdirs();
            new File(outputDir, "src/test/resources").mkdirs();
            getLog().info("Created test source directories");

            // Generate ArchitectureTest
            String archTestPackagePath = basePackage.replace('.', '/');
            File archTestFile =
                    new File(
                            outputDir,
                            "src/test/java/" + archTestPackagePath + "/arch/ArchitectureTest.java");
            generator.generateArchitectureTest(ctx, archTestFile);
            getLog().info("Generated ArchitectureTest.java");

            // Edit monolith pom: remove {module}-app, add {module}-core
            File monolithPom = new File(baseDirectory, "application/monolith-application/pom.xml");
            if (monolithPom.exists()) {
                PomEditor editor = new PomEditor();
                editor.removeAppAddCore(monolithPom, module, projectGroupId);
                getLog().info(
                                "Updated monolith-application/pom.xml: removed "
                                        + module
                                        + "-app, added "
                                        + module
                                        + "-core");
            }

            // Edit aggregator pom: add {module}-application module
            File aggregatorPom = new File(baseDirectory, "application/pom.xml");
            if (aggregatorPom.exists()) {
                PomEditor editor = new PomEditor();
                editor.addModuleToAggregator(aggregatorPom, module);
                getLog().info("Updated aggregator pom: added " + module + "-application module");
            }

            getLog().info("Split complete. Output at: " + outputDir.getAbsolutePath());

        } catch (IllegalStateException e) {
            throw e; // Let idempotency errors bubble up directly
        } catch (Exception e) {
            throw new MojoExecutionException("Split failed", e);
        }
    }
}
