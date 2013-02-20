package com.ullink

import org.gradle.internal.os.OperatingSystem
import org.junit.Before

import static org.junit.Assert.*
import groovy.xml.MarkupBuilder
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test


class MsbuildPluginTest {

    @Before
    public void beforeMethod() {
        org.junit.Assume.assumeTrue OperatingSystem.current().windows
    }

    @Test
    public void msbuildPluginAddsMsbuildTaskToProject() {
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'msbuild'
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
        
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'msbuild'
        project.msbuild {
            projectFile = file
        }
        project.tasks.msbuild.execute()
    }
}
