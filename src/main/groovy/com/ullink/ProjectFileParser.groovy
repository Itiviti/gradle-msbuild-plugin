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

    File getDotnetAssemblyFile() {
        if (properties.TargetPath) {
            project.file(properties.TargetPath)
        }
    }

    List getDotnetDebugFile() {
        if (properties.DebugSymbols) {
            if (eval._DebugSymbolsOutputPath) {
                eval._DebugSymbolsOutputPath.collect { new File (it.FullPath) }
            } else {
                [ new File (properties.TargetPath, properties.TargetName + (OperatingSystem.current().windows ? '.pdb' : '.mdb')) ]
            }
        }
    }

    List getDocumentationFile() {
        if (eval.FinalDocFile) {
            eval.FinalDocFile.collect { new File (it.FullPath) }
        } else if (properties.DocumentationFile) {
            // Mono
            [ new File (properties.ProjectDir, properties.DocumentationFile) ]
        }
    }
    
    File getConfigFile() {
        if (properties.TargetDir && properties.TargetFileName) {
            new File (properties.TargetDir, properties.TargetFileName + ".config")
        }
    }

    FileCollection getDotnetArtifacts() {
        project.files({
            def ret = [];
            // Missing localization resources
            if (dotnetAssemblyFile) ret << dotnetAssemblyFile
            if (dotnetDebugFile) ret << dotnetDebugFile
            if (documentationFile) ret << documentationFile
            if (configFile?.exists()) ret << configFile
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