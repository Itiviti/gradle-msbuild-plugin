package com.ullink

import com.google.common.io.Files
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
        eval.Properties ?: [:]
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

    def static renameExtension(File file, String newExtension) {
        return new File(file.getParent(), Files.getNameWithoutExtension(file.toString()) + newExtension)
    }

    File getDotnetAssemblyFile() {
        if (properties.TargetPath) project.file(properties.TargetPath) else null
    }

    File getDotnetDebugFile() {
        File target = getDotnetAssemblyFile()
        if (target == null) return null
        renameExtension(target, OperatingSystem.current().windows ? '.pdb' : '.mdb')
    }

    FileCollection getDotnetArtifacts() {
        project.files({
            def assembly = dotnetAssemblyFile
            if (assembly) {
                // Single-TFM: TargetPath was set — use it directly.
                def ret = [assembly]
                if (dotnetDebugFile?.exists()) ret += dotnetDebugFile
                File doc = getProjectPropertyPath('DocumentationFile')
                if (doc?.exists()) ret += doc
                return ret
            }
            // Multi-TFM outer build: TargetPath absent. Collect per-TFM outputs.
            return dotnetArtifactsForAllTfms
        }) {
            builtBy msbuild
        }
    }

    /**
     * For multi-targeted projects the outer MSBuild build has no single TargetPath.
     * Fall back to the standard AppendTargetFrameworkToOutputPath=true layout:
     *   {OutDir}/{tfm}/{AssemblyName}.dll (+ .pdb/.mdb)
     * Returns empty when OutDir/AssemblyName cannot be determined (e.g. projects that
     * use fully TFM-conditioned OutputPath blocks with no unconditional OutDir).
     */
    List<File> getDotnetArtifactsForAllTfms() {
        def assemblyName    = properties.AssemblyName?.toString()
        def targetFrameworks = properties.TargetFrameworks?.toString()
        if (!assemblyName || !targetFrameworks) return []
        def outDir = (properties.OutDir ?: properties.OutputPath)?.toString()
        if (!outDir) return []
        def debugExt = OperatingSystem.current().windows ? '.pdb' : '.mdb'
        def ret = []
        targetFrameworks.split(';').collect { it.trim() }.findAll { it }.each { tfm ->
            def tfmDir = new File(findProjectFile(outDir), tfm)
            def dll = new File(tfmDir, "${assemblyName}.dll")
            def dbg = new File(tfmDir, "${assemblyName}${debugExt}")
            if (dll.exists()) ret << dll
            if (dbg.exists()) ret << dbg
        }
        ret
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
