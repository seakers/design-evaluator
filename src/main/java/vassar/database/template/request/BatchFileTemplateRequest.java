package vassar.database.template.request;

import com.evaluator.MeasurementAttributeQuery;
import vassar.database.service.QueryAPI;
import vassar.database.template.TemplateRequest;
import vassar.database.template.TemplateResponse;
import vassar.jess.attribute.EOAttribute;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class BatchFileTemplateRequest extends TemplateRequest {

    public static class Builder extends TemplateRequest.Builder<Builder> {

        public Builder() {}

        public BatchFileTemplateRequest build() { return new BatchFileTemplateRequest(this); }

    }

    protected BatchFileTemplateRequest(Builder builder){

        super(builder);
    }

    public TemplateResponse processRequest(QueryAPI api) {
        try {

            // PASS TEMPLATE FILE PATH FOR BATCH
            return new TemplateResponse.Builder()
                                        .setTemplateStringBatch(this.templateFilePath)
                                        .build();
        }
        catch (Exception e) {
            System.out.println("Error processing orbit template request: " +e.getClass() + " : " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

}

