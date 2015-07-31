package com.ullink

import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.TaskAction

class SourceLinkIndexing extends ConventionTask {
    def projectFile

    SourceLinkIndexing() {

        conventionMapping.map "projectFile", { project.tasks.msbuild.mainProject?.projectFile }
        conventionMapping.map "properties", { project.tasks.msbuild.initProperties }

        project.afterEvaluate {
            if (!url) return;
            if (project.tasks.msbuild.mainProject?.projectFile == getProjectFile()) {
                dependsOn project.tasks.msbuild
                project.tasks.msbuild.finalizedBy this
            }
        }
    }

    def sourcelinkDir
    def properties = [:]
    def url

    @TaskAction
    void run() {
        def args = [ "$sourcelinkDir/SourceLink.exe", 'index',
                        '-pr', getProjectFile(),
                        '-u', url]
        getProperties().each {
            args += [ '-pp', it.key, it.value ]
        }
        project.exec {
            commandLine args
        }
    }
}
