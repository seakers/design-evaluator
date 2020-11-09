(deffunction beep () ((call java.awt.Toolkit getDefaultToolkit) beep))
(deffunction thread (?f) ((new Thread (implement Runnable using ?f)) start))

;(
;    defglobal ?*matlab* = ((new vassar.matlab.MatlabProxyFactoryWrapper) getProxy)
;)

;(
;    defglobal ?*matlab* = (new vassar.matlab.MatlabEngineWrapper)
;)





(
    deffunction matlab (?code)
    (printout t "--> CALLING matlab" crlf )
    (call vassar.matlab.MatlabEngineWrapper eval ?code)
    ; (?*matlab* eval ?code)
)

(
    deffunction matlabf0 (?f $?argv)
    (printout t "--> CALLING matlabf0" crlf )
    (call vassar.matlab.MatlabEngineWrapper feval ?f ?argv)
    ; (?*matlab* feval ?f ?argv)
)

(
    deffunction matlabf1 (?f $?argv)
    (printout t "--> CALLING matlabf1"  crlf )
    (matlab-value (nth$ 1 (matlabfn ?f 1 $?argv)))
)

(
    deffunction matlabfn (?f ?nout $?argv)
    (printout t "--> CALLING matlabfn " ?f " " ?nout " " ?argv crlf )
    (call vassar.matlab.MatlabEngineWrapper returningFeval ?f ?nout ?argv)
    ; (?*matlab* returningFeval ?f ?nout ?argv)
)





(
    deffunction matlabf (?f $?argv)
    (printout t "--> CALLING matlabf" crlf )
    (bind ?nout (matlab-nargout ?f))
    (if (= ?nout 0) then (matlabf0 ?f $?argv) elif (= ?nout 1) then (matlabf1 ?f $?argv) else (matlabfn ?f ?nout $?argv))
)

(
    deffunction matlab-nargout (?f)
    (printout t "--> CALLING matlab-nargout" crlf )
    (bind ?result (matlabf1 nargout ?f))
    (if (>= ?result 0) then ?result else (- 0 ?result 1))
)



(
    deffunction matlab-value (?v)
    (printout t "--> CALLING matlab-value" crlf )
    (if (not (java-objectp ?v)) then (return ?v) elif (not ((?v getClass) isArray)) then (return ?v) else (bind ?l (as-list ?v)) (if (> (length$ ?l) 1) then (return ?l) else (return (nth$ 1 ?l))))
)