(deffacts AGGREGATION::init-aggregation-facts


 (AGGREGATION::VALUE (sh-scores (repeat$ -1.0 {{items.size()}})) (sh-fuzzy-scores (repeat$ -1.0 {{items.size()}})) (weights  (create$ {% for item in items %} {{item.weight()}} {% endfor %})) (factHistory F0))

{% for item in items %}
 (AGGREGATION::STAKEHOLDER (id {{item.index_id()}}) (index {{loop.index + 1}}) (obj-fuzzy-scores (repeat$ -1.0 {{item.objectives().size()}})) (obj-scores (repeat$ -1.0 {{item.objectives().size()}})) (weights (create$ {% for objec in item.objectives() %} {{objec.weight()}} {% endfor %})) (factHistory F0))
{% endfor %}


{% for item in items %}
{% for objec in item.objectives() %}
 (AGGREGATION::OBJECTIVE (id {{objec.name()}}) (parent {{item.index_id()}}) (index {{loop.index + 1}}) (subobj-fuzzy-scores (repeat$ -1.0 {{objec.subobjectives().size()}})) (subobj-scores (repeat$ -1.0 {{objec.subobjectives().size()}})) (weights (create$ {% for subobjec in objec.subobjectives() %} {{subobjec.weight()}} {% endfor %})) (factHistory F0))
{% endfor %}
{% endfor %}


 )