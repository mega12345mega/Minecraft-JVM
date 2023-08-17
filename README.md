# Minecraft JVM
Compile Java into a Minecraft datapack

## How to use
1. Create a Java project and include the latest version's jar file on the buildpath
2. Write your code, making sure to *avoid anything in the standard library*, including the `java.lang` package (unless you provide your own `@MCJNativeImpl` using `@MCJImplFor` to specify the target class)
3. Export your code as a jar file, *making sure to include the MCJ library*; the API package it provides will get compiled along with your code
4. Call `java -jar MCJ.jar file/to/compile.jar path/to/world/datapacks/datapack namespace [-delete]`, where `-delete` causes the output folder to get deleted prior to compilation (if the output folder exists and `-delete` isn't provided, then the compiler may fail)
5. Launch 23w31a or later (macros are not yet in a release, and they are used extensively to make this possible)
6. Use `/reload` and `/function namespace:main`

## Limitations
* Most bytecode instructions are not currently implemented; this is currently a proof of concept, but I plan to complete support for all Java code at some point
  * The last commit contains most bytecode instructions, but is not well tested. It is missing:
    * static fields
    * some duplicate instructions (DUP_X2, DUP2, DUP2_X1, DUP2_X2); if it fails to compile, try to rearrange the code :/
    * some primitive type casting instructions
    * floats and doubles
    * all math (except int variable += some int)
    * invokedynamic (used in lambdas)
    * inheritance
    * instanceof
* There are no exceptions and no runtime checks, meaning that there is no type safety and you can attempt to create negative length arrays (which is undefined behavior)
* You can't use anything in the standard library, including `java.lang`; as mentioned above, use `@MCJNativeImpl` and `@MCJImplFor` to provide your own implementation for a class, allowing you to use it again (refer to `com.luneruniverse.minecraft.mcj.api.java.lang.StringBuilder` for an example

## API
The `com.luneruniverse.minecraft.mcj.api` package provides useful methods and native implementations. Refer to the `MinecraftServer` class for useful methods, such as `exec(String)` and `broadcast(String)`.

## Example Code
```
public class Example {
	public static void main(String[] args) {
		for (int i = 0; i < 5; i++) {
			MinecraftServer.broadcast("Hello - " + i);
		}
	}
}
```