package com.ullink
import groovy.xml.MarkupBuilder
import org.apache.commons.io.FileUtils
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertNotNull
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
    public void givenSolutionFileWithDifferentProjectName_mainProjectIsCorrectlyParsed() {
        def solutionDir = createPhonySolutionStructure()

        Project p = ProjectBuilder.builder().build()
        p.apply plugin: 'msbuild'
        p.msbuild {
            solutionFile = solutionDir
        }
        p.tasks.msbuild.execute()

        assertNotNull(p.tasks.msbuild.mainProject)
        assertEquals("phony-project", p.tasks.msbuild.mainProject.projectName)
    }

    @Test
    public void givenProjectFile_projectIsCorrectlyParsed() {
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
        p.tasks.msbuild.execute()

        assertEquals("test", p.tasks.msbuild.mainProject.projectName)
    }
    @Test
    public void execution_nonExistentProjectFile_throwsGradleException() {
        Project p = ProjectBuilder.builder().build()
        p.apply plugin: MsbuildPlugin
        p.msbuild {
            projectFile = "C:\\con" // we can never create a windows file called `con`
        }

        expectedException.expect(GradleException.class);

        p.tasks.msbuild.execute()
    }

    File createPhonySolutionStructure() {
        def tempDir = File.createTempDir()

        [
            "phony-solution.sln",
            "phony-project.csproj",
            "AssemblyInfo.cs"
        ].each{
            new File(tempDir, it).newOutputStream().leftShift(getClass().getResourceAsStream("/phony-solution/$it")).close()
        }

        FileUtils.forceDeleteOnExit(tempDir)
        new File(tempDir, 'phony-solution.sln')
    }
}
