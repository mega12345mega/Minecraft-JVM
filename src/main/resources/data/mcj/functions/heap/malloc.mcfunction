# inc heap_uuid
execute store result score math_a mcj_data run data get storage mcj:data heap_uuid
scoreboard players add math_a mcj_data 1
execute store result storage mcj:data heap_uuid int 1 run scoreboard players get math_a mcj_data

# _malloc heap_uuid
function mcj:heap/_malloc with storage mcj:data

# return pointer
return run data get storage mcj:data heap_uuid