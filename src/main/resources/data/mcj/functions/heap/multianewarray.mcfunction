# Warning: depends on impl of mcj:stack/invoke

# args.countPtr, intstack.push = stack.length - numDimensions
execute store result score math_a mcj_data run data get storage mcj:data stack
$scoreboard players remove math_a mcj_data $(numDimensions)
data modify storage mcj:data stack append value {}
execute store result storage mcj:data stack[-1].value int 1 run scoreboard players get math_a mcj_data
data modify storage mcj:data intstack append from storage mcj:data stack[-1]

# args.countPtrPtr = intstack.length + 1
execute store result score math_a mcj_data run data get storage mcj:data intstack
scoreboard players add math_a mcj_data 1
data modify storage mcj:data stack append value {}
execute store result storage mcj:data stack[-1].value int 1 run scoreboard players get math_a mcj_data

# args.numDimensions = numDimensions
$data modify storage mcj:data stack append value {value:$(numDimensions)}

function mcj:stack/invoke {method:"$(class~mcj:BytecodeImpl~multianewarray(III)Ljava/lang/Object;)",num_args:"3",has_return:"true"}

$scoreboard players set math_a mcj_data $(numDimensions)
function mcj:heap/_multianewarray with storage mcj:data intstack[-1]
function mcj:intstack/pop