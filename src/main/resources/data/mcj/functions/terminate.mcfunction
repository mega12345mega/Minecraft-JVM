# Stops the functions in their current state by hitting the maxCommandChainLength limit
# DO NOT USE if maxCommandChainLength is very large

tellraw @a {"text":"TERMINATED: Minecraft JVM has been forcefully halted while executing '","color":"red","extra":[{"nbt":"executing","storage":"mcj:data"},{"text":"' and may be in a corrupted state. Use /function mcj:setup to reset."}]}
data remove storage mcj:data running
function mcj:_terminate