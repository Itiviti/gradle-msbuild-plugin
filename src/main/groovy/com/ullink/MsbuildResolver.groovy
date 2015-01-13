package com.ullink

class MsbuildResolver implements IExecutableResolver {
    static final String MSBUILD_TOOLS_PATH = 'MSBuildToolsPath'
    static final String MSBUILD_PREFIX = "SOFTWARE\\Microsoft\\MSBuild\\ToolsVersions\\"
    static final String MSBUILD_WOW6432_PREFIX = "SOFTWARE\\Wow6432Node\\Microsoft\\MSBuild\\ToolsVersions\\"

    void setupExecutable(Msbuild msbuild) {
        (msbuild.version == null ?
            KNOWN_VERSIONS :
            [msbuild.version]).find { x ->
            trySetMsbuild(msbuild, MSBUILD_WOW6432_PREFIX + x) ||
                trySetMsbuild(msbuild, MSBUILD_PREFIX + x)
        }
        msbuild.executable = 'msbuild.exe'
    }

    @Override
    ProcessBuilder executeDotNet(File exe) {
        return new ProcessBuilder(exe.toString())
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


