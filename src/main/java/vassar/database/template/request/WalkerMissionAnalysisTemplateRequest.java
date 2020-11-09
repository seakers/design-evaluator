package vassar.database.template.request;

import com.evaluator.WalkerMissionAnalysisQuery;
import vassar.database.service.QueryAPI;
import vassar.database.template.TemplateRequest;
import vassar.database.template.TemplateResponse;

import java.util.List;

public class WalkerMissionAnalysisTemplateRequest extends TemplateRequest{


    private String template_header;

    public static class Builder extends TemplateRequest.Builder<Builder> {

        private String template_header;

        public Builder() {}



        public Builder templateHeader(String template_header) {
            this.template_header = template_header;
            return this;
        }

        public WalkerMissionAnalysisTemplateRequest build() { return new WalkerMissionAnalysisTemplateRequest(this); }

    }

    protected WalkerMissionAnalysisTemplateRequest(WalkerMissionAnalysisTemplateRequest.Builder builder) {

        super(builder);
        this.template_header   = builder.template_header;
    }



    public TemplateResponse processRequest(QueryAPI api) {
        try{
            List<WalkerMissionAnalysisQuery.Item> items = api.walkerMissionAnalysisQuery();

            this.context.put("items", items);


            // EVALUATE
            this.template.evaluate(this.writer, this.context);
            return new TemplateResponse.Builder()
                    .setTemplateString(this.writer.toString())
                    .build();
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }





}
