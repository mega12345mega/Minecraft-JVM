package com.luneruniverse.minecraft.mcj.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Change the namespace, package, or class name of the compiled class
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
@MCJIgnore
public @interface MCJImplFor {
	/**
	 * The namespace argument is optional (and usually shouldn't be used)<br>
	 * If a namespace is provided, then the <code>path/to/Class$Nested</code> is not required<br>
	 * @return The new class name, in the format <code>[&lt;namespace&gt;:]path/to/Class$Nested</code>
	 */
	public String value();
}
