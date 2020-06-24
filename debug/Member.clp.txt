(deffunction member (?elem ?list)
    (if (listp ?list) then
     (neq (member$ ?elem ?list) FALSE)
     else (?list contains ?elem)))