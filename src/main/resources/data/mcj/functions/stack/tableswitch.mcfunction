function mcj:stack/pop
execute if score math_a mcj_data matches ..0 run data modify storage mcj:data switch_label set from storage mcj:data switch_label[0]
$execute if data storage mcj:data switch_label[$(value)] run data modify storage mcj:data switch_label set from storage mcj:data switch_label[$(value)]
execute if data storage mcj:data switch_label[0] run data modify storage mcj:data switch_label set from storage mcj:data switch_label[0]
function mcj:stack/_tableswitch with storage mcj:data