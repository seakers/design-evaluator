(deffacts {{template_header}}

{% for item in items %}
    ({{instrument_header}}
    (Name {{item.Instrument().name()}})
    {% for attribute in item.Instrument().attributes() %}
        {% if attribute.Instrument_Attribute().name().equalsIgnoreCase("Intent")  %}
             ({{ attribute.Instrument_Attribute().name() }} "{{ attribute.value() }}")
        {% elseif attribute.Instrument_Attribute().name().equalsIgnoreCase("Concept")  %}
             ({{ attribute.Instrument_Attribute().name() }} "{{ attribute.value() }}")
        {% else %}
             ({{ attribute.Instrument_Attribute().name() }} {{ attribute.value() }})
        {% endif %}
    {% endfor %} (factHistory F{{loop.index}}))
{% endfor %}
)