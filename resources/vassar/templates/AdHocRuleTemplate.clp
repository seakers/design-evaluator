(deftemplate DATABASE::list-of-instruments (multislot list) (slot factHistory))
(deffacts DATABASE::list-of-instruments (DATABASE::list-of-instruments (list

(create$
    {% for item in items %}
         {{item}}
         {# {item.instrument().name()} #}
    {% endfor %}
))

(factHistory 0)))
