$data modify storage mcj:data invokevirtual set value {num_args:"$(num_args)",has_return:"$(has_return)",vtable:"$(vtable)",method:"$(method)"}
$function mcj:stack/_invokevirtual with storage mcj:data stack[-$(num_args)]
function mcj:stack/_2invokevirtual with storage mcj:data invokevirtual
function mcj:stack/invoke with storage mcj:data invokevirtual