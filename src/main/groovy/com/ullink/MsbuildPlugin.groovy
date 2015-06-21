package com.ullink
import org.gradle.api.Plugin
import org.gradle.api.Project

class MsbuildPlugin implements Plugin<Project> {
    void apply(Project project) {
        project.apply plugin: 'base'
        project.task('msbuild', type: Msbuild)
        project.tasks.clean.dependsOn project.tasks.cleanMsbuild

        project.task('assemblyInfoPatcher', type: AssemblyInfoVersionPatcher)
    }
}

