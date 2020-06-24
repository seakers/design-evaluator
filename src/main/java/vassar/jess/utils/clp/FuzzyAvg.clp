(deffunction fuzzy-avg (?v1 ?v2)
    (if (or (and (eq ?v1 High) (eq ?v2 Low)) (and (eq ?v1 Low) (eq ?v2 High))) then
     "Medium"
     else (fuzzy-min Accuracy ?v1 ?v2)))