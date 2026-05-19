package com.github.monodula.maven;

import com.github.monodula.maven.analyzer.MavenDependencyAnalyzer;
import com.github.monodula.maven.model.MavenModule;
import com.github.monodula.maven.strategy.PomEditor;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "remove", aggregator = true)
public class MonodulaRemoveMojo extends AbstractMojo {

    @Parameter(property = "module", required = true)
    String module;

    @Parameter(property = "purge", defaultValue = "false")
    boolean purge;

    @Parameter(defaultValue = "${project.basedir}", readonly = true)
    File baseDirectory;

    @Parameter(defaultValue = "${project.groupId}", readonly = true)
    String projectGroupId;

    @Override
    public void execute() throws MojoFailureException {
        validateModuleName();

        File rootPom = new File(baseDirectory, "pom.xml");
        File moduleDir = new File(baseDirectory, module);

        if (!moduleDir.exists() || !moduleDir.isDirectory()) {
            throw new MojoFailureException(
                    "Module directory not found: "
                            + moduleDir.getAbsolutePath()
                            + ". Cannot remove module '"
                            + module
                            + "'.");
        }

        List<MavenModule> allModules = new MavenDependencyAnalyzer().scanModules(rootPom);
        checkNoDependents(allModules);

        try {
            new PomEditor().removeModuleEntry(rootPom, module);
        } catch (Exception e) {
            throw new MojoFailureException(
                    "Failed to update root pom.xml for module '" + module + "'", e);
        }

        try {
            new PomEditor().removeDependencyManagementEntries(rootPom, module);
            getLog().info(
                            "Updated root pom.xml: removed "
                                    + module
                                    + "-{api,core,app} from dependencyManagement");
        } catch (Exception e) {
            throw new MojoFailureException(
                    "Failed to remove dependencyManagement entries for module '" + module + "'", e);
        }

        File monolithPom = new File(baseDirectory, "application/monolith-application/pom.xml");
        if (monolithPom.exists()) {
            try {
                new PomEditor().removeDependency(monolithPom, module + "-app");
            } catch (Exception e) {
                throw new MojoFailureException(
                        "Failed to update monolith pom for module '" + module + "'", e);
            }
        }

        if (purge) {
            deleteRecursively(moduleDir);
        }
    }

    private void validateModuleName() throws MojoFailureException {
        if (module == null || module.isEmpty()) {
            throw new MojoFailureException("Parameter 'module' is required and must not be empty.");
        }
        if (!module.matches("[a-z][a-z0-9-]*")) {
            throw new MojoFailureException(
                    "Parameter 'module' must match [a-z][a-z0-9-]*. Got: '" + module + "'.");
        }
    }

    private void checkNoDependents(List<MavenModule> allModules) throws MojoFailureException {
        Set<String> targetArtifacts = Set.of(module + "-api", module + "-core", module + "-app");

        List<String> dependentLines = new ArrayList<>();
        for (MavenModule m : allModules) {
            // Skip modules that are part of the target module being removed
            if (targetArtifacts.contains(m.getArtifactId())) {
                continue;
            }
            for (String dep : m.getDependencies()) {
                if (targetArtifacts.contains(dep)) {
                    String relativePath =
                            baseDirectory.toPath().relativize(m.getBasedir().toPath()).toString()
                                    + "/pom.xml";
                    dependentLines.add("  - " + m.getArtifactId() + " (" + relativePath + ")");
                    break;
                }
            }
        }

        if (!dependentLines.isEmpty()) {
            String msg =
                    "Cannot remove module '"
                            + module
                            + "': it is depended on by:\n"
                            + String.join("\n", dependentLines)
                            + "\nPlease remove these dependencies first.";
            throw new MojoFailureException(msg);
        }
    }

    private void deleteRecursively(File file) throws MojoFailureException {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        try {
            Files.delete(file.toPath());
        } catch (IOException e) {
            throw new MojoFailureException("Failed to delete: " + file.getAbsolutePath(), e);
        }
    }
}
