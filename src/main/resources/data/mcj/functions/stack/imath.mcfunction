execute store result score math_a mcj_data run data get storage mcj:data stack[-2].value
execute store result score math_b mcj_data run data get storage mcj:data stack[-1].value
function mcj:stack/pop
$scoreboard players operation math_a mcj_data $(op) math_b mcj_data
execute store result storage mcj:data stack[-1].value int 1 run scoreboard players get math_a mcj_data