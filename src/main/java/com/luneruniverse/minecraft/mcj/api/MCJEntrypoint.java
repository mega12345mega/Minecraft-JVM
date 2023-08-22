package com.luneruniverse.minecraft.mcj.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Adds the function <code>namespace:entrypoint_name</code> to call the annotated function<br>
 * You must supply arguments via <code>function namespace:entrypoint_name {argName1:"1",argName2:"\"str\""}</code><br>
 * <br>
 * If applied to the <code>public static void main(String[])</code> method, the <code>namespace:main</code>
 * function will not be auto-generated. Note that the replacement method will require a <code>String[]</code> argument,
 * unlike <code>namespace:main</code>. It will also change {@link #setup()} to be disabled by default.
 * {@link #gc()} is already disabled by default<br>
 * <br>
 * You can optionally enable a <code>mcj:setup</code> call before running the entrypoint using {@link #setup()}<br>
 * You can optionally enable a <code>mcj:heap/free_all</code> call after running the entrypoint using {@link #gc()}<br>
 * <br>
 * <strong>Warning:</strong> If providing an array argument, use the format <code>[{value:0},{value:1},{value:2}]</code>
 * instead of <code>[0,1,2]</code>; this matches Minecraft JVM's internal representation of arrays
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
@MCJIgnore
public @interface MCJEntrypoint {
	/**
	 * If you specify <code>.mcfunction</code>, you will receive a warning, but it will continue as expected
	 * @return The name of the function, without the namespace or <code>.mcfunction</code>
	 */
	public String value();
	/**
	 * @return If <code>function mcj:setup</code> should be called before executing this entrypoint
	 * @see #gc()
	 */
	public boolean setup() default false;
	/**
	 * @return If <code>function mcj:heap/free_all</code> should be called after executing this entrypoint
	 * @see #setup()
	 * @see MinecraftServer#free(Object)
	 */
	public boolean gc() default false;
}
