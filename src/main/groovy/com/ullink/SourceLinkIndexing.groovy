package com.ullink

import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.TaskAction

class SourceLinkIndexing extends ConventionTask {
    def projectFile
    def sourcelinkDir
    def properties = [:]
    def url
    def commit

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

    @TaskAction
    void run() {
        def args = [ "$sourcelinkDir/SourceLink.exe", 'index' ]
        args += [ '-pr', getProjectFile() ]
        if (commit) args += [ '-c', commit]
        if (url) args += [ '-u', url]
        getProperties().each {
            args += [ '-pp', it.key, it.value ]
        }
        project.exec {
            commandLine args
        }
    }
}
