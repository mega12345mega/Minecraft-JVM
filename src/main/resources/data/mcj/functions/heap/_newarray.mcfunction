execute if score math_a mcj_data matches 0 run return run function mcj:stack/pop
$data modify storage mcj:data heap.v$(value).value append value {value:0}
scoreboard players remove math_a mcj_data 1
function mcj:heap/_newarray with storage mcj:data stack[-1]