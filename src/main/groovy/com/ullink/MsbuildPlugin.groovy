package com.ullink
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.BasePlugin
import org.gradle.language.base.plugins.LifecycleBasePlugin

class MsbuildPlugin implements Plugin<Project> {
    void apply(Project project) {
        project.pluginManager.apply(BasePlugin)

        def assemblyInfoPatcher = project.tasks.register('assemblyInfoPatcher', AssemblyInfoVersionPatcher)
        project.tasks.register('msbuild', Msbuild) {
            dependsOn assemblyInfoPatcher
        }
        def cleanMsbuild = project.tasks.register('cleanMsbuild')
        project.tasks.named(LifecycleBasePlugin.CLEAN_TASK_NAME).configure {
            dependsOn cleanMsbuild
        }
    }
}

