package com.luneruniverse.minecraft.mcj.api;

@MCJImplFor("mcj:")
public class Dimension {
	
	private final String name;
	
	public Dimension(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
	
	public Player[] getPlayers() {
		String[] names = new String[0];
		getPlayers_getList(names);
		return Player.getPlayers(names);
	}
	public String[] getPlayerNames() {
		String[] names = new String[0];
		getPlayers_getList(names);
		return names;
	}
	@MCJNativeImpl({"""
			function mcj:localvars/push_var_to_stack {index:"0"}
			function mcj:heap/getfield {name:"name"}
			data modify storage mcj:data stack[-1].names set from storage mcj:data localvars.v1.value
			function $(~pointer_handler) with storage mcj:data stack[-1]
			""", """
			# pointer_handler
			$execute in $(value) as @a[distance=0..] run function $(~add_player) {names:"$(names)"}
			""", """
			# add_player
			$data modify storage mcj:data heap.v$(names).value append value {}
			execute in mcj:data run setblock 0 0 0 chest
			execute in mcj:data run loot replace block 0 0 0 container.0 loot mcj:get_own_head
			$execute in mcj:data run data modify storage mcj:data heap.v$(names).value[-1].value set from block 0 0 0 Items[0].tag.SkullOwner.Name
			"""})
	private native void getPlayers_getList(String[] names);
	
}
