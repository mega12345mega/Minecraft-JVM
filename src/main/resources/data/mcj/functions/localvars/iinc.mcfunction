$execute store result score math_a mcj_data run data get storage mcj:data localvars.v$(index).value
$scoreboard players $(action) math_a mcj_data $(amount)
$execute store result storage mcj:data localvars.v$(index).value int 1 run scoreboard players get math_a mcj_data