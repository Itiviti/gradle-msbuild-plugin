package com.ullink
import org.gradle.internal.os.OperatingSystem
import org.junit.Before
import org.junit.Test

import static org.junit.Assert.assertNotNull

class XbuildLocateTest {

    @Before
    public void beforeMethod() {
        org.junit.Assume.assumeTrue OperatingSystem.current().linux
        org.junit.Assume.assumeTrue "mono --version".execute().in.text != null
    }

    @Test
    public void testXBuildCanBeFound() {
        def resolver = new XbuildResolver()
        def xbuildDir = resolver.getXBuildDir(null)
        assertNotNull(xbuildDir)
    }

}
