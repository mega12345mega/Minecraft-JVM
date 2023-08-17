package com.luneruniverse.minecraft.mcj.api.java.lang;

import com.luneruniverse.minecraft.mcj.api.MCJImplFor;
import com.luneruniverse.minecraft.mcj.api.MCJNativeImpl;

/**
 * This provides implementations for some bytecode instructions<br>
 * java.lang.BytecodeImpl is not a real class<br>
 * <strong>Warning:</strong> This is NOT designed to be used directly,
 * and relies on the behavior of some mcfunctions in the mcj namespace <strong>(very internal stuff)</strong>
 */
@MCJImplFor("mcj:java/lang/BytecodeImpl")
class BytecodeImpl {
	
	private static Object multianewarray(int countPtr, int numDimensions) {
		int count = multianewarray_getCount(countPtr);
		Object[] array = new Object[count];
		int nextNumDimensions = numDimensions - 1;
		if (nextNumDimensions == 0)
			return array;
		int nextCountPtr = countPtr + 1;
		for (int i = 0; i < count; i++) {
			array[i] = multianewarray(nextCountPtr, nextNumDimensions);
		}
		return array;
	}
	@MCJNativeImpl({"""
			function $(~METHOD_PATH~)/pointer_handler with storage mcj:data localvars.v0
			""", """
			# pointer_handler
			data modify storage mcj:data stack append from storage mcj:data stack[$(value)]
			"""})
	private static native int multianewarray_getCount(int countPtr);
	
}
