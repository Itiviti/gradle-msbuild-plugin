Plugin changelog
====================

2.10
-------
* assemblyInfoPatcher is now automatically enabled if a version is provided
* added dotnetAssemblyFile / dotnetDebugFile / dotnetArtifacts properties on msbuild projects
* removed input/output for msbuild, it wasn't working properly


2.9
-------

* Added support for assemblyInfoPatcher task
 * task will hook before msbuild, and patch AssemblyInfo.cs/fs with provided version number
