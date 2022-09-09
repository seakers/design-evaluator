(deftemplate {{template_header}}

{% for item in items %}
    ({{ item.slot_type() }} {{ item.name() }})
{% endfor %}


)