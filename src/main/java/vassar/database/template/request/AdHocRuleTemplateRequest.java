package vassar.database.template.request;

import com.evaluator.EnabledInstrumentsQuery;
import com.evaluator.ProblemInstrumentsQuery;
import vassar.GlobalScope;
import vassar.database.service.QueryAPI;
import vassar.database.template.TemplateRequest;
import vassar.database.template.TemplateResponse;
import vassar.database.template.response.BatchTemplateResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class AdHocRuleTemplateRequest extends TemplateRequest {


    public static class Builder extends TemplateRequest.Builder<Builder> {

        public Builder() {}

        public AdHocRuleTemplateRequest build() { return new AdHocRuleTemplateRequest(this); }

    }

    protected AdHocRuleTemplateRequest(Builder builder) {

        super(builder);
    }


    public TemplateResponse processRequest(QueryAPI api) {
        try {

            List<ProblemInstrumentsQuery.Item> items = api.problemInstrumentQuery();

            ArrayList<String> substitute = new ArrayList<>();

            for (ProblemInstrumentsQuery.Item item: items){
                substitute.add(item.Instrument().name());
            }

//            substitute.add("SMAP_RAD");
//            substitute.add("SMAP_MWR");
//            substitute.add("CMIS");
//            substitute.add("VIIRS");
//            substitute.add("BIOMASS");
//
//            substitute.add("SMAP_ANT");
//            substitute.add("CLOUD_MASK");

            // BUILD CONTEXT
            this.context.put("items", substitute);

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
