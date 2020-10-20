package vassar.database.template.request;

import com.evaluator.FuzzyAttributeQuery;
import com.evaluator.RequirementRuleAttributeQuery;
import com.evaluator.RequirementRuleCaseQuery;
import evaluator.EvaluatorApp;
import org.checkerframework.checker.units.qual.A;
import vassar.database.service.QueryAPI;
import vassar.database.template.TemplateRequest;
import vassar.database.template.TemplateResponse;

import java.util.ArrayList;
import java.util.List;

public class RequirementRuleCaseTemplateRequest extends TemplateRequest {


        public static class Builder extends TemplateRequest.Builder<Builder> {

        public Builder() {}

        public RequirementRuleCaseTemplateRequest build() { return new RequirementRuleCaseTemplateRequest(this); }

    }

    protected RequirementRuleCaseTemplateRequest(Builder builder){

        super(builder);
    }



    public TemplateResponse processRequest(QueryAPI api){
        try {
            // QUERY
            List<RequirementRuleCaseQuery.Item> items = api.requirementRuleCaseQuery();

            ArrayList<String> declared_objectives = new ArrayList<>();
            ArrayList<String> declared_subobjectives = new ArrayList<>();
            for(RequirementRuleCaseQuery.Item item: items){
                String obj = "(defglobal ?*obj-"+item.objective().name()+"* = 0)";
                if(!declared_objectives.contains(obj)){
                    declared_objectives.add(obj);
                }
                String subobj = "(defglobal ?*subobj-"+item.subobjective().name()+"* = 0)";
                if(!declared_subobjectives.contains(subobj)){
                    declared_subobjectives.add(subobj);
                }
            }

            declared_objectives.addAll(declared_subobjectives);
            String all_dec = "";
            for(String itm: declared_objectives){
                all_dec += (itm + "\n");
            }

            this.context.put("items", items);
            this.template.evaluate(this.writer, context);

            return new TemplateResponse.Builder()
                    .setTemplateString(all_dec + " " + this.writer.toString())
                    .build();
        }
        catch (Exception e){
            System.out.println("Error processing orbit template request: " +e.getClass() + " : " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }


}
