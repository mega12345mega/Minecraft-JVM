package com.luneruniverse.minecraft.mcj.test;

import com.luneruniverse.minecraft.mcj.api.Dimension;
import com.luneruniverse.minecraft.mcj.api.MCJEntrypoint;
import com.luneruniverse.minecraft.mcj.api.MCJNativeImpl;
import com.luneruniverse.minecraft.mcj.api.Player;

public class Teleporting {
	
	public static void main(String[] args) {
		Dimension data = new Dimension("mcj:data");
		Dimension overworld = new Dimension("minecraft:overworld");
		Player[] players = overworld.getPlayers();
		for (int i = 0; i < players.length; i++) {
			players[i].tp(data, 0, 0, 0);
		}
	}
	
	@MCJEntrypoint("overworld")
	public static void tpOverworld() {
		Player.getNearestPlayer().tp("minecraft:overworld", 0, 0, 0);
	}
	
	@MCJEntrypoint("data")
	public static void tpData() {
		Player.getNearestPlayer().tp("mcj:data", 0, 0, 0);
	}
	
	@MCJEntrypoint("tp")
	public static void tp(String dimension) {
		Player.getNearestPlayer().tp(dimension, 0, 0, 0);
	}
	
	@MCJEntrypoint("native/overworld")
	@MCJNativeImpl("""
			execute as @p in minecraft:overworld run tp 0 0 0
			""")
	public static native void tpOverworldNative();
	
	@MCJEntrypoint("native/data")
	@MCJNativeImpl("""
			execute as @p in mcj:data run tp 0 0 0
			""")
	public static native void tpDataNative();
	
	@MCJEntrypoint("native/tp")
	@MCJNativeImpl({"""
			function $(~dimension_handler) with storage mcj:data localvars.v0
			""", """
			# dimension_handler
			$execute as @p in $(value) run tp 0 0 0
			"""})
	public static native void tpNative(String dimension);
	
}
