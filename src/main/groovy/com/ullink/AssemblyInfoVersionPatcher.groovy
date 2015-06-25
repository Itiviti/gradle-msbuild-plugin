package com.ullink

import org.apache.commons.io.FilenameUtils
import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

class AssemblyInfoVersionPatcher extends ConventionTask {
    def file

    AssemblyInfoVersionPatcher() {
        conventionMapping.map "file", { mainProjectAssemblyInfo }
        conventionMapping.map "fileVersion", { version }

        project.afterEvaluate {
            if (!version) return;
            if (mainProjectAssemblyInfo == getFile())
                project.tasks.msbuild.dependsOn this
        }
    }

    File getMainProjectAssemblyInfo() {
       project.tasks.msbuild.mainProject?.eval.Compile.collect {
           project.tasks.msbuild.mainProject.findProjectFile(it.Include)
       }.find {
           FilenameUtils.getBaseName(it.name) == 'AssemblyInfo'
       }
    }

    @Input
    def version

    @Input
    def fileVersion

    @InputFile
    @OutputFile
    File getPatchedFile() {
        project.file(getFile())
    }

    @TaskAction
    void run() {
        logger.info("Replacing version attributes in ${getPatchedFile()}")
        replace('AssemblyVersion', getVersion())
        replace('AssemblyFileVersion', getFileVersion())
    }

    void replace(def name, def value) {
        if (FilenameUtils.getExtension(getPatchedFile().name) == 'fs')
            project.ant.replaceregexp(file: getPatchedFile(), match: /^\[<assembly: $name\(".*"\)>\]$/, replace: "[<assembly: ${name}(\"${value}\")>]", byline: true)
        else
            project.ant.replaceregexp(file: getPatchedFile(), match: /^\[assembly: $name\(".*"\)\]$/, replace: "[assembly: ${name}(\"${value}\")]", byline: true)

    }
}
