(deffunction numerical-to-fuzzy (?num ?values ?mins ?maxs)
(bind ?ind 1)(bind ?n (length$ ?values))(while (<= ?ind ?n)
(if (and (< ?num (nth$ ?ind ?maxs)) (>= ?num (nth$ ?ind ?mins))) then (return (nth$ ?ind ?values))
else (++ ?ind))))