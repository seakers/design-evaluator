package vassar.database.template.request;

        import com.evaluator.*;
        import vassar.database.service.QueryAPI;
        import vassar.database.template.TemplateRequest;
        import vassar.database.template.TemplateResponse;
        import vassar.database.template.response.BatchTemplateResponse;

        import java.util.ArrayList;
        import java.util.HashMap;
        import java.util.List;
        import java.util.Map;


public class AttributeInheritanceTemplateRequest extends TemplateRequest {


    public static class Builder extends TemplateRequest.Builder<Builder> {

        public Builder() {}


        public AttributeInheritanceTemplateRequest build() { return new AttributeInheritanceTemplateRequest(this); }

    }

    protected AttributeInheritanceTemplateRequest(Builder builder){

        super(builder);
    }





    public TemplateResponse processRequest(QueryAPI api) {
        try {

            // QUERY
            List<AttributeInheritanceQuery.Item> items = api.attributeInheritanceQuery();

            this.context.put("items", items);
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
