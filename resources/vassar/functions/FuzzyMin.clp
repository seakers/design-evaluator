(deffunction fuzzy-min (?att ?v1 ?v2)
    (if (<= (SameOrBetter ?att ?v1 ?v2) 0) then
    ?v1 else ?v2))