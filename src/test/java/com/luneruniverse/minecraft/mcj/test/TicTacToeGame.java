package com.luneruniverse.minecraft.mcj.test;

import com.luneruniverse.minecraft.mcj.api.EventManager;
import com.luneruniverse.minecraft.mcj.api.MCJEntrypoint;
import com.luneruniverse.minecraft.mcj.api.MCJImplFor;
import com.luneruniverse.minecraft.mcj.api.MCJNativeImpl;
import com.luneruniverse.minecraft.mcj.api.MinecraftServer;

@MCJImplFor("tic_tac_toe:")
public class TicTacToeGame {
	
	private static final TicTacToeGame game = new TicTacToeGame();
	
	public static void main(String[] args) {
		MinecraftServer.exec("fill -1 1 5 1 3 5 minecraft:oak_button[face=wall,facing=north]");
		MinecraftServer.exec("fill -2 0 5 2 4 5 minecraft:bedrock replace minecraft:red_concrete");
		MinecraftServer.exec("fill -2 0 5 2 4 5 minecraft:bedrock replace minecraft:green_concrete");
		EventManager.registerEventHandler(EventManager.TICK_EVENT, "tic_tac_toe:tick");
	}
	
	@MCJEntrypoint("tick")
	private static void staticTick() {
		game.tick();
	}
	
	
	private boolean greenTurn;
	private int[][] grid;
	private boolean won;
	
	public TicTacToeGame() {
		greenTurn = false;
		grid = new int[3][3];
		won = false;
	}
	
	public void tick() {
		if (won)
			return;
		if (handleButtons(greenTurn ? "minecraft:green_concrete" : "minecraft:red_concrete")) {
			greenTurn = !greenTurn;
			
			int winner = 0;
			for (int x = 0; x < 3; x++) {
				boolean allRed = true;
				boolean allGreen = true;
				for (int y = 0; y < 3; y++) {
					int value = getGridValue(1 - x, 3 - y);
					grid[x][y] = value;
					if (value != 1)
						allRed = false;
					if (value != 2)
						allGreen = false;
				}
				if (allRed)
					winner = 1;
				else if (allGreen)
					winner = 2;
			}
			if (winner == 0) {
				for (int y = 0; y < 3; y++) {
					boolean allRed = true;
					boolean allGreen = true;
					for (int x = 0; x < 3; x++) {
						int value = grid[x][y];
						if (value != 1)
							allRed = false;
						if (value != 2)
							allGreen = false;
					}
					if (allRed)
						winner = 1;
					else if (allGreen)
						winner = 2;
				}
				if (winner == 0) {
					boolean allRed = true;
					boolean allGreen = true;
					for (int xy = 0; xy < 3; xy++) {
						int value = grid[xy][xy];
						if (value != 1)
							allRed = false;
						if (value != 2)
							allGreen = false;
					}
					if (allRed)
						winner = 1;
					else if (allGreen)
						winner = 2;
					else {
						allRed = true;
						allGreen = true;
						for (int xy = 0; xy < 3; xy++) {
							int value = grid[xy][2 - xy];
							if (value != 1)
								allRed = false;
							if (value != 2)
								allGreen = false;
						}
						if (allRed)
							winner = 1;
						else if (allGreen)
							winner = 2;
					}
				}
			}
			if (winner != 0) {
				won = true;
				if (winner == 1)
					MinecraftServer.exec("fill -2 0 5 2 4 5 minecraft:red_concrete replace minecraft:bedrock");
				else
					MinecraftServer.exec("fill -2 0 5 2 4 5 minecraft:green_concrete replace minecraft:bedrock");
			}
		}
	}
	
	@MCJNativeImpl({"""
			function $(~concrete_handler) with storage mcj:data localvars.v1
			""", """
			# concrete_handler
			data modify storage mcj:data stack append value {}
			$execute store success storage mcj:data stack[-1].value int 1 run fill -1 1 5 1 3 5 $(value) replace #minecraft:buttons[powered=true]
			"""})
	private native boolean handleButtons(String concrete);
	
	@MCJNativeImpl({"""
			data modify storage mcj:data localvars.v1.y set from storage mcj:data localvars.v2.value
			function $(~pos_handler) with storage mcj:data localvars.v1
			""", """
			# pos_handler
			data modify storage mcj:data stack append value {value:0}
			$execute if block $(value) $(y) 5 minecraft:red_concrete run data modify storage mcj:data stack[-1].value set value 1
			$execute if block $(value) $(y) 5 minecraft:green_concrete run data modify storage mcj:data stack[-1].value set value 2
			"""})
	private native int getGridValue(int x, int y);
	
}
