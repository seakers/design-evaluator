(deffacts {{template_header}}

{% for item in items %}
    ({{orbit_header}}

        (id {{item.Orbit().name()}})
        {% for attribute in item.Orbit().attributes() %}
        {% if attribute.value().matches("\[(.+)(,(.+))+\]") %}
        ({{ attribute.Orbit_Attribute().name() }} {{ createJessList(attribute.value()) }})
        {% else %}
        ({{ attribute.Orbit_Attribute().name() }}   {{ attribute.value() }})
        {% endif %}
        {% endfor %}
        (factHistory F{{loop.index}})

    )
{% endfor %}
)