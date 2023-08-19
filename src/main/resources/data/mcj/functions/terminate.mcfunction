# Stops the functions in their current state by hitting the maxCommandChainLength limit
# DO NOT USE if maxCommandChainLength is very large

tellraw @a {"text":"TERMINATED: Minecraft JVM has been forcefully halted and may be in a corrupted state. Use /function mcj:setup to reset.","color":"red"}
function mcj:terminate