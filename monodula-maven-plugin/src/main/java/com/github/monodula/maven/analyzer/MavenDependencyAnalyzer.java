package com.github.monodula.maven.analyzer;

import com.github.monodula.maven.model.MavenModule;
import com.github.monodula.maven.model.ModuleType;
import com.github.monodula.maven.model.ViolationReport;
import com.github.monodula.maven.rule.DependencyRule;
import com.github.monodula.maven.rule.RuleEngine;
import com.github.monodula.maven.rule.rules.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class MavenDependencyAnalyzer {

    private final RuleEngine ruleEngine;

    public MavenDependencyAnalyzer() {
        List<DependencyRule> rules =
                List.of(
                        new CoreCannotDependOnOtherCoreRule(),
                        new CoreCannotDependOnOtherAppRule(),
                        new ApiCannotDependOnCoreRule(),
                        new ApiCannotDependOnAppRule(),
                        new CommonCannotDependOnBusinessRule(),
                        new AppCannotDependOnOtherAppRule(),
                        new ModuleCannotDependOnItselfRule());
        this.ruleEngine = new RuleEngine(rules);
    }

    public List<MavenModule> scanModules(File rootPom) {
        List<MavenModule> modules = new ArrayList<>();
        if (rootPom == null || !rootPom.exists()) {
            return modules;
        }
        File parentDir = rootPom.getParentFile();
        File[] subDirs = parentDir.listFiles(File::isDirectory);
        if (subDirs == null) {
            return modules;
        }
        for (File dir : subDirs) {
            scanModuleTree(dir, modules);
        }
        return modules;
    }

    private void scanModuleTree(File dir, List<MavenModule> modules) {
        File pomFile = new File(dir, "pom.xml");
        if (pomFile.exists()) {
            String artifactId = readElementFromPom(pomFile, "artifactId");
            if (artifactId != null) {
                String groupId = readElementFromPom(pomFile, "groupId");
                String version = readElementFromPom(pomFile, "version");
                List<String> deps = readDependencyArtifactIds(pomFile);

                ModuleType type = ModuleClassifier.classify(artifactId);
                if (type != ModuleType.UNKNOWN) {
                    modules.add(new MavenModule(artifactId, type, groupId, version, dir, deps));
                }
            }
        }
        // Always recurse into subdirectories, even if this dir has no pom.xml
        File[] subDirs = dir.listFiles(File::isDirectory);
        if (subDirs != null) {
            for (File subDir : subDirs) {
                scanModuleTree(subDir, modules);
            }
        }
    }

    /**
     * Reads all dependency artifactIds from the pom's {@code <dependencies>} block.
     *
     * <p>Iterates {@code <dependency>} child nodes and extracts their direct {@code <artifactId>}
     * child — never uses {@code getElementsByTagName("artifactId")} which would also match {@code
     * <parent>} and {@code <plugin>} blocks.
     */
    static List<String> readDependencyArtifactIds(File pomFile) {
        List<String> result = new ArrayList<>();
        try {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(pomFile);
            doc.getDocumentElement().normalize();

            // Find <dependencies> as a DIRECT child of <project>, not inside <build><plugins>
            Node depsBlock = null;
            NodeList projectChildren = doc.getDocumentElement().getChildNodes();
            for (int i = 0; i < projectChildren.getLength(); i++) {
                Node child = projectChildren.item(i);
                if (child.getNodeType() == Node.ELEMENT_NODE
                        && "dependencies".equals(child.getNodeName())) {
                    depsBlock = child;
                    break;
                }
            }

            if (depsBlock == null) {
                return result;
            }
            NodeList children = depsBlock.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if (child.getNodeType() == Node.ELEMENT_NODE
                        && "dependency".equals(child.getNodeName())) {
                    // Iterate direct children of <dependency> to find <artifactId>
                    NodeList depChildren = child.getChildNodes();
                    for (int j = 0; j < depChildren.getLength(); j++) {
                        Node depChild = depChildren.item(j);
                        if (depChild.getNodeType() == Node.ELEMENT_NODE
                                && "artifactId".equals(depChild.getNodeName())) {
                            String text = depChild.getTextContent().trim();
                            if (!text.isEmpty()) {
                                result.add(text);
                            }
                            break;
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return result;
    }

    /** Reads the first direct child element with the given tag from a pom.xml. */
    static String readElementFromPom(File pomFile, String tagName) {
        try {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(pomFile);
            doc.getDocumentElement().normalize();
            org.w3c.dom.NodeList children = doc.getDocumentElement().getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                org.w3c.dom.Node child = children.item(i);
                if (child.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE
                        && tagName.equals(child.getNodeName())) {
                    return child.getTextContent().trim();
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    public ViolationReport analyze(MavenModule source, MavenModule target, String location) {
        return ruleEngine.check(source, target, location);
    }
}
