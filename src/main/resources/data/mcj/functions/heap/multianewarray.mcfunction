execute store result score math_a mcj_data run data get storage mcj:data stack
scoreboard players remove math_a mcj_data $(numDimensions)
data modify storage mcj:data stack append value {}
execute store result storage mcj:data stack[-1].value int 1 run scoreboard players get math_a mcj_data
data modify storage mcj:data stack append value {value:$(numDimensions)}
data modify storage mcj:data intstack append from mcj:data stack[-1]

function mcj:stack/invokestatic {method:"mcj:package_java/package_lang/class_BytecodeImpl/method_multianewarray/entry",num_args:"2",has_return:"true"}

scoreboard players set math_a mcj_data $(numDimensions)
function mcj:heap/_multianewarray with storage mcj:data intstack[-1]
function mcj:intstack/pop