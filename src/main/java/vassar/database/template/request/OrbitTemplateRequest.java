package vassar.database.template.request;

import com.evaluator.ProblemOrbitJoinQuery;
import com.evaluator.ProblemOrbitsQuery;
import vassar.database.service.QueryAPI;
import vassar.database.template.TemplateRequest;
import vassar.database.template.TemplateResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class OrbitTemplateRequest extends TemplateRequest {

    private String template_header;
    private String orbit_header;

    public static class Builder extends TemplateRequest.Builder<Builder> {

        private String template_header;
        private String orbit_header;

        public Builder() {}

        public Builder orbitHeader(String orbit_header) {
            this.orbit_header = orbit_header;
            return this;
        }

        public Builder templateHeader(String template_header) {
            this.template_header = template_header;
            return this;
        }

        public OrbitTemplateRequest build() { return new OrbitTemplateRequest(this); }

    }

    protected OrbitTemplateRequest(Builder builder) {

        super(builder);
        this.template_header      = builder.template_header;
        this.orbit_header         = builder.orbit_header;
    }




    public TemplateResponse processRequest(QueryAPI api) {
        try {
            // QUERY
            // List<OrbitInformationQuery.Item> items = api.orbitQuery();

            List<ProblemOrbitsQuery.Item>    items         = api.problemOrbitQuery();
            List<ProblemOrbitJoinQuery.Item> problem_items = api.problemOrbitJoin();

//            for (ProblemOrbitsQuery.Item item : items){
//                item.Orbit().name();
//                item.Orbit().attributes();
//                for (ProblemOrbitsQuery.Attribute attribute: item.Orbit().attributes()){
//                    attribute.Orbit_Attribute().name();
//                    attribute.value();
//                }
//            }


            this.problemBuilder.setOrbitList(problem_items);

            // BUILD CONTEXT
            this.context.put("template_header", this.template_header);
            this.context.put("orbit_header", this.orbit_header);
            this.context.put("items", items);

            // EVALUATE
            this.template.evaluate(this.writer, this.context);


            return new TemplateResponse.Builder()
                                       .setTemplateString(
                                               this.writer.toString()
                                       ).build();
        }
        catch (Exception e) {
            System.out.println("Error processing orbit template request: " +e.getClass() + " : " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

}
