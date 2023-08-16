execute store result score cmp_a mcj_data run data get storage mcj:data stack[-2].value
execute store result score cmp_b mcj_data run data get storage mcj:data stack[-1].value
function mcj:stack/pop

execute if score cmp_a mcj_data < cmp_b mcj_data run data modify storage mcj:data stack[-1].value set value -1
execute if score cmp_a mcj_data = cmp_b mcj_data run data modify storage mcj:data stack[-1].value set value 0
execute if score cmp_a mcj_data > cmp_b mcj_data run data modify storage mcj:data stack[-1].value set value 1

execute if score cmp_a mcj_data < cmp_b mcj_data run data modify storage mcj:data stack[-1].lt set value 1b
execute if score cmp_a mcj_data <= cmp_b mcj_data run data modify storage mcj:data stack[-1].le set value 1b
execute if score cmp_a mcj_data = cmp_b mcj_data run data modify storage mcj:data stack[-1].eq set value 1b
execute if score cmp_a mcj_data >= cmp_b mcj_data run data modify storage mcj:data stack[-1].ge set value 1b
execute if score cmp_a mcj_data > cmp_b mcj_data run data modify storage mcj:data stack[-1].gt set value 1b
execute unless score cmp_a mcj_data = cmp_b mcj_data run data modify storage mcj:data stack[-1].ne set value 1b