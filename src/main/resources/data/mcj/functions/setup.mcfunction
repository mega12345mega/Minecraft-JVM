data remove storage mcj:data
data modify storage mcj:data stack set value []
data modify storage mcj:data intstack set value []
data modify storage mcj:data localvars set value {}
data modify storage mcj:data heap set value {}
data modify storage mcj:data heap_uuid set value -1
data modify storage mcj:data true set value 1b

scoreboard objectives add mcj_data dummy

execute in mcj:data run forceload add 0 0