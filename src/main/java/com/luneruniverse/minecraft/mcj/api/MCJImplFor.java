package com.luneruniverse.minecraft.mcj.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Change the package and class name of the compiled class (used internally)<br>
 * <strong>Warning:</strong> this does NOT apply this change to anything referring to the class
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
@MCJIgnore
public @interface MCJImplFor {
	/**
	 * @return The new class name, in the format <code>path/to/Class$Nested</code>
	 */
	public String value();
}
