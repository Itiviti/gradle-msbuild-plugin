package com.ullink

import org.apache.commons.io.FilenameUtils
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.TaskAction

class AssemblyInfoVersionPatcher extends ConventionTask {
    def files
    List<String> projects = []

    AssemblyInfoVersionPatcher() {
        conventionMapping.map "projects", { [ project.tasks.msbuild.mainProject?.projectName ] }
        conventionMapping.map "files", {
            getProjects()
                .collect { project.tasks.msbuild.projects[it] }
                .collect {
                    it?.getItems('Compile').find { FilenameUtils.getBaseName(it.name) == 'AssemblyInfo' }
                }
        }
        conventionMapping.map "fileVersion", { version }

        project.afterEvaluate {
            if (!version) return;
            project.tasks.withType(Msbuild) { task ->
                task.projects.each { proj ->
                    if (proj.value.getItems('Compile').intersect(files)) {
                        task.dependsOn this
                    }
                }
            }
        }
    }

    @Input
    def version

    @Input
    def fileVersion

    @Input
    def charset = "UTF-8"
    
    @InputFiles
    @OutputFiles
    FileCollection getPatchedFiles() {
        project.files(getFiles())
    }

    @TaskAction
    void run() {
        getPatchedFiles().each {
            logger.info("Replacing version attributes in $it")
            replace(it, 'AssemblyVersion', getVersion())
            replace(it, 'AssemblyFileVersion', getFileVersion())
        }
    }

    void replace(def file, def name, def value) {
        if (FilenameUtils.getExtension(file.name) == 'fs')
            project.ant.replaceregexp(file: file, match: /^\[<assembly: $name\s*\(".*"\)\s*>\]$/, replace: "[<assembly: ${name}(\"${value}\")>]", byline: true, charset: charset)
        else
            project.ant.replaceregexp(file: file, match: /^\[assembly: $name\s*\(".*"\)\s*\]$/, replace: "[assembly: ${name}(\"${value}\")]", byline: true, charset: charset)
    }
}
