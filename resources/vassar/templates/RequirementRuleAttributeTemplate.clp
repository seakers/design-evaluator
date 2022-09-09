 {# rule.subobjective #}
 {# rule.measurement #}
 {# rule.NAstring #}
 {# rule.index: row[0].split("-",2)[1] #}
 {# rule.parent: row[0].split("-",2)[0] #}

 {# rule.attributes - list of object: attribute #}
 {# attribute.name #}
 {# attribute.thresholds #}
 {# attribute.scores #}
 {# attribute.justification #}


  {% for rule in rules %}

        ;;; BEGIN RULE: {{rule.subobjective}} ;;;

        (defrule REQUIREMENTS::{{rule.subobjective}}-attrib ?m <- (REQUIREMENTS::Measurement (taken-by ?whom)  (power-duty-cycle# ?pc) (data-rate-duty-cycle# ?dc)
        (Parameter "{{rule.measurement}}")
        {% for attribute in rule.attributes %}({{attribute.name}} ?val{{loop.index + 1}}&~nil) {% endfor %})

        =>

        (bind ?reason "") (bind ?new-reasons (create$ {{rule.NAstring}}))

        {% autoescape false %}
        {% for attribute in rule.attributes %}
            (bind ?x{{loop.index + 1}} (nth$ (find-bin-num ?val{{loop.index + 1}}  (create$ {{attribute.thresholds}}) ) (create$ {{attribute.scores}}))) (if (< ?x{{loop.index + 1}} 1.0) then (bind ?new-reasons (replace$  ?new-reasons {{loop.index + 1}} {{loop.index + 1}} "{{attribute.justification}}" )) (bind ?reason (str-cat ?reason  "{{attribute.justification}}")))
        {% endfor %}
        {% endautoescape %}

        (bind ?list (create$ {% for attribute in rule.attributes %} ?x{{loop.index + 1}} {% endfor %}))

        (assert (AGGREGATION::SUBOBJECTIVE (id {{rule.subobjective}}) (attributes {% for attribute in rule.attributes %} {{attribute.name}} {% endfor %}) (index {{rule.index}}) (parent {{rule.parent}}) (attrib-scores ?list) (satisfaction (*$ ?list)) (reasons ?new-reasons) (satisfied-by ?whom) (reason ?reason ) (requirement-id (?m getFactId)) (factHistory (str-cat "{R" (?*rulesMap* get REQUIREMENTS::{{rule.subobjective}}-attrib) " A" (call ?m getFactId) "}")))))

  {% endfor %}




  (deffacts REQUIREMENTS::init-subobjectives

  {% for rule in rules %}
    (AGGREGATION::SUBOBJECTIVE (satisfaction 0.0) (id {{rule.subobjective}}) (index {{rule.index}}) (parent {{rule.parent}}) (reasons (create$ {{rule.NAstring}})) {%- if loop.index == (loop.length - 1) %}(requirement-id -1){% endif %} (factHistory F0))
  {% endfor %}

  )



















