# Warning: impl of mcj:stack/setup_icmp depends on this impl

execute store result score cmp_a mcj_data run data get storage mcj:data stack[-2].value
execute store result score cmp_b mcj_data run data get storage mcj:data stack[-1].value
function mcj:stack/pop

execute if score cmp_a mcj_data < cmp_b mcj_data run data modify storage mcj:data stack[-1].value set value -1
execute if score cmp_a mcj_data = cmp_b mcj_data run data modify storage mcj:data stack[-1].value set value 0
execute if score cmp_a mcj_data > cmp_b mcj_data run data modify storage mcj:data stack[-1].value set value 1