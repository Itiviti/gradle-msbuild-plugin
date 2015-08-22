package com.ullink

import org.apache.commons.io.FilenameUtils
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.internal.os.OperatingSystem

// http://msdn.microsoft.com/en-us/library/5dy88c2e.aspx
class ProjectFileParser {
    Msbuild msbuild
    Map<String, Object> eval

    Object getProp(String key) {
        return properties[key]
    }

    def getProjectFile() {
        properties.MSBuildProjectFullPath
    }

    def getProjectName() {
        properties.MSBuildProjectName
    }

    def getProperties() {
        eval.Properties
    }

    def getReferences() {
        eval.Reference
    }

    Project getProject() {
        msbuild?.project
    }

    Collection<File> getItems(def section) {
        eval[section].collect {
            findProjectFile(it.Include)
        }
    }

    def renameExtension(def file, def newExtension) {
        FilenameUtils.removeExtension(file) + newExtension
    }

    File getOutputPath() {
        if (msbuild.destinationDir) {
            project.file(msbuild.destinationDir)
        }
        if (properties.TargetDir) {
            new File (properties.TargetDir)
        }
    }

    File getDotnetAssemblyFile() {
        if (outputPath && properties.TargetFileName) {
            new File (outputPath, properties.TargetFileName)
        }
    }

    File getDotnetDebugFile() {
        if (outputPath && properties.TargetName) {
            new File (outputPath, properties.TargetName + (OperatingSystem.current().windows ? '.pdb' : '.mdb'))
        }
    }
    
    File getDocumentationFile() {
        def docFile = getProjectPropertyPath('DocumentationFile')
        if (outputPath && docFile) {
            new File (outputPath, docFile.name)
        } else {
            docFile
        }
    }

     FileCollection getDotnetArtifacts() {
        project.files({
            def ret = [];
            if (dotnetAssemblyFile) ret += dotnetAssemblyFile
            if (dotnetDebugFile?.exists()) ret += dotnetDebugFile;
            if (documentationFile?.exists()) ret += documentationFile;
            ret
        }) {
            builtBy msbuild
        }
    }

    File findProjectFile(String str) {
        findImportFile(project.file(projectFile).parentFile, str)
    }

    File getProjectPropertyPath(String path) {
        if (getProp(path)) {
            findProjectFile(getProp(path))
        }
    }

    static String ospath(String path) {
        path.replaceAll("\\\\|/", "\\" + System.getProperty("file.separator"))
    }

    static File findImportFile(File baseDir, String s) {
        def path = ospath(s)
        def file = new File(path)
        if (!file.isAbsolute()) {
            file = new File(baseDir, path)
        }
        file.canonicalFile
    }
}