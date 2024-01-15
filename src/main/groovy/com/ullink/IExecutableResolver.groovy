package com.ullink

interface IExecutableResolver {
    ProcessBuilder executeDotNet(File exe)

    ProcessBuilder executeDotNetApp(File exe)

    void setupExecutable(Msbuild msbuild);
}
