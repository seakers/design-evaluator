; --------------- RULE

{% for item in items %}

(defrule {{item.module()}}::inherit-{{item.template1().split("::")[1]}}-{{item.copySlotName1()}}-TO-{{item.template2().split("::")[1].trim()}}

    (declare (no-loop TRUE))

    {% if  item.copySlotType1().equalsIgnoreCase("slot")  %}?sub <- ({{item.template1()}} ({{item.copySlotName1()}} ?x&~nil){% else %}?sub <- ({{item.template1()}} ({{item.copySlotName1()}} $?x&:(> (length$ $?x) 0)){% endif %}{% if  item.matchingSlotType1.equalsIgnoreCase("slot")  %}({{item.matchingSlotName1()}} ?id&~nil) ){% else %}({{item.matchingSlotName1()}} $?id&:(> (length$ $?id) 0)) ){% endif %}

    ?old <- ({{item.template2()}}{% if  item.matchingSlotType1.equalsIgnoreCase("slot")  %}({{item.matchingSlotName2()}} ?id) (factHistory ?fh){% else %}({{item.matchingSlotName2()}} $?id) (factHistory ?fh){% endif %}{% if  item.matchingSlotType1().equalsIgnoreCase("slot")  %}({{item.copySlotName2()}} nil){% else %}({{item.copySlotName2()}} $?x&:(eq (length$ $?x) 0)){% endif %})

    =>

    (modify ?old ({{item.copySlotName2()}} ?x)(factHistory (str-cat "{R"(?*rulesMap* get {{item.module()}}::inherit-{{item.template1().split("::")[1]}}-{{item.copySlotName1()}}-TO-{{item.template2().split("::")[1]}}) " " ?fh " S"(call ?sub getFactId) "}")))
)

{% endfor %}


