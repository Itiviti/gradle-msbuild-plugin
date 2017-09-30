package com.ullink

import static java.lang.Float.parseFloat
import groovy.io.FileType
import java.lang.ProcessBuilder
import java.nio.file.Files
import org.apache.commons.io.FileUtils

class MsbuildResolver implements IExecutableResolver {
    static final String MSBUILD_TOOLS_PATH = 'MSBuildToolsPath'
    static final String MSBUILD_PREFIX = "SOFTWARE\\Microsoft\\MSBuild\\ToolsVersions\\"
    static final String MSBUILD_WOW6432_PREFIX = "SOFTWARE\\Wow6432Node\\Microsoft\\MSBuild\\ToolsVersions\\"

    // Find msbuild >= 15.0 by vswhere
    static def findMsbuildByVsWhere(Msbuild msbuild) {
        File tempDir = Files.createTempDirectory('vswhere').toFile()
        tempDir.deleteOnExit()

        def vswhereURL = MsbuildResolver.getResource("/META-INF/vswhere.exe")
        def vswhereFile = new File(tempDir, 'vwshere.exe')
        FileUtils.copyURLToFile(vswhereURL, vswhereFile)
        def vswhere = new ProcessBuilder(vswhereFile.toString())
        vswhere.command() << '-latest' << '-products' << '*' << '-requires' << 'Microsoft.Component.MSBuild' << '-property' << 'installationPath'

        def proc = vswhere.start()
        proc.waitFor()
        def location = proc.in.text?.trim();
        if (location) {
            def msbuildDir = new File(location, 'MSBuild')
            msbuild.logger.info("Found following MSBuild installation folder: ${msbuildDir}")
            msbuildDir.eachDirMatch(~/\d+(\.\d+)*/) { dir ->
                msbuild.msbuildDir = new File(dir, 'Bin')
                return
            }
        }
    }

    static def findMsbuildFromRegistry(Msbuild msbuild) {
        List<String> availableVersions =
            getMsBuildVersionsFromRegistry(MSBUILD_WOW6432_PREFIX) +
            getMsBuildVersionsFromRegistry(MSBUILD_PREFIX)
        msbuild.logger.debug("Found following MSBuild versions in the registry: ${availableVersions}")

        List<String> versionsToCheck
        if (msbuild.version != null) {
            versionsToCheck = [ MSBUILD_WOW6432_PREFIX + msbuild.version, MSBUILD_PREFIX + msbuild.version ]
            msbuild.logger.info("MSBuild version explicitly set to: '${msbuild.version}'")
        } else {
            versionsToCheck = availableVersions
        }

        versionsToCheck.find( { trySetMsbuild(msbuild, it) } )
    }

    void setupExecutable(Msbuild msbuild) {
        if (msbuild.msbuildDir == null) {
            findMsbuildByVsWhere(msbuild)
        }
        if (msbuild.msbuildDir == null) {
            findMsbuildFromRegistry(msbuild)
        }

        if (msbuild.msbuildDir)
            msbuild.logger.info("Resolved MSBuild to ${msbuild.msbuildDir}")
        else
            msbuild.logger.warn("Couldn't resolve MSBuild in the system")

        msbuild.executable = 'msbuild.exe'
    }

    @Override
    ProcessBuilder executeDotNet(File exe) {
        return new ProcessBuilder(exe.toString())
    }

    static List<String> getMsBuildVersionsFromRegistry(String key) {
        (Registry.getKeys(Registry.HKEY_LOCAL_MACHINE, key) ?: []).sort({ -parseFloat(it) }).collect({ key + it })
    }

    static boolean trySetMsbuild(Msbuild msbuild, String key) {
        def v = Registry.getValue(Registry.HKEY_LOCAL_MACHINE, key, MSBUILD_TOOLS_PATH)
        if (v != null && new File(v).isDirectory()) {
            msbuild.msbuildDir = v
        }
    }
}


