package com.luneruniverse.minecraft.mcj.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Prevents MCJ from compiling the entire class
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
@MCJIgnore
public @interface MCJIgnore {
	
}
