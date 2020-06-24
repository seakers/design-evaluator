package vassar.database.template.request;

import vassar.database.service.QueryAPI;
import vassar.database.template.TemplateRequest;
import vassar.database.template.TemplateResponse;


public class FileTemplateRequest extends TemplateRequest {

    public static class Builder extends TemplateRequest.Builder<Builder> {

        public Builder() {}

        public FileTemplateRequest build() { return new FileTemplateRequest(this); }

    }

    protected FileTemplateRequest(Builder builder){

        super(builder);
    }

    public TemplateResponse processRequest(QueryAPI api) {
        try {

            // PASS TEMPLATE FILE PATH FOR BATCH
            return new TemplateResponse.Builder()
                    .setTemplateString(this.templateFilePath)
                    .build();
        }
        catch (Exception e) {
            System.out.println("Error processing orbit template request: " +e.getClass() + " : " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

}

