package com.luneruniverse.minecraft.mcj.api;

@MCJImplFor("mcj:")
public class EventManager {
	public static final String TICK_EVENT = "tick";
	
	@MCJNativeImpl({"""
			function $(~event_handler) with storage mcj:data localvars.v0
			""", """
			# event_handler
			$data modify storage mcj:data event_handlers.$(value) append from storage mcj:data localvars.v1
			"""})
	public static native void registerEventHandler(String event, String function);
	
	@MCJEntrypoint(value = "call_event")
	public static void callEvent(String event) {
		String[] handlers = getEventHandlers(event);
		int size = handlers.length;
		for (int i = 0; i < size; i++) {
			MinecraftServer.execFunction(handlers[i]);
		}
		MinecraftServer.free(handlers);
	}
	@MCJNativeImpl({"""
			function mcj:heap/malloc
			data modify storage mcj:data stack[-1].event set from storage mcj:data localvars.v0.value
			function $(~pointer_handler) with storage mcj:data stack[-1]
			""", """
			# pointer_handler
			$data modify storage mcj:data heap.v$(value).value set value []
			$data modify storage mcj:data heap.v$(value).value set from storage mcj:data event_handlers.$(event)
			"""})
	private static native String[] getEventHandlers(String event);
}
