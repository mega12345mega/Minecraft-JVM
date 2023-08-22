package com.luneruniverse.minecraft.mcj.api;

@MCJImplFor("mcj:")
public class Player {
	
//	private static final CompoundMap<String, Player> players = new CompoundMap<>();
	
	public static Player getPlayer(String name) {
		return new Player(name);
//		Player output = players.get(name);
//		if (output == null) {
//			output = new Player(name);
//			players.put(name, output);
//		}
//		return output;
	}
	
	public static Player[] getPlayers(String[] names) {
		Player[] output = new Player[names.length];
		for (int i = 0; i < names.length; i++) {
			output[i] = getPlayer(names[i]);
		}
		return output;
	}
	
	/**
	 * If /execute hasn't been used to move the position, and the caller is a player, then this will return the caller
	 * @return The nearest player
	 */
	public static Player getNearestPlayer() {
		return new Player(getNearestPlayerName());
	}
	/**
	 * If /execute hasn't been used to move the position, and the caller is a player, then this will return the caller
	 * @return The nearest player
	 */
	@MCJNativeImpl({"""
			execute as @p run function $(~get_name)
			""", """
			# get_name
			data modify storage mcj:data stack append value {}
			execute in mcj:data run setblock 0 0 0 chest
			execute in mcj:data run loot replace block 0 0 0 container.0 loot mcj:get_own_head
			execute in mcj:data run data modify storage mcj:data stack[-1].value set from block 0 0 0 Items[0].tag.SkullOwner.Name
			"""})
	public static native String getNearestPlayerName();
	
	private final String name;
	
	private Player(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	
	public Dimension getDimension() {
		return new Dimension(getDimensionName());
	}
	@MCJNativeImpl({"""
			function mcj:localvars/push_var_to_stack {index:"0"}
			function mcj:heap/getfield {name:"name"}
			function $(~name_handler) with storage mcj:data stack[-1]
			""", """
			# name_handler
			$data modify storage mcj:data stack[-1].value set from entity $(value) Dimension
			"""})
	public native String getDimensionName();
	
	public void tp(int x, int y, int z) {
		MinecraftServer.exec("tp " + name + " " + x + " " + y + " " + z);
	}
	public void tp(Dimension dimension, int x, int y, int z) {
		MinecraftServer.exec("execute in " + dimension.getName() + " run tp " + name + " " + x + " " + y + " " + z);
	}
	public void tp(String dimension, int x, int y, int z) {
		MinecraftServer.exec("execute in " + dimension + " run tp " + name + " " + x + " " + y + " " + z);
	}
	
}
