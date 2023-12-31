package com.luneruniverse.minecraft.mcj.test;

import com.luneruniverse.minecraft.mcj.api.Array;
import com.luneruniverse.minecraft.mcj.api.EventManager;
import com.luneruniverse.minecraft.mcj.api.MCJEntrypoint;
import com.luneruniverse.minecraft.mcj.api.MCJImplFor;
import com.luneruniverse.minecraft.mcj.api.MCJNativeImpl;
import com.luneruniverse.minecraft.mcj.api.MinecraftServer;
import com.luneruniverse.minecraft.mcj.api.Player;

@MCJImplFor("glow_effect:")
public class GlowEffect {
	
	private static final Array<String> players = new Array<>();
	
	public static void main(String[] args) {
		EventManager.registerEventHandler(EventManager.TICK_EVENT, "glow_effect:tick");
		MinecraftServer.exec("""
				tellraw @a {"text":"Glow Effect: ","color":"gold","extra":[\
				{"text":"[Enable]","color":"green","clickEvent":{"action":"run_command","value":"/function glow_effect:toggle {enabled:1b}"}},\
				{"text":" "},\
				{"text":"[Disable]","color":"red","clickEvent":{"action":"run_command","value":"/function glow_effect:toggle {enabled:0b}"}}]}""");
	}
	
	@MCJEntrypoint(value = "tick")
	public static void tick() {
		for (int i = 0; i < players.size(); i++) {
			giveEffect(players.get(i));
		}
	}
	@MCJEntrypoint(value = "toggle")
	public static void toggle(boolean enabled) {
		String player = Player.getNearestPlayerName();
		boolean currentlyEnabled = players.contains(player);
		if (currentlyEnabled != enabled) {
			if (enabled)
				players.add(player);
			else {
				players.remove(player);
				MinecraftServer.exec("effect clear " + player + " minecraft:glowing");
			}
		}
	}
	@MCJNativeImpl({"""
			function $(~player_handler) with storage mcj:data localvars.v0
			""", """
			# player_handler
			$effect give $(value) minecraft:glowing 1 0 true
			"""})
	private static native void giveEffect(String player);
	
}
