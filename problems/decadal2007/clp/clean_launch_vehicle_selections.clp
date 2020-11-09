

(defrule CLEAN::clean-LVs
    (declare (salience 30))
    ?orig <- (MANIFEST::Mission (launch-vehicle ?lv&~nil))
    =>
    (modify ?orig (launch-vehicle nil) (launch-cost# 0))
)