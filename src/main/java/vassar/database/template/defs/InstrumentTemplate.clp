(deffacts {{template_header}}
{% for item in items %}
    ({{instrument_header}} (Name {{item.name()}})
    {% for attribute in item.attributes() %}
        ({{ attribute.Instrument_Attribute().name() }} {{ attribute.value() }})
    {% endfor %} (factHistory F{{loop.index}}))
{% endfor %}
)