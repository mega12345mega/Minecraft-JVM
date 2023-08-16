scoreboard players remove math_a mcj_data 1
execute store result storage mcj:data pop_vars_from_stack_amount int 1 run scoreboard players get math_a mcj_data
function mcj:localvars/_2pop_vars_from_stack with storage mcj:data
function mcj:stack/pop
execute unless score math_a mcj_data matches 0 run function mcj:localvars/_pop_vars_from_stack