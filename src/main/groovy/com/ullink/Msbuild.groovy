package com.ullink

import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.StopActionException
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskOutputs
import org.gradle.internal.os.OperatingSystem

class Msbuild extends ConventionTask {

    String version
    String msbuildDir
    def solutionFile
    def projectFile
    String loggerAssembly
    Boolean optimize
    Boolean debugSymbols
    String debugType
    String platform
    def destinationDir
    def intermediateDir
    Boolean generateDoc
    String projectName
    String configuration
    List<String> defineConstants
    ProjectFileParser parser
    List<String> targets
    String verbosity
    Map<String, Object> parameters = [:]
    Map<String, ProjectFileParser> projects = [:]
    String executable
    
    Msbuild() {
        description = 'Executes MSBuild on the specified project/solution'

        IExecutableResolver resolver =
            OperatingSystem.current().windows ? new MsbuildResolver() : new XbuildResolver()

        resolver.setupExecutable(this)

        if (msbuildDir == null) {
            throw new StopActionException("$executable not found")
        }
        conventionMapping.map "solutionFile", { project.file(project.name + ".sln").exists() ? project.name + ".sln" : null }
        conventionMapping.map "projectFile", { project.file(project.name + ".csproj").exists() ? project.name + ".csproj" : null }
        conventionMapping.map "projectName", { project.name }
        inputs.files {
            if (isSolutionBuild()) {
                project.file(getSolutionFile())
            } else if (isProjectBuild()) {
                project.file(getProjectFile())
            }
        }
        inputs.files {
            if (resolveProject()) {
                parser.gatherInputs()
            }
        }
        outputs.upToDateWhen {
            resolveProject()
        }
        TaskOutputs output = outputs.dir {
            if (resolveProject()) {
                parser.getOutputDirs().collect {
                    project.fileTree(dir: it, excludes: ['*.vshost.exe', '*.vshost.exe.*'] )
                }
            }
        }
        output
    }

    boolean isSolutionBuild() {
        projectFile == null && getSolutionFile() != null
    }
    
    boolean isProjectBuild() {
        solutionFile == null && getProjectFile() != null
    }
    
    ProjectFileParser getMainProject() {
        if (resolveProject()) {
            parser
        }
    }
    
    boolean resolveProject() {
        if (parser == null) {
            if (isSolutionBuild()) {
                def solParser = new SolutionFileParser(
                        msbuild: this,
                        solutionFile: getSolutionFile(),
                        properties: getInitProperties())
                solParser.readSolutionFile()
                parser = solParser.initProjectParser
            } else if (isProjectBuild()) {
                parser = new ProjectFileParser(
                        msbuild: this,
                        projectFile: getProjectFile(),
                        initProperties: { getInitProperties() })
                parser.readProjectFile()
            }
            if (parser != null && logger.debugEnabled) {
                logger.debug "Resolved Msbuild properties:"
                parser.properties.sort({ a,b->a.key <=> b.key }).each({ logger.debug it.toString() })
                logger.debug "Resolved Msbuild items:"
                parser.items.sort({ a,b->a.key <=> b.key }).each({ logger.debug it.toString() })
            }
        }
        parser != null
    }
    
    void setTarget(String s) {
        targets = [s]
    }
    
    @TaskAction
    def build() {
        
        def commandLineArgs = [ new File(msbuildDir, executable) ]

        commandLineArgs += '/nologo'

        if (getSolutionFile()) {
            commandLineArgs += project.file(getSolutionFile())
        } else if (getProjectFile()) {
            commandLineArgs += project.file(getProjectFile())
        }
        
        if (loggerAssembly) {
            commandLineArgs += '/l:'+loggerAssembly
        }
        if (targets && !targets.isEmpty()) {
            commandLineArgs += '/t:'+targets.join(';')
        }
        String verb = verbosity
        if (!verb) {
            if (logger.debugEnabled) {
                verb = 'detailed'
            } else if (logger.infoEnabled) {
                verb = 'normal'
            } else {
                verb = 'minimal' // 'quiet'
            }
        }
        if (verb) {
            commandLineArgs += '/v:'+verb
        }

        def cmdParameters = getInitProperties()
        
        cmdParameters.each {
            if (it.value) {
                commandLineArgs += '/p:' + it.key + '='+ it.value
            }
        }
        
        project.exec {
            commandLine = commandLineArgs
        }
    }

    Map getInitProperties() {
        def cmdParameters = new HashMap<String, Object>()
        if (parameters != null) {
            cmdParameters.putAll(parameters)
        }
        cmdParameters.Project = getProjectName()
        cmdParameters.GenerateDocumentation = generateDoc
        cmdParameters.DebugType = debugType
        cmdParameters.Optimize = optimize
        cmdParameters.DebugSymbols = debugSymbols
        cmdParameters.OutputPath = destinationDir == null ? null : project.file(destinationDir)
        cmdParameters.IntermediateOutputPath = intermediateDir == null ? null : project.file(intermediateDir)
        cmdParameters.Configuration = configuration
        cmdParameters.Platform = platform
        if (defineConstants != null && !defineConstants.isEmpty()) {
            cmdParameters.DefineConstants = defineConstants.join(';')
        }
        def iter = cmdParameters.iterator()
        while (iter.hasNext()) {
            Map.Entry<String, Object> entry = iter.next()
            if (entry.value == null) {
                iter.remove()
            } else if (entry.value instanceof File) {
                entry.value = entry.value.path
            } else if (!entry.value instanceof String) {
                entry.value = entry.value.toString()
            }
        }
        ['OutDir', 'OutputPath', 'BaseIntermediateOutputPath', 'IntermediateOutputPath', 'PublishDir'].each {
            if (cmdParameters[it] && !cmdParameters[it].endsWith('\\')) {
                cmdParameters[it] += '\\'
            }
        }
        return cmdParameters
    }
}
