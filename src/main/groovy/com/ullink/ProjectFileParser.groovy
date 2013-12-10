package com.ullink

import groovy.util.slurpersupport.GPathResult
import org.gradle.api.Project
import org.gradle.api.logging.Logger

// http://msdn.microsoft.com/en-us/library/5dy88c2e.aspx
class ProjectFileParser {
    Msbuild msbuild
    String projectFile
    String version
    Map<String, Object> globalProperties = new HashMap<String, Object>()
    Map<String, Object> properties = new HashMap<String, Object>()
    Map<String, List<Map>> items = new HashMap<String, List<Map>>()
    Closure initProperties
    
    void readProjectFile() {
        def file = findImportFile(project.projectDir, projectFile)
        if (initProperties) {
            globalProperties = initProperties(file)
        }
        def name = getFileNameWithoutExtension(file)
        logger.info("Reading project ${name} with properties: ${globalProperties}")
        msbuild.allProjects[name] = this
        importProjectFile(file)
    }
    
    Object getProp(String key) {
        if (globalProperties[key]) {
            return globalProperties[key];
        }
        return properties[key];
    }
    
    Project getProject() {
        msbuild.project
    }
    
    File findProjectFile(String str) {
        findImportFile(getProp('MSBuildProjectFullPath').parentFile, str)
    }
    
    Logger getLogger() {
        project.logger
    }
    
    File getProjectPropertyPath(String path) {
        if (getProp(path)) {
            findProjectFile(getProp(path))
        }
    }
    
    String getFileNameWithoutExtension(def file) {
        String s = file instanceof File ? file.name : new File(file.toString()).name
        s.replaceFirst(~/(?<=.)\.[^\.]+$/, '')
    }
    
    Collection<File> getOutputDirs() {
        ['IntermediateOutputPath', 'OutputPath'].findResults { getProjectPropertyPath(it) }
    }
    
    Collection<File> gatherInputs() {
        Set<File> ret = [findImportFile(project.projectDir, projectFile)]
        ['Compile','EmbeddedResource','None','Content'].each {
            items[it].each {
                // TODO: support wildcards
                ret += findProjectFile(it.Include)
            }
        }
        items.ProjectReference.each {
            def file = findProjectFile(it.Include).canonicalPath
            def name = getFileNameWithoutExtension(file)
            def parser
            if (msbuild.allProjects[name]) {
                parser = msbuild.allProjects[name]
            } else {
                parser = new ProjectFileParser(msbuild: msbuild, projectFile: file, initProperties: initProperties)
                parser.readProjectFile()
            }
            ret.addAll parser.gatherInputs()
        }
        items.Reference.each {
            if (it.HintPath) {
                ret += findProjectFile(it.HintPath)
            }
        }
        ret
    }
    
    static File findImportFile(File baseDir, String s) {
        def file = new File(s)
        if (!file.isAbsolute()) {
            file = new File(baseDir, s)
        }
        file.canonicalFile
    }
    
    void importProjectFile(File file) {
        def prevFile = globalProperties.MSBuildThisFileFullPath
        globalProperties.MSBuildThisFileFullPath = file
        globalProperties.MSBuildThisFile = file.name
        globalProperties.MSBuildThisFileDirectory = file.parentFile
        globalProperties.MSBuildThisFileDirectoryNoRoot = file.parentFile.canonicalPath.substring(3)
        globalProperties.MSBuildThisFileName = getFileNameWithoutExtension(file)
        globalProperties.MSBuildThisFileExtension = file.name.replaceFirst(~/^.+(?=\.)/, '')
        def xml = new XmlSlurper().parse(file)
        if (version == null) {
            version = !xml.@ToolsVersion.isEmpty() ? xml.@ToolsVersion.toString() : "4.0"
        }
        if (globalProperties.MSBuildToolsVersion == null) {
            globalProperties.BuildingInsideVisualStudio = ''
            globalProperties.MSBuildToolsVersion = version
            globalProperties.MSBuildToolsPath = globalProperties.MSBuildBinPath = Registry.getValue(Registry.HKEY_LOCAL_MACHINE, Msbuild.MSBUILD_PREFIX+version, Msbuild.MSBUILD_TOOLS_PATH)
            if (System.getenv()['ProgramFiles(x86)']) {
                globalProperties.MSBuildExtensionsPath32 = System.getenv()['ProgramFiles(x86)']+/\MSBuild/
                globalProperties.MSBuildExtensionsPath64 = System.getenv()['ProgramFiles']+/\MSBuild/
                globalProperties.MSBuildExtensionsPath = System.getenv()['ProgramFiles']+/\MSBuild/
                globalProperties.MSBuildProgramFiles32 = System.getenv()['ProgramFiles(x86)']
            } else {
                globalProperties.MSBuildExtensionsPath32 = System.getenv()['ProgramFiles']+/\MSBuild/
                globalProperties.MSBuildExtensionsPath = System.getenv()['ProgramFiles']+/\MSBuild/
                globalProperties.MSBuildProgramFiles32 = System.getenv()['ProgramFiles']
            }
            globalProperties.MSBuildProjectFullPath = file
            globalProperties.MSBuildProjectFile = file.name
            globalProperties.MSBuildProjectDirectory = globalProperties.MSBuildThisFileDirectory
            globalProperties.MSBuildProjectDirectoryNoRoot = globalProperties.MSBuildThisFileDirectoryNoRoot
            globalProperties.MSBuildProjectName = globalProperties.MSBuildThisFileName
            globalProperties.MSBuildProjectExtension = globalProperties.MSBuildThisFileExtension
            // TODO
            // MSBuildStartupDirectory
            // MSBuildProjectDefaultTargets
            // MSBuildOverrideTasksPath
            // MSBuildNodeCount
            // MSBuildLastTaskResult
        }
        eachFilterCondition xml.children(), {
            if (it.name() == "Choose") {
                if (!it.children().any {
                    if (it.name == "When" && (it.@Condition.isEmpty() || isConditionTrue(it.@Condition))) {
                        it.children().each {
                            parsePropertiesAndItems it
                        }
                        return true
                    }
                    false
                }) {
                    it.children().any {
                        if (it.name == "Otherwise") {
                            it.children().each {
                                parsePropertiesAndItems it
                            }
                            return true
                        }
                        false
                    }
                }
                eachFilterCondition it.children(), {
                    parsePropertiesAndItems it
                }
            } else if (it.name() == "ImportGroup") {
                eachFilterCondition it.children(), {
                    parseImport file, it
                }
            } else if (it.name() == "ItemDefinitionGroup") {
                // TODO default properties for items
            } else if (it.name() == "Target") {
                // ignored
            } else if (it.name() == "UsingTask") {
                // ignored
            } else if (it.name() == "ProjectExtensions") {
                // ignored
            } else if (parseImport(file, it)) {
            } else if (parsePropertiesAndItems(it)) {
            } else {
                logger.debug "UNSUPPORTED: "+it.name()
            }
        }
        if (prevFile) {
            globalProperties.MSBuildThisFileFullPath = prevFile
            globalProperties.MSBuildThisFile = prevFile.name
            globalProperties.MSBuildThisFileDirectory = prevFile.parentFile
            globalProperties.MSBuildThisFileDirectoryNoRoot = prevFile.parentFile.canonicalPath.substring(3)
            globalProperties.MSBuildThisFileName = prevFile.name.replaceFirst(~/(?<=.)\.[^\.]+$/, '')
            globalProperties.MSBuildThisFileExtension = prevFile.name.replaceFirst(~/^.+(?=\.)/, '')
        }
    }
    
    boolean parseImport(File file, GPathResult node) {
        if (node.name() == "Import") {
            String str = eval(node.@Project)
            if (str.endsWith('\\*')) {
                File folder = findImportFile(file.parentFile, str.substring(0, str.length()-2))
                if (folder.isDirectory()) {
                    folder.eachFile {
                        importProjectFile(it)
                    }
                }
			} else {
				def projFile = new File(str);
					parseImportWithWildcard(projFile.isAbsolute() ? projFile : new File(file.parentFile, str))
			
            }
            return true
        }
        false
    }
	
	
	void parseImportWithWildcard(File file) {

		def parent = file.getParent()
		def fileName = file.getName()
		def ant = new AntBuilder();

		def scanner = ant.fileScanner {
			fileset(dir:parent, casesensitive:false) {
				include(name:fileName)
				type(type:'file')
			}
		}
		
		scanner.each{
			importProjectFile(findImportFile(new File(parent), it.getName()))
		}

	}
    
    boolean parsePropertiesAndItems(GPathResult node) {
        if (node.name() == "PropertyGroup") {
            eachFilterCondition node.children(), {
                // see http://blogs.msdn.com/b/msbuild/archive/2006/10/05/_2f00_p-property-values-are-immutable-_2d00_-_2800_sort-of_2900__2e00__2e00__2e00__2e00_.aspx
                // Global properties are immutable
                if (!globalProperties[it.name()]) {
                    logger.debug("[${globalProperties.MSBuildProjectFile}] Set property ${it.name()} = ${eval(it)}")
                    properties[it.name()] = eval(it)
                }
            }
            return true
        } else if (node.name() == "ItemGroup") {
            eachFilterCondition node.children(), {
                if (!items[it.name()])
                    items[it.name()] = []
                items[it.name()] += mapItem it
            }
            return true
        }
        false
    }
    
    Map mapItem(GPathResult node) {
        def ret = [Include: eval(node.@Include)]
        if (!node.@Exclude.isEmpty())
            ret.Exclude = eval(node.@Exclude)
        eachFilterCondition node.children(), {
            ret[it.name()] = eval(it)
        }
        ret
    }
    
    void eachFilterCondition(GPathResult nodes, Closure close) {
        nodes.each {
            if (!it.@Condition.isEmpty() && !isConditionTrue(it.@Condition, it.name())) {
                return;
            }
            close(it);
        }
    }
    
    
    // poor's man msbuild expression syntax parser, can later replace with a full-fledged one
    // see for instance mono parsing:
    // https://github.com/mono/mono/tree/master/mcs/class/Microsoft.Build.Engine/Microsoft.Build.BuildEngine
    
    String getPropertyValue(String str, String contextName = null) {
        def repl = getProp(str)
        if (repl != null) {
            return repl
        }
        repl = System.getenv()[str]
        if (repl != null) {
            return repl
        }
        def matcher = str =~ /Registry:(.*?)\\(.*)@(.*)/
        if (matcher.matches()) {
            repl = Registry.getValue(Registry.getHkey(matcher.group(1)), matcher.group(2), matcher.group(3))
        }
        if (repl) {
            return repl
        }
        if (contextName != str) {
            logger.debug "Property not found: " + str
        }
        ''
    }
    
    def TRUE = ' true '
    def FALSE = ' false '
    boolean isConditionTrue(condition, String contextName = null) {
        
        // TODO:
        // abc=='ABC' -> true
        // $(Foo)=='false' -> false
        // $(Foo)=='true' -> false
        
        String str = condition.toString()
        str = str.replaceAll(~/\s*\$\((.*?)\)\s*==\s*'([^']*)'\s*/, {
            it[2].equalsIgnoreCase(getPropertyValue(it[1], contextName)) ? TRUE : FALSE
        })
        str = str.replaceAll(~/\s*'([^']*)'\s*==\s*\$\((.*?)\)\s*/, {
            it[2].equalsIgnoreCase(getPropertyValue(it[1], contextName)) ? TRUE : FALSE
        })
        str = str.replaceAll(~/\s*\$\((.*?)\)\s*!=\s*'([^']*)'\s*/, {
            it[2].equalsIgnoreCase(getPropertyValue(it[1], contextName)) ? FALSE : TRUE
        })
        str = str.replaceAll(~/\s*'([^']*)'\s*!=\s*\$\((.*?)\)\s*/, {
            it[2].equalsIgnoreCase(getPropertyValue(it[1], contextName)) ? FALSE : TRUE
        })
        str = eval(str, contextName)
        str = str.replaceAll(~/\s*(?i)Exists\s*\(\s*'([^']*)'\s*\)\s*/, {
            new File(it[1]).exists() ? TRUE : FALSE
        })
        str = str.replaceAll(~/\s*(?i)HasTrailingSlash\s*\(\s*'([^']*)'\s*\)\s*/, {
            it[1].endsWith('\\') ? TRUE : FALSE
        })
        def init = str
        while (true) {
            init = str
            str = str.replaceAll(~/\s*(?i)'([^']*)'\s*==\s*'\1'\s*/, TRUE)
            str = str.replaceAll(~/\s*'[^']*'\s*==\s*'[^']*'\s*/, FALSE)
            str = str.replaceAll(~/\s*(?i)'([^']*)'\s*!=\s*'\1'\s*/, FALSE)
            str = str.replaceAll(~/\s*'[^']*'\s*!=\s*'[^']*'\s*/, TRUE)
            str = str.replaceAll(~/\s*(?i)false\s+and\s+(false|true)\s*/, FALSE)
            str = str.replaceAll(~/\s*(?i)true\s+and\s+false\s*/, FALSE)
            str = str.replaceAll(~/\s*(?i)true\s+and\s+true\s*/, TRUE)
            str = str.replaceAll(~/\s*(?i)true\s+or\s+(false|true)\s*/, TRUE)
            str = str.replaceAll(~/\s*(?i)false\s+or\s+true\s*/, TRUE)
            str = str.replaceAll(~/\s*(?i)false\s+or\s+false\s*/, FALSE)
            str = str.replaceAll(~/\s*(?i)\(\s*(false|true)\s*\)\s*/, ' $1 ')
            str = str.replaceAll(~/\s*(?i)!\s*false\s*/, TRUE)
            str = str.replaceAll(~/\s*(?i)!\s*true\s*/, FALSE)
            if (str == init)
                break
        }
        if (str ==~ /^(?i)\s*false\s*$/) {
            return false
        } else if (str ==~ /^(?i)\s*true\s*$/) {
            return true;
        }
        logger.info "Unevaluable expression: ${condition} -> ${str} -> ?"
        false
    }
    
    String eval(input, String contextName = null) {
        
        // TODO:
        // %XX replacements
        // .Contains()
        // .Replace()
        // $([MSBuild]::Escape($([System.IO.Path]::GetFullPath(`$([System.IO.Path]::Combine(`$(MSBuildProjectDirectory)`, `$(OutDir)`))`))))
        // $([System.IO.Path]::Combine('$([System.IO.Path]::GetTempPath())','$(TargetFrameworkMoniker).AssemblyAttributes$(DefaultLanguageSourceExtension)'))
        // !Exists('@(_DebugSymbolsIntermediatePath)')
        // $(Project.Replace(%27,'').Replace(...).Replace(...))
        // $(ProjectNameCleaned.Contains('test'))
        // @(_OutputPathItem->'%(FullPath)')
        // <_MSBuildProjectReferenceExistent Include="@(_MSBuildProjectReference)" Condition="Exists('%(Identity)')"/>
        
        
        String str = input instanceof String ? input.toString() : input
        while (true) {
            def init = str
            str = str.replaceAll(~/\s*\r?\n\s*/,'')
            str = str.replaceAll(~/\$\(([^\(\)\[\]]*?)\)/, {
                getPropertyValue(it[1], contextName)
            })
            str = str.replace('$([System.IO.Path]::GetTempPath())', System.getProperty('java.io.tmpdir'))
            str = str.replaceAll(~/\$\(\[System\.IO\.Path\]::Combine\(\s*([`'])(.*?)\1\s*,\s*([`'])(.*?)\3\s*\)\s*\)/, {
                def ret = new File(it[2], it[4]).path
                return it[4].endsWith(File.separator) ? ret+File.separator : ret
            })
            str = str.replaceAll(~/\$\(\[System\.IO\.Path\]::GetFullPath\(\s*([`'])(.*?)\1\s*\)\s*\)/, {
                def ret = new File(it[2]).canonicalPath
                return it[2].endsWith(File.separator) ? ret+File.separator : ret
            })
            /*
            str = str.replaceAll(~/\$\(([^\(\)\[\]]*?)\.Replace\(\s*([`'])(.*?)\2\s*,\s*([`'])(.*?)\4\s*\)\s*\)/, {
                def ret = getPropertyValue(it[1], contextName)
                ret.replace(it[3],it[5])
            })
            str = str.replaceAll(~/\$\(([^\(\)\[\]]*?)\.Contains\(\s*([`'])(.*?)\2\s*\)\s*\)/, {
                def ret = getPropertyValue(it[1], contextName)
                ret.contains(it[3]) ? TRUE : FALSE
            })
            str = str.replaceAll(~/\@\(([^\(\)\[\]]*?)->([`'])(.*?)\2\s*\)/, {
                String ret = ''
                String content = it[3]
                items[it[1]].each {
                    String cur = content
                    cur = cur.replace('%(RelativeDir)','')
                    cur = cur.replace('%(FullPath)','')
                    cur = cur.replace('%(OutputResource)','')
                    cur = cur.replace('%(WithCulture)','')
                    cur = cur.replace('%(Version)','')
                    cur = cur.replace('%(Identity)','')
                    cur = cur.replace('%(Filename)','')
                    cur = cur.replace('%(Extension)','')
                    if (ret)
                        ret += ';'
                    ret += cur
                }
            })
            */
            str = str.replaceAll(~/\$\(\[MSBuild\]::Escape\(\s*(.*?)\s*\)\s*\)/, {
                String toEscape = it[1]
                char[] charArrayToEscape = [ '%', '*', '?', '@', '$', '(', ')', ';', '\'' ]
                String charsToEscape  = '%*?@$();\''
                //if (toEscape.any(charsToEscape.indexOf(it) != -1))
                charArrayToEscape.each {
                    toEscape = toEscape.replace(it.toString(), '%'+Integer.toHexString((int)it) )
                }
                toEscape
            })

            if (str == init)
                break
        }
        // TODO
        str
    }
    

}
