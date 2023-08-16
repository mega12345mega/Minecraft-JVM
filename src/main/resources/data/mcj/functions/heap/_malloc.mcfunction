# malloc
$data modify storage mcj:data heap.v$(heap_uuid) set value {}

# mcj:stack/push pointer
$function mcj:stack/push_const {value:"$(heap_uuid)"}