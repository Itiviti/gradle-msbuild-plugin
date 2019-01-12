package com.ullink
import groovy.xml.MarkupBuilder
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.internal.os.OperatingSystem
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException

import static org.junit.Assert.assertTrue

class MsbuildPluginTest {
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void msbuildPluginAddsMsbuildTaskToProject() {
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: MsbuildPlugin
        assertTrue(project.tasks.msbuild instanceof Msbuild)
    }

    @Test
    public void testExecution() {
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

        Project p = ProjectBuilder.builder().build()
        p.apply plugin: MsbuildPlugin
        p.msbuild {
            projectFile = file
        }
        p.tasks.msbuild.build()
    }
    @Test
    public void execution_nonExistentProjectFile_throwsGradleException() {
        Project p = ProjectBuilder.builder().build()
        p.apply plugin: MsbuildPlugin
        p.msbuild {
            projectFile = OperatingSystem.current().isWindows() ? 'C:\\con' : '/con' // we can never create a file called `con` in root
        }

        expectedException.expect(GradleException.class);

        p.tasks.msbuild.build()
    }
}
