# Minecraft JVM
Compile Java into a Minecraft datapack

## How to use
1. Create a Java project and include the latest version's jar file on the buildpath
2. Write your code, making sure to *avoid anything in the standard library*, including the `java.lang` package (unless you provide your own `@MCJNativeImpl` using `@MCJImplFor` to specify the target class)
3. Export your code as a jar file, *making sure to include the MCJ library*; the API package it provides will get compiled along with your code
4. Call `java -jar MCJ.jar file/to/compile.jar path/to/world/datapacks/datapack namespace [-delete] [-expandedPaths] [-sysout]`, where `-delete` causes the output folder to get deleted prior to compilation (if the output folder exists and `-delete` isn't provided, then the compiler may fail), `-expandedPaths` exports with full paths rather than an md5 hash for non-entrypoint functions, and `-sysout` prints progress updates to the console
5. Launch 23w31a or later (macros are not yet in a release, and they are used extensively to make this possible)
6. Use `/reload` and `/function namespace:main`

Use `@MCJEntrypoint` to provide and customize additional entrypoints instead of just `namespace:main`<br>
Use the `EventManager` to call code when something happens, like a server tick

## Limitations
* Some bytecode instructions are only partially or not at all implemented, so these things aren't currently possible:
  * static fields
  * some duplicate instructions (DUP_X2, DUP2, DUP2_X1, DUP2_X2); if it fails to compile, try to rearrange the code :/
  * some primitive type casting instructions
  * floats and doubles (longs untested and not to Java spec)
  * binary math (like bit-shifting and xor)
  * invokedynamic (used in lambdas)
  * inheritance (including for-each's usage of Iterable)
    * static method inheritance is now supported (it is handled at MCJ compile time)
  * instanceof
* There are no exceptions and no runtime checks, meaning that there is no type safety and you can attempt to create negative length arrays (which is undefined behavior)
* You can't use anything in the standard library, including `java.lang`; as mentioned above, use `@MCJNativeImpl` and `@MCJImplFor` to provide your own implementation for a class, allowing you to use it again (refer to `com.luneruniverse.minecraft.mcj.api.java.lang.StringBuilder` for an example

## API
The `com.luneruniverse.minecraft.mcj.api` package provides useful methods and standard library replacements. Refer to the `MinecraftServer` class for useful methods, such as `exec(String)` and `getPlayers()`. Use `Array`, `CompoundMap`, and `IterableMap` for more advanced data handling.

## Example Code
Refer to [`src/test/java/com/luneruniverse/minecraft/mcj/test`](src/test/java/com/luneruniverse/minecraft/mcj/test) for more examples
```
public class Example {
	public static void main(String[] args) {
		// Prints out all online players
		Player[] players = MinecraftServer.getPlayers();
		for (int i = 0; i < players.length; i++) {
			MinecraftServer.broadcast(players[i].getName());
		}
	}
}
```