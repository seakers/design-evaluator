{# call2 #}

{# currentSubobj --> row[0] #}
{# index  --> row[0].split("-",2)[1] #}
{# parent --> row[0].split("-",2)[0] #}
{# naString --> StringUtils.repeat("N-A ", numAttrib) #}
{# params.nof #}

(deffacts REQUIREMENTS::init-subobjectives


 {% for rule in rules.rules %}

    (AGGREGATION::SUBOBJECTIVE (satisfaction 0.0) (id {{rule.currentSubobj}}) (index {{rule.index}}) (parent {{rule.parent}}) (reasons (create$ {{rule.naString}} ))
    (factHistory F{{loop.index}}))

 {% endfor %}

 )