package com.ullink

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

class MsbuildLocateSpec extends Specification {
    def testMsBuildCanBeFound() {
        given:
        def resolver = new MsbuildResolver()

        when:
        Project p = ProjectBuilder.builder().build()
        p.apply plugin: MsbuildPlugin
        resolver.setupExecutable(p.tasks.msbuild)

        then:
        p.tasks.msbuild.msbuildDir != null
    }
}