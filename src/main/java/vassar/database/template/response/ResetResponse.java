package vassar.database.template.response;

import jess.Rete;
import vassar.database.template.TemplateResponse;

public class ResetResponse extends TemplateResponse {

    private String focus;

    public static class Builder extends TemplateResponse.Builder<Builder> {

        private String focus;

        public Builder() {}

        public Builder setFocus(String focus){
            this.focus = focus;
            return this;
        }

        public ResetResponse build() { return new ResetResponse(this); }

    }

    protected ResetResponse(Builder builder){

        super(builder);
        this.focus = builder.focus;
    }

    public void evaluate(Rete r){

        try {
            r.reset();
            r.setFocus(this.focus);
            r.run();
        }
        catch (Exception e) {
            System.out.println("Error executing reset response" + e.getMessage());
        }

    }

    public String getTemplateString() {

        return "----> RESET REQUEST";
    }

}
