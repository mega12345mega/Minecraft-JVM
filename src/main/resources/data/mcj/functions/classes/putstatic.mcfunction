$function $(clinit)
$data modify storage mcj:data classes."$(class)".fields.$(name).value set from storage mcj:data stack[-1].value
function mcj:stack/pop