package com.luneruniverse.minecraft.mcj;

import java.io.File;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import com.luneruniverse.minecraft.mcj.MCJPathProvider.ClassPathProvider;
import com.luneruniverse.minecraft.mcj.MCJPathProvider.MethodPathProvider;
import com.luneruniverse.minecraft.mcj.api.MCJIgnore;
import com.luneruniverse.minecraft.mcj.api.MCJImplFor;

public class MCJClassVisitor extends ClassVisitor {
	
	private class MCJImplForAnnotationVisitor extends AnnotationVisitor {
		public MCJImplForAnnotationVisitor() {
			super(Opcodes.ASM9);
		}
		@Override
		public void visit(String name, Object obj) {
			String value = (String) obj;
			String namespace = classProvider.getNamespace();
			int i = value.indexOf(':');
			if (i != -1) {
				namespace = value.substring(0, i);
				value = value.substring(i + 1);
				classProvider = classProvider.changeNamespace(namespace);
			}
			if (value.isEmpty())
				value = classProvider.getClassName();
			else {
				String path = MCJUtil.formatClassPath(value);
				classProvider = classProvider.changeClassPath(
						new File(classProvider.getCompiledFunctions(), path),
						namespace + ":" + path, value);
			}
			init(value);
		}
	}
	
	private MCJPathProvider mainProvider;
	private ClassPathProvider classProvider;
	private boolean isIgnored;
	
	public MCJClassVisitor(MCJPathProvider mainProvider) {
		super(Opcodes.ASM9);
		this.mainProvider = mainProvider;
	}
	
	private void init(String name) {
		String thePackage = name.substring(0, name.lastIndexOf('/') + 1);
		if (thePackage.equals("com/luneruniverse/minecraft/mcj/") || // Compile com.luneruniverse.minecraft.mcj.api
				thePackage.startsWith("org/objectweb/asm/")) {
			isIgnored = true;
			return;
		}
		
		String path = MCJUtil.formatClassPath(name);
		
		MCJPathProvider provider = (classProvider == null ? mainProvider : classProvider);
		classProvider = new ClassPathProvider(provider,
				new File(provider.getCompiledFunctions(), path),
				provider.getNamespace() + ":" + path,
				name,
				provider.getMainClass().equals(name));
	}
	
	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		init(name);
	}
	
	@Override
	public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
		if (descriptor.equals(MCJIgnore.class.descriptorString()))
			isIgnored = true;
		if (descriptor.equals(MCJImplFor.class.descriptorString()))
			return new MCJImplForAnnotationVisitor();
		return null;
	}
	
	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		if (isIgnored)
			return null;
		
		if (exceptions != null && exceptions.length > 0) {
			throw new MCJException("MCJ cannot compile exceptions! Remove all 'throw(s)'s and 'try's");
		}
		
		if (MCJUtil.hasOpcode(access, Opcodes.ACC_SYNCHRONIZED)) {
			// Ignore; it is not currently possible to start another thread anyway
		}
		
		boolean isMain = classProvider.isMainClass() &&
				name.equals("main") &&
				MCJUtil.hasOpcodes(access, Opcodes.ACC_PUBLIC, Opcodes.ACC_STATIC) &&
				descriptor.equals("([Ljava/lang/String;)V");
		
		String md5 = MCJUtil.md5(name + descriptor);
		if (name.equals("<init>"))
			name = "constructor";
		else
			name = "method_" + name.toLowerCase();
		name += "_" + md5;
		
		return new MCJMethodVisitor(new MethodPathProvider(classProvider,
				new File(classProvider.getClassFunctions(), name),
				classProvider.getClassFunctionsPath() + "/" + name,
				MCJUtil.getParamCount(descriptor),
				isMain,
				MCJUtil.hasOpcode(access, Opcodes.ACC_STATIC),
				MCJUtil.hasOpcode(access, Opcodes.ACC_NATIVE),
				descriptor.endsWith("V")));
	}
	
}
