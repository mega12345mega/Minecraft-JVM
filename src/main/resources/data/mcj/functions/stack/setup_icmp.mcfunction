# Warning: depends on impl of mcj:stack/lcmp

function mcj:stack/lcmp

execute if score cmp_a mcj_data < cmp_b mcj_data run data modify storage mcj:data stack[-1].lt set value 1b
execute if score cmp_a mcj_data <= cmp_b mcj_data run data modify storage mcj:data stack[-1].le set value 1b
execute if score cmp_a mcj_data = cmp_b mcj_data run data modify storage mcj:data stack[-1].eq set value 1b
execute if score cmp_a mcj_data >= cmp_b mcj_data run data modify storage mcj:data stack[-1].ge set value 1b
execute if score cmp_a mcj_data > cmp_b mcj_data run data modify storage mcj:data stack[-1].gt set value 1b
execute unless score cmp_a mcj_data = cmp_b mcj_data run data modify storage mcj:data stack[-1].ne set value 1b