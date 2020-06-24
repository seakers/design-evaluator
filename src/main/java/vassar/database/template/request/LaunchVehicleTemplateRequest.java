package vassar.database.template.request;

import com.evaluator.LaunchVehicleInformationQuery;
import vassar.database.service.QueryAPI;
import vassar.database.template.TemplateRequest;
import vassar.database.template.TemplateResponse;

import java.util.List;


public class LaunchVehicleTemplateRequest extends TemplateRequest {

    private String template_header;
    private String launch_vehicle_header;

    public static class Builder extends TemplateRequest.Builder<Builder> {

        private String template_header;
        private String launch_vehicle_header;

        public Builder() {}

        public Builder launchVehicleHeader(String launch_vehicle_header) {
            this.launch_vehicle_header = launch_vehicle_header;
            return this;
        }

        public Builder templateHeader(String template_header) {
            this.template_header = template_header;
            return this;
        }

        public LaunchVehicleTemplateRequest build() { return new LaunchVehicleTemplateRequest(this); }

    }

    protected LaunchVehicleTemplateRequest(Builder builder) {

        super(builder);
        this.template_header       = builder.template_header;
        this.launch_vehicle_header = builder.launch_vehicle_header;
    }

    public TemplateResponse processRequest(QueryAPI api) {
        try {
            // QUERY
            List<LaunchVehicleInformationQuery.Item> items = api.launchVehicleQuery();

            // BUILD CONTEXT
            this.context.put("template_header", this.template_header);
            this.context.put("launch_vehicle_header", this.launch_vehicle_header);
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
