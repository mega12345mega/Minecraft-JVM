$data modify storage mcj:data stack[-1].class set value "$(class)"
$data modify storage mcj:data stack[-1].name set value "$(name)"
function mcj:heap/_getfield with storage mcj:data stack[-1]