$data modify storage mcj:data print_target set value "$(target)"
$data modify storage mcj:data print_value set from storage mcj:data $(target)
function mcj:_print with storage mcj:data