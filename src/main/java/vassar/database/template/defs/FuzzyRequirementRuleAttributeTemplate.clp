{# reqRule #}

{# subobj / rule.currentSubobj --> row[0] #}
{# index  --> row[0].split("-",2)[1] #}
{# parent --> row[0].split("-",2)[0] #}
{# param --> row[1] #}
{# m.toJessList(thresholds) --> row[4] #}
{# attrib --> type row[3] + threshold row[4] + scores row[5] #}
{# numAttrib -> index of attribute for that rule #}
{# StringUtils.repeat("N-A ",numAttrib) #}
{# justif --> row[6] #}
{# attribs --> row[2] concat for rule #}


 {% for rule in rules.rules %}


    {# lhs #}
    (defrule FUZZY-REQUIREMENTS::{{rule.subobj}}-attrib ?m <- (REQUIREMENTS::Measurement (taken-by ?whom)  (power-duty-cycle# ?pc) (data-rate-duty-cycle# ?dc)  (Parameter "{{rule.param}}")
    ({{rule.attrib}} ?val{{rule.numAttrib}}&~nil)

    {# rhs0 #}
     ) => (bind ?reason "") (bind ?new-reasons (create$ {{rule.naFuzzyString}}))

    {# rhs #}
     (bind ?x{{rule.numAttrib}} (nth$ (find-bin-num ?val{{rule.numAttrib}} {{m.toJessList(rule.thresholds)}}){{m.toJessList(scores)}}))

     {% autoescape false %}
     (if (< ?x{{rule.numAttrib}} 1.0) then (bind ?new-reasons (replace$  ?new-reasons {{rule.numAttrib}} {{rule.numAttrib}} "{{rule.justif}}" )) (bind ?reason (str-cat ?reason  "{{rule.justif}}")))
     {% endautoescape %}

    {# rhs2 - finished #}
     (bind ?list (create$ ?x{{rule.numAttrib}} ?dc ?pc))


      (assert (AGGREGATION::SUBOBJECTIVE (id {{rule.currentSubobj}}) (attributes {{rule.attribs}} data-rate-duty-cycle# power-duty-cycle#) (index {{rule.index}}) (parent {{rule.parent}} )
      (attrib-scores ?list) (satisfaction (*$ ?list)) (fuzzy-value (new FuzzyValue "Value" (call (new FuzzyValue "Value" (new Interval "interval" (*$ ?list) (*$ ?list)) "utils" (MatlabFunctions getValueHashmap)) getFuzzy_val) "utils" (MatlabFunctions getValueInvHashmap))) (reasons ?new-reasons) (satisfied-by ?whom) (reason ?reason ) (factHistory (str-cat "{R" (?*rulesMap* get (?*rulesMap* get FUZZY-REQUIREMENTS::{{rule.subobj}}-attrib)) " A" (call ?m getFactId) "}"))))


 {% endfor %}