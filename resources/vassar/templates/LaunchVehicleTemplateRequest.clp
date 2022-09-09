(deffacts {{template_header}}

{% for item in items %}
    ({{launch_vehicle_header}}

        (id {{item.Launch_Vehicle().name()}})
        {% for attribute in item.Launch_Vehicle().attributes() %}

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