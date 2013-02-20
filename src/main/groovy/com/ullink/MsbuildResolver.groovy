package com.ullink

import org.apache.commons.lang.NotImplementedException

interface IExecutableResolver {
    void setupExecutable(Msbuild msbuild);
}

class MsbuildResolver implements IExecutableResolver {
    static final String MSBUILD_TOOLS_PATH = 'MSBuildToolsPath'
    static final String MSBUILD_PREFIX         = "SOFTWARE\\Microsoft\\MSBuild\\ToolsVersions\\"
    static final String MSBUILD_WOW6432_PREFIX = "SOFTWARE\\Wow6432Node\\Microsoft\\MSBuild\\ToolsVersions\\"

    void setupExecutable(Msbuild msbuild) {
        (msbuild.version == null ?
            ["4.0", "3.5","2.0"] :
            [msbuild.version]).find { x ->
                trySetMsbuild(msbuild, MSBUILD_WOW6432_PREFIX + x) ||
                        trySetMsbuild(msbuild, MSBUILD_PREFIX + x)
        }
        msbuild.executable = 'msbuild.exe'
    }

    boolean trySetMsbuild(Msbuild msbuild, String key) {
        def v = Registry.getValue(Registry.HKEY_LOCAL_MACHINE, key, MSBUILD_TOOLS_PATH)
        if (v != null && new File(v).isDirectory()) {
            msbuild.msbuildDir = v
            return true
        }
        false
    }
}

class XbuildResolver implements IExecutableResolver {

    void setupExecutable(Msbuild msbuild) {
        msbuild.executable = 'xbuild'
        throw new NotImplementedException()
    }

}

