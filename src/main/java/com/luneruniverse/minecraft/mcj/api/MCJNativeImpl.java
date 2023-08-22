package com.luneruniverse.minecraft.mcj.api;

import java.io.File;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.luneruniverse.minecraft.mcj.MCJ;

/**
 * Specify the mcfunction implementation of native methods<br>
 * Use {@link #value()} if you want the mcfunction code in the annotation itself,
 * and {@link #files()} if you want the mcfunction code in other file(s)
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
@MCJIgnore
public @interface MCJNativeImpl {
	/**
	 * The first value is entered into the <code>&lt;method path&gt;/entry.mcfunction</code> file<br>
	 * The next values are entered into <code>&lt;method path&gt;/&lt;name&gt;.mcfunction</code> files,
	 * where <code>&lt;name&gt;</code> is supplied as a comment on the first line of the value<br>
	 * <code>$(~&lt;name&gt;)</code> will be replaced with the path to the function<br>
	 * <code>$(method~&lt;method_name&gt;&lt;descriptor&gt;[/&lt;name&gt;])</code> allows you to target a different method<br>
	 * <code>$(class~package/Class$Nested~&lt;method_name&gt;&lt;descriptor&gt;[/&lt;name&gt;])</code>
	 * allows you to target a different class<br>
	 * Example:<br>
	 * <code><pre>
	 * @MCJNativeImpl({"""
	 * 		say foo
	 * 		function $(~helper)
	 * 		function $(method~&lt;init&gt;(I)V)
	 * 		function $(method~toString()Ljava/lang/String;/helper2)
	 * 		function $(class~java/lang/StringBuilder~append(Ljava/lang/String;)Ljava/lang/StringBuilder;)
	 * 		""", """
	 * 		# helper
	 * 		say bar
	 * 		"""})
	 * </pre></code>
	 * @return A list of values
	 * @see #files()
	 */
	public String[] value() default {};
	/**
	 * This is an alternative to {@link #value()}, where the values are loaded from a file rather than being
	 * specified directly in the annotation. You do not need to comment the file name at the top of additional files,
	 * but you can optionally include this comment. To avoid confusion with a normal comment,
	 * you may want to include it anyway.<br>
	 * Files will be resolved relative to the .jar passed into {@link MCJ#MCJ(File, String)}<br>
	 * <code>$(~&lt;name&gt;)</code> will be replaced with the path to the function<br>
	 * <code>$(method~&lt;method_name&gt;&lt;descriptor&gt;[/&lt;name&gt;])</code> allows you to target a different method<br>
	 * <code>$(class~package/Class$Nested~&lt;method_name&gt;&lt;descriptor&gt;[/&lt;name&gt;])</code>
	 * allows you to target a different class<br>
	 * @return A list of files
	 * @see #value()
	 */
	public String[] files() default {};
}
