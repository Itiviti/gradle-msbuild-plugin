# Gradle MsBuild Plugin

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
            classpath "com.ullink.gradle:gradle-msbuild-plugin:1.1"
        }
    }
    
    apply plugin:'msbuild'

    msbuild {
      // mandatory
      projectFile = 'my-project.csproj'
      
      // MsBuild project name (/p:Project=...)
      projectName = project.name
      
      // Verbosity (/v:detailed, by default uses gradle logging level)
      verbosity = 'detailed'
      
      // targets to execute (/t:Clean;Rebuild, no default)
      targets = ['Clean', 'Rebuild']
      
      // Below values can override settings from the project file
      
      // overrides project OutputPath
      destinationDir = 'build/msbuild/bin"
      
      // overrides project IntermediaryOutputPath
      intermediateDir = 'build/msbuild/obj"
      
      // Generates XML documentation file (from javadoc through custom DocLet)
      generateDoc = false
      
      // Other msbuild options can be set:
      // loggerAssembly, generateDoc, debugType, optimize, debugSymbols, configuration, platform, defineConstants ...
      
      // you can also provide properties by name (/p:SomeProperty=Value)
      parameters.SomeProperty = 'Value'
    }

# License

All these plugins are licensed under the [Creative Commons — CC0 1.0 Universal](http://creativecommons.org/publicdomain/zero/1.0/) license with no warranty (expressed or implied) for any purpose.