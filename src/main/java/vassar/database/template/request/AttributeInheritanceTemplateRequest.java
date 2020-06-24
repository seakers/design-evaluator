package vassar.database.template.request;

        import com.evaluator.*;
        import vassar.database.service.QueryAPI;
        import vassar.database.template.TemplateRequest;
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

    public BatchTemplateResponse processRequest(QueryAPI api) {
        try {
            ArrayList<String> batch = new ArrayList<>();

            // QUERY
            List<AttributeInheritanceQuery.Item> items = api.attributeInheritanceQuery();

            for ( AttributeInheritanceQuery.Item item : items){
                Map<String, Object> context = new HashMap<>();
                this.context.put("item", item);
                this.template.evaluate(this.writer, this.context);
                batch.add(this.writer.toString());
            }

            return new BatchTemplateResponse.Builder()
                                            .setBatch(batch)
                                            .build();
        }
        catch (Exception e) {
            System.out.println("Error processing orbit template request: " +e.getClass() + " : " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

}
