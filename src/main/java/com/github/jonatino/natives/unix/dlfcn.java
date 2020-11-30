package com.github.jonatino.natives.unix;

import com.sun.jna.*;

/**
 * Created by jonathan on 06/01/16.
 */
public final class dlfcn {

	static {
		Native.register(NativeLibrary.getInstance("c"));
		Native.register(NativeLibrary.getInstance("dl"));
	}

	public static native long dlopen(String filename, int flags);
	
	public static native int dlclose(long handle);

	public static native long dlsym(long handle, String symbol);	

	public static int RTLD_LAZY = 0x1;
	public static int RTLD_NOW = 0x2;
	public static int RTLD_LOCAL = 0x4;
	public static int RTLD_GLOBAL = 0x8;
	public static int RTLD_NOLOAD = 0x10;
	public static int RTLD_NODELETE = 0x80;
	public static int RTLD_NEXT = -1;
	public static int RTLD_DEFAULT = -2;

}