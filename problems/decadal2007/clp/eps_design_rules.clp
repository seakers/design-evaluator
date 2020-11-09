(defquery EPS-DESIGN::search-orbit-by-name
    (declare (variables ?orbit))
    (DATABASE::Orbit (id ?orbit) (fraction-of-sunlight# ?frac) (period# ?period)
        (worst-sun-angle# ?angle) (max-eclipse-time# ?maxtime)
        )
    )
(deffunction get-orbit-info (?orbit)
    ;(printout t "getting orbit info for " ?orbit crlf )
    (bind ?result (run-query* EPS-DESIGN::search-orbit-by-name ?orbit))
    (?result next)
    (bind ?frac (?result getDouble frac))
    (bind ?angle (?result getDouble angle))
    (bind ?period (?result getDouble period))
    (bind ?list (create$ ?frac ?angle ?period))
    (return ?list)
    )

(deffunction get-orbit-altitude (?orbit)
    (return (matlabf get_orbit_altitude ?orbit))
    )

(deffunction get-dod (?orbit) ; see SMAD Page 422
    (bind ?type (matlabf get_orbit_type ?orbit))
    (bind ?raan (matlabf get_orbit_raan ?orbit))
    (if (eq ?type GEO) then (bind ?dod 0.8)
        elif (and (eq ?type SSO) (eq ?raan DD)) then (bind ?dod 0.6)
        else (bind ?dod 0.4)
        )
    (return ?dod)
    )



(defrule EPS-DESIGN::design-EPS
    ?miss<- (MANIFEST::Mission (payload-power# ?p&~nil) (payload-peak-power# ?peak&~nil) (EPS-mass# nil) (in-orbit ?orb)
        (payload-mass# ?m) (satellite-BOL-power# nil) (lifetime ?life))
    =>
    (bind ?orbit-info (get-orbit-info ?orb))
    (bind ?frac (nth$ 1 ?orbit-info))
    (bind ?angle (nth$ 2 ?orbit-info))
    (bind ?T (nth$ 3 ?orbit-info))
    (bind ?dod (get-dod ?orb))
    (bind ?epsm (matlabf mass_EPS ?p ?peak ?frac ?angle ?T ?life ?m ?dod))
    (bind ?pow (matlabf power_EPS ?p ?peak ?frac ?angle ?T ?life ?m ?dod))
    (if (> ?pow 15000) then (bind ?pen 4.0) elif (> ?pow 10000) then (bind ?pen 3.0) elif (> ?pow 7500) then (bind ?pen 2.0) else (bind ?pen 1.0)); if more than 10kW BOL then complexity becomes exponential
    ;(printout t "penalty eps = " ?pen crlf)
    (modify ?miss (EPS-mass# (* ?epsm ?pen)) (satellite-BOL-power# ?pow))
    )

;(defrule EPS-DESIGN::compute-instrument-duty-cycles
;    "This rule computes instrument duty cycles as min (10kW /sat BOL power,1).
;    These duty cycles are then used as multipliers of the science value of the instrument:
;    If an instrument has an 80% duty cycle, then its value is multiplied by 0.8"
;
;    ?i <- (CAPABILITIES::Manifested-instrument (flies-in ?miss) (duty-cycle# nil))
;    (MANIFEST::Mission (Name ?miss) (instruments $?instr) (satellite-BOL-power# ?pow&~nil))
;    =>
;    (bind ?dc (min 1 (/ 10000 ?pow)))
;    (modify ?i (duty-cycle# ?dc))
    ;(assert (CAPABILITIES::resource-limitations (mission ?miss) (instruments $?instr) (duty-cycle# ?dc) (reason "Power limitations")))
;    )