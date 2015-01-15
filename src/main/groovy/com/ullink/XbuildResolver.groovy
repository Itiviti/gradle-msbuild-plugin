package com.ullink

import org.gradle.api.tasks.StopActionException

class XbuildResolver implements IExecutableResolver {

    XbuildResolver(){
        def execute = "mono --version".execute()
        execute.waitFor()
        if (execute.in.text == null)
            throw new StopActionException("Mono must be on PATH.")
    }

    @Override
    ProcessBuilder executeDotNet(File exe) {
        return new ProcessBuilder("mono", exe.toString())
    }

    void setupExecutable(Msbuild msbuild) {
        msbuild.executable = 'xbuild.exe'
        msbuild.msbuildDir = getXBuildDir(msbuild.version)
    }

    public static String getXBuildDir(version) {
        def which = "which mono".execute()
        which.waitFor()
        def monoRoot = which.in.text
        if (monoRoot == null || monoRoot.isEmpty())
            throw new StopActionException("Can't get mono location. Mono default installation prefix is usually /usr/lib/")
        monoRoot = monoRoot - "/bin/mono\n"

        def versions = version == null ?
            IExecutableResolver.KNOWN_VERSIONS :
            [version]

        for (v in versions) {
            def file = new File("$monoRoot/lib/mono/$v/xbuild.exe")
            if (file.exists()) {
                return file.getParent()
            }
        }

        throw new StopActionException("Can't find xbuild binary. Is Mono-Devel package is installed?")
    }

    String getFileParserPath() {
        return '/META-INF/bin/ProjectFileParser.exe'
    }
}
