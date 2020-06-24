package vassar.database.template.request;

import vassar.GlobalScope;
import vassar.database.service.QueryAPI;
import vassar.database.template.TemplateRequest;
import vassar.database.template.TemplateResponse;

import java.util.ArrayList;
import java.util.Map;

public class SynergyRuleTemplateRequest extends TemplateRequest {


    public static class Builder extends TemplateRequest.Builder<Builder> {

        public Builder() {}

        public SynergyRuleTemplateRequest build() { return new SynergyRuleTemplateRequest(this); }

    }

    protected SynergyRuleTemplateRequest(Builder builder) {

        super(builder);
    }


    public class SynergyRules {

        public ArrayList<Rule> rules;

        public SynergyRules() {
            rules = new ArrayList<>();
            for(Map.Entry<String, ArrayList<String>> es : GlobalScope.measurementsToSubobjectives.entrySet()){
                rules.add(
                    new Rule(es)
                );
            }
        }

        public class Rule {
            public String            meas;
            public ArrayList<String> subobjList;
            public String            meas_num;
            public Rule(Map.Entry<String, ArrayList<String>> es) {
                subobjList = new ArrayList<>();
                meas       = es.getKey();
                meas_num   = meas.substring(0, meas.indexOf(" "));
                for (String subobj: es.getValue()) {
                    subobjList.add(subobj);
                }
            }
        }

    }

    public TemplateResponse processRequest(QueryAPI api) {
        try {

            System.out.println("---------- GLOBAL MEASUREMENT TO SUBOBJECTIVES");
            System.out.println(GlobalScope.measurementsToSubobjectives);

            SynergyRules rules = new SynergyRules();


            // BUILD CONTEXT
            this.context.put("rules", rules.rules);

            // EVALUATE
            this.template.evaluate(this.writer, this.context);


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
