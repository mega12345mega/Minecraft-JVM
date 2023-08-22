package com.luneruniverse.minecraft.mcj.test;

import com.luneruniverse.minecraft.mcj.api.Dimension;
import com.luneruniverse.minecraft.mcj.api.MinecraftServer;
import com.luneruniverse.minecraft.mcj.api.Player;

public class OverworldPlayers {
	
	public static void main(String[] args) {
		Dimension overworld = new Dimension("minecraft:overworld");
		Player[] players = overworld.getPlayers(); // overworld.getPlayerNames() would be more efficient
		for (int i = 0; i < players.length; i++) {
			MinecraftServer.broadcast(players[i].getName());
		}
	}
	
}
