function mcj:heap/malloc
execute store result score math_a mcj_data run data get storage mcj:data stack[-2].value
data remove storage mcj:data stack[-2]
function mcj:heap/_newarray with storage mcj:data stack[-1]
function mcj:heap/_2newarray with storage mcj:data stack[-1]