package com.luneruniverse.minecraft.mcj;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import com.luneruniverse.minecraft.mcj.api.MCJExpandPath;
import com.luneruniverse.minecraft.mcj.api.MCJIgnore;
import com.luneruniverse.minecraft.mcj.api.MCJImplFor;

public class MCJClassInitVisitor extends ClassVisitor {
	
	private final MCJPathProvider provider;
	private final Runnable onIgnore;
	private String name;
	private String superClass;
	private String[] superInterfaces;
	private boolean ignored;
	private String namespace;
	private String newPath;
	private boolean expandedPaths;
	
	public MCJClassInitVisitor(MCJPathProvider provider, Runnable onIgnore) {
		super(Opcodes.ASM9);
		this.provider = provider;
		this.onIgnore = onIgnore;
	}
	
	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		this.name = name;
		this.superClass = superName;
		this.superInterfaces = (interfaces == null ? new String[0] : interfaces);
		this.namespace = provider.getNamespace();
		this.newPath = name;
		this.expandedPaths = provider.isExpandedPaths();
		
		String thePackage = name.substring(0, name.lastIndexOf('/') + 1);
		if (thePackage.equals("com/luneruniverse/minecraft/mcj/") || // Compile com.luneruniverse.minecraft.mcj.api
				thePackage.startsWith("org/objectweb/asm/")) {
			ignored = true;
			onIgnore.run();
		}
	}
	
	@Override
	public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
		if (ignored)
			return null;
		
		if (descriptor.equals(MCJIgnore.class.descriptorString())) {
			ignored = true;
			onIgnore.run();
		} else if (descriptor.equals(MCJImplFor.class.descriptorString())) {
			return new AnnotationVisitor(Opcodes.ASM9) {
				@Override
				public void visit(String name, Object value) {
					if (name.equals("value") && value instanceof String path) {
						int i = path.indexOf(':');
						if (i != -1) {
							namespace = path.substring(0, i);
							path = path.substring(i + 1);
						}
						if (path.isEmpty())
							path = MCJClassInitVisitor.this.name;
						newPath = path;
					}
				}
			};
		} else if (descriptor.equals(MCJExpandPath.class.descriptorString()))
			expandedPaths = true;
		
		return null;
	}
	
	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		if (ignored)
			return null;
		
		provider.getInheritanceTracker().track(newPath, superClass, superInterfaces, name, descriptor);
		return null;
	}
	
	@Override
	public void visitEnd() {
		if (ignored)
			return;
		
		provider.getImplForTracker().track(name, namespace, newPath, expandedPaths);
	}
	
}
