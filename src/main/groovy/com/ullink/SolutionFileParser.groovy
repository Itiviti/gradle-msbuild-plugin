package com.ullink

import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project
import org.gradle.tooling.BuildException;

class SolutionFileParser {
	
	static interface KnownProjectTypeGuid
	{
		String VisualBasic = "{F184B08F-C81C-45F6-A57F-5ABD9991F28F}";
		String CSharp = "{FAE04EC0-301F-11D3-BF4B-00C04F79EFBC}";
		String JSharp = "{E6FDF86B-F3D1-11D4-8576-0002A516ECE8}";
		String FSharp = "{F2A71F9B-5D33-465A-A702-920D77279786}";
		String SolutionFolder = "{2150E333-8FDC-42A3-9474-1A3956D46DE8}";
		String VisualC = "{8BC9CEB8-8B4A-11D0-8D11-00A0C91BC942}";
		String Setup = "{54435603-DBB4-11D2-8724-00A0C9A8B90C}";
		String WebProject = "{E24C65DC-7377-472B-9ABA-BC803B73C61A}";
	}
	
	Msbuild msbuild
	String solutionFile
	String version
	Map<String, String> properties = [:]
	
	final Section root = new Section()
	Collection<MSBuildProject> projects
	MSBuildProject initProject
	ProjectFileParser initProjectParser
	
	Project getProject() {
		msbuild.project
	}
    
	void readSolutionFile() {
		if (!properties.Configuration) {
			properties.Configuration = 'Debug' // or the first configuration defined in solution ?
		}
		if (!properties.Platform) {
			properties.Platform = 'Any CPU'
		}
		read(project.file(solutionFile).newReader("UTF-8"));
		projects = root.children.findAll { it.name == 'Project' } .collect { it.toMSBuildProject() }
		project.logger.info "Solution projects: ${projects.collect { it.name }}"

        def buildableProjects = projects.findAll { getProjectConfigurationProperty(it, 'Build.0') }
        if (buildableProjects.empty)
            throw new InvalidUserDataException("No project found in solution for Configuration|Platform = ${properties.Configuration}|${properties.Platform}");
        initProject = msbuild.getProjectName() ? buildableProjects.find { it.name == msbuild.getProjectName()} : buildableProjects.first()
        if (!initProject)
            throw new InvalidUserDataException("Selected init project '${msbuild.getProjectName()}' isn't part of specified Configuration|Platform = ${properties.Configuration}|${properties.Platform}");

        project.logger.info "Solution buildable projects for ${properties.Configuration}|${properties.Platform}: ${buildableProjects.collect { it.name }}"
        project.logger.info "Init project will be: ${initProject.name}"
        buildableProjects.each {
            if (!msbuild.allProjects[it.name]) {
                def projectParser = new ProjectFileParser(msbuild: msbuild, initProperties: { getInitProperties(it) }, projectFile: ProjectFileParser.findImportFile(project.file(solutionFile).parentFile, it.path).canonicalPath)
                projectParser.readProjectFile()
            }
        }
        initProjectParser = msbuild.allProjects[initProject.name]
	}
	
	def getInitProperties(File file) {
		MSBuildProject proj = projects.find { file.canonicalPath == ProjectFileParser.findImportFile(project.file(solutionFile).parentFile, it.path).canonicalPath }
		def ret = properties.clone()
		if (proj) {
			String activeCfg = getProjectConfigurationProperty(proj, 'ActiveCfg')
			if (activeCfg) {
				String[] split = activeCfg.split(/\|/)
				assert split.length == 2
				ret.Configuration = split[0]
				ret.Platform = split[1]
			}
		}
		if (ret.Platform == 'Any CPU') {
			// http://connect.microsoft.com/VisualStudio/feedback/details/503935/msbuild-inconsistent-platform-for-any-cpu-between-solution-and-project
			ret.Platform = 'AnyCPU'
		}
		ret
	}
	
	String getProjectConfigurationProperty(MSBuildProject proj, String prop) {
		projectConfigurationPlatforms[proj.guid + '.' + properties.Configuration + '|' + properties.Platform + '.' + prop]
	}
	
	def getProjectConfigurationPlatforms() {
		def ret = new HashMap()
		root.children.findAll { it.name == 'Global' } .collectMany { it.children.findAll { it.name == 'GlobalSection' && it.arg == 'ProjectConfigurationPlatforms' } } .each { ret.putAll(it.properties) }
		ret
	}
	
	void read(BufferedReader reader, Closure closure) {
		String line;
		while ((line = reader.readLine()) != null) {
			line = line.trim()
			if (line.startsWith('#') || line.isEmpty()) {
				continue
			}
			closure(line)
		}
	}
	
	boolean read(BufferedReader reader) {
		Section section = root
		try {
			read(reader) {
				def matcher = (it =~ /^(\w+)\s*(?:\(\s*(.*?)\s*\)\s*=\s*(.*?))?\s*$/)
				if (matcher.matches()) {
					def name = matcher.group(1)
					def arg = matcher.group(2)
					def others = matcher.group(3)
					if (name.startsWith('End')) {
						assert !arg
						assert !others
						assert section.name == name.substring(3)
						section = section.parent
					} else {
						section = section.child(name, arg, others)
					}
				} else if ((matcher = (it =~ /^(.*?)\s*=\s*(.*?)$/)).matches()) {
					section.properties[matcher.group(1)] = matcher.group(2)
				}
			}
		} finally {
			reader.close()
		}
	}
	
	static class Section {
		Section parent
		String name, arg, others
		final List<Section> children = new ArrayList<Section>()
		final Map<String, String> properties = new HashMap<String, String>()

		Section child(String name, String arg, String others) {
			def child = new Section(name: name, arg: arg, others: others, parent: this)
			children.add child
			child
		}
		
		MSBuildProject toMSBuildProject() {
			String[] o = split(others)
			assert o.length == 3
			new MSBuildProject(name: unQuote(o[0]), guid: unQuote(o[2]), path: unQuote(o[1]), typeGuid: unQuote(arg))
		}
	}
	
	static class MSBuildProject {
		String name, guid, path, typeGuid
	}
	
	static String unQuote(String input) {
		if (input.startsWith('"') && input.endsWith('"')) {
			return input.substring(1, input.length()-1)
		}
		input
	}

	static String[] split(String others) {
		return others.split(/\s*,\s*/)
	}
}
