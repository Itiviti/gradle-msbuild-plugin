package com.ullink

interface IExecutableResolver {
    ProcessBuilder executeDotNet(File exe)

    void setupExecutable(Msbuild msbuild);

    String getFileParserPath()

}
