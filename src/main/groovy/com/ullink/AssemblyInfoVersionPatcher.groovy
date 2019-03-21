package com.ullink

import com.google.common.io.Files
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.TaskAction

import java.nio.file.Paths

class AssemblyInfoVersionPatcher extends DefaultTask {
    ListProperty<File> files
    ListProperty<String> projects

    AssemblyInfoVersionPatcher() {
        projects = project.getObjects().listProperty(String)
        projects.set(project.provider({
            project.tasks.msbuild.projects.collect { it.key }
        }))

        files = project.getObjects().listProperty(File)
        files.set(project.provider({
            projects.get()
                .collect { project.tasks.msbuild.projects[it] }
                .collect {
                    def path

                    if (it.properties.UsingMicrosoftNETSdk == 'true') {
                        path = it.properties.MSBuildProjectFullPath
                    } else {
                        path = it?.getItems('Compile').find { Files.getNameWithoutExtension(it.name) == 'AssemblyInfo' }
                    }
                    project.logger.info("AssemblyInfoPatcher: found file ${path} (${path?.class}) for project ${it.projectName}")

                    path
                }
                .findAll { it != null }
                .unique()
        }))

        fileVersion = project.getObjects().property(String)
        informationalVersion = project.getObjects().property(String)
        fileVersion.set(project.provider ({ version }))
        informationalVersion.set(project.provider ({ version }))

        project.afterEvaluate {
            if (!version) return
            project.tasks.withType(Msbuild) { task ->
                task.projects.each { proj ->
                    def parsedFiles = files.get()
                    if (proj.value.getItems('Compile')?.intersect(parsedFiles)) {
                        task.dependsOn this
                    }
                    if (parsedFiles.contains(proj.properties.MSBuildProjectFullPath)) {
                        task.dependsOn this
                    }
                }
            }
        }
    }

    @Input
    String version

    @Input
    Property<String> fileVersion

    @Input
    Property<String> informationalVersion

    @Input
    String title = ''

    @Input
    String company = ''

    @Input
    String product = ''

    @Input
    String copyright = ''

    @Input
    String trademark = ''

    @Input
    String assemblyDescription = ''

    @Input
    def charset = 'UTF-8'

    @InputFiles
    @OutputFiles
    FileCollection getPatchedFiles() {
        project.files(files.get())
    }

    @TaskAction
    void run() {
        getPatchedFiles().each {
            logger.info("Replacing version attributes in $it")
            replace(it, 'AssemblyVersion', version)
            replace(it, 'AssemblyFileVersion', fileVersion.get())
            replace(it, 'AssemblyInformationalVersion', informationalVersion.get())
            replace(it, 'AssemblyDescription', assemblyDescription)
            replace(it, 'AssemblyTitle', title)
            replace(it, 'AssemblyCompany', company)
            replace(it, 'AssemblyProduct', product)
            replace(it, 'AssemblyCopyright', copyright)
            replace(it, 'AssemblyTrademark', trademark)
        }
    }

    void replace(File file, def name, def value) {
        // only change the assembly values if they specified here (not blank or null)
        // if the parameters are blank, then keep whatever is already in the assemblyInfo file.
        if (!value) {
            return
        }

        def extension = Files.getFileExtension(file.absolutePath)
        switch (extension) {
            case 'fs':
                project.ant.replaceregexp(file: file, match: /^\[<assembly: $name\s*\(".*"\)\s*>\]$/, replace: "[<assembly: ${name}(\"${value}\")>]", byline: true, encoding: charset)
                break
            case 'vb':
                project.ant.replaceregexp(file: file, match: /^\[<assembly: $name\s*\(".*"\)\s*>\]$/, replace: "[<assembly: ${name}(\"${value}\")>]", byline: true, encoding: charset)
                break
            // project file
            case ~/.*proj$/:
                if (name != 'AssemblyVersion' && name != 'AssemblyTitle' && name.startsWith('Assembly')) {
                    name = name.substring('Assembly'.length())
                }
                project.ant.replaceregexp(file: file, match: /<$name>\s*([^\s]+)\s*\<\/$name>$/, replace: "<$name>$value</$name>", byline: true, encoding: charset)
                break
            default:
                project.ant.replaceregexp(file: file, match: /^\[assembly: $name\s*\(".*"\)\s*\]$/, replace: "[assembly: ${name}(\"${value}\")]", byline: true, encoding: charset)
                break

        }
    }
}
