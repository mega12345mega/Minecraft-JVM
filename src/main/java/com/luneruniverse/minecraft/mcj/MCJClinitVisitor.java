package com.luneruniverse.minecraft.mcj;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class MCJClinitVisitor extends MethodVisitor {
	
	private final Consumer<String> onPut;
	private final Set<String> handledFields;
	
	public MCJClinitVisitor(Consumer<String> onPut) {
		super(Opcodes.ASM9);
		this.onPut = onPut;
		this.handledFields = new HashSet<>();
	}
	
	@Override
	public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
		String nameAndDesc = name + ":" + descriptor;
		switch (opcode) {
			case Opcodes.GETSTATIC -> handledFields.add(nameAndDesc);
			case Opcodes.PUTSTATIC -> {
				if (handledFields.add(nameAndDesc))
					onPut.accept(nameAndDesc);
			}
		}
	}
	
}
