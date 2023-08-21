package com.luneruniverse.minecraft.mcj.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Expands paths regardless of the '-expandedPaths' flag (used internally)<br>
 * <strong>Warning:</strong> this does NOT apply this change to anything referring to the class,
 * unless the thing referring to the class is within this class
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
@MCJIgnore
public @interface MCJExpandPath {
	
}
