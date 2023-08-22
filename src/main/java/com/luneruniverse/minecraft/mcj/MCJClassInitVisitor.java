package com.luneruniverse.minecraft.mcj;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

import com.luneruniverse.minecraft.mcj.api.MCJExpandPath;
import com.luneruniverse.minecraft.mcj.api.MCJImplFor;

public class MCJClassInitVisitor extends ClassVisitor {
	
	private final MCJPathProvider provider;
	private String name;
	private String namespace;
	private String newPath;
	private boolean expandedPaths;
	
	public MCJClassInitVisitor(MCJPathProvider provider) {
		super(Opcodes.ASM9);
		this.provider = provider;
	}
	
	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		this.name = name;
		this.namespace = provider.getNamespace();
		this.newPath = name;
		this.expandedPaths = provider.isExpandedPaths();
	}
	
	@Override
	public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
		if (descriptor.equals(MCJImplFor.class.descriptorString())) {
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
		}
		if (descriptor.equals(MCJExpandPath.class.descriptorString()))
			expandedPaths = true;
		return null;
	}
	
	@Override
	public void visitEnd() {
		provider.getTracker().track(name, namespace, newPath, expandedPaths);
	}
	
}
