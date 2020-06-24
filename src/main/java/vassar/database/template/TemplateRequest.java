package vassar.database.template;

import com.mitchellbosecke.pebble.PebbleEngine;
import com.mitchellbosecke.pebble.extension.AbstractExtension;
import com.mitchellbosecke.pebble.template.PebbleTemplate;
import vassar.database.service.QueryAPI;
import vassar.database.template.response.BatchTemplateResponse;
import vassar.problem.Problem;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

public class TemplateRequest {
    // template file path
    //

    protected PebbleEngine        engine;
    protected PebbleTemplate      template;
    protected StringWriter        writer;
    protected String              templateFilePath;
    protected Map<String, Object> context;
    protected Problem.Builder     problemBuilder;


    public static class Builder<T extends Builder<T>>{

        private PebbleEngine   engine;
        private PebbleTemplate template;
        private StringWriter   writer;
        private String         templateFilePath;

        public Builder() {
            this.engine = new PebbleEngine.Builder().build();
            this.writer = new StringWriter();
            this.templateFilePath = "";
        }

        public T applyExtension(AbstractExtension extension) {
            this.engine           = new PebbleEngine.Builder().extension(extension).build();
            return (T) this;
        }

        public T templateFilePath(String templateFilePath){
            this.templateFilePath = templateFilePath;
            this.template = this.engine.getTemplate(templateFilePath);
            return (T) this;
        }

        public TemplateRequest build() { return new TemplateRequest(this);}

    }

    protected TemplateRequest(Builder<?> builder) {

        this.context          = new HashMap<>();
        this.engine           = builder.engine;
        this.writer           = builder.writer;
        this.templateFilePath = builder.templateFilePath;
        this.template         = builder.template;

    }

    public TemplateResponse processRequest(QueryAPI api) {
        try {
            this.template.evaluate(this.writer);
            return new TemplateResponse.Builder()
                                    .setTemplateString(this.writer.toString())
                                    .build();

        }
        catch (Exception e) {
            System.out.println("Error processing template request" + e.getMessage());
        }
        return null;
    }

    public void setProblemBuilder(Problem.Builder problemBuilder) {
        this.problemBuilder = problemBuilder;
    }

    public String getTemplateFilePath() {
        return this.templateFilePath;
    }

}
