package com.luneruniverse.minecraft.mcj.test;

import com.luneruniverse.minecraft.mcj.api.MCJImplFor;
import com.luneruniverse.minecraft.mcj.api.MinecraftServer;

@MCJImplFor("hello_world:")
public class HelloWorld {
	
	public static void main(String[] args) {
		boolean toggle = false;
		for (int i = 0; i < 5; i++) {
			MinecraftServer.broadcast("[Server] Hello World #" + i + " - " + toggle);
			toggle = !toggle;
		}
	}
	
}
