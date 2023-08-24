package com.luneruniverse.minecraft.mcj.api.java.lang;

import com.luneruniverse.minecraft.mcj.api.MCJImplFor;
import com.luneruniverse.minecraft.mcj.api.MCJNativeImpl;

@MCJImplFor("mcj:java/lang/String")
@Deprecated // To make it clear when you use the wrong one
public class String {
	
	public static java.lang.String valueOf(Object obj) {
		if (obj == null)
			return "null";
		return (java.lang.String) obj;
	}
	
	public static java.lang.String valueOf(boolean b) {
		return b ? "true" : "false";
	}
	
	@MCJNativeImpl("""
			data modify storage mcj:data stack append value {}
			data modify storage mcj:data stack[-1].value set string storage mcj:data localvars.v0.value
			""")
	public static native java.lang.String valueOf(int i);
	
}
