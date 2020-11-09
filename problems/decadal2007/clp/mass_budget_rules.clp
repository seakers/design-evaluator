;; ***************
;; Interference penalties (salience 20)
;; ***************

(defquery MASS-BUDGET::search-instrument-by-name
    (declare (variables ?name))
    (DATABASE::Instrument (Name ?name) (Thermal-control ?thermal) (Illumination ?illum)
        (scanning ?scan) (has-deployment-mechanism ?dep) (average-data-rate# ?rb)
        (band ?band) (Pointing-requirements ?point)
        )
    )


; thermal penalty if any instrument has active cryocooling
(deffunction get-thermal-control (?instr)
    (bind ?result (run-query* MASS-BUDGET::search-instrument-by-name ?instr))
    (?result next)
    (bind ?thermal (?result getString thermal))
    (return ?thermal)
    )

(defrule MASS-BUDGET::thermal-penalty-check
    "This rule finds out whether there is any instrument in the mission
    requiring active cryo-cooling and updates thermal-penalty boolean value"
    (declare (salience 20))
    ?miss <- (MANIFEST::Mission (thermal-penalty nil) (instruments $?payload))
    =>
    (bind ?penalty 0)
    (foreach ?instr ?payload
        (bind ?pen (get-thermal-control ?instr))
        ;(printout t "th penalty of " ?instr " is " ?pen crlf )
        (if (and (eq ?pen "Active-cryocooler") (neq ?penalty 1)) then
           (modify ?miss (thermal-penalty 1))
                (bind ?penalty 1)
            ;(printout t "th penalty in = " ?penalty crlf )
            )
        )
    ;(printout t "th penalty out = " ?penalty crlf )
    (if (eq ?penalty 0) then (modify ?miss (thermal-penalty 0)))
)

; ADCS penalty if any instrument has high pointing requiremetns
(deffunction get-pointing-requirements (?instr)
    (bind ?result (run-query* MASS-BUDGET::search-instrument-by-name ?instr))
    (?result next)
    (bind ?point (?result getString point))
    (return ?point)
    )

(defrule MASS-BUDGET::ADCS-penalty-check
    "This rule finds out whether there is any instrument in the mission
    with high pointing requirements and updates ADCS-penalty boolean value"
    (declare (salience 20))
    ?miss <- (MANIFEST::Mission (ADCS-penalty nil) (instruments $?payload))
    =>
    (bind ?penalty 0)
    (foreach ?instr ?payload
        (bind ?pen (get-pointing-requirements ?instr))
        (if (and (eq ?pen "High") (neq ?penalty 1)) then
            (modify ?miss (ADCS-penalty 1))
               (bind ?penalty 1)
            	;(printout t "ADCS penalty in = " ?penalty crlf )
            )
        )
    ;(printout t "ADCS penalty out = " ?penalty crlf )
    (if (eq ?penalty 0) then (modify ?miss (ADCS-penalty 0)))
    )

; EMC penalty
(deffunction get-illumination (?instr)
    (bind ?result (run-query* MASS-BUDGET::search-instrument-by-name ?instr))
    (?result next)
    (bind ?illum (?result getString illum))
    (return ?illum)
    )

(deffunction get-spectral-band (?instr)
    (bind ?result (run-query* MASS-BUDGET::search-instrument-by-name ?instr))
    (?result next)
    (bind ?band (?result getString band))
    (return ?band)
    )

(defrule MASS-BUDGET::EMC-penalty-found
    "This rule finds out whether there are any pair of instruments in the mission
    in which one is active, the other passive, and both use the same MW band"
    (declare (salience 20))
    ?miss <- (MANIFEST::Mission (EMC-penalty nil) (instruments $?payload))
    (test (> (length$ ?payload) 0))
    (DATABASE::Instrument (Name ?n1) (Illumination Active) (band ?band&~nil))
    (DATABASE::Instrument (Name ?n2) (Illumination Passive) (band ?band&~nil))
    (test (eq (sub-string 1 2 ?band) MW))
    (test (integerp (member$ ?n1 ?payload)))
    (test (integerp (member$ ?n2 ?payload)))

    =>
    (modify ?miss (EMC-penalty 1))

    )

(defrule MASS-BUDGET::EMC-penalty-to-zero
    "If EMC penalty is nil put it to zero"
    (declare (salience 18))
    ?miss <- (MANIFEST::Mission (EMC-penalty nil) )

    =>
    (modify ?miss (EMC-penalty 0))

    )


; mechanisms penalty
(deffunction get-deployment-mechanism (?instr)
    (bind ?result (run-query* MASS-BUDGET::search-instrument-by-name ?instr))
    (?result next)
    (bind ?dep (?result getString dep))
    (return ?dep)
    )

(defrule MASS-BUDGET::mechanisms-penalty-check
    "This rule finds out whether there is any instrument in the mission
    with a deployment mechanism and updates mechanism-penalty boolean value"
    (declare (salience 20))
    ?miss <- (MANIFEST::Mission (mechanisms-penalty nil) (instruments $?payload))
    =>
    (bind ?penalty 0)
    (foreach ?instr ?payload
        (bind ?dep (get-deployment-mechanism ?instr))
        (if (and (eq ?dep "yes") (neq ?penalty 1)) then
            (modify ?miss (mechanisms-penalty 1))
               (bind ?penalty 1)
            ;(printout t "MECH penalty in = " ?penalty crlf )
            )
        )
    ;(printout t "MECH penalty out = " ?penalty crlf )
    (if (eq ?penalty 0) then (modify ?miss (mechanisms-penalty 0)))
    )

; scanning penalty
(deffunction get-scanning (?instr)
    (bind ?result (run-query* MASS-BUDGET::search-instrument-by-name ?instr))
    (?result next)
    (bind ?scan (?result getString scan))
    (return ?scan)
    )

(defrule MASS-BUDGET::scanning-penalty-check
    "This rule finds out whether there is any instrument in the mission
    with a scanning requirement and updates scanning-penalty boolean value"
    (declare (salience 20))
    ?miss <- (MANIFEST::Mission (scanning-penalty nil) (instruments $?payload))
    =>
    (bind ?penalty 0)
    (foreach ?instr ?payload
        (bind ?scan (get-scanning ?instr))
        (if (and (neq ?scan "no-scanning") (neq ?scan nil) (neq ?penalty 1)) then
             (modify ?miss (scanning-penalty 1))
                 (bind ?penalty 1)
            ;(printout t "SCAN penalty in = " ?penalty crlf )
            )
        )
    ;(printout t "SCAN penalty out = " ?penalty crlf )
    (if (eq ?penalty 0) then (modify ?miss (scanning-penalty 0))
        elif (eq ?penalty 1) then (modify ?miss (scanning-penalty 1)))
    )

; data rate penalty
(deffunction get-data-rate (?instr)
    (bind ?result (run-query* MASS-BUDGET::search-instrument-by-name ?instr))
    (?result next)
    (bind ?rb (?result getString rb))
    (return ?rb)
    )

(defrule MASS-BUDGET::datarate-penalty-check
    "This rule finds out whether this a data rate intensive mission and updates the corresponding penalty"
    (declare (salience 20))
    ?miss <- (MANIFEST::Mission (datarate-penalty nil) (instruments $?payload))
    =>
    (bind ?penalty 0)
    (foreach ?instr ?payload
        (bind ?rb (get-data-rate ?instr))
        (if (and (> ?rb 100) (neq ?penalty 1)) then
            (modify ?miss (datarate-penalty 1))
                (bind ?penalty 1)
            ;(printout t "DATARATE penalty in = " ?penalty crlf )
            )
        )
    ;(printout t "DATARATE penalty out = " ?penalty crlf )
    (if (eq ?penalty 0) then (modify ?miss (datarate-penalty 0)))
    )

;; ***************
;; Subsystem design (salience 10)
;; ***************
; structure

(defrule MASS-BUDGET::design-structure-subsystem
    "Computes structure subsystem mass using rules of thumb"
    (declare (salience 10))
    ?miss <- (MANIFEST::Mission (structure-mass# nil) (mechanisms-penalty ?mech-pen)
        (payload-mass# ?m) (thermal-penalty ?th-pen) (EMC-penalty ?emc-pen)
        (scanning-penalty ?sc-pen))
    =>
    (if (eq ?mech-pen nil) then (bind ?mech-pen 0))
    (if (eq ?th-pen nil) then (bind ?th-pen 0))
    (if (eq ?emc-pen nil) then (bind ?emc-pen 0))
    (if (eq ?sc-pen nil) then (bind ?sc-pen 0))
    (bind ?struct-mass (* 0.5462 ?m)); 0.75
    (bind ?struct-mass (+ ?struct-mass (* ?mech-pen 0.05 ?m)))
    (if (eq ?emc-pen nil) then (bind ?emc-pen 0))
    (bind ?struct-mass (+ ?struct-mass (* ?emc-pen 0.05 ?m)))
    (bind ?struct-mass (+ ?struct-mass (* ?sc-pen 0.05 ?m)))
    (bind ?struct-mass (+ ?struct-mass (* ?th-pen 0.05 ?m)))
    (modify ?miss (structure-mass# ?struct-mass))
    )

; power is done

; comm and obdh
(defrule MASS-BUDGET::design-comm-subsystem
    "Computes comm subsystem mass using rules of thumb"
    (declare (salience 10))
    ?miss <- (MANIFEST::Mission (comm-OBDH-mass# nil) (datarate-penalty ?pen) (payload-mass# ?m) (sat-data-rate-per-orbit# ?rbo&~nil))
    =>
    (if (eq ?pen nil) then (bind ?pen 0))
    (if (eq ?pen 1) then (bind ?comm-mass-coeff 0.1387); 0.44
        else (bind ?comm-mass-coeff 0.0983); 0.22
        )
    (if (> ?rbo 50) then (bind ?pen2 2.0) else (bind ?pen2 1.0)); non proportionate penalty for very high complexity
    (bind ?comm-mass (* ?m ?comm-mass-coeff ?pen2))
    (modify ?miss (comm-OBDH-mass# ?comm-mass))
    )

; adcs
(defrule MASS-BUDGET::design-ADCS-subsystem
    "Computes ADCS subsystem mass using rules of thumb"
    (declare (salience 10))
    ?miss <- (MANIFEST::Mission (ADCS-mass# nil) (ADCS-penalty ?pen) (orbit-altitude# ?h) (payload-mass# ?m))
    =>
    (if (eq ?pen nil) then (bind ?pen 0))
    (if (eq ?pen 1) then (bind ?adcs-mass-coeff 0.1792); 0.44
        else (bind ?adcs-mass-coeff 0.1301); 0.22
        )
    (bind ?adcs-mass (* ?m ?adcs-mass-coeff))
    (if  (< ?h 500) then (bind ?adcs-mass (* ?adcs-mass 3)))
    (modify ?miss (ADCS-mass# ?adcs-mass))
    )

; thermal
(defrule MASS-BUDGET::design-thermal-subsystem
    "Computes thermal subsystem mass using rules of thumb"
    (declare (salience 10))
    ?miss <- (MANIFEST::Mission (thermal-mass# nil) (thermal-penalty ?pen) (payload-mass# ?m))
    =>
    (if (eq ?pen nil) then (bind ?pen 0))
    (if (eq ?pen 1) then (bind ?thermal-mass-coeff 0.0925)
        else (bind ?thermal-mass-coeff 0.0607)
        )
    (bind ?thermal-mass (* ?m ?thermal-mass-coeff))
    (modify ?miss (thermal-mass# ?thermal-mass))
    )

; propulsion

(defrule MASS-BUDGET::design-propulsion-subsystem
    "Computes propulsion subsystem mass using rules of thumb"
    (declare (salience 10))
    ?miss <- (MANIFEST::Mission (propulsion-mass# nil) (payload-mass# ?m) (orbit-altitude# ?h&~nil))
    =>
   	(if (< ?h 500) then (bind ?dV 25) else (bind ?dV 5))
    (bind ?prop-mass (* ?m 0.1763)); 0.14
    (modify ?miss (propulsion-mass# ?prop-mass) (delta-V# ?dV))
    )

;; ***************
;; Overall mass budget (salience 0)
;; ***************
(defrule MASS-BUDGET::add-subsystem-masses
    "Computes the sum of subsystem masses"
    ?miss <- (MANIFEST::Mission (propulsion-mass# ?prop-mass&~nil) (structure-mass# ?struct-mass&~nil)
        (comm-OBDH-mass# ?comm-mass&~nil) (ADCS-mass# ?adcs-mass&~nil) (EPS-mass# ?eps-mass&~nil)
        (thermal-mass# ?thermal-mass&~nil) (payload-mass# ?payload&~nil) (satellite-mass# nil)
        (delta-V# ?dV&~nil) (lifetime ?life&~nil))
    =>

    (bind ?sat-mass (+ ?prop-mass ?struct-mass ?eps-mass ?adcs-mass ?comm-mass ?payload ?thermal-mass)); dry mass
    (bind ?mp (* ?sat-mass (- (exp (/ (* ?dV ?life) (* 9.81 200))) 1))); propellant mass for ISp=200s
    ;(printout t "dry mass " ?sat-mass " propellant mass " ?mp crlf)
    (modify ?miss (satellite-mass# (+ ?sat-mass ?mp))); wet mass
    )

(defquery MASS-BUDGET::get-mass-budget
    (declare (variables ?name))
    ?miss <- (MANIFEST::Mission (Name ?name) (propulsion-mass# ?prop-mass&~nil) (structure-mass# ?struct-mass&~nil)
        (comm-OBDH-mass# ?comm-mass&~nil) (ADCS-mass# ?adcs-mass&~nil) (EPS-mass# ?eps-mass&~nil)
        (thermal-mass# ?thermal-mass&~nil) (payload-mass# ?payload&~nil) (satellite-mass# ?tot)
        )
    )