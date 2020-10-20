package vassar.database.template.rules;

import com.evaluator.OrbitAttributeQuery;
import com.mitchellbosecke.pebble.template.PebbleTemplate;
import vassar.database.service.QueryAPI;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OrbitAttribute extends Rules {

    public static class Builder{

        private QueryAPI api;
        private String   template_file;

        public Builder(QueryAPI api){
            this.api = api;
        }

        public Rules buildRules(){
            List<OrbitAttributeQuery.Item> items = api.orbitAttributeQuery();
            Rules rules = new OrbitAttribute(this.template_file, items);

            return rules;
        }

    }


    public List<OrbitAttributeQuery.Item> rules;
    public ArrayList<Integer> chromosome;
    public String template_header;

    public OrbitAttribute(String template_file, List<OrbitAttributeQuery.Item> rules){
        super(template_file);

        this.rules = rules;

        // Initialize chromosome to all true
        this.chromosome = new ArrayList<>();
        for(int x = 0; x < this.rules.size(); x++){
            this.chromosome.add(1);
        }

        this.template_header = "DATABASE::Orbit";
    }




    @Override
    public String toString(ArrayList<Integer> chromosome){
        List<OrbitAttributeQuery.Item> active_rules = new ArrayList<>();

        int counter = 0;
        for(int x = 0; x < chromosome.size(); x++){
            Integer bit = chromosome.get(x);
            OrbitAttributeQuery.Item attribute = this.rules.get(x);

            if(bit == 1){
                active_rules.add(attribute);
            }
            counter++;
        }

        return this.get_string(active_rules);
    }


    @Override
    public String toString(){

        return this.get_string(this.rules);
    }

    public String get_string(List<OrbitAttributeQuery.Item> active_rules){

        this.context.put("template_header", this.template_header);
        this.context.put("items", active_rules);
        try{
            this.template.evaluate(this.writer, this.context);
        }
        catch (Exception e){
            e.printStackTrace();
        }

        return this.writer.toString();
    }

}
