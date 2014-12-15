package com.ullink
import groovy.xml.MarkupBuilder
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

import static org.junit.Assert.assertTrue


class MsbuildPluginTest {

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
        
        Project p = ProjectBuilder.builder().build()
        p.apply plugin: 'msbuild'
        p.msbuild {
            projectFile = file
        }
        p.tasks.msbuild.execute()
    }
}
