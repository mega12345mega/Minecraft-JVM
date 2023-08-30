$data modify storage mcj:data print.target set value "$(target)"
$data modify storage mcj:data print.value set from storage mcj:data $(target)
function mcj:_print with storage mcj:data print