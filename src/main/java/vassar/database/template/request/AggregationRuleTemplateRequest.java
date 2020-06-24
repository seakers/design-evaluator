package vassar.database.template.request;

import com.evaluator.AggregationRuleQuery;
import com.evaluator.CapabilityRuleQuery;
import vassar.database.service.QueryAPI;
import vassar.database.template.TemplateRequest;
import vassar.database.template.TemplateResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;


public class AggregationRuleTemplateRequest extends TemplateRequest {

    public static class Builder extends TemplateRequest.Builder<Builder> {

        public Builder() {}

        public AggregationRuleTemplateRequest build() { return new AggregationRuleTemplateRequest(this); }
    }
    protected AggregationRuleTemplateRequest(Builder builder){

        super(builder);
    }


    // PROCESS REQUEST
    public TemplateResponse processRequest(QueryAPI api) {
        try {
            // QUERY
            List<AggregationRuleQuery.Item> items = api.aggregationRuleQuery();

            this.problemBuilder.setAggregationRules(items);

            this.context.put("items", items);
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
