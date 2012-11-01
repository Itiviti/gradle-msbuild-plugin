package com.ullink

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.internal.os.OperatingSystem

class MsbuildPlugin implements Plugin<Project> {
    void apply(Project project) {
        project.apply plugin: 'base'
        if (OperatingSystem.current().windows) {
            project.task('msbuild', type: Msbuild)
            project.tasks.clean.dependsOn project.tasks.cleanMsbuild
        }
    }
}

