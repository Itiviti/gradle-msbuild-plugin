# gradle-msbuild-plugin changelog

## 2.22
### Fixed
* Upgrade download-task to fix expires header parsing

## 2.21
### Fixed
* Use msbuild on Linux if available

## 2.20
### Fixed
* Resolving msbuild failure when msbuild version has been defined in build script

## 2.19

### Added
* added support for updating assembly info for `VB.Net` projects in AssemblyInfo.vb files.

### Changed
* changed the `AssemblyInfoVersionPatcher` so that it will not overwrite values in the AssemblyInfo files if the value being set is blank.
* changed NuGet.exe dependency from `2.8.6` to version `4.4.0`
* support resolving projects for ToolsVersion `15.0`
* minimum required .Net Framework version for now is `v4.6`

## 2.18

### Added
* ProjectFileParser now parses "NugetDependencies" as an array of dependencies on the project.
  Available properties for a dependency are "Id", "Version", "TargetFramework", "IsDevelopmentDependency", "RequireReinstallation", "VersionConstraint"

## 2.17
* support resolving msbuild for version >= 15.0 by vswhere

## 2.16

### Fixed
* Registry keys are found when looking for msbuild versions. The issue is related to the JNA version used in other plugins.

## 2.15

### Fixed
* although the recommended use is to build through the solution, parsing the project file only now works (again, regression since 2.13)
* support for mono '-api' suffixed platform files

## 2.14

### Fixed
* solutionFile / projectFile now work also if relative (non-rooted)
 paths are provided, as it used to before 2.13


## 2.13

### Added
* support ToolsVersion upto `14.0`

### Changed
* ProjectFileParser now requires .Net Framework 4.5.2


## 2.12

### Changed
* `AssemblyInfoVersionPatcher` task now also supports setting AssemblyInformationalVersion


## 2.10

### Changed
* assemblyInfoPatcher is now automatically enabled if a version is provided
* removed input/output for msbuild, it wasn't working properly

### Added
* added dotnetAssemblyFile / dotnetDebugFile / dotnetArtifacts properties on msbuild projects


## 2.9

### Added
* Added support for assemblyInfoPatcher task
 * task will hook before msbuild, and patch AssemblyInfo.cs/fs with provided version number
