package com.luneruniverse.minecraft.mcj.api;

@MCJImplFor("mcj:")
public class MinecraftServer {
	/**
	 * Execute a command as if it was listed in the mcfunction file;
	 * this means that it will be executed as the initial caller, unless <code>execute as</code> was used somewhere<br>
	 * Use {@link #execFunction(String)} if you are using <code>"function " + function</code> for performance
	 * @param cmd
	 * @see #execFunction(String)
	 */
	@MCJNativeImpl({"""
			function $(~_exec) with storage mcj:data localvars.v0
			""", """
			# _exec
			$$(value)
			"""})
	public static native void exec(String cmd);
	
	/**
	 * Executes a function<br>
	 * This is preferable to {@link #exec(String)} with <code>"function " + function</code> since a macro is used instead
	 * of string concatenation
	 * @param function The function to execute
	 * @see #exec(String)
	 */
	@MCJNativeImpl({"""
			function $(~_exec_function) with storage mcj:data localvars.v0
			""", """
			# _exec_function
			$function $(value)
			"""})
	public static native void execFunction(String function);
	
	/**
	 * Calls <code>mcj:terminate</code>, which stops the functions in their
	 * current state by hitting the maxCommandChainLength limit<br>
	 * <strong>Warning:</strong> DO NOT USE if maxCommandChainLength is very large
	 */
	@MCJNativeImpl("""
			function mcj:terminate
			""")
	public static native void terminate();
	
	/**
	 * Escape a string, so it may be used in places like tellraw
	 * @param msg The string to escape
	 * @return The original string, with quotes and backslashes escaped
	 */
	@MCJNativeImpl({"""
			execute in mcj:data run function $(~_escape)
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
	
	public static Player[] getPlayers() {
		String[] names = new String[0];
		getPlayers_getList(names);
		return Player.getPlayers(names);
	}
	public static String[] getPlayerNames() {
		String[] names = new String[0];
		getPlayers_getList(names);
		return names;
	}
	@MCJNativeImpl({"""
			execute as @a run function $(~add_player) with storage mcj:data localvars.v0
			""", """
			# add_player
			$data modify storage mcj:data heap.v$(value).value append value {}
			execute in mcj:data run setblock 0 0 0 chest
			execute in mcj:data run loot replace block 0 0 0 container.0 loot mcj:get_own_head
			$execute in mcj:data run data modify storage mcj:data heap.v$(value).value[-1].value set from block 0 0 0 Items[0].tag.SkullOwner.Name
			"""})
	private static native void getPlayers_getList(String[] names);
	
	@MCJNativeImpl("""
			function mcj:heap/free with storage mcj:data localvars.v0
			""")
	public static native void free(Object obj);
}
