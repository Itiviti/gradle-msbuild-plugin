### 📢 DEPRECATED
[gradle-dotnet-plugin](https://github.com/Itiviti/gradle-dotnet-plugin) is now available for building projects via dotnet command line tool chains. The plugin supports running nunit, code coverage, nuget restore and push. 

# Gradle MsBuild Plugin [![Build status](https://ci.appveyor.com/api/projects/status/dx29ov4txa120okx/branch/master?svg=true)](https://ci.appveyor.com/project/gluck/gradle-msbuild-plugin/branch/master) [![Build Status](https://travis-ci.org/Itiviti/gradle-msbuild-plugin.svg?branch=master)](https://travis-ci.org/Itiviti/gradle-msbuild-plugin)

This plugin allows to compile an MsBuild project.
It also supports project file parsing, and some basic up-to-date checks to skip the build.
I doubt it'll work on anything else than .csproj files right now, but adding support for more will be easy.

Plugin applies the base plugin automatically, and hooks msbuild output folders into the clean task process.
Below tasks are provided by the plugin:

## Prerequisites
* .Net Framework 4.6

## msbuild

Prior to execution, this task will parse the provided project file and gather all its inputs (which are added to the task inputs):
- included files (Compile, EmbeddedResource, None, Content)
- ProjectReference (recursively gathers its inputs) // TODO: should use outputs instead ?
- References with a HintPath

OutputPath (e.g. bin/Debug) & Intermediary (e.g. obj/Debug) are set as output directories for the task.

To apply the plugin:

```groovy
// Starting from gradle 2.1
plugins {
  id 'com.ullink.msbuild' version '3.9'
}
```

or
```groovy
buildscript {
    repositories {
      mavenCentral()
    }

    dependencies {
        classpath 'com.ullink.gradle:gradle-msbuild-plugin:3.9'
    }
}
apply plugin:'com.ullink.msbuild'
```

and configure by:

```groovy
msbuild {
  // mandatory (one of those)
  solutionFile = 'my-solution.sln'
  projectFile = file('src/my-project.csproj')

  // MsBuild project name (/p:Project=...)
  projectName = project.name

  // Verbosity (/v:detailed, by default uses gradle logging level)
  verbosity = 'detailed'

  // targets to execute (/t:Clean;Rebuild, no default)
  targets = ['Clean', 'Rebuild']


  // MsBuild resolution
  // it support to search the msbuild tools from vswhere (by default it searches the latest)
  version = '15.0'
  // or define the exact msbuild dir explicity
  msbuildDir = 'C:\\Program Files (x86)\\Microsoft Visual Studio\\2017\\BuildTools\\MSBuild\\15.0\\bin'


  // Below values can override settings from the project file

  // overrides project OutputPath
  destinationDir = 'build/msbuild/bin'

  // overrides project IntermediaryOutputPath
  intermediateDir = 'build/msbuild/obj'

  // Generates XML documentation file (from javadoc through custom DocLet)
  generateDoc = false

  // Other msbuild options can be set:
  // loggerAssembly, generateDoc, debugType, optimize, debugSymbols, configuration, platform, defineConstants ...

  // you can also provide properties by name (/p:SomeProperty=Value)
  parameters.SomeProperty = 'Value'

  // Or, if you use built-in msbuild parameters that aren't directly available here,
  // you can take advantage of the ExtensionAware interface
  ext["flp1"] = "LogFile=" + file("${project.name}.errors.log").path + ";ErrorsOnly;Verbosity=diag"
}

assemblyInfoPatcher {
  // mandatory if you want to patch your AssemblyInfo.cs/fs/vb

  // replaces the AssemblyVersion value in your AssemblyInfo file.
  // when explicitly set to blank, AssemblyVersion will not be updated and will keep the existing value in your AssemblyInfo file
  // TODO: not yet normalized, beware than .Net version must be X.Y.Z.B format, with Z/B optionals
  version = project.version + '.0.0'

  // replaces the AssemblyFileVersion value in your AssemblyInfo file.
  // defaults to above version, fewer restrictions on the format
  // when explicitly set to blank, AssemblyFileVersion will not be updated and will keep the existing value in your AssemblyInfo file
  fileVersion = version + '-Beta'

  // replaces the AssemblyInformationalVersion value in your AssemblyInfo file.
  // defaults to above version, fewer restrictions on the format
  // when explicitly set to blank, AssemblyInformationalVersion will not be updated and will keep the existing value in your AssemblyInfo file
  informationalVersion = version + '-Beta'

  // replaces the AssemblyDescription in the your AssemblyInfo file.
  // when set to blank (default), AssemblyDescription will not be updated and will keep the existing value in your AssemblyInfo file
  description = 'My Project Description'

  // default to msbuild main project (of solution)
  projects = [ 'MyProject1', 'MyProject2' ]
}
```

## Custom tasks

You can create custom `msbuild` and `assemblyInfoPatcher` tasks like so:

```groovy
import com.ullink.Msbuild
import com.ullink.AssemblyInfoVersionPatcher

task compileFoo(type: Msbuild) {
    projectFile = "Foo.vcxproj"
    // Other properties
}

task versionPatchFoo(type: AssemblyInfoVersionPatcher) {
    projects = ['Foo']
    // Other properties
}
```

# See also

[Gradle NuGet plugin](https://github.com/Ullink/gradle-nuget-plugin) - Allows to restore NuGet packages prior to building the projects with this plugin, and to pack&push nuget packages.

[Gradle NUnit plugin](https://github.com/Ullink/gradle-nunit-plugin) - Allows to execute NUnit tests from CI (used with this plugin to build the projects prior to UT execution)

[Gradle OpenCover plugin](https://github.com/Ullink/gradle-opencover-plugin) - Allows to execute the UTs through OpenCover for coverage reports.

You can see these 4 plugins in use on [ILRepack](https://github.com/gluck/il-repack) project ([build.gradle](https://github.com/gluck/il-repack/blob/master/build.gradle)).

# License

All these plugins are licensed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html) with no warranty (expressed or implied) for any purpose.
