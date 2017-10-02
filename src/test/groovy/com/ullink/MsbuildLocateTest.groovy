package com.ullink

import org.gradle.api.Project
import org.gradle.internal.os.OperatingSystem
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException

import static org.junit.Assert.assertNotNull

class MsbuildLocateTest {
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void beforeMethod() {
        org.junit.Assume.assumeTrue OperatingSystem.current().isWindows()
    }

    @Test
    public void testMsBuildCanBeFound() {
        def resolver = new MsbuildResolver()

        Project p = ProjectBuilder.builder().build()
        p.apply plugin: MsbuildPlugin
        resolver.setupExecutable(p.tasks.msbuild)
        assertNotNull(p.tasks.msbuild.msbuildDir)
    }
}