package com.luneruniverse.minecraft.mcj.api;

/**
 * This provides implementations for some bytecode instructions<br>
 * <strong>Warning:</strong> This is NOT designed to be used directly,
 * and relies on the behavior of some mcfunctions in the mcj namespace <strong>(very internal stuff)</strong>
 */
@MCJImplFor("mcj:")
class BytecodeImpl {
	
	private static Object multianewarray(int countPtr, int countPtrPtr, int numDimensions) {
		int count = multianewarray_getCount(countPtr, countPtrPtr);
		Object[] array = new Object[count];
		numDimensions--;
		if (numDimensions == 0)
			return array;
		countPtr++;
		for (int i = 0; i < count; i++) {
			array[i] = multianewarray(countPtr, countPtrPtr, numDimensions);
		}
		return array;
	}
	@MCJNativeImpl({"""
			data modify storage mcj:data localvars.v1.countPtr set from storage mcj:data localvars.v0.value
			function $(~METHOD_PATH~)/pointer_handler with storage mcj:data localvars.v1
			""", """
			# pointer_handler
			$data modify storage mcj:data stack append from storage mcj:data intstack[$(value)].value[$(countPtr)]
			"""})
	private static native int multianewarray_getCount(int countPtr, int countPtrPtr);
	
}
