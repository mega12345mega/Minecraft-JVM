data remove storage mcj:data
data modify storage mcj:data stack set value []
data modify storage mcj:data intstack set value []
data modify storage mcj:data localvars set value {}
data modify storage mcj:data heap set value {}
data modify storage mcj:data heap_uuid set value 0
data modify storage mcj:data classes set value {}
data modify storage mcj:data event_handlers set value {}
data modify storage mcj:data running set value 1b
data remove storage mcj:data executing
data modify storage mcj:data true set value 1b

scoreboard objectives add mcj_data dummy

execute in mcj:data run forceload add 0 0