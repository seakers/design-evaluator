package vassar.database.template.request;

import com.evaluator.InstrumentAttributeQuery;
import vassar.database.service.QueryAPI;
import vassar.database.template.TemplateRequest;
import vassar.database.template.TemplateResponse;
import vassar.database.template.response.BatchTemplateResponse;

import java.util.List;


public class HeaderTemplateRequest extends TemplateRequest {

    private String appPath;

    public static class Builder extends TemplateRequest.Builder<Builder> {

        private String appPath;

        public Builder() {}

        public Builder setAppPath(String appPath) {
            this.appPath = appPath;
            return this;
        }

        public HeaderTemplateRequest build() { return new HeaderTemplateRequest(this); }

    }

    protected HeaderTemplateRequest(Builder builder){

        super(builder);
        this.appPath      = builder.appPath;
    }

    public TemplateResponse processRequest(QueryAPI api) {
        try {

            // BUILD CONTEXT
            this.context.put("appPath", this.appPath);

            // EVALUATE
            this.template.evaluate(this.writer, this.context);

            return new TemplateResponse.Builder()
                                        .setTemplateString(this.writer.toString())
                                        .build();
        }
        catch (Exception e) {
            System.out.println("Error processing header template request: " +e.getClass() + " : " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

}


