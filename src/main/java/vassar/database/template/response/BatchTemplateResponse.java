package vassar.database.template.response;

import vassar.database.template.TemplateResponse;
import jess.Rete;

import java.util.ArrayList;


public class BatchTemplateResponse extends TemplateResponse {

    private ArrayList<String> batch;
    private String            batchString;
    private boolean           useString;


    public static class Builder extends TemplateResponse.Builder<Builder> {

        private ArrayList<String> batch;
        private String            batchString;
        private boolean           useString;

        public Builder() {}

        public Builder setBatch(ArrayList<String> batch) {
            this.batch     = batch;
            this.useString = false;
            return this;
        }

        public Builder setBatchString(String batchString) {
            this.batchString = batchString;
            this.useString   = true;
            return this;
        }

        public BatchTemplateResponse build() { return new BatchTemplateResponse(this); }

    }

    protected BatchTemplateResponse(Builder builder){

        super(builder);
        this.batch       = builder.batch;
        this.batchString = builder.batchString;
        this.useString   = builder.useString;
    }

    public void evaluate(Rete r){

        if (this.useString) {
            try {
                r.batch(this.batchString);
            }
            catch (Exception e) {
                System.out.println("Rete batch string evaluation error " + e.getMessage());
            }
        }
        else {
            for(String item : this.batch){
                try {
                    r.eval(item);
                }
                catch (Exception e) {
                    System.out.println("Rete batch list evaluation error" + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    public String getTemplateString() {

        if (this.useString) {
            return this.batchString;
        }
        else {
            String batch_str = "";
            for (String item: this.batch) {
                batch_str += (" " + item);
            }
            return batch_str;

        }

    }

}
