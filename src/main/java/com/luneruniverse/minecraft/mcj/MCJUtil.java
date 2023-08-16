package com.luneruniverse.minecraft.mcj;

public class MCJUtil {
	
	public static boolean hasOpcode(int num, int opcode) {
		return (num & opcode) != 0;
	}
	
	public static boolean hasOpcodes(int num, int... opcodes) {
		for (int opcode : opcodes) {
			if (!hasOpcode(num, opcode))
				return false;
		}
		return true;
	}
	
}
