package com.ullink

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.internal.os.OperatingSystem
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.util.VersionNumber
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException

import static org.junit.Assert.assertNotNull

class PosixMsbuildLocateTests {
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void beforeMethod() {
        org.junit.Assume.assumeTrue OperatingSystem.current().unix
        org.junit.Assume.assumeTrue "mono --version".execute().in.text != null
    }

    @Test
    public void testMsBuildCanBeFound() {
        def resolver = new PosixMsbuildResolver()

        // Unspecified version
        Project p = ProjectBuilder.builder().build()
        p.apply plugin: MsbuildPlugin

        def msbuild = resolver.locateMsBuild(p.tasks.msbuild.version)
        assertNotNull(msbuild)

        // Specified version
        p = ProjectBuilder.builder().build()
        p.apply plugin: MsbuildPlugin
        def msbuildVersion = msbuild[1]
        p.msbuild {
            version = msbuildVersion
        }
        msbuild = resolver.locateMsBuild(VersionNumber.parse(p.tasks.msbuild.version))
        assertNotNull(msbuild)
    }

    @Test
    public void givenInvalidMSBuildVersion_throwsException() {
        def resolver = new PosixMsbuildResolver()
        Project p = ProjectBuilder.builder().build()
        p.apply plugin: MsbuildPlugin
        p.msbuild {
            version = 999.3
        }

        expectedException.expect(GradleException.class)
        expectedException.expectMessage("Cannot find an msbuild binary")
        resolver.locateMsBuild(VersionNumber.parse(p.tasks.msbuild.version))

    }
}
