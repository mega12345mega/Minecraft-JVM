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
	
	public static String formatClassPath(String classPath) {
		StringBuilder path = new StringBuilder();
		String[] parts = classPath.toLowerCase().split("/");
		for (int i = 0; i < parts.length - 1; i++) {
			path.append("package_");
			path.append(parts[i]);
			path.append('/');
		}
		path.append("class_");
		path.append(parts[parts.length - 1].replace("$", "/class_"));
		return path.toString();
	}
	
	public static int getParamCount(String descriptor) {
		int output = 0;
		boolean parsingClass = false;
		for (char c : descriptor.toCharArray()) {
			if (parsingClass) {
				if (c == ';')
					parsingClass = false;
			} else {
				if (c == '(' || c == '[')
					;
				else if (c == ')')
					break;
				else if (c == 'B' || c == 'C' || c == 'D' || c == 'F' || c == 'I' || c == 'J' || c == 'S' || c == 'Z')
					output++;
				else if (c == 'L') {
					output++;
					parsingClass = true;
				}
			}
		}
		return output;
	}
	
}
