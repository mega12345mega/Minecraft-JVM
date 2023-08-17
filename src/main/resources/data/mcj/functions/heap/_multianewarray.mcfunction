$data remove storage mcj:data stack[$(value)]
scoreboard players remove math_a mcj_data 1
execute unless score math_a mcj_data matches 0 run function mcj:heap/_multianewarray {value:$(value)}