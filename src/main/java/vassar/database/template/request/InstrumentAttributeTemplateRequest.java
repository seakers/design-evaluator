package vassar.database.template.request;

        import com.evaluator.InstrumentAttributeQuery;
        import com.evaluator.InstrumentQuery;
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


public class InstrumentAttributeTemplateRequest extends TemplateRequest {

    private String template_header;

    public static class Builder extends TemplateRequest.Builder<Builder> {

        private String template_header;

        public Builder() {}

        public Builder templateHeader(String template_header) {
            this.template_header = template_header;
            return this;
        }

        public InstrumentAttributeTemplateRequest build() { return new InstrumentAttributeTemplateRequest(this); }

    }

    protected InstrumentAttributeTemplateRequest(Builder builder){

        super(builder);
        this.template_header      = builder.template_header;
    }

    public TemplateResponse processRequest(QueryAPI api) {
        try {
            // QUERY
            List<InstrumentAttributeQuery.Item> items = api.instrumentAttributeQuery();

            // SET GLOBALS
            this.setGlobals(items);

            // BUILD CONTEXT
            this.context.put("template_header", this.template_header);
            this.context.put("items", items);

            // EVALUATE
            this.template.evaluate(this.writer, this.context);

            return new TemplateResponse.Builder()
                                       .setTemplateString(this.writer.toString())
                                       .build();
        }
        catch (Exception e) {
            System.out.println("Error processing instrument template request: " +e.getClass() + " : " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }



    private void setGlobals( List<InstrumentAttributeQuery.Item> items) {
        HashMap<String, Integer> attribsToKeys = new HashMap<>();
        HashMap<Integer, String> keysToAttribs = new HashMap<>();
        HashMap<String, String> attribsToTypes = new HashMap<>();
        HashMap<String, EOAttribute> attribSet = new HashMap<>();

        for (InstrumentAttributeQuery.Item item : items) {
            String slotType = item.slot_type();
            String name     = item.name();
            int id          = item.id();
            String type     = item.type();

            attribsToKeys.put(name, id);
            keysToAttribs.put(id, name);
            attribsToTypes.put(name, type);

            if (type.equalsIgnoreCase("NL") || type.equalsIgnoreCase("OL")) {
                Hashtable<String, Integer> acceptedValues = new Hashtable<>();

                int counter = 0;
                for (InstrumentAttributeQuery.Value value : item.values()){
                    String acceptedValue = value.Accepted_Value().value();
                    acceptedValues.put(acceptedValue, counter);
                    counter++;
                }

                EOAttribute attrib    = AttributeBuilder.make(type, name, "N/A");
                attrib.acceptedValues = acceptedValues;
                attribSet.put(name, attrib);
            }
            else {
                EOAttribute attrib = AttributeBuilder.make(type, name, "N/A");
                attribSet.put(name, attrib);
            }
        }

        GlobalScope.defineInstrument(attribsToKeys, keysToAttribs, attribsToTypes, attribSet);
    }



}

