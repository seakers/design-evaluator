{# RULE OBJECT #}


{% for rule in rules %}


    {% for subobj in rule.subobjList %}
        (defrule SYNERGIES::stop-improving-{{rule.meas_num}} ?fsat <- (REASONING::fully-satisfied (subobjective {{subobj}}) (factHistory ?fh))

         => (assert (REASONING::stop-improving (Measurement "{{rule.meas}}")
         (factHistory (str-cat "{R" (?*rulesMap* get SYNERGIES::stop-improving-{{rule.meas_num}}) " A" (call ?fsat getFactId) "}"))
         )))

    {% endfor %}



{% endfor %}