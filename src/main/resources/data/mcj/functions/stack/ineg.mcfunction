execute store result score math_a mcj_data run data get storage mcj:data stack[-1].value
scoreboard players set math_b mcj_data -1
scoreboard players operation math_a mcj_data *= math_b mcj_data
execute store result storage mcj:data stack[-1].value int 1 run scoreboard players get math_a mcj_data