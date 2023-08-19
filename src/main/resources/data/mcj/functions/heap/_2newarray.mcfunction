execute if score math_a mcj_data matches 0 run return 0
$data modify storage mcj:data heap.v$(value).value append value {value:0}
scoreboard players remove math_a mcj_data 1
function mcj:heap/_2newarray with storage mcj:data stack[-1]