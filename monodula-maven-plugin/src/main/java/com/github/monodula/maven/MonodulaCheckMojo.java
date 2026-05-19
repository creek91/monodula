package com.github.monodula.maven;

import com.github.monodula.maven.analyzer.MavenDependencyAnalyzer;
import com.github.monodula.maven.analyzer.ModuleClassifier;
import com.github.monodula.maven.model.MavenModule;
import com.github.monodula.maven.model.ModuleType;
import com.github.monodula.maven.model.Violation;
import com.github.monodula.maven.model.ViolationReport;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

@Mojo(name = "check")
public class MonodulaCheckMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    @Parameter(property = "monodula.check.skip", defaultValue = "false")
    private boolean skip;

    @Override
    public void execute() throws MojoFailureException {
        if (skip) {
            getLog().info("monodula:check skipped.");
            return;
        }
        getLog().info("monodula:check - Scanning modules...");

        MavenDependencyAnalyzer analyzer = new MavenDependencyAnalyzer();
        List<MavenModule> modules = analyzer.scanModules(project.getFile());

        if (modules.isEmpty()) {
            getLog().info("No modules found.");
            return;
        }

        getLog().info("Found " + modules.size() + " modules.");

        ViolationReport report = new ViolationReport();

        Set<String> reactorArtifactIds =
                project.getCollectedProjects().stream()
                        .map(MavenProject::getArtifactId)
                        .collect(Collectors.toSet());

        // Check actual dependencies from each reactor module
        for (MavenProject reactorProject : project.getCollectedProjects()) {
            String sourceArtifactId = reactorProject.getArtifactId();
            ModuleType sourceType = ModuleClassifier.classify(sourceArtifactId);
            if (sourceType == ModuleType.UNKNOWN) {
                continue;
            }
            MavenModule source = new MavenModule(sourceArtifactId, sourceType);

            for (var dep : reactorProject.getDependencies()) {
                String targetArtifactId = dep.getArtifactId();
                if (!reactorArtifactIds.contains(targetArtifactId)) {
                    continue;
                }
                ModuleType targetType = ModuleClassifier.classify(targetArtifactId);
                if (targetType == ModuleType.UNKNOWN) {
                    continue;
                }
                MavenModule target = new MavenModule(targetArtifactId, targetType);

                ViolationReport pairReport =
                        analyzer.analyze(source, target, reactorProject.getName());
                for (Violation v : pairReport.getViolations()) {
                    report.add(v);
                }
            }
        }

        if (report.isClean()) {
            getLog().info("All modules passed.");
        } else {
            for (Violation v : report.getViolations()) {
                getLog().error(
                                String.format(
                                        "[%s] %s: %s → %s",
                                        v.getSeverity(),
                                        v.getRuleId(),
                                        v.getSourceModule(),
                                        v.getTargetModule()));
            }
            if (report.getErrorCount() > 0) {
                throw new MojoFailureException(
                        "Monodula boundary violations found: " + report.getErrorCount());
            }
        }
    }
}
