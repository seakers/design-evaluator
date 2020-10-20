(defrule REQUIREMENTS::WEA1-1-attribnominal
    ?m <- (REQUIREMENTS::Measurement
            (Parameter "Cloud liquid water")
            (Region-of-interest ?x1&~nil)
            (Coverage-of-region-of-interest Global)
            (Horizontal-Spatial-Resolution ?x2&~nil)
            (Accuracy ?x3&~nil)(taken-by ?who)
        )

        (test (ContainsRegion ?x1 Global))
        (test (>= (SameOrBetter Horizontal-Spatial-Resolution ?x2 Very-low-10-100km) 0))
        (test (>= (SameOrBetter Accuracy ?x3 Medium) 0))

        =>

        (assert
            (REASONING::fully-satisfied
                (subobjective WEA1-1)
                (parameter "Cloud liquid water")
                (value 1)
                (objective " Boundary Layer Processes (W-1a)")
                (attribute "Conditions for full satisfaction ")
                (taken-by ?who)
                (requirement-id (?m getFactId))
                (factHistory (str-cat "{R" (?*rulesMap* get REQUIREMENTS::WEA1-1-attribnominal ) " A" (call ?m getFactId) "}"))
            )
        )
        (bind ?*subobj-WEA1-1* (max ?*subobj-WEA1-1* 1 ))
)