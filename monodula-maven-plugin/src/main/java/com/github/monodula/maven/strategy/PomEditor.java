package com.github.monodula.maven.strategy;

import java.io.File;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/** Edits existing pom.xml files: modifies monolith-application pom and aggregator pom. */
public class PomEditor {

    /** Removes {module}-app dependency from monolith pom and adds {module}-core if absent. */
    public void removeAppAddCore(File pomFile, String module, String groupId) throws Exception {
        Document doc = parse(pomFile);

        String appArtifactId = module + "-app";
        String coreArtifactId = module + "-core";

        NodeList deps = doc.getElementsByTagName("dependency");
        boolean hasCoreAlready = false;
        Node depsParent = null;

        // Find and remove {module}-app, check for {module}-core
        for (int i = deps.getLength() - 1; i >= 0; i--) {
            Node dep = deps.item(i);
            String artifactId = getChildText(dep, "artifactId");
            if (appArtifactId.equals(artifactId)) {
                depsParent = dep.getParentNode();
                // Remove whitespace text node before dependency
                Node prev = dep.getPreviousSibling();
                if (prev != null && prev.getNodeType() == Node.TEXT_NODE) {
                    prev.getParentNode().removeChild(prev);
                }
                depsParent.removeChild(dep);
            } else if (coreArtifactId.equals(artifactId)) {
                hasCoreAlready = true;
                if (depsParent == null) {
                    depsParent = dep.getParentNode();
                }
            } else {
                if (depsParent == null) {
                    depsParent = dep.getParentNode();
                }
            }
        }

        // If no deps parent found, find or create dependencies element
        if (depsParent == null) {
            NodeList depsEls = doc.getElementsByTagName("dependencies");
            if (depsEls.getLength() > 0) {
                depsParent = depsEls.item(0);
            } else {
                depsParent = doc.createElement("dependencies");
                doc.getDocumentElement().appendChild(depsParent);
            }
        }

        // Add {module}-core if not already present
        if (!hasCoreAlready) {
            Element dep = doc.createElement("dependency");

            Element gId = doc.createElement("groupId");
            gId.setTextContent(groupId);
            dep.appendChild(gId);

            Element aId = doc.createElement("artifactId");
            aId.setTextContent(coreArtifactId);
            dep.appendChild(aId);

            depsParent.appendChild(dep);
        }

        write(doc, pomFile);
    }

    /**
     * Appends three &lt;dependencyManagement&gt; entries for a new business module ({module}-api,
     * {module}-core, {module}-app) using {@code ${project.version}} as the version. Idempotent:
     * entries already present are skipped.
     */
    public void addDependencyManagementEntries(File pomFile, String groupId, String module)
            throws Exception {
        Document doc = parse(pomFile);

        // Locate or create <dependencyManagement>
        NodeList dmEls = doc.getElementsByTagName("dependencyManagement");
        Node dmEl;
        if (dmEls.getLength() > 0) {
            dmEl = dmEls.item(0);
        } else {
            dmEl = doc.createElement("dependencyManagement");
            doc.getDocumentElement().appendChild(dmEl);
        }

        // Locate or create <dependencies> inside <dependencyManagement>
        NodeList depsEls = ((Element) dmEl).getElementsByTagName("dependencies");
        Node depsEl;
        if (depsEls.getLength() > 0) {
            depsEl = depsEls.item(0);
        } else {
            depsEl = doc.createElement("dependencies");
            dmEl.appendChild(depsEl);
        }

        // Collect already-managed artifactIds to skip duplicates
        java.util.Set<String> existing = new java.util.HashSet<>();
        NodeList depNodes = ((Element) depsEl).getElementsByTagName("dependency");
        for (int i = 0; i < depNodes.getLength(); i++) {
            String aid = getChildText(depNodes.item(i), "artifactId");
            if (aid != null) {
                existing.add(aid);
            }
        }

        for (String suffix : new String[] {"-api", "-core", "-app"}) {
            String artifactId = module + suffix;
            if (existing.contains(artifactId)) {
                continue;
            }
            Element dep = doc.createElement("dependency");
            Element gId = doc.createElement("groupId");
            gId.setTextContent(groupId);
            dep.appendChild(gId);
            Element aId = doc.createElement("artifactId");
            aId.setTextContent(artifactId);
            dep.appendChild(aId);
            Element ver = doc.createElement("version");
            ver.setTextContent("${project.version}");
            dep.appendChild(ver);
            depsEl.appendChild(dep);
        }

        write(doc, pomFile);
    }

    /** Adds {moduleName}-application to the aggregator pom's modules section (idempotent). */
    public void addModuleToAggregator(File pomFile, String moduleName) throws Exception {
        addModuleEntry(pomFile, moduleName + "-application");
    }

    /** Appends &lt;module&gt;{moduleEntry}&lt;/module&gt; to &lt;modules&gt; block (idempotent). */
    public void addModuleEntry(File pomFile, String moduleEntry) throws Exception {
        Document doc = parse(pomFile);

        NodeList modulesEls = doc.getElementsByTagName("modules");
        Node modulesEl;
        if (modulesEls.getLength() > 0) {
            modulesEl = modulesEls.item(0);
        } else {
            modulesEl = doc.createElement("modules");
            doc.getDocumentElement().appendChild(modulesEl);
        }

        NodeList moduleEls = ((Element) modulesEl).getElementsByTagName("module");
        for (int i = 0; i < moduleEls.getLength(); i++) {
            if (moduleEntry.equals(moduleEls.item(i).getTextContent().trim())) {
                return;
            }
        }

        Element newModule = doc.createElement("module");
        newModule.setTextContent(moduleEntry);
        modulesEl.appendChild(newModule);
        write(doc, pomFile);
    }

    /**
     * Appends a dependency (groupId, artifactId) to &lt;dependencies&gt; (idempotent). Version is
     * managed by the root pom's dependencyManagement.
     */
    public void addDependency(File pomFile, String groupId, String artifactId) throws Exception {
        Document doc = parse(pomFile);

        NodeList depsEls = doc.getElementsByTagName("dependencies");
        Node depsEl;
        if (depsEls.getLength() > 0) {
            depsEl = depsEls.item(0);
        } else {
            depsEl = doc.createElement("dependencies");
            doc.getDocumentElement().appendChild(depsEl);
        }

        NodeList depNodes = ((Element) depsEl).getElementsByTagName("dependency");
        for (int i = 0; i < depNodes.getLength(); i++) {
            if (artifactId.equals(getChildText(depNodes.item(i), "artifactId"))) {
                return;
            }
        }

        Element dep = doc.createElement("dependency");
        Element gId = doc.createElement("groupId");
        gId.setTextContent(groupId);
        dep.appendChild(gId);
        Element aId = doc.createElement("artifactId");
        aId.setTextContent(artifactId);
        dep.appendChild(aId);
        depsEl.appendChild(dep);
        write(doc, pomFile);
    }

    /**
     * Removes &lt;module&gt;{moduleEntry}&lt;/module&gt; from &lt;modules&gt; block (idempotent).
     */
    public void removeModuleEntry(File pomFile, String moduleEntry) throws Exception {
        Document doc = parse(pomFile);

        NodeList modulesEls = doc.getElementsByTagName("modules");
        if (modulesEls.getLength() == 0) {
            return;
        }
        Node modulesEl = modulesEls.item(0);
        NodeList moduleEls = ((Element) modulesEl).getElementsByTagName("module");
        Node toRemove = null;
        for (int i = 0; i < moduleEls.getLength(); i++) {
            if (moduleEntry.equals(moduleEls.item(i).getTextContent().trim())) {
                toRemove = moduleEls.item(i);
                break;
            }
        }
        if (toRemove == null) {
            return;
        }
        modulesEl.removeChild(toRemove);
        write(doc, pomFile);
    }

    /**
     * Removes the &lt;dependency&gt; whose direct child &lt;artifactId&gt; matches the given value
     * (idempotent). Does NOT use getElementsByTagName("artifactId") to avoid matching parent/plugin
     * blocks.
     */
    public void removeDependency(File pomFile, String artifactId) throws Exception {
        Document doc = parse(pomFile);

        NodeList depsEls = doc.getElementsByTagName("dependencies");
        if (depsEls.getLength() == 0) {
            return;
        }
        Node depsEl = depsEls.item(0);
        NodeList depNodes = ((Element) depsEl).getElementsByTagName("dependency");
        Node toRemove = null;
        for (int i = 0; i < depNodes.getLength(); i++) {
            if (artifactId.equals(getChildText(depNodes.item(i), "artifactId"))) {
                toRemove = depNodes.item(i);
                break;
            }
        }
        if (toRemove == null) {
            return;
        }
        depsEl.removeChild(toRemove);
        write(doc, pomFile);
    }

    /**
     * Removes &lt;dependencyManagement&gt; entries for {module}-api, {module}-core, {module}-app
     * (idempotent). No-op if the block or individual entries are absent.
     */
    public void removeDependencyManagementEntries(File pomFile, String module) throws Exception {
        Document doc = parse(pomFile);

        NodeList dmEls = doc.getElementsByTagName("dependencyManagement");
        if (dmEls.getLength() == 0) {
            return;
        }
        Node dmEl = dmEls.item(0);
        NodeList depsEls = ((Element) dmEl).getElementsByTagName("dependencies");
        if (depsEls.getLength() == 0) {
            return;
        }
        Node depsEl = depsEls.item(0);

        java.util.Set<String> targets =
                new java.util.HashSet<>(
                        java.util.Arrays.asList(
                                module + "-api", module + "-core", module + "-app"));

        NodeList depNodes = ((Element) depsEl).getElementsByTagName("dependency");
        java.util.List<Node> toRemove = new java.util.ArrayList<>();
        for (int i = 0; i < depNodes.getLength(); i++) {
            String aid = getChildText(depNodes.item(i), "artifactId");
            if (targets.contains(aid)) {
                toRemove.add(depNodes.item(i));
            }
        }
        if (toRemove.isEmpty()) {
            return;
        }
        for (Node n : toRemove) {
            depsEl.removeChild(n);
        }
        write(doc, pomFile);
    }

    private Document parse(File pomFile) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        Document doc = factory.newDocumentBuilder().parse(pomFile);
        doc.getDocumentElement().normalize();
        return doc;
    }

    private String getChildText(Node parent, String tagName) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE && tagName.equals(child.getNodeName())) {
                return child.getTextContent().trim();
            }
        }
        return null;
    }

    private void write(Document doc, File pomFile) throws Exception {
        removeWhitespaceTextNodes(doc.getDocumentElement());
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        transformer.transform(new DOMSource(doc), new StreamResult(pomFile));
    }

    /** Removes whitespace-only text nodes to prevent double-indentation when re-serializing. */
    private void removeWhitespaceTextNodes(Node node) {
        Node child = node.getFirstChild();
        while (child != null) {
            Node next = child.getNextSibling();
            if (child.getNodeType() == Node.TEXT_NODE && child.getTextContent().trim().isEmpty()) {
                node.removeChild(child);
            } else if (child.hasChildNodes()) {
                removeWhitespaceTextNodes(child);
            }
            child = next;
        }
    }
}
