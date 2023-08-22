package com.luneruniverse.minecraft.mcj.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Expands paths for this class regardless of the '-expandedPaths' flag
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
@MCJIgnore
public @interface MCJExpandPath {
	
}
