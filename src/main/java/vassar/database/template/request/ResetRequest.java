package vassar.database.template.request;

import com.evaluator.MissionAttributeQuery;
import vassar.database.service.QueryAPI;
import vassar.database.template.TemplateRequest;
import vassar.database.template.TemplateResponse;
import vassar.database.template.response.ResetResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResetRequest extends TemplateRequest {

    private String focus;

    public static class Builder extends TemplateRequest.Builder<Builder> {

        private String focus;

        public Builder() {}

        public Builder setFocus(String focus){
            this.focus = focus;
            return this;
        }

        public ResetRequest build() { return new ResetRequest(this); }

    }

    protected ResetRequest(Builder builder){

        super(builder);
        this.focus = builder.focus;
    }

    public TemplateResponse processRequest(QueryAPI api) {
        try {
            // RESET
            return new ResetResponse.Builder()
                    .setFocus(this.focus)
                    .build();
        }
        catch (Exception e) {
            System.out.println("Error processing reset request: " +e.getClass() + " : " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

}

