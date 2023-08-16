package com.luneruniverse.minecraft.mcj.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Run an execute command on a method call<br>
 * Example: <code>@MCJExecute("in example:dimension")</code> causes the method to be executed in the dimension example:dimension<br>
 * This may also be used to conditionally run a method: <code>@MCJExecute("if entity @a")</code> causes the method
 * to only execute if there are players online<br>
 * <strong>Warning:</strong> If you use <code>@MCJExecute("as @a")</code>
 * (or any other technique to call the method multiple times), then the <em>method is called using the same localvars</em>,
 * meaning that, similar to the <code>switch</code> statement, changes to the arguments will "fall through" into the next call<br>
 * Example:<br>
 * <code><pre>
 * @MCJExecute("as @a")
 * public void method(int num) {
 * 	num++;
 * 	MinecraftServer.broadcast("" + num);
 * }
 * ...
 * method(0); // Outputs 1, 2, 3, etc., one number per player online
 * </pre></code>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
@MCJIgnore
public @interface MCJExecute {
	/**
	 * @return The text to be inserted into <code>execute &lt;value&gt; run function ...</code>
	 */
	public String value();
	/**
	 * Disable the "fall through" warning when using <code>@MCJExecute("as ...")</code><br>
	 * Warning details here: {@link MCJExecute}
	 * @return true to disable the warning
	 */
	public boolean fallThrough() default false;
}
