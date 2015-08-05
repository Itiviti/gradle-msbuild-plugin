package com.ullink

import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.TaskAction

import java.lang.reflect.Array

class SourceLinkIndexing extends ConventionTask {
    def projectFile
    def sourcelinkDir
    def properties = [:]
    def url
    def commit
    def pdbFile
    def sourceFiles

    SourceLinkIndexing() {

        conventionMapping.map "projectFile", { pdbFile ? null : project.tasks.msbuild.mainProject?.projectFile }
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
        if (getProjectFile()) args += [ '-pr', getProjectFile() ]
        if (commit) args += [ '-c', commit]
        addArgs(args, '-p', pdbFile, { project.file(it) })
        if (url) args += [ '-u', url]
        addArgs(args, '-f', sourceFiles, { it })
        getProperties().each {
            args += [ '-pp', it.key, it.value ]
        }
        project.exec {
            commandLine args
        }
    }

    void addArgs(List args, def prefix, def value, def mapper) {
        if (!value) return;
        if (value instanceof Iterable || value instanceof Array) {
            value.each {
                args.addAll([prefix, mapper(it)])
            }
        } else {
            args.addAll([prefix, mapper(value)])
        }
    }
}
