package com.luneruniverse.minecraft.mcj.api.java.lang;

import com.luneruniverse.minecraft.mcj.api.MCJImplFor;
import com.luneruniverse.minecraft.mcj.api.MCJNativeImpl;

/**
 * This allows for string concatenation via str1 + str2<br>
 * <strong>Warning:</strong> This is NOT designed to be used directly, and only supports {@link #append(java.lang.String)}
 */
@MCJImplFor("mcj:java/lang/StringBuilder")
@Deprecated // To make it clear when you use the wrong one
public class StringBuilder {
	
	private java.lang.String value;
	
	public StringBuilder() {
		this.value = "";
	}
	public StringBuilder(java.lang.String value) {
		this.value = value;
	}
	
	// Old technique - doesn't work with double quotes or backslashes
	/*@MCJNativeImpl({"""
			function $(~pointer_handler) with storage mcj:data localvars.v0
			data modify storage mcj:data stack append from storage mcj:data localvars.v0
			""", """
			# pointer_handler
			$data modify storage mcj:data concat_a set from storage mcj:data heap.v$(value).value.value
			data modify storage mcj:data concat_b set from storage mcj:data localvars.v1.value
			function $(~concat) with storage mcj:data
			$data modify storage mcj:data heap.v$(value).value.value set from storage mcj:data concat_a
			""", """
			# concat
			$data modify storage mcj:data concat_a set value "$(concat_a)$(concat_b)"
			"""})*/
	
	@MCJNativeImpl({"""
			function $(~pointer_handler) with storage mcj:data localvars.v0
			data modify storage mcj:data stack append from storage mcj:data localvars.v0
			""", """
			# pointer_handler
			$data modify storage mcj:data concat.a set from storage mcj:data heap.v$(value).value.value
			data modify storage mcj:data concat.b set value 0
			execute store result score cmp_a mcj_data run data modify storage mcj:data concat.b set from storage mcj:data localvars.v1.value
			execute if score cmp_a mcj_data matches 0 run data modify storage mcj:data concat.b set value "null"
			execute in mcj:data run function $(~concat)
			$data modify storage mcj:data heap.v$(value).value.value set from storage mcj:data concat.a
			""", """
			# concat
			setblock 0 0 0 air
			setblock 0 0 0 oak_sign{front_text:{messages:['{"nbt":"concat.a","interpret":"false","storage":"mcj:data"}','{"text":""}','{"text":""}','{"text":""}']}}
			data modify storage mcj:data concat.a set string block 0 0 0 front_text.messages[0] 9 -2
			
			setblock 0 0 0 air
			setblock 0 0 0 oak_sign{front_text:{messages:['{"nbt":"concat.b","interpret":"false","storage":"mcj:data"}','{"text":""}','{"text":""}','{"text":""}']}}
			data modify storage mcj:data concat.b set string block 0 0 0 front_text.messages[0] 9 -2
			
			function $(~_concat) with storage mcj:data concat
			""", """
			# _concat
			$data modify storage mcj:data concat.a set value "$(a)$(b)"
			"""})
	public native java.lang.StringBuilder append(java.lang.String value);
	
	@MCJNativeImpl("""
			execute store result score cmp_a mcj_data run data get storage mcj:data localvars.v1.value
			execute if score cmp_a mcj_data matches 1 run data modify storage mcj:data localvars.v1 set value {value:"true"}
			execute if score cmp_a mcj_data matches 0 run data modify storage mcj:data localvars.v1 set value {value:"false"}
			function $(method~appendUnsafe(Ljava/lang/String;)Ljava/lang/StringBuilder;)
			""")
	public native java.lang.StringBuilder append(boolean value);
	
//	@MCJNativeImpl("""
//			function $(method~append(Ljava/lang/String;)Ljava/lang/StringBuilder;)
//			""")
//	public native java.lang.StringBuilder append(char value); TODO
	
	@MCJNativeImpl("""
			function $(method~appendUnsafe(Ljava/lang/String;)Ljava/lang/StringBuilder;)
			""")
	public native java.lang.StringBuilder append(int value);
	
//	@MCJNativeImpl("""
//			function $(method~append(Ljava/lang/String;)Ljava/lang/StringBuilder;)
//			""")
//	public native java.lang.StringBuilder append(long value); TODO
	
//	@MCJNativeImpl("""
//			function $(method~append(Ljava/lang/String;)Ljava/lang/StringBuilder;)
//			""")
//	public native java.lang.StringBuilder append(float value); TODO
	
//	@MCJNativeImpl("""
//			function $(method~append(Ljava/lang/String;)Ljava/lang/StringBuilder;)
//			""")
//	public native java.lang.StringBuilder append(double value); TODO
	
	@MCJNativeImpl({"""
		function $(~pointer_handler) with storage mcj:data localvars.v0
		data modify storage mcj:data stack append from storage mcj:data localvars.v0
		""", """
		# pointer_handler
		$data modify storage mcj:data concat.a set from storage mcj:data heap.v$(value).value.value
		data modify storage mcj:data concat.b set from storage mcj:data localvars.v1.value
		execute in mcj:data run function $(~concat)
		$data modify storage mcj:data heap.v$(value).value.value set from storage mcj:data concat.a
		""", """
		# concat
		setblock 0 0 0 air
		setblock 0 0 0 oak_sign{front_text:{messages:['{"nbt":"concat.a","interpret":"false","storage":"mcj:data"}','{"text":""}','{"text":""}','{"text":""}']}}
		data modify storage mcj:data concat.a set string block 0 0 0 front_text.messages[0] 9 -2
		
		function $(~_concat) with storage mcj:data concat
		""", """
		# _concat
		$data modify storage mcj:data concat.a set value "$(a)$(b)"
		"""})
	private native java.lang.StringBuilder appendUnsafe(java.lang.String value);
	
	@MCJNativeImpl({"""
			function mcj:localvars/push_var_to_stack {index:"0"}
			function mcj:heap/getfield {name:"value"}
			function $(~free) with storage mcj:data localvars.v0
			""", """
			# free
			$data remove storage mcj:data heap.v$(value)
			"""})
	@Override
	public native java.lang.String toString();
	
}
