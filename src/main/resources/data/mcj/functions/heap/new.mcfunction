$function $(clinit)
function mcj:heap/malloc
$data modify storage mcj:data new set value "$(class)"
function mcj:heap/_new with storage mcj:data stack[-1]