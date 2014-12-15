package com.ullink

interface IExecutableResolver {
    static final ArrayList<String> KNOWN_VERSIONS = ["4.0", "3.5", "2.0"]
    void setupExecutable(Msbuild msbuild);
}
