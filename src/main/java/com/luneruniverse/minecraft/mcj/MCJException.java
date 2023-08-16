package com.luneruniverse.minecraft.mcj;

@SuppressWarnings("serial")
public class MCJException extends RuntimeException {
	
	public MCJException(String message) {
		super(message);
	}
	
	public MCJException(String message, Throwable cause) {
		super(message, cause);
	}
	
}
