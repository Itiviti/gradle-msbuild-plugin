package com.ullink

import com.google.common.io.Resources

import java.nio.file.Files

import static java.lang.Float.parseFloat

class MsbuildResolver implements IExecutableResolver {
    static final String MSBUILD_TOOLS_PATH = 'MSBuildToolsPath'
    static final String MSBUILD_PREFIX = "SOFTWARE\\Microsoft\\MSBuild\\ToolsVersions\\"
    static final String MSBUILD_WOW6432_PREFIX = "SOFTWARE\\Wow6432Node\\Microsoft\\MSBuild\\ToolsVersions\\"

    // Find msbuild >= 15.0 by vswhere
    static def findMsbuildByVsWhere(Msbuild msbuild) {
        File tempDir = Files.createTempDirectory(msbuild.temporaryDir.toPath(), 'vswhere').toFile()
        tempDir.deleteOnExit()

        def vswhereFile = new File(tempDir, 'vwshere.exe')
        Resources.asByteSource(MsbuildResolver.getResource("/vswhere.exe")).copyTo(com.google.common.io.Files.asByteSink(vswhereFile))

        def vswhere = new ProcessBuilder(vswhereFile.toString())
        if (msbuild.version) {
            vswhere.command() << '-version' << msbuild.version << '-latest'
        } else {
            vswhere.command() << '-latest'
        }
        vswhere.command() << '-products' << '*' << '-requires' << 'Microsoft.Component.MSBuild' << '-property' << 'installationPath'

        def proc = vswhere.start()
        proc.waitFor()
        def location = proc.in.text?.trim();
        if (!location) {
            return
        }
        def msbuildDir = new File(location, 'MSBuild')
        if (!msbuildDir.exists()) {
            return
        }
        msbuild.logger.info("Found the following MSBuild (using vswhere) installation folder: ${msbuildDir}")
        msbuildDir.eachDirMatch(~/(?i)(\d+(\.\d+)*|current)/) { dir ->
            def hasMsbuildExe = new File(dir, 'Bin\\msbuild.exe').exists()
            msbuild.logger.debug("Found MSBuild directory: $dir; ${hasMsbuildExe ? 'OK' : 'Does not have msbuild.exe'}")
            if (hasMsbuildExe) {
                msbuild.msbuildDir = new File(dir, 'Bin')
            }
        }
    }

    static def findMsbuildFromRegistry(Msbuild msbuild) {
        List<String> availableVersions =
            getMsBuildVersionsFromRegistry(MSBUILD_WOW6432_PREFIX) +
            getMsBuildVersionsFromRegistry(MSBUILD_PREFIX)
        msbuild.logger.info("Found the following MSBuild (in the registry) versions: ${availableVersions}")

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

    @Override
    ProcessBuilder executeDotNetApp(File exe) {
        return executeDotNet(exe)
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


