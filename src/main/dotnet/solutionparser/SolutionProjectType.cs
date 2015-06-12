namespace ProjectFileParser.solutionparser
{
    /// <summary>
    /// Replication of the internal enum Microsoft.Build.Construction.SolutionProjectType
    /// </summary>
    public enum SolutionProjectType
    {
        Unknown,
        KnownToBeMSBuildFormat,
        SolutionFolder,
        WebProject,
        WebDeploymentProject,
        EtpSubProject
    }
}
