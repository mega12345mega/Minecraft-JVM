data modify storage mcj:data stack[-1].pointer set from storage mcj:data stack[-3].value
data modify storage mcj:data stack[-1].index set from storage mcj:data stack[-2].value
function mcj:heap/_astore with storage mcj:data stack[-1]