(deffacts {{template_header}}

{% for item in items %}
    ({{instrument_header}}
    (Name {{item.Instrument().name()}})
    {% for attribute in item.Instrument().attributes() %}
        ({{ attribute.Instrument_Attribute().name() }} {{ attribute.value() }})
    {% endfor %} (factHistory F{{loop.index}}))
{% endfor %}
)