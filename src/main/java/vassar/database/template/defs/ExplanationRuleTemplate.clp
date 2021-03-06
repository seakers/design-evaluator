(defquery REQUIREMENTS::search-all-measurements-by-parameter "Finds all measurements of this parameter in the campaign"
(declare (variables ?param))
(REQUIREMENTS::Measurement (Parameter ?param) (flies-in ?flies) (launch-date ?ld) (lifetime ?lt) (Instrument ?instr)
(Temporal-resolution ?tr) (All-weather ?aw) (Horizontal-Spatial-Resolution ?hsr) (Spectral-sampling ?ss)
(taken-by ?tk) (Vertical-Spatial-Resolution ?vsr) (sensitivity-in-low-troposphere-PBL ?tro) (sensitivity-in-upper-stratosphere ?str)))