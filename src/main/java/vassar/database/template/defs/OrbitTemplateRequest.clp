(deffacts {{template_header}}

{% for item in items %}
    ({{orbit_header}}

        (id {{item.name()}})
        {% for attribute in item.attributes() %}

            {% if attribute.value().matches("\[(.+)(,(.+))+\]") %}
                    ({{ attribute.attribute().name() }} {{ createJessList(attribute.value()) }})
            {% else %}
                    ({{ attribute.attribute().name() }}   {{ attribute.value() }})
            {% endif %}

        {% endfor %}
        (factHistory F{{loop.index}})

    )
{% endfor %}
)