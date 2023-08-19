package com.luneruniverse.minecraft.mcj.api;

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
	
	private final String name;
	
	private Player(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	
}
