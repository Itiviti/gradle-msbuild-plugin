using System;
using System.Collections.Generic;
using System.Linq;

namespace ProjectFileParser
{
    /// <summary>
    /// Helper that do reflection over Microsoft.Build.Shared.FileUtilities.ItemSpecModifiers
    /// to retrieve all const fields
    /// </summary>
    public static class FileUtilities
    {
        static readonly Type s_FileUtilities;
        public static readonly IEnumerable<string> ItemSpecModifiers;
        private static readonly Type s_ItemSpecModifiers;

        static FileUtilities()
        {
            s_FileUtilities = Type.GetType("Microsoft.Build.Shared.FileUtilities, Microsoft.Build, Version=4.0.0.0, Culture=neutral, PublicKeyToken=b03f5f7f11d50a3a", false, false);
            s_ItemSpecModifiers = s_FileUtilities.GetNestedType("ItemSpecModifiers", System.Reflection.BindingFlags.NonPublic | System.Reflection.BindingFlags.Static);
            ItemSpecModifiers = s_ItemSpecModifiers.GetFields(System.Reflection.BindingFlags.NonPublic | System.Reflection.BindingFlags.Static).Where(field => field.IsLiteral && !field.IsInitOnly).Select(field => field.GetValue(null) as string).ToList();
        }
    }
}
