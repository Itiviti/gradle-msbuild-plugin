# Gradle MsBuild Plugin [![Build status](https://ci.appveyor.com/api/projects/status/dx29ov4txa120okx/branch/master?svg=true)](https://ci.appveyor.com/project/gluck/gradle-msbuild-plugin/branch/master) [![Build Status](https://travis-ci.org/Ullink/gradle-msbuild-plugin.svg?branch=master)](https://travis-ci.org/Ullink/gradle-msbuild-plugin)

This plugin allows to compile an MsBuild project.
It also supports project file parsing, and some basic up-to-date checks to skip the build.
I doubt it'll work on anything else than .csproj files right now, but adding support for more will be easy.

Plugin applies the base plugin automatically, and hooks msbuild output folders into the clean task process.
Below tasks are provided by the plugin:

## msbuild

Prior to execution, this task will parse the provided project file and gather all its inputs (which are added to the task inputs):
- included files (Compile, EmbeddedResource, None, Content)
- ProjectReference (recursively gathers its inputs) // TODO: should use outputs instead ?
- References with an HintPath

OutputPath (e.g. bin/Debug) & Intermediary (e.g. obj/Debug) are set as output directories for the task.

Sample usage:

    buildscript {
        repositories {
          mavenCentral()
        }
    
        dependencies {
            classpath "com.ullink.gradle:gradle-msbuild-plugin:2.1"
        }
    }
    
    apply plugin:'msbuild'

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
      // optional task, you need to enable it if you want to patch your AssemblyInfo.cs/fs
      enable = true

      // defaults to project.version, but you might want to tweak it
      version = project.version + '.0.0'

      // defaults to above version
      fileVersion = version + '-Beta'
    }

# See also

[Gradle NuGet plugin](https://github.com/Ullink/gradle-nuget-plugin) - Allows to restore NuGet packages prior to building the projects with this plugin, and to pack&push nuget packages.

[Gradle NUnit plugin](https://github.com/Ullink/gradle-nunit-plugin) - Allows to execute NUnit tests from CI (used with this plugin to build the projects prior to UT execution)

[Gradle OpenCover plugin](https://github.com/Ullink/gradle-opencover-plugin) - Allows to execute the UTs through OpenCover for coverage reports.

You can see these 4 plugins in use on [ILRepack](https://github.com/gluck/il-repack) project ([build.gradle](https://github.com/gluck/il-repack/blob/master/build.gradle)).

# License

All these plugins are licensed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html) with no warranty (expressed or implied) for any purpose.
