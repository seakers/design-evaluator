(defglobal ?*p* = (new java.util.Vector))

; Instrument-orbit relationship

(defrule CRITIQUE-PERFORMANCE::passive-optica-instrument-in-DD-orbit
    "Passive optical instruments should not be in DD orbits"
    (CAPABILITIES::can-measure (in-orbit ?o) (instrument ?instr) (orbit-type SSO) (orbit-RAAN DD))
    (DATABASE::Instrument (Name ?instr) (Illumination Passive))
    =>
    (call ?*p* addElement (new java.lang.String 
        (str-cat "N:Instrument " ?instr " should not be in orbit " ?o "." "}" )))
    (call ?*p* addElement (new java.lang.String 
        (str-cat "E:Passive optical instruments like " ?instr " should not be in DD orbits like " ?o "." "}" ))))

(defrule CRITIQUE-PERFORMANCE::atmospheric-chemistry-instrument-in-AM-orbit
    "Atmosperic chemistry instruments should not be in AM orbits"
    (CAPABILITIES::can-measure (in-orbit ?o) (instrument ?instr) (orbit-type SSO) (orbit-RAAN AM))
    (DATABASE::Instrument (Name ?instr) (Intent ?i))
    (or (test (neq (str-index "chemistry" ?i) FALSE)) (test (neq (str-index "pollut" ?i) FALSE)))
    =>
    (call ?*p* addElement (new java.lang.String
        (str-cat "N:Instrument " ?instr " should not be in orbit " ?o "." )))
    (call ?*p* addElement (new java.lang.String
        (str-cat "E:Atmospheric chemistry instruments like " ?instr " should not be in AM orbits like " ?o "." ))))

(defrule CRITIQUE-PERFORMANCE::side-looking-instrument-in-less-400-km-orbit
    "Off-nadir instruments should not be in < 400 km orbits"
    (CAPABILITIES::can-measure (in-orbit ?o) (instrument ?instr) (orbit-altitude# ?h&nil&:(<= ?h 400)))
    (DATABASE::Instrument (Name ?instr) (Geometry slant))
    =>
    (call ?*p* addElement (new java.lang.String
        (str-cat "N:Instrument " ?instr " should not be in orbit " ?o "." )))
    (call ?*p* addElement (new java.lang.String
        (str-cat "E:Off-nadir instruments like " ?instr " should not be in <400km orbits like " ?o "." ))))

; Interfering instrument pairs

(defrule CRITIQUE-PERFORMANCE::two-lidars-working-at-the-same-frequency
    "Two lidars at same frequency can interfere with each other"
    (CAPABILITIES::can-measure (in-orbit ?o)(instrument ?ins1) (can-take-measurements yes))
    (CAPABILITIES::can-measure (in-orbit ?o)(instrument ?ins2&~?ins1) (can-take-measurements yes))
    (DATABASE::Instrument (Name ?ins1) (Intent "Laser altimeters") (spectral-bands $?sr))
    (DATABASE::Instrument (Name ?ins2) (Intent "Laser altimeters") (spectral-bands $?sr))
    =>
    (call ?*p* addElement (new java.lang.String
        (str-cat "N:Instruments "  ?ins1 " and " ?ins2 " should not be together.")))
    (call ?*p* addElement (new java.lang.String
        (str-cat "E:Two lidars at the same frequency like "  ?ins1 " and " ?ins2 " should not be together as they can interfere with each other."))))

; Total number of instruments

(defrule CRITIQUE-PERFORMANCE::num-of-instruments
    (CRITIQUE-PERFORMANCE-PARAM::total-num-of-instruments (value ?v&:(> ?v 14)))
    =>
    (call ?*p* addElement (new java.lang.String
        (str-cat "N:There are too many instruments in this design: "?v ".")))
    (call ?*p* addElement (new java.lang.String
        (str-cat "E:There are too many instruments in this design: "?v ". Complex missions tend to have cost overruns."))))

; Instrument Duty Cycle

(defrule CRITIQUE-PERFORMANCE::resource-limitations-datarate
    (MANIFEST::Mission (Name ?miss) (datarate-duty-cycle# ?dc&:(< ?dc 1.0)))
    =>
    (call ?*p* addElement (new java.lang.String
        (str-cat "N:Cumulative spacecraft data rate in orbit " ?miss " is too high: " (format nil "%2.2f" ?dc) ". Try removing an instrument.")))
    (call ?*p* addElement (new java.lang.String
        (str-cat "E:Cumulative spacecraft data rate in orbit " ?miss " is too high: " (format nil "%2.2f" ?dc) ". Instruments might not be able to download all their data back to Earth."))))

(defrule CRITIQUE-PERFORMANCE::resource-limitations-power
    "Technology to provide more than 10kW is currently expensive"
    (MANIFEST::Mission (Name ?miss) (power-duty-cycle# ?dc&:(< ?dc 1.0)))
    =>
    (call ?*p* addElement (new java.lang.String
        (str-cat "N:Cumulative spacecraft power in orbit " ?miss " is too high: " (format nil "%2.2f" ?dc) ". Try removing an instrument.")))
    (call ?*p* addElement (new java.lang.String
        (str-cat "E:Cumulative spacecraft power in orbit " ?miss " is too high: " (format nil "%2.2f" ?dc) ". Technology to provide this amount of energy is currently expensive."))))

;(defrule CRITIQUE-PERFORMANCE::fairness-check
;    (CRITIQUE-PERFORMANCE-PARAM::fairness (flag 1) (value ?v)(stake-holder1 ?sh1) (stake-holder2 ?sh2))
;    =>
;    (call ?*p* addElement (new java.lang.String
;        (str-cat "Satisfaction value for stakeholder " ?sh1 " is larger than " ?sh2 ": " (format nil "%2.2f" ?v) "."))))
