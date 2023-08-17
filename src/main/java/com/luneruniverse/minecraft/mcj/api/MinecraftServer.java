package com.luneruniverse.minecraft.mcj.api;

public class MinecraftServer {
	/**
	 * Execute a command as if it was listed in the mcfunction file;
	 * this means that it will be executed as the initial caller, unless <code>execute as</code> was used somewhere
	 * @param cmd
	 */
	@MCJNativeImpl({"""
			function $(~METHOD_PATH~)/_exec with storage mcj:data localvars.v0
			""", """
			# _exec
			$$(value)
			"""})
	public static native void exec(String cmd);
	
	/**
	 * Escape a string, so it may be used in places like tellraw
	 * @param msg The string to escape
	 * @return The original string, with quotes and backslashes escaped
	 */
	@MCJNativeImpl({"""
			execute in mcj:data run function $(~METHOD_PATH~)/_escape
			""", """
			# _escape
			setblock 0 0 0 air
			setblock 0 0 0 oak_sign{front_text:{messages:['{"nbt":"localvars.v0.value","interpret":"false","storage":"mcj:data"}','{"text":""}','{"text":""}','{"text":""}']}}
			data modify storage mcj:data stack append value {}
			data modify storage mcj:data stack[-1].value set string block 0 0 0 front_text.messages[0] 9 -2
			"""})
	public static native String escape(String msg);
	
	/**
	 * Display <code>[username] msg</code> in chat
	 * @param username The username (doesn't have to be an actual player)
	 * @param msg The message
	 */
	@MCJExecute("in mcj:data")
	public static void sayAs(String username, String msg) {
		exec("summon armor_stand 0 0 0 {CustomName:\"{\\\"text\\\":\\\"" + escape(escape(username)) + "\\\"}\",Invisible:1b,Marker:1b,Tags:[\"mcj:console\"]}");
		exec("execute as @e[type=armor_stand,nbt={Tags:[\"mcj:console\"]}] run say " + msg);
		exec("kill @e[type=armor_stand,nbt={Tags:[\"mcj:console\"]}]");
	}
	
	public static void broadcast(String msg) {
		exec("tellraw @a {\"text\":\"" + escape(msg) + "\"}");
	}
}
