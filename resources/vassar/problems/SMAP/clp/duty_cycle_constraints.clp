(defrule DUTY-CYCLE-CONSTRAINTS::apply-data-rate-constraint-cold
    "Change temporal resolution continuous value with data-rate-duty-cycle"
    ?m1 <- CAPABILITIES::Manifested-instrument  (avg-revisit-time-cold-regions# ?rcold&~nil) (data-rate-duty-cycle# ?dcycle&~nil))
    =>
    (modify ?m1 (avg-revisit-time-cold-regions# (* ?rcold ?dcycle)))
)

(defrule DUTY-CYCLE-CONSTRAINTS::apply-data-rate-constraint-global
    "Change temporal resolution continuous value with data-rate-duty-cycle"
    ?m1 <- CAPABILITIES::Manifested-instrument  (avg-revisit-time-global# ?rglobal&~nil) (data-rate-duty-cycle# ?dcycle&~nil))
    =>
    (modify ?m1 (avg-revisit-time-global# (* ?rglobal ?dcycle)))
)

(defrule DUTY-CYCLE-CONSTRAINTS::apply-data-rate-constraint-nh
    "Change temporal resolution continuous value with data-rate-duty-cycle"
    ?m1 <- CAPABILITIES::Manifested-instrument  (avg-revisit-time-northern-hemisphere# ?rnh&~nil) (data-rate-duty-cycle# ?dcycle&~nil))
    =>
    (modify ?m1 (avg-revisit-time-northern-hemisphere# (* ?rnh ?dcycle)))
)

(defrule DUTY-CYCLE-CONSTRAINTS::apply-data-rate-constraint-sh
    "Change temporal resolution continuous value with data-rate-duty-cycle"
    ?m1 <- CAPABILITIES::Manifested-instrument  (avg-revisit-time-southern-hemisphere# ?rsh&~nil) (data-rate-duty-cycle# ?dcycle&~nil))
    =>
    (modify ?m1 (avg-revisit-time-southern-hemisphere# (* ?rsh ?dcycle)))
)

(defrule DUTY-CYCLE-CONSTRAINTS::apply-data-rate-constraint-tropics
    "Change temporal resolution continuous value with data-rate-duty-cycle"
    ?m1 <- CAPABILITIES::Manifested-instrument  (avg-revisit-time-tropics# ?rtropics&~nil) (data-rate-duty-cycle# ?dcycle&~nil))
    =>
    (modify ?m1 (avg-revisit-time-tropics# (* ?rtropics ?dcycle)))
)

(defrule DUTY-CYCLE-CONSTRAINTS::apply-data-rate-constraint-us
    "Change temporal resolution continuous value with data-rate-duty-cycle"
    ?m1 <- CAPABILITIES::Manifested-instrument  (avg-revisit-time-US# ?rus&~nil) (data-rate-duty-cycle# ?dcycle&~nil))
    =>
    (modify ?m1 (avg-revisit-time-US# (* ?rus ?dcycle)))
)




    (slot avg-revisit-time-cold-regions#)
    (slot avg-revisit-time-global#)
    (slot avg-revisit-time-northern-hemisphere#)
    (slot avg-revisit-time-southern-hemisphere#)
    (slot avg-revisit-time-tropics#)
    (slot avg-revisit-time-US#)