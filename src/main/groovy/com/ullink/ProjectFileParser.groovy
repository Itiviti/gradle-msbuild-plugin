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

    File getDotnetAssemblyFile() {
        project.file(properties.TargetPath)
    }

    File getDotnetDebugFile() {
        File target = project.file(properties.TargetPath)
        new File(renameExtension(target.path, OperatingSystem.current().windows ? '.pdb' : '.mdb'))
    }

     FileCollection getDotnetArtifacts() {
        project.files({
            def ret = [dotnetAssemblyFile];
            if (dotnetDebugFile?.exists()) ret += dotnetDebugFile;
            File doc = getProjectPropertyPath('DocumentationFile')
            if (doc?.exists()) ret += doc;
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

    File findReferencedDll(String  dllName) {
        def dllPath = references.find {
            it.Filename.startsWith(dllName)
        }.HintPath
        println dllPath
        if(dllPath == null || "".equals(dllPath))
            null
        findProjectFile(dllPath)
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
