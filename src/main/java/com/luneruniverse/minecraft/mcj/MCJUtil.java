package com.luneruniverse.minecraft.mcj;

import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.luneruniverse.minecraft.mcj.MCJPathProvider.ClassPathProvider;
import com.luneruniverse.minecraft.mcj.MCJPathProvider.MethodPathProvider;

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
	
	public static String processNativeFile(MCJPathProvider provider, String fileContents) {
		List<Map.Entry<Integer, Integer>> pathRanges = new ArrayList<>();
		int pathStart = -1;
		int parenCount = 0;
		for (int i = 0; i < fileContents.length(); i++) {
			char c = fileContents.charAt(i);
			if (c == '$') {
				pathStart = i;
				parenCount = 0;
			} else if (c != '(' && pathStart == i - 1)
				pathStart = -1;
			else if (pathStart != -1) {
				if (c == '(')
					parenCount++;
				else if (c == ')') {
					parenCount--;
					if (parenCount == 0) {
						pathRanges.add(Map.entry(pathStart, i + 1));
						pathStart = -1;
					}
				}
			}
		}
		
		for (int i = pathRanges.size() - 1; i >= 0; i--) {
			Map.Entry<Integer, Integer> range = pathRanges.get(i);
			String path = fileContents.substring(range.getKey() + "$(".length(), range.getValue() - ")".length());
			String replacement;
			
			MCJPathProvider mainProvider = provider;
			ClassPathProvider classProvider = (provider instanceof ClassPathProvider casted ? casted : null);
			MethodPathProvider methodProvider = (provider instanceof MethodPathProvider casted ? casted : null);
			
			if (path.startsWith("class~")) {
				path = path.substring("class~".length());
				
				int classEnd = path.indexOf('~');
				if (classEnd == -1)
					continue;
				
				String name = path.substring(0, classEnd);
				int namespaceEnd = name.indexOf(':');
				if (namespaceEnd != -1) {
					mainProvider = mainProvider.changeNamespace(name.substring(0, namespaceEnd));
					name = name.substring(namespaceEnd + 1);
				}
				String classPath = MCJUtil.formatClassPath(name);
				
				classProvider = new ClassPathProvider(mainProvider,
						new File(mainProvider.getCompiledFunctions(), classPath),
						mainProvider.getNamespace() + ":" + classPath,
						name,
						mainProvider.getMainClass().equals(name));
				path = "method" + path.substring(classEnd);
			}
			
			if (path.startsWith("method~")) {
				if (classProvider == null)
					throw new MCJException("Illegal usage of 'method~'; did you mean 'class~package/Class$Nested~'?");
				path = path.substring("method~".length());
				
				int descStart = path.indexOf('(');
				if (descStart == -1)
					continue;
				int descEnd = path.indexOf(')');
				if (descEnd == -1 || descEnd + 1 == path.length())
					continue;
				if (path.charAt(descEnd + 1) == 'L') {
					descEnd = path.indexOf(';', descEnd);
					if (descEnd == -1)
						continue;
				} else
					descEnd++;
				descEnd++;
				
				StringBuilder fullPath = new StringBuilder(classProvider.getClassFunctionsPath());
				fullPath.append('/');
				fullPath.append(getMethodName(path.substring(0, descStart), path.substring(descStart, descEnd)));
				if (descEnd == path.length())
					fullPath.append("/entry");
				else if (path.charAt(descEnd) != '/')
					continue;
				else
					fullPath.append(path.substring(descEnd));
				
				replacement = classProvider.getActualFunctionPath(fullPath.toString());
			} else if (path.startsWith("~")) {
				if (methodProvider == null)
					throw new MCJException("Illegal usage of '~'; did you mean 'method~<name><descriptor>/'?");
				replacement = mainProvider.getActualFunctionPath(methodProvider.getMethodFunctionsPath() + "/" + path.substring(1));
			} else {
				continue;
			}
			
			fileContents = fileContents.substring(0, range.getKey()) + replacement + fileContents.substring(range.getValue());
		}
		
		return fileContents;
	}
	
	public static String getMethodName(String name, String descriptor) {
		if (name.equals("<clinit>"))
			return "clinit";
		String md5 = md5(name + descriptor);
		if (name.equals("<init>"))
			name = "init_";
		else
			name = "method_" + name.toLowerCase() + "_";
		name += md5;
		return name;
	}
	
}
