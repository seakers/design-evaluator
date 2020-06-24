package vassar.database.template.request;

import com.evaluator.InstrumentQuery;
import com.evaluator.LaunchVehicleInformationQuery;
import com.evaluator.MeasurementAttributeQuery;
import vassar.GlobalScope;
import vassar.database.service.QueryAPI;
import vassar.database.template.TemplateRequest;
import vassar.database.template.TemplateResponse;
import vassar.jess.attribute.AttributeBuilder;
import vassar.jess.attribute.EOAttribute;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

public class InstrumentTemplateRequest extends TemplateRequest {

    private String template_header;
    private String instrument_header;

    public static class Builder extends TemplateRequest.Builder<Builder> {

        private String template_header;
        private String instrument_header;

        public Builder() {}

        public Builder instrumentHeader(String instrument_header) {
            this.instrument_header = instrument_header;
            return this;
        }

        public Builder templateHeader(String template_header) {
            this.template_header = template_header;
            return this;
        }

        public InstrumentTemplateRequest build() { return new InstrumentTemplateRequest(this); }

    }

    protected InstrumentTemplateRequest(Builder builder) {

        super(builder);
        this.template_header       = builder.template_header;
        this.instrument_header = builder.instrument_header;
    }





    public TemplateResponse processRequest(QueryAPI api) {

        try {
            // QUERY
            List<InstrumentQuery.Item> items = api.instrumentQuery();

            // BUILD PROBLEM
            String[] instrumentList = new String[items.size()];
            for(int x=0; x < items.size(); x++){
                instrumentList[x] = items.get(x).name();
            }
            this.problemBuilder.setInstrumentList(instrumentList);

            // BUILD CONTEXT
            this.context.put("template_header", this.template_header);
            this.context.put("instrument_header", this.instrument_header);
            this.context.put("items", items);

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
