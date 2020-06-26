package vassar.database.template.request;

import com.evaluator.CapabilityRuleQuery;
import com.evaluator.RequirementRuleAttributeQuery;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import vassar.database.service.QueryAPI;
import vassar.database.template.TemplateRequest;
import vassar.database.template.TemplateResponse;

import java.math.BigDecimal;
import java.util.*;


public class CapabilityRuleTemplateRequest extends TemplateRequest {

    public static class Builder extends TemplateRequest.Builder<Builder> {

        public Builder() {}

        public CapabilityRuleTemplateRequest build() { return new CapabilityRuleTemplateRequest(this); }
    }
    protected CapabilityRuleTemplateRequest(Builder builder){

        super(builder);
    }

    // PROCESS REQUEST
    public TemplateResponse processRequest(QueryAPI api) {
        try {
            // QUERY
            List<CapabilityRuleQuery.Item> items              = api.capabilityRuleQuery();
            CapabilityRules rules                             = new CapabilityRules(items); // Record all the instruments
            ArrayList<CapabilityRules.Instrument> instruments = rules.buildRules();

            this.problemBuilder.setInstrumentMeasurementData(instruments);

            this.context.put("items", instruments);
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



    public class CapabilityRules{

        private List<CapabilityRuleQuery.Item>      items;
        public  ArrayList<Instrument>               instruments;
        public  HashMap<String, ArrayList<CapabilityRuleQuery.Item>> instrument_partition;

        public CapabilityRules(List<CapabilityRuleQuery.Item> items) {
            this.items                = items;
            this.instruments          = new ArrayList<>();
            this.instrument_partition = new HashMap<>();

            for (CapabilityRuleQuery.Item item : this.items) {

                // NEW INSTRUMENT
                if (!this.instrument_partition.containsKey(item.instrument().name())) {
                    this.instrument_partition.put(item.instrument().name(), new ArrayList<>());
                    // this.instrument_partition.get(item.instrument().name()).add(item);
                }
                this.instrument_partition.get(item.instrument().name()).add(item);

            }
        }

        public ArrayList<Instrument> buildRules(){
            Set<String> keys = this.instrument_partition.keySet();
            for (String key : keys) {
                this.instruments.add(new Instrument(key, this.instrument_partition.get(key)));
            }
            return this.instruments;
        }

        public class Instrument{
            public String                                               name;
            public ArrayList<Measurement>                               measurements;
            public HashMap<String, ArrayList<CapabilityRuleQuery.Item>> measurement_partition;
            public String listOfMeasurements;

            public Instrument(String name, List<CapabilityRuleQuery.Item> items) {
                this.measurement_partition = new HashMap<>();
                this.measurements          = new ArrayList<>();
                this.name                  = name;
                this.listOfMeasurements    = "";
                for (CapabilityRuleQuery.Item item : items) {

                    // NEW MEASUREMENT
                    if (!this.measurement_partition.containsKey(item.measurement().name())) {
                        this.measurement_partition.put(item.measurement().name(), new ArrayList<>());
                        // this.measurement_partition.get(item.measurement().name()).add(item);
                    }
                    this.measurement_partition.get(item.measurement().name()).add(item);
                }

                Set<String> keys = this.measurement_partition.keySet();
                int counter = 0;
                for (String key : keys) {
                    this.measurements.add(new Measurement(key, this.measurement_partition.get(key)));
                    this.listOfMeasurements+= (" " + this.name + counter + " ");
                    counter++;
                }
            }

        }

        public class Measurement{
            public class MPair{
                public String key;
                public String value;
                public MPair(String key, String value){
                    this.key = key;
                    this.value = value;
                }
            }
            public String name;
            public ArrayList<MPair> attributes;
            public Measurement(String name,  List<CapabilityRuleQuery.Item> items) {
                this.name       = name;
                this.attributes = new ArrayList<>();

                for (CapabilityRuleQuery.Item item : items) {
                    MPair pair = new MPair(
                            item.measurement_attribute().name(),
                            item.measurement_attribute_value()
                    );
                    this.attributes.add(pair);
                }
            }
        }

    }

}
