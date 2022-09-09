(deffunction revisit-time-to-temporal-resolution (?region ?values)
    (if (eq ?region Global) then
    (return (nth$ 1 ?values))
     elif (eq ?region Tropical-regions) then
    (return (nth$ 2 ?values))
     elif (eq ?region Northern-hemisphere) then
    (return (nth$ 3 ?values))
     elif (eq ?region Southern-hemisphere) then
    (return (nth$ 4 ?values))
     elif (eq ?region Cold-regions) then
    (return (nth$ 5 ?values))
     elif (eq ?region US) then
    (return (nth$ 6 ?values))
     else (throw new JessException "revisit-time-to-temporal-resolution: The region of interest is invalid ")
    ))