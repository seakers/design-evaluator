{# INSTRUMENT CAPABILITY RULES #}

{% for instrument in items %}

    ;;; {{instrument.name}} CAPABILITY RULES ;;;

    {# call #}
(defrule MANIFEST::{{instrument.name}}-init-can-measure
        (declare (salience -20))
        ?this <- (CAPABILITIES::Manifested-instrument  (Name ?ins&{{instrument.name}}) (Id ?id) (flies-in ?miss) (Intent ?int) (Spectral-region ?sr) (orbit-type ?typ) (orbit-altitude# ?h) (orbit-inclination ?inc) (orbit-RAAN ?raan) (orbit-anomaly# ?ano) (Illumination ?il) (factHistory ?fh))
        (not (CAPABILITIES::can-measure (instrument ?ins) (in-orbit ?miss) (can-take-measurements no))) ; CONDITION ;
        =>
        (assert (CAPABILITIES::can-measure (in-mission ?miss) (instrument ?ins) (orbit-type ?typ) (orbit-altitude# ?h) (orbit-inclination ?inc) (data-rate-duty-cycle# nil) (power-duty-cycle# nil)(orbit-RAAN ?raan) (in-orbit (eval (str-cat ?typ "-" ?h "-" ?inc "-" ?raan))) (can-take-measurements yes) (reason "by default") (copied-to-measurement-fact no)(factHistory (str-cat "{R" (?*rulesMap* get MANIFEST::{{instrument.name}}-init-can-measure) " A" (call ?this getFactId) "}"))))
    )


    {# call2 #}
(defrule CAPABILITIES-GENERATE::{{instrument.name}}-measurements
        ?this  <- (CAPABILITIES::Manifested-instrument  (Name ?ins&{{instrument.name}}) (Id ?id) (flies-in ?miss) (Intent ?int) (orbit-string ?orb) (Spectral-region ?sr) (orbit-type ?typ) (orbit-altitude# ?h) (orbit-inclination ?inc) (orbit-RAAN ?raan) (orbit-anomaly# ?ano) (Illumination ?il) (factHistory ?fh1)
            (avg-revisit-time-cold-regions# ?rcold)
            (avg-revisit-time-global# ?rglobal)
            (avg-revisit-time-northern-hemisphere# ?rnh)
            (avg-revisit-time-southern-hemisphere# ?rsh)
            (avg-revisit-time-tropics# ?rtropics)
            (avg-revisit-time-US# ?rus)
        )
        ?this2 <- (CAPABILITIES::can-measure (instrument ?ins) (in-orbit ?orb) (can-take-measurements yes) (data-rate-duty-cycle# ?dc-d) (power-duty-cycle# ?dc-p) (copied-to-measurement-fact no)(factHistory ?fh2))
        =>
        (if (and (numberp ?dc-d) (numberp ?dc-p)) then (bind ?*science-multiplier* (min ?dc-d ?dc-p)) else (bind ?*science-multiplier* 1.0))
        (if (and (numberp ?dc-d) (numberp ?dc-p)) then (bind ?mult (min ?dc-d ?dc-p)) else (bind ?mult 1.0))
        ;(bind ?*science-multiplier* 1.0) (bind ?mult 1.0)
        ;(printout t "--> SCIENCE MULTIPLIER: " ?*science-multiplier* crlf)
        (assert (CAPABILITIES::resource-limitations (data-rate-duty-cycle# ?dc-d) (power-duty-cycle# ?dc-p) (factHistory (str-cat "{R" (?*rulesMap* get CAPABILITIES::{{instrument.name}}-measurements) " A" (call ?this getFactId) " A" (call ?this2 getFactId) "}"))))
{% for measurement in instrument.measurements %}
        (assert (REQUIREMENTS::Measurement (Parameter "{{measurement.name}}") (science-multiplier ?mult) {% for attribute in measurement.attributes %}{% if (attribute.value.equalsIgnoreCase("nil")) %}{% else %}({{attribute.key}} {{attribute.value}}) {% endif %}{% endfor %} (taken-by {{instrument.name}}) (flies-in ?miss) (orbit-altitude# ?h) (orbit-RAAN ?raan) (orbit-anomaly# ?ano) (Id {{instrument.name}}{{loop.index}}) (Instrument {{instrument.name}}) (factHistory (str-cat "{R" (?*rulesMap* get CAPABILITIES::{{instrument.name}}-measurements) " A" (call ?this getFactId) " A" (call ?this2 getFactId) "}"))
            (avg-revisit-time-cold-regions# ?rcold)
            (avg-revisit-time-global# ?rglobal)
            (avg-revisit-time-northern-hemisphere# ?rnh)
            (avg-revisit-time-southern-hemisphere# ?rsh)
            (avg-revisit-time-tropics# ?rtropics)
            (avg-revisit-time-US# ?rus)
        ))
{% endfor %}
        (assert (SYNERGIES::cross-registered (measurements {{instrument.listOfMeasurements}} ) (degree-of-cross-registration instrument) (platform ?id  ) (factHistory (str-cat "{R" (?*rulesMap* get CAPABILITIES::{{instrument.name}}-measurements) " A" (call ?this getFactId) " A" (call ?this2 getFactId) "}"))))
        (modify ?this (measurement-ids {{instrument.listOfMeasurements}}) (factHistory (str-cat "{R" (?*rulesMap* get CAPABILITIES::{{instrument.name}}-measurements) " " ?fh1 " S" (call ?this2 getFactId) "}")))
        (modify ?this2 (copied-to-measurement-fact yes) (factHistory (str-cat "{R" (?*rulesMap* get CAPABILITIES::{{instrument.name}}-measurements) " " ?fh1 " S" (call ?this2 getFactId) "}")))
    )

{% endfor %}