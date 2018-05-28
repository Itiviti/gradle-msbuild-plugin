package com.ullink

import org.gradle.api.GradleException
import org.gradle.util.VersionNumber

class PosixMsbuildResolver implements IExecutableResolver {

    String[] msbuild

    PosixMsbuildResolver(VersionNumber version)
    {
        msbuild = locateMsBuild(version)
    }

    boolean msBuildFound() {
        return msbuild != null
    }

    @Override
    ProcessBuilder executeDotNet(File exe) {
        return new ProcessBuilder("mono", exe.toString())
    }

    void setupExecutable(Msbuild msbuild) {
        msbuild.executable = 'MSBuild.dll'
        if (msbuild.msbuildDir == null) {
            msbuild.msbuildDir = getMsBuildDir(locateMsBuild(msbuild.version))
        }
    }

    public static List<String[]> locateMsBuilds() {

        List<String> msbuildRoots = [XbuildResolver.getMonoBinaryRootDirectory()] + XbuildResolver.getOSXMonoRootDirectories()

        def existingMsBuilds = msbuildRoots
                .collectMany { ["$it/lib/mono", "$it/lib/mono/msbuild"] }
                .collectMany { XbuildResolver.getVersionDirectories(it) }
                .collectMany { [
                [new File(it[0], "MSBuild.dll"), it[1]],
                [new File(it[0], "bin/MSBuild.dll"), it[1]]
        ]}
        .findAll { it[0].exists() }

        return existingMsBuilds
    }

    public static String[] locateMsBuild(VersionNumber version = null) {
        def msbuilds = locateMsBuilds()
        String[] msbuild = null
        if(version == null) {
            msbuild = msbuilds.first()
        }
        else {
            msbuild = msbuilds.find { (version == it[1]) }
        }

        return msbuild
    }

    public static String getMsBuildDir(String[] msbuild) {
        return new File(msbuild.first()).getParent()
    }
}
