import java.nio.file.*

def modules = request.properties.get("modules")
def groupId = request.properties.get("groupId")
def artifactId = request.properties.get("artifactId")
def version = request.properties.get("version")
def pkg = request.properties.get("package")

def moduleList = modules.split(",")*.trim()
def projectDir = Paths.get(request.outputDirectory, request.artifactId)

def packagePath = pkg.replace('.', '/')

def toPascalCase = { str ->
    str.split('-').collect { seg -> seg.substring(0, 1).toUpperCase() + seg.substring(1) }.join('')
}

// Strip hyphens so module name is a valid Java package segment
def toPackageName = { str -> str.replace('-', '') }

// Business modules: copy from module-template into {moduleName}/
moduleList.each { moduleName ->
    def moduleDir = projectDir.resolve("${moduleName}")
    def templateDir = projectDir.resolve("module-template")

    // Copy template directory structure
    Files.walk(templateDir).each { source ->
        def relative = templateDir.relativize(source)
        def targetPath = relative.toString()
            .replace('__moduleNameAsPackage__', toPackageName(moduleName))
            .replace('__moduleName__', moduleName)
            .replace('__packageInPathFormat__', packagePath)
            .replace('__ModuleName__', toPascalCase(moduleName))
        def target = moduleDir.resolve(targetPath)

        if (Files.isDirectory(source)) {
            Files.createDirectories(target)
        } else {
            Files.createDirectories(target.parent)
            def content = new String(Files.readAllBytes(source), "UTF-8")
            content = content.replace('__ModuleName__', toPascalCase(moduleName))
            content = content.replace('__moduleNameAsPackage__', toPackageName(moduleName))
            content = content.replace('__moduleName__', moduleName)
            // No need to replace __rootArtifactId__ in parent artifactId anymore
            // since first-level modules no longer include the rootArtifactId prefix
            Files.write(target, content.getBytes("UTF-8"))
        }
    }

    // Rename files containing __ModuleName__ in their filename
    Files.walk(moduleDir).each { path ->
        def fileName = path.getFileName().toString()
        if (fileName.contains('__ModuleName__')) {
            def newFileName = fileName.replace('__ModuleName__', toPascalCase(moduleName))
            def newPath = path.getParent().resolve(newFileName)
            Files.move(path, newPath, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    // Generate domain aggregator pom.xml
    def aggregatorPom = moduleDir.resolve("pom.xml")
    def pomContent = """<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>${groupId}</groupId>
        <artifactId>${artifactId}</artifactId>
        <version>${version}</version>
    </parent>

    <artifactId>${moduleName}</artifactId>
    <packaging>pom</packaging>

    <modules>
        <module>${moduleName}-api</module>
        <module>${moduleName}-core</module>
        <module>${moduleName}-app</module>
    </modules>
</project>
"""
    Files.write(aggregatorPom, pomContent.getBytes("UTF-8"))
}

// Remove template directory
def templateDir = projectDir.resolve("module-template")
if (Files.exists(templateDir)) {
    Files.walk(templateDir)
        .sorted(Comparator.reverseOrder())
        .each { Files.deleteIfExists(it) }
}

// Common and application directories keep their original names (no prefix renaming)

// Create standard Maven directory structure for all sub-modules
def mavenDirs = ["src/main/java", "src/main/resources", "src/test/java", "src/test/resources"]
def packagedDirs = ["src/main/java", "src/test/java"]

// Common sub-modules: {package}/common/{api|core|app}
def commonBase = projectDir.resolve("common")
["common-api": "common/api", "common-core": "common/core", "common-infras": "common/infras", "common-app": "common/app"].each { sub, subPkg ->
    def subDir = commonBase.resolve(sub)
    mavenDirs.each { dir ->
        def path = subDir.resolve(dir)
        if (dir in packagedDirs) {
            path = path.resolve(packagePath).resolve(subPkg)
        }
        Files.createDirectories(path)
    }
}

// Application sub-module: {package}/app/monolith
def appBase = projectDir.resolve("application")
def monolithDir = appBase.resolve("monolith-application")
mavenDirs.each { dir ->
    def path = monolithDir.resolve(dir)
    if (dir in packagedDirs) {
        path = path.resolve(packagePath).resolve("app/monolith")
    }
    Files.createDirectories(path)
}

// Make mvnw executable
def mvnw = projectDir.resolve("mvnw")
if (Files.exists(mvnw)) {
    mvnw.toFile().setExecutable(true)
}
