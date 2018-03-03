using Microsoft.Build.Construction;
using System;
using System.Reflection;

namespace ProjectFileParser
{
    // We have to set MSBUILD_EXE_PATH to some valid path for faking the BuildEnvironment on Mono
    // or else will throw PlatformUnsupportedException
    internal class MonoHack : IDisposable
    {
        private const string MSBUILD_EXE_PATH = "MSBUILD_EXE_PATH";
        private readonly string _msbuildExePath;
        private readonly bool _isMono;

        public MonoHack()
        {
            _isMono = Type.GetType("Mono.Runtime") != null;
            if (_isMono)
            {
                var nativeSharedMethod = typeof(SolutionFile).Assembly.GetType("Microsoft.Build.Shared.NativeMethodsShared");
                var isMonoField = nativeSharedMethod.GetField("_isMono", BindingFlags.Static | BindingFlags.NonPublic);
                isMonoField.SetValue(null, true);

                _msbuildExePath = Environment.GetEnvironmentVariable(MSBUILD_EXE_PATH);
                Environment.SetEnvironmentVariable(MSBUILD_EXE_PATH, typeof(Program).Assembly.Location);
            }
        }

        public void Dispose()
        {
            if (_isMono)
            {
                Environment.SetEnvironmentVariable(MSBUILD_EXE_PATH, _msbuildExePath);
            }
        }
    }
}
