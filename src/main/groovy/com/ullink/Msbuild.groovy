package com.ullink
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.nio.file.Files
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.gradle.api.GradleException
import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.os.OperatingSystem


class Msbuild extends ConventionTask {

    @Input @Optional
    String version
    @Input @Optional
    String msbuildDir
    @Input @Optional
    def solutionFile
    @Input @Optional
    def projectFile
    @Input @Optional
    String loggerAssembly
    @Input @Optional
    Boolean optimize
    @Input @Optional
    Boolean debugSymbols
    @Input @Optional
    String debugType
    @Input @Optional
    String platform
    @Input @Optional
    def destinationDir
    @Input @Optional
    def intermediateDir
    @Input @Optional
    Boolean generateDoc
    @Input @Optional
    String projectName
    @Input @Optional
    String configuration
    @Input @Optional
    List<String> defineConstants
    @Input @Optional
    List<String> targets
    @Input @Optional
    String verbosity
    @Input @Optional
    Map<String, Object> parameters = [:]
    @Input @Optional
    Map<String, ProjectFileParser> allProjects = [:]
    @Input @Optional
    String executable
    @Internal
    ProjectFileParser projectParsed
    @Internal
    IExecutableResolver resolver
    @Internal
    Boolean parseProject = true

    Msbuild() {
        description = 'Executes MSBuild on the specified project/solution'
        resolver =
                OperatingSystem.current().windows ? new MsbuildResolver() : new XbuildResolver()

        conventionMapping.map "solutionFile", {
            project.file(project.name + ".sln").exists() ? project.name + ".sln" : null
        }
        conventionMapping.map "projectFile", {
            project.file(project.name + ".csproj").exists() ? project.name + ".csproj" : null
        }
        conventionMapping.map "projectName", { project.name }
    }

    @Internal
    boolean isSolutionBuild() {
        projectFile == null && getSolutionFile() != null
    }

    @Internal
    boolean isProjectBuild() {
        solutionFile == null && getProjectFile() != null
    }

    @Internal
    def getRootedProjectFile() {
        project.file(getProjectFile())
    }

    @Internal
    def getRootedSolutionFile() {
        project.file(getSolutionFile())
    }

    @Internal
    Map<String, ProjectFileParser> getProjects() {
        resolveProject()
        allProjects
    }

    @Internal
    ProjectFileParser getMainProject() {
        if (resolveProject()) {
            projectParsed
        } else {
            logger.warn "Main project was resolved to null due to a parse error. The .sln file might be missing or incorrectly named."
            throw new GradleException("Failed to resolve main project. Make sure the name of the .sln file matches the one of the repository")
        }
    }

    def parseProjectFile(def file) {
        logger.info "Parsing file $file ..."
        if (!file.exists()) {
            throw new GradleException("Project/Solution file $file does not exist")
        }
        File tempDir = Files.createTempDirectory(temporaryDir.toPath(), 'ProjectFileParser').toFile()
        tempDir.deleteOnExit()

        this.class.getResourceAsStream('/META-INF/ProjectFileParser.zip').withCloseable  {
            ZipInputStream zis = new ZipInputStream(it)
            ZipEntry ze = zis.getNextEntry()
            while (ze != null) {
                String fileName = ze.getName()
                if (ze.isDirectory()) {
                    File subFolder = new File(tempDir, fileName)
                    subFolder.mkdir()
                    ze = zis.getNextEntry()
                    continue
                }
                File target = new File(tempDir, fileName)
                target.newOutputStream().leftShift(zis).close()
                ze = zis.getNextEntry()
            }
        }

        def executable = new File(tempDir, 'ProjectFileParser.exe')
        def builder = resolver.executeDotNet(executable)
        builder.command().add(file.toString())
        def proc = builder.start()

        def stderrBuffer = new StringBuffer()
        proc.consumeProcessErrorStream(stderrBuffer)
        def stdoutBuffer = new StringBuffer()
        proc.consumeProcessErrorStream(stdoutBuffer)

        try {
            def initPropertiesJson = JsonOutput.toJson(getInitProperties())
            logger.debug "Sending ${initPropertiesJson} to ProjectFileParser"
            proc.out.leftShift(initPropertiesJson).close()
            return new JsonSlurper().parseText(new FilterJson(proc.in).toString())
        }
        finally {
            def hasErrors = proc.waitFor() != 0
            logger.debug "Output from ProjectFileParser: "
            stdoutBuffer.eachLine { line ->
                 logger.debug line
            }
            stderrBuffer.eachLine { line ->
                if (hasErrors)
                    logger.error line
                else
                    logger.debug line
            }
            if (hasErrors) {
                throw new GradleException('Project file parsing failed')
            }
        }
    }

    boolean resolveProject() {
        if (projectParsed == null && parseProject) {
            if (isSolutionBuild()) {
                def rootSolutionFile = getRootedSolutionFile()
                def result = parseProjectFile(rootSolutionFile)
                allProjects = result.collectEntries { [it.key, new ProjectFileParser(msbuild: this, eval: it.value)] }
                def projectName = getProjectName()
                if (projectName == null || projectName.isEmpty()) {
                    parseProject = false
                } else {
                    projectParsed = allProjects[projectName]
                    if (projectParsed == null) {
                        parseProject = false
                        logger.warn "Project ${projectName} not found in solution"
                    }
                }
            } else if (isProjectBuild()) {
                def rootProjectFile = getRootedProjectFile()
                def result = parseProjectFile(rootProjectFile)
                allProjects = result.collectEntries {[it.key, new ProjectFileParser(msbuild: this, eval: it.value)]}
                projectParsed = allProjects.values().first()
                 if (!projectParsed) {
                    logger.warn "Parsed project ${rootProjectFile} is null (not a solution / project build)"
                }
            }
        }

        projectParsed != null
    }

    void setTarget(String s) {
        targets = [s]
    }

    @TaskAction
    def build() {
        project.exec {
            commandLine = getCommandLineArgs()
        }
    }

    @Internal
    def getCommandLineArgs() {
        resolver.setupExecutable(this)

        if (msbuildDir == null) {
            throw new GradleException("$executable not found")
        }
        def commandLineArgs = resolver.executeDotNet(new File(msbuildDir, executable)).command()

        commandLineArgs += '/nologo'

        if (isSolutionBuild()) {
            commandLineArgs += getRootedSolutionFile()
        } else if (isProjectBuild()) {
            commandLineArgs += getRootedProjectFile()
        }

        if (loggerAssembly) {
            commandLineArgs += '/l:' + loggerAssembly
        }
        if (targets && !targets.isEmpty()) {
            commandLineArgs += '/t:' + targets.join(';')
        }

        String verb = getMSVerbosity(verbosity)
        if (verb) {
            commandLineArgs += '/v:' + verb
        }

        def cmdParameters = getInitProperties()

        cmdParameters.each {
            if (it.value) {
                commandLineArgs += '/p:' + it.key + '=' + it.value
            }
        }

        def extMap = getExtensions()?.getExtraProperties()?.getProperties()
        if (extMap != null) {
            commandLineArgs += extMap.collect { k, v ->
                v ? "/$k:$v" : "/$k"
            }
        }

        commandLineArgs
    }

    String getMSVerbosity(String verbosity) {
        if (verbosity) return verbosity
        if (logger.debugEnabled) return 'detailed'
        if (logger.infoEnabled) return 'normal'
        return 'minimal' // 'quiet'
    }

    @Internal
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
