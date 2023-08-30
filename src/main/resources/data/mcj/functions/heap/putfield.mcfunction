$data modify storage mcj:data stack[-2].class set value "$(class)"
$data modify storage mcj:data stack[-2].name set value "$(name)"
function mcj:heap/_putfield with storage mcj:data stack[-2]