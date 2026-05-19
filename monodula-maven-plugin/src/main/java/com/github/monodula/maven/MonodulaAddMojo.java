package com.github.monodula.maven;

import com.github.monodula.maven.strategy.AddContext;
import com.github.monodula.maven.strategy.ModuleGenerator;
import com.github.monodula.maven.strategy.PomEditor;
import java.io.File;
import java.util.regex.Pattern;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "add", aggregator = true)
public class MonodulaAddMojo extends AbstractMojo {

    private static final Pattern MODULE_NAME_PATTERN = Pattern.compile("[a-z][a-z0-9-]*");

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
        // 1. Parameter validation
        if (module == null || module.isEmpty()) {
            throw new MojoExecutionException(
                    "Parameter 'module' is required. Format: [a-z][a-z0-9-]*");
        }
        if (!MODULE_NAME_PATTERN.matcher(module).matches()) {
            throw new MojoExecutionException(
                    "Invalid module name '"
                            + module
                            + "'. Must match [a-z][a-z0-9-]*"
                            + " (lowercase letters, digits, and hyphens; must start with [a-z]).");
        }

        // 2. Root pom existence check
        File rootPom = new File(baseDirectory, "pom.xml");
        if (!rootPom.exists()) {
            throw new MojoExecutionException(
                    "Root pom.xml not found at: " + rootPom.getAbsolutePath());
        }

        // Sanitize basePackage: remove hyphens from each segment (safe for explicitly set values
        // too)
        basePackage = sanitizePackage(basePackage);

        // 3. Idempotency check
        File moduleDir = new File(baseDirectory, module);
        if (moduleDir.exists()) {
            throw new MojoExecutionException(
                    "Module directory already exists: "
                            + moduleDir.getAbsolutePath()
                            + ". Remove it manually if you want to regenerate.");
        }

        try {
            // 4. Build context
            AddContext ctx =
                    new AddContext(
                            module, projectGroupId, rootArtifactId, projectVersion, basePackage);

            // 5. Generate module files
            new ModuleGenerator().generate(ctx, baseDirectory);
            getLog().info("monodula:add - Generated module: " + module);

            // 6. Update root pom: register in <modules>
            new PomEditor().addModuleEntry(rootPom, module);
            getLog().info("Updated root pom.xml: added <module>" + module + "</module>");

            // 6b. Update root pom: register in <dependencyManagement>
            new PomEditor().addDependencyManagementEntries(rootPom, projectGroupId, module);
            getLog().info(
                            "Updated root pom.xml: added "
                                    + module
                                    + "-{api,core,app} to dependencyManagement");

            // 7. Update monolith pom (optional - skip with warning if missing)
            File monolithPom = new File(baseDirectory, "application/monolith-application/pom.xml");
            if (monolithPom.exists()) {
                new PomEditor().addDependency(monolithPom, projectGroupId, module + "-app");
                getLog().info("Updated monolith-application/pom.xml: added " + module + "-app");
            } else {
                getLog().warn(
                                "monolith-application/pom.xml not found, skipping dependency update.");
            }

            getLog().info(
                            "monodula:add complete. Module '"
                                    + module
                                    + "' created at: "
                                    + moduleDir.getAbsolutePath());

        } catch (MojoExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to add module '" + module + "'", e);
        }
    }

    static String sanitizePackage(String pkg) {
        if (pkg == null) {
            return pkg;
        }
        String[] parts = pkg.split("\\.", -1);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                sb.append('.');
            }
            sb.append(parts[i].replace("-", ""));
        }
        return sb.toString();
    }
}
