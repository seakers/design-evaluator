package vassar.database.template.request;

import com.evaluator.*;
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
        this.template_header   = builder.template_header;
        this.instrument_header = builder.instrument_header;
    }





    public TemplateResponse processRequest(QueryAPI api) {

        try {

            // QUERY
            // List<InstrumentQuery.Item> items = api.instrumentQuery();
            List<EnabledInstrumentsQuery.Item> enabled_instruments = api.enabledInstrumentQuery();
            List<ProblemInstrumentsQuery.Item> items               = api.problemInstrumentQuery();

            // BUILD PROBLEM
            this.problemBuilder.setInstrumentList(enabled_instruments);

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
