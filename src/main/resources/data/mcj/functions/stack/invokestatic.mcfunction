function mcj:localvars/push_to_intstack
$function mcj:localvars/pop_vars_from_stack {amount:"$(num_args)"}
function mcj:stack/push_to_intstack

$function $(method)

$execute if data storage mcj:data $(has_return) run data modify storage mcj:data return set from storage mcj:data stack[-1]
function mcj:stack/pop_from_intstack
function mcj:localvars/pop_from_intstack
$execute if data storage mcj:data $(has_return) run data modify storage mcj:data stack append from storage mcj:data return