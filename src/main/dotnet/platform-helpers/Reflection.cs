namespace ProjectFileParser.platform_helpers
{
    internal class Reflection
    {
        public static void Invoke<U>(U instance, string method, params object[] args)
        {
            Invoke<object, U>(instance, method, args);
        }

        public static T Invoke<T, U>(U instance, string method, params object[] args)
        {
            return (T)typeof(U).GetMethod(method, System.Reflection.BindingFlags.Instance | System.Reflection.BindingFlags.NonPublic).Invoke(instance, args);
        }

        public static T GetProperty<T, U>(U instance, string property)
        {
            return (T)typeof(U).GetProperty(property, System.Reflection.BindingFlags.Instance | System.Reflection.BindingFlags.NonPublic).GetValue(instance, new object[0]);
        }

        public static T GetField<T, U>(U instance, string field)
        {
            return (T)typeof(U).GetField(field, System.Reflection.BindingFlags.Instance | System.Reflection.BindingFlags.NonPublic).GetValue(instance);
        }

        public static void SetField<T, U>(U instance, string field, T value)
        {
            typeof(U).GetField(field, System.Reflection.BindingFlags.Instance | System.Reflection.BindingFlags.NonPublic).SetValue(instance, value);
        }
    }
}
