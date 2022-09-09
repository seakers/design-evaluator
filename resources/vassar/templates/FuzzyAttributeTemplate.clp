

{% for item in items %}

    ;;; BEGIN FUZZY ATTRIBUTE RULE: {{item.attributeName}} ;;;

(defrule FUZZY::numerical-to-fuzzy-{{item.attributeName}}

    ?m <- (REQUIREMENTS::Measurement
        {% if item.parameter.equalsIgnoreCase("all") %}({{item.attributeName}}# ?num&~nil){% else %}(Parameter "{{item.parameter}}")({{item.shortAttributeName}}# ?num&~nil){% endif %}

        ({{item.attributeName}} nil)
        (factHistory ?fh))

        =>

        (bind ?value
            (numerical-to-fuzzy ?num
                (create$ {{item.valueList}})
                (create$ {{item.minList}})
                (create$ {{item.maxList}})
            )
        )
        (modify ?m ({{item.attributeName}} ?value) (factHistory (str-cat "{R" (?*rulesMap* get FUZZY::numerical-to-fuzzy-{{item.attributeName}}) " " ?fh "}")))
)

{% endfor %}