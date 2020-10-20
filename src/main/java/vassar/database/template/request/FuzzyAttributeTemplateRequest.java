package vassar.database.template.request;

import com.evaluator.*;
import vassar.database.service.QueryAPI;
import vassar.database.template.TemplateRequest;
import vassar.database.template.TemplateResponse;
import vassar.database.template.response.BatchTemplateResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class FuzzyAttributeTemplateRequest extends TemplateRequest {


    public static class Builder extends TemplateRequest.Builder<Builder> {

        public Builder() {}


        public FuzzyAttributeTemplateRequest build() { return new FuzzyAttributeTemplateRequest(this); }

    }

    protected FuzzyAttributeTemplateRequest(Builder builder){

        super(builder);
    }


    public class FuzzyAttributeRules{

        public ArrayList<FuzzyAttributeRules.Rule> rules;

        public FuzzyAttributeRules(List<FuzzyAttributeQuery.Item> items){
            rules = new ArrayList<>();
            for(FuzzyAttributeQuery.Item item : items){
                rules.add(
                        new FuzzyAttributeRules.Rule(item)
                );
            }
        }

        public ArrayList<FuzzyAttributeRules.Rule> getRules(){
            return rules;
        }


        public class Rule{

            public String parameter;
            public String attributeName;
            public String shortAttributeName;
            public String valueList;
            public String minList;
            public String maxList;

            public Rule(FuzzyAttributeQuery.Item item){
                parameter          = item.parameter();
                attributeName      = item.name();
                shortAttributeName = attributeName.substring(0, attributeName.length() - 1);

                valueList = "";
                minList = "";
                maxList = "";
                for (FuzzyAttributeQuery.Value val: item.values()){
                    valueList += (val.value() + " ");
                    minList += (val.minimum() + " ");
                    maxList += (val.maximum() + " ");
                }
            }
        }
    }



    public TemplateResponse processRequest(QueryAPI api) {
        try {

            // QUERY
            List<FuzzyAttributeQuery.Item> items = api.fuzzyAttributeQuery();
            FuzzyAttributeRules ruleBuilder      = new FuzzyAttributeRules(items);

            this.context.put("items", ruleBuilder.getRules());
            this.template.evaluate(this.writer, context);


            return new TemplateResponse.Builder()
                                        .setTemplateString(this.writer.toString())
                                        .build();
        }
        catch (Exception e) {
            System.out.println("Error processing orbit template request: " +e.getClass() + " : " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

}
