function mcj:stack/pop
$execute if data storage mcj:data switch_label.f$(value) run data modify storage mcj:data switch_label set from storage mcj:data switch_label.f$(value)
execute if data storage mcj:data switch_label.default run data modify storage mcj:data switch_label set from storage mcj:data switch_label.default
function mcj:stack/_lookupswitch with storage mcj:data