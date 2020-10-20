package vassar.database.template;

import jess.Rete;

import java.util.ArrayList;

public class TemplateResponse {

    protected String  templateString;
    protected String  templateStringBatch;
    protected boolean isBatch;


    public static class Builder<T extends Builder<T>>{

        private String  templateString;
        private String  templateStringBatch;
        private boolean isBatch;

        public Builder() {
            this.templateString = "...empty...";
        }

        public T setTemplateString(String templateString){
            this.templateString = templateString;
            this.isBatch        = false;
            return (T) this;
        }

        public T setTemplateStringBatch(String templateStringBatch){
            this.templateStringBatch = templateStringBatch;
            this.isBatch             = true;
            return (T) this;
        }

        public TemplateResponse build() { return new TemplateResponse(this);}
    }

    protected TemplateResponse(Builder<?> builder) {

        this.templateString      = builder.templateString;
        this.templateStringBatch = builder.templateStringBatch;
        this.isBatch             = builder.isBatch;
    }

    // -------------------------------------------------
    // ------------------ EVALUATE ---------------------
    // -------------------------------------------------
    public void evaluateChromosome(ArrayList<Integer> chromosome){




    }




    public void evaluate(Rete r){
        if (this.isBatch) {
            try{
                r.batch(this.templateStringBatch);
            }
            catch (Exception e) {
                System.out.println("Rete batch evaluation error: " + e.getMessage());
                System.out.println("Rete batch stack trace: ");
                e.printStackTrace();
            }
        }
        else {
            try{
                r.eval(this.templateString);
            }
            catch (Exception e) {
                System.out.println("Rete single evaluation error: " + e.getMessage());
            }
        }
    }

    public String getTemplateString() {
        if (this.isBatch){
            return this.templateStringBatch;
        }
        else {
            return this.templateString;
        }
    }
}
