package com.luneruniverse.minecraft.mcj;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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
	
	private static MessageDigest md5;
	public static String md5(String str) {
		if (md5 == null) {
			try {
				md5 = MessageDigest.getInstance("MD5");
			} catch (NoSuchAlgorithmException e) {
				throw new MCJException("Unable to find md5", e);
			}
		}
		md5.update(str.getBytes());
		byte[] digest = md5.digest();
		StringBuilder output = new StringBuilder();
		for (byte b : digest) {
			output.append(String.format("%02x", b));
		}
		return output.toString();
	}
	
}
