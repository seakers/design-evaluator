(defglobal ?*q* = (new java.util.Vector))


(defrule CRITIQUE-COST::mass-check 
    "limits the dry-mass of a satellite"
    
    (MANIFEST::Mission (Name ?n)(satellite-dry-mass ?sdm&:(> ?sdm 3000)))
    =>
    (call ?*q* addElement (new java.lang.String
        (str-cat "N:Satellite in orbit " ?n " is too heavy: dry-mass is " (format nil "%2.2f" ?sdm) " kg.")))
    (call ?*q* addElement (new java.lang.String
        (str-cat "E:Satellite in orbit " ?n " is too heavy: dry-mass is " (format nil "%2.2f" ?sdm) " kg. Heavy satellites increase launch costs exponentially."))))



(defrule CRITIQUE-COST::satellite-size-comparison
    
    (CRITIQUE-COST-PARAM::satellite-max-size-ratio (value ?r&:(> ?r 2.5)) (big-name ?bn) (small-name ?sn))
    =>
    (if (<> ?bn ?sn) then
    (call ?*q* addElement (new java.lang.String
        (str-cat "N:It is desirable to have satellites of similar size: satellite " ?bn " is bigger than " ?sn " by a ratio of " (format nil "%2.2f" ?r) "." ))))
    (call ?*q* addElement (new java.lang.String
        (str-cat "E:It is desirable to have satellites of similar size to ensure buses can be reused and costs can be managed: satellite " ?bn " is bigger than " ?sn " by a ratio of " (format nil "%2.2f" ?r) "." ))))



(defrule CRITIQUE-COST::satellite-cost-comparison

    (CRITIQUE-COST-PARAM::satellite-max-cost-ratio (value ?r&:(> ?r 2.5))(big-name ?bn) (small-name ?sn))
    =>
    (if (<> ?bn ?sn) then
        (call ?*q* addElement (new java.lang.String
            (str-cat "N:It is desirable to have satellites of similar cost: satellite " ?bn " costs more than " ?sn " by a ratio of " (format nil "%2.2f" ?r) ".")))
        (call ?*q* addElement (new java.lang.String
            (str-cat "E:It is desirable to have satellites of similar cost: satellite " ?bn " costs more than " ?sn " by a ratio of " (format nil "%2.2f" ?r) ".")))))



(defrule CRITIQUE-COST::launch-packaging-factors

    (CRITIQUE-COST-PARAM::launch-packaging-factors (name ?n)(performance-mass-ratio ?r-pm) 
    (diameter-ratio ?r-dia) (height-ratio ?r-h))
    =>
    (bind ?m (min$ (create$ (bind ?f1 (- 1 ?r-pm)) (bind ?f2 (- 1 ?r-dia)) (bind ?f3 (- 1 ?r-h)))))
    (if (= ?m ?f1) then (bind ?lf "mass"))
    (if (= ?m ?f2) then (bind ?lf "diameter"))
    (if (= ?m ?f3) then (bind ?lf "height"))
    (if (> ?m 0.2)
    then (
        call ?*q* addElement (new java.lang.String
            (str-cat "N:It is desirable to fill launch vehicles to their capacity. They are currently only used at X %" ?n " is " ?lf ": " ?m ".")))
        call ?*q* addElement (new java.lang.String
            (str-cat "E:It is desirable to fill launch vehicles to their capacity to opptimize the launch investment. They are currently only used at X %" ?n " is " ?lf ": " ?m "."))))