data modify storage mcj:data stack[-1] set value {value:0b}
$execute unless data storage mcj:data heap.v$(value) run return 0
$data modify storage mcj:data instanceof.type set from storage mcj:data heap.v$(value).type
function mcj:heap/_2instanceof with storage mcj:data instanceof