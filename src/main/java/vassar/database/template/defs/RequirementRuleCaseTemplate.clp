






{% for rule in items %}


(defrule REQUIREMENTS::{{rule.subobjective().name()}}-attrib{{rule.rule()}}
    ?m <- (REQUIREMENTS::Measurement
            (Parameter "{{rule.measurement().name()}}") (science-multiplier ?mult)
            {% for attr in rule.measurement_attributes() %}
                {% if attr.operation().equalsIgnoreCase("ContainsRegion") %}
                    ({{attr.Measurement_Attribute().name()}} ?val{{loop.index + 1}}&~nil)
                {% elseif attr.operation().equalsIgnoreCase("SameOrBetter") %}
                    ({{attr.Measurement_Attribute().name()}} ?val{{loop.index + 1}}&~nil)
                {% else %}
                    ({{attr.Measurement_Attribute().name()}} {{attr.value()}})
                {% endif %}
            {% endfor %}
            (taken-by ?who)
           )

    {# TEST FACT #}
    {% for attr in rule.measurement_attributes() %}

        {% if attr.operation().equalsIgnoreCase("ContainsRegion") %}
            (test (ContainsRegion ?val{{loop.index + 1}} {{attr.value()}}))
        {% elseif attr.operation().equalsIgnoreCase("SameOrBetter") %}
            (test (>= (SameOrBetter {{attr.Measurement_Attribute().name()}} ?val{{loop.index + 1}} {{attr.value()}}) 0))
        {% endif %}

    {% endfor %}

    =>

    (assert

        (REASONING::{% if rule.rule().equalsIgnoreCase("nominal") %}fully-satisfied{% else %}partially-satisfied{% endif %}
            (subobjective {{rule.subobjective().name()}})
            (parameter "{{rule.measurement().name()}}")
            (value {{rule.value()}})
            (objective "{{rule.text()}}")
            (attribute "{{rule.description()}}")
            (taken-by ?who)
            (requirement-id (?m getFactId))
        )
    )
    (bind ?*subobj-{{rule.subobjective.name()}}* (max ?*subobj-{{rule.subobjective.name()}}* (* {{rule.value()}} ?mult)))
    ;(bind ?*subobj-{{rule.subobjective.name()}}* (max ?*subobj-{{rule.subobjective.name()}}* {{rule.value()}}))
)




{% endfor %}