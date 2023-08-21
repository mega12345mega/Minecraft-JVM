package com.luneruniverse.minecraft.mcj;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Opcodes;

import com.luneruniverse.minecraft.mcj.MCJPathProvider.MethodPathProvider;

public class MCJNativeImplAnnotationVisitor extends AnnotationVisitor {
	
	private static class ArrayVisitor extends AnnotationVisitor {
		private final List<String> contents;
		public ArrayVisitor() {
			super(Opcodes.ASM9);
			this.contents = new ArrayList<>();
		}
		@Override
		public void visit(String name, Object value) {
			if (value instanceof String str)
				contents.add(str);
			else
				throw new MCJException("Malformed @MCJNativeImpl; invalid type '" + value.getClass().getName() + "'");
		}
		@Override
		public void visitEnd() {
			if (contents.isEmpty())
				throw new MCJException("Malformed @MCJNativeImpl; the 'value' or 'files' argument cannot be empty");
		}
	}
	
	private final MethodPathProvider provider;
	private ArrayVisitor value;
	private ArrayVisitor files;
	
	public MCJNativeImplAnnotationVisitor(MethodPathProvider provider) {
		super(Opcodes.ASM9);
		this.provider = provider;
	}
	
	@Override
	public AnnotationVisitor visitArray(String name) {
		return switch (name) {
			case "value" -> value = new ArrayVisitor();
			case "files" -> files = new ArrayVisitor();
			default -> throw new MCJException("Malformed @MCJNativeImpl; unexpected '" + name + "'");
		};
	}
	
	@Override
	public void visitEnd() {
		if ((value == null) == (files == null))
			throw new MCJException("Malformed @MCJNativeImpl; there must be exactly one of the 'value' and 'files' arguments");
		
		Map<String, String> values = new HashMap<>();
		
		if (value != null) {
			values.put("entry", value.contents.get(0));
			for (int i = 1; i < value.contents.size(); i++) {
				String[] helperFile = value.contents.get(i).replace("\r", "").split("\n", 2);
				if (helperFile.length == 0 || !helperFile[0].startsWith("# "))
					throw new MCJException("Invalid value in @MCJNativeImpl; the first line must be '# <file name>'");
				String fileName = processFileName(helperFile[0].substring("# ".length()));
				if (values.put(fileName, helperFile.length < 2 ? "" : helperFile[1]) != null)
					throw new MCJException("Duplicate @MCJNativeImpl file names: " + fileName + " (" + provider.getMethodFunctionsPath() + ")");
			}
		}
		
		if (files != null) {
			try {
				values.put("entry", Files.readString(new File(files.contents.get(0)).toPath()));
				for (int i = 1; i < files.contents.size(); i++) {
					File helperFileRef = new File(files.contents.get(i));
					String helperFile = Files.readString(helperFileRef.toPath()).replace("\r", "");
					
					int extensionIndex = helperFileRef.getName().lastIndexOf('.');
					String fileName = extensionIndex == -1 ? helperFileRef.getName() :
						helperFileRef.getName().substring(0, extensionIndex);
					String fileContents = helperFile;
					
					if (helperFile.startsWith("# ")) {
						int headerEnd = helperFile.indexOf('\n');
						int contentStart = headerEnd + 1;
						if (headerEnd == -1) {
							headerEnd = helperFile.length();
							contentStart = headerEnd;
						}
						fileName = processFileName(helperFile.substring("# ".length(), headerEnd));
						fileContents = helperFile.substring(contentStart);
					}
					
					if (values.put(fileName, fileContents) != null)
						throw new MCJException("Duplicate @MCJNativeImpl file names: " + fileName + " (" + provider.getMethodFunctionsPath() + ")");
				}
			} catch (IOException e) {
				throw new MCJException("Error reading a @MCJNativeImpl file", e);
			}
		}
		
		values.forEach((fileName, fileContents) -> {
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
				if (path.startsWith("method~")) {
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
					
					StringBuilder fullPath = new StringBuilder(provider.getClassFunctionsPath());
					fullPath.append('/');
					String name = path.substring(0, descStart);
					if (name.equals("<init>"))
						fullPath.append("constructor");
					else {
						fullPath.append("method_");
						fullPath.append(name.toLowerCase());
					}
					fullPath.append('_');
					fullPath.append(MCJUtil.md5(path.substring(0, descEnd)));
					if (descEnd == path.length())
						fullPath.append("/entry");
					else if (path.charAt(descEnd) != '/')
						continue;
					else
						fullPath.append(path.substring(descEnd));
					
					replacement = provider.getActualFunctionPath(fullPath.toString());
				} else if (path.startsWith("~"))
					replacement = provider.getActualFunctionPath(provider.getMethodFunctionsPath() + "/" + path.substring(1));
				else
					continue;
				fileContents = fileContents.substring(0, range.getKey()) + replacement + fileContents.substring(range.getValue());
			}
			
			try {
				provider.writeToFile(new File(provider.getMethodFunctions(), fileName + ".mcfunction"), fileContents);
			} catch (IOException e) {
				throw new MCJException("Error writing a native file", e);
			}
		});
	}
	
	private String processFileName(String fileName) {
		if (fileName.endsWith(".mcfunction")) {
			fileName = fileName.substring(0, fileName.length() - ".mcfunction".length());
			System.out.println("[Warning] You don't have to specify .mcfunction in the file name comment (" + provider.getMethodFunctionsPath() + "/" + fileName + ")");
		}
		return fileName;
	}
	
}
