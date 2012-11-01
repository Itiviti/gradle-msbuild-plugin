package com.ullink

import com.sun.jna.platform.win32.Advapi32Util
import com.sun.jna.platform.win32.WinReg
import com.sun.jna.platform.win32.WinReg.HKEY

class Registry {
    static boolean supported
    static HKEY HKEY_LOCAL_MACHINE
    
    static {
        String prev = System.properties['jna.boot.library.name']
        System.properties['jna.boot.library.name'] = 'jnadispatchmsbuild'
        try {
            HKEY_LOCAL_MACHINE = WinReg.HKEY_LOCAL_MACHINE
            supported = true
        } catch (Error e) {
            supported = false
        }
        if (prev) {
            System.properties['jna.boot.library.name'] = prev
        } else {
            System.properties.remove('jna.boot.library.name')
        }
    }
    
    static HKEY getHkey(String name) {
        if (!supported) {
            return null
        }
        try {
            return WinReg[name]
        } catch (Exception e) {
            return null
        }
    }
    
    static String getValue(HKEY hkey, String node, String name) {
        if (!supported) {
            return null
        }
        try {
            return Advapi32Util.registryGetStringValue(hkey, node, name)
        } catch (Exception e) {
            return null
        }
    }
}
