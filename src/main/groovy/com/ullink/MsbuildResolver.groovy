package com.ullink

import static java.lang.Float.parseFloat

class MsbuildResolver implements IExecutableResolver {
    static final String MSBUILD_TOOLS_PATH = 'MSBuildToolsPath'
    static final String MSBUILD_PREFIX = "SOFTWARE\\Microsoft\\MSBuild\\ToolsVersions\\"
    static final String MSBUILD_WOW6432_PREFIX = "SOFTWARE\\Wow6432Node\\Microsoft\\MSBuild\\ToolsVersions\\"

    void setupExecutable(Msbuild msbuild) {
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

        if (versionsToCheck.find( { trySetMsbuild(msbuild, it) } ))
            msbuild.logger.info("Resolved MSBuild to ${msbuild.msbuildDir}")
        else
            msbuild.logger.warn("Couldn't resolve MSBuild in the system (existing versions: ${availableVersions}).")

        msbuild.executable = 'msbuild.exe'
    }

    @Override
    ProcessBuilder executeDotNet(File exe) {
        return new ProcessBuilder(exe.toString())
    }

    static List<String> getMsBuildVersionsFromRegistry(String key) {
        Registry.getKeys(Registry.HKEY_LOCAL_MACHINE, key).sort({ -parseFloat(it) }).collect({ key + it })
    }

    static boolean trySetMsbuild(Msbuild msbuild, String key) {
        def v = Registry.getValue(Registry.HKEY_LOCAL_MACHINE, key, MSBUILD_TOOLS_PATH)
        if (v != null && new File(v).isDirectory()) {
            msbuild.msbuildDir = v
            return true
        }
        false
    }

    String getFileParserPath() {
        return '/META-INF/bin/ProjectFileParser_Win.exe'
    }
}


