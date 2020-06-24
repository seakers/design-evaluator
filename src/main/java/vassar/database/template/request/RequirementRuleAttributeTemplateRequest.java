package vassar.database.template.request;

import com.evaluator.AttributeInheritanceQuery;
import com.evaluator.RequirementRuleAttributeQuery;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import vassar.GlobalScope;
import vassar.database.service.QueryAPI;
import vassar.database.template.TemplateRequest;
import vassar.database.template.TemplateResponse;
import vassar.database.template.response.BatchTemplateResponse;
import vassar.problem.Problem;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class RequirementRuleAttributeTemplateRequest extends TemplateRequest {

    private String wrapperTemplatePath;

    public static class Builder extends TemplateRequest.Builder<Builder> {

        private String wrapperTemplatePath;

        public Builder() {}

        public Builder setWrapperTemplatePath(String wrapperTemplatePath) {
            this.wrapperTemplatePath = wrapperTemplatePath;
            return this;
        }

        public RequirementRuleAttributeTemplateRequest build() { return new RequirementRuleAttributeTemplateRequest(this); }
    }

    protected RequirementRuleAttributeTemplateRequest(Builder builder){

        super(builder);
        this.wrapperTemplatePath = builder.wrapperTemplatePath;
    }





    // REQUIREMENT RULE
    public class RequirementRules{

        // This will hold the real rules
        public HashMap<String, SubobjectiveRule> requirementRules;

        public RequirementRules(Problem.Builder problemBuilder, List<RequirementRuleAttributeQuery.Item> items){
            requirementRules = new HashMap<>();

            for(RequirementRuleAttributeQuery.Item item: items){
                String subobjective = item.objective().name();

                if(!requirementRules.containsKey(subobjective)){
                    requirementRules.put(
                            subobjective, new SubobjectiveRule(item, problemBuilder)
                    );
                }
                else{
                    requirementRules.get(subobjective).addAttribute(item);
                }
            }
        }

        public ArrayList<SubobjectiveRule> getRules(){
            ArrayList<SubobjectiveRule> rules = new ArrayList<>();
            for (String key: requirementRules.keySet()){
                rules.add(
                        requirementRules.get(key)
                );
            }
            return rules;
        }

        // REQUIREMENT RULE - PER SUBOBJECTIVE
        // -- FORM --
        // lhs1: (defrule REQUIREMENTS::{{subobjective}}-attrib ?m <- (REQUIREMENTS::Measurement (taken-by ?whom)  (power-duty-cycle# ?pc) (data-rate-duty-cycle# ?dc)
        // lhs2: (Parameter "{{measurement}}")
        // lhs3: (attribute ?val{{attributeNum}}&~nil) ...
        // =>
        // (bind ?reason "") (bind ?new-reasons (create$ N-A ...))
        // (bind ?x1 (nth$ (find-bin-num ?val1  (create$ {{thresholds 4200 12000 30000}}) ) (create$ {{scores 1.0 0.67 0.33 0.0}}))) (if (< ?x1 1.0) then (bind ?new-reasons (replace$  ?new-reasons 1 1 "{{justification}}" )) (bind ?reason (str-cat ?reason  "{{justification}}"))) ...
        // (bind ?list (create$  ?x1 ?x2 ?x3 ?x4 ?x5 ?x6 ?x7 ?x8 ?x9 ?x10))
        // (assert (AGGREGATION::SUBOBJECTIVE (id WEA1-1) (attributes  Horizontal-Spatial-Resolution# Temporal-resolution# Accuracy# soil-penetration# Swath# sensitivity# image-distortion# orbit-inclination orbit-RAAN All-weather) (index {{rule number}}) (parent WEA1 ) (attrib-scores ?list) (satisfaction (*$ ?list)) (reasons ?new-reasons) (satisfied-by ?whom) (reason ?reason ) (requirement-id (?m getFactId)) (factHistory (str-cat "{R" (?*rulesMap* get REQUIREMENTS::WEA1-1-attrib) " A" (call ?m getFactId) "}")))))
        public class SubobjectiveRule{

            public String            subobjective;
            public String            measurement;
            public String            index;
            public String            parent;
            public String            NAstring;

            public ArrayList<Attribute> attributes;

            public SubobjectiveRule(RequirementRuleAttributeQuery.Item item, Problem.Builder problemBuilder) {
                subobjective = item.objective().name();
                measurement  = item.measurement().name();
                parent = subobjective.split("-",2)[0];
                index  = subobjective.split("-",2)[1];

                problemBuilder.subobjectivesToMeasurements.put(subobjective, measurement);
                GlobalScope.subobjectivesToMeasurements.put(subobjective, measurement);

                attributes = new ArrayList<>();
                this.addAttribute(item);
                NAstring = " N-A";
            }

            public void addAttribute(RequirementRuleAttributeQuery.Item item){
                attributes.add(
                        new Attribute(item)
                );
                NAstring += " N-A";
            }

            public class Attribute{

                public String name;
                public String thresholds;
                public String scores;
                public String justification;

                public Attribute(RequirementRuleAttributeQuery.Item item){
                    name          = item.measurement_attribute().name();
                    thresholds    = arrayListToString((ArrayList<String>) item.thresholds()); //ArrayList<string / decimal>
                    scores        = arrayListToString((ArrayList<String>) item.scores());
                    justification = item.justification();
                }

                public String arrayListToString(ArrayList<String> ary) {
                    String toReturn = "";

                    for (int i = 0; i < ary.size(); i++) {
                        toReturn = toReturn + " " + objToString(ary.get(i));
                    }

                    return toReturn;
                }

                public String objToString(Object obj){
                    if (obj instanceof BigDecimal)
                        return ((BigDecimal) obj).toString();
                    else
                        return (String) obj;
                }
            }
        }

    }


    // PROCESS REQUEST
    public TemplateResponse processRequest(QueryAPI api) {
        try {
            // QUERY
            List<RequirementRuleAttributeQuery.Item> items = api.requirementRuleAttributeQuery();
            RequirementRules                         rules = new RequirementRules(this.problemBuilder, items);

            // PARSING
            this.context.put("rules", rules.getRules());
            this.template.evaluate(this.writer, this.context);
            String ruleStr = this.writer.toString();

            // RETURN
            GlobalScope.buildMeasurementsToSubobjectives();
            return new TemplateResponse.Builder()
                    .setTemplateString(ruleStr)
                    .build();
        }
        catch (Exception e) {
            System.out.println("Error processing orbit template request: " +e.getClass() + " : " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

}
