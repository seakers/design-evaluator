package vassar.database.template.rules;

import com.mitchellbosecke.pebble.PebbleEngine;
import com.mitchellbosecke.pebble.extension.AbstractExtension;
import com.mitchellbosecke.pebble.template.PebbleTemplate;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Rules {

    protected PebbleEngine   engine;
    protected PebbleTemplate template;
    protected StringWriter writer;
    public Map<String, Object> context;


    public Rules(String template_file_path){
        this.engine = new PebbleEngine.Builder().build();
        this.template = this.engine.getTemplate(template_file_path);
        this.writer = new StringWriter();
        this.context = new HashMap<>();
    }

    public Rules(String template_file_path, AbstractExtension extension){
        this.engine = new PebbleEngine.Builder().extension(extension).build();
        this.template = this.engine.getTemplate(template_file_path);
        this.writer = new StringWriter();
        this.context = new HashMap<>();
    }







    public String toString(ArrayList<Integer> chromosome){
        return "";
    }

    public String toString(){
        return "";
    }
}
