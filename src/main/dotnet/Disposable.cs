using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace ProjectFileParser
{
    internal class Disposable : IDisposable
    {
        private Action action;

        public static IDisposable Create(Action action)
        {
            return new Disposable { action = action };
        }

        public void Dispose()
        {
            action();
        }
    }
}
