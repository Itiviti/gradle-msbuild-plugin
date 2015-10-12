package com.ullink
import groovy.xml.MarkupBuilder
import org.gradle.api.GradleException
import org.gradle.api.Project
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

    @Test
    public void testGetReferenceLibPath() {
        def writer = new StringWriter()
        def xml = new MarkupBuilder(writer)
        xml.Project(ToolsVersion:"4.0", DefaultTargets:"Test", xmlns:"http://schemas.microsoft.com/developer/msbuild/2003") {
            Target(Name:'Test')
            ItemGroup {
                Reference (Include:"DevExpress.Charts.v10.2.Core, Version=10.2.5.0, Culture=neutral, PublicKeyToken=6935958ad4a06599, processorArchitecture=MSIL") {
                    HintPath ("packages\\DevExpress.10.2.5.WinForms.10.2.5.3\\lib\net35\\DevExpress.Charts.v10.2.Core.dll")
                    Private: "True"
                }
            }
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
        println file
        println p.tasks.msbuild.mainProject.findReferencedDll("DevExpress.Charts").getCanonicalPath()
        println file.getParentFile().getCanonicalPath() + File.separator + "packages\\DevExpress.10.2.5.WinForms.10.2.5.3\\lib\net35\\DevExpress.Charts.v10.2.Core.dll"
        assertTrue((file.getParentFile().getCanonicalPath() + File.separator + "packages\\DevExpress.10.2.5.WinForms.10.2.5.3\\lib\net35\\DevExpress.Charts.v10.2.Core.dll").equals(p.tasks.msbuild.mainProject.findReferencedDll("DevExpress.Charts").getCanonicalPath()))
    }

    @Test
    public void execution_nonExistentProjectFile_throwsGradleException() {
        Project p = ProjectBuilder.builder().build()
        p.apply plugin: 'msbuild'
        p.msbuild {
            projectFile = "C:\\con" // we can never create a windows file called `con`
        }

        expectedException.expect(GradleException.class);

        p.tasks.msbuild.execute()
    }
}
