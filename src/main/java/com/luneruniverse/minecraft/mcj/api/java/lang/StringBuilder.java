package com.luneruniverse.minecraft.mcj.api.java.lang;

import com.luneruniverse.minecraft.mcj.api.MCJImplFor;
import com.luneruniverse.minecraft.mcj.api.MCJNativeImpl;

/**
 * This allows for string concatenation via str1 + str2<br>
 * <strong>Warning:</strong> This is NOT designed to be used directly, and only supports {@link #append(String)}
 */
@MCJImplFor("java/lang/StringBuilder")
public class StringBuilder {
	
	private String value;
	
	public StringBuilder(String value) {
		this.value = value;
	}
	
	// Old technique - doesn't work with double quotes or backslashes
	/*@MCJNativeImpl({"""
			function $(~METHOD_PATH~)/pointer_handler with storage mcj:data localvars.v0
			data modify storage mcj:data stack append from storage mcj:data localvars.v0
			""", """
			# pointer_handler
			$data modify storage mcj:data concat_a set from storage mcj:data heap.v$(value).value.value
			data modify storage mcj:data concat_b set from storage mcj:data localvars.v1.value
			function $(~METHOD_PATH~)/concat with storage mcj:data
			$data modify storage mcj:data heap.v$(value).value.value set from storage mcj:data concat_a
			""", """
			# concat
			$data modify storage mcj:data concat_a set value "$(concat_a)$(concat_b)"
			"""})*/
	
	@MCJNativeImpl({"""
			function $(~METHOD_PATH~)/pointer_handler with storage mcj:data localvars.v0
			data modify storage mcj:data stack append from storage mcj:data localvars.v0
			""", """
			# pointer_handler
			$data modify storage mcj:data concat.a set from storage mcj:data heap.v$(value).value.value
			data modify storage mcj:data concat.b set from storage mcj:data localvars.v1.value
			execute in mcj:data run function $(~METHOD_PATH~)/concat
			$data modify storage mcj:data heap.v$(value).value.value set from storage mcj:data concat.a
			""", """
			# concat
			setblock 0 0 0 air
			setblock 0 0 0 oak_sign{front_text:{messages:['{"nbt":"concat.a","interpret":"false","storage":"mcj:data"}','{"text":""}','{"text":""}','{"text":""}']}}
			data modify storage mcj:data concat.a set string block 0 0 0 front_text.messages[0] 9 -2
			
			setblock 0 0 0 air
			setblock 0 0 0 oak_sign{front_text:{messages:['{"nbt":"concat.b","interpret":"false","storage":"mcj:data"}','{"text":""}','{"text":""}','{"text":""}']}}
			data modify storage mcj:data concat.b set string block 0 0 0 front_text.messages[0] 9 -2
			
			function $(~METHOD_PATH~)/_concat with storage mcj:data concat
			""", """
			# _concat
			$data modify storage mcj:data concat.a set value "$(a)$(b)"
			"""})
	public native java.lang.StringBuilder append(String value);
	
	@MCJNativeImpl("""
			execute store result score cmp_a mcj_data run data get storage mcj:data localvars.v1.value
			execute if score cmp_a mcj_data matches 1 run data modify storage mcj:data localvars.v1 set value {value:"true"}
			execute if score cmp_a mcj_data matches 0 run data modify storage mcj:data localvars.v1 set value {value:"false"}
			function $(~CLASS_PATH~)/method_append_6f35b7d97976f8fce498b2b46d9d1483/entry
			""")
	public native java.lang.StringBuilder append(boolean value);
	
//	@MCJNativeImpl("""
//			function $(~CLASS_PATH~)/method_append_6f35b7d97976f8fce498b2b46d9d1483/entry
//			""")
//	public native java.lang.StringBuilder append(char value); TODO
	
	@MCJNativeImpl("""
			function $(~CLASS_PATH~)/method_append_6f35b7d97976f8fce498b2b46d9d1483/entry
			""")
	public native java.lang.StringBuilder append(int value);
	
//	@MCJNativeImpl("""
//			function $(~CLASS_PATH~)/method_append_6f35b7d97976f8fce498b2b46d9d1483/entry
//			""")
//	public native java.lang.StringBuilder append(long value); TODO
	
//	@MCJNativeImpl("""
//			function $(~CLASS_PATH~)/method_append_6f35b7d97976f8fce498b2b46d9d1483/entry
//			""")
//	public native java.lang.StringBuilder append(float value); TODO
	
//	@MCJNativeImpl("""
//			function $(~CLASS_PATH~)/method_append_6f35b7d97976f8fce498b2b46d9d1483/entry
//			""")
//	public native java.lang.StringBuilder append(double value); TODO
	
	@MCJNativeImpl({"""
			function mcj:localvars/push_var_to_stack {index:"0"}
			function mcj:heap/getfield {name:"value"}
			function $(~METHOD_PATH~)/free with storage mcj:data localvars.v0
			""", """
			# free
			$data remove storage mcj:data heap.v$(value)
			"""})
	@Override
	public native String toString();
	
}
