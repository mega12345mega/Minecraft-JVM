execute unless data storage mcj:data running run return 0
execute if data storage mcj:data executing run tellraw @a {"text":"TERMINATED: Minecraft JVM has hit the maxCommandChainLength limit while executing '","color":"red","extra":[{"nbt":"executing","storage":"mcj:data"},{"text":"'. Use /function mcj:setup to reset."}]}
execute if data storage mcj:data executing run data remove storage mcj:data running
execute if data storage mcj:data running run function mcj:call_event {event:"\"tick\""}