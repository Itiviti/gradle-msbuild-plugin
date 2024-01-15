package com.ullink
import groovy.xml.MarkupBuilder
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.internal.os.OperatingSystem
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

class MsbuildPluginSpec extends Specification {
    def msbuildPluginAddsMsbuildTaskToProject() {
        given:
        Project project = ProjectBuilder.builder().build()

        when:
        project.apply plugin: MsbuildPlugin

        then:
        project.tasks.msbuild instanceof Msbuild
    }

    def testExecution() {
        given:
        def writer = new StringWriter()
        def xml = new MarkupBuilder(writer)
        xml.Project(ToolsVersion:"4.0", DefaultTargets:"Test", xmlns:"http://schemas.microsoft.com/developer/msbuild/2003") {
          Target(Name:'Test')
        }
        File file = File.createTempFile("temp",".scrap");
        file.with {
            deleteOnExit()
            write writer.toString()
        }

        when:
        Project p = ProjectBuilder.builder().build()
        p.apply plugin: MsbuildPlugin
        p.msbuild {
            projectFile = file
        }
        p.tasks.msbuild.build()

        then:
        noExceptionThrown()
    }

    def execution_nonExistentProjectFile_throwsGradleException() {
        given:
        Project p = ProjectBuilder.builder().build()

        when:
        p.apply plugin: MsbuildPlugin
        p.msbuild {
            projectFile = OperatingSystem.current().isWindows() ? 'C:\\con' : '/con' // we can never create a file called `con` in root
        }

        and:
        p.tasks.msbuild.build()

        then:
        thrown(GradleException)
    }
}
