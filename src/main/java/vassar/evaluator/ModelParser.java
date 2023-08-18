package vassar.evaluator;

import jess.Defquery;
import jess.Defrule;
import jess.HasLHS;
import jess.Rete;
import com.google.gson.*;
import vassar.jess.func.RawSafety;

import java.io.FileWriter;
import java.util.*;

public class ModelParser {
    public Rete engine;
    public Map<String, List<Defrule>> rulemap;
    public ModelParser(Rete engine) throws Exception{
        this.engine = engine;
    }

    public void saveVocabulary() throws Exception{
        System.out.println("\n\n\n--> SAVING VOCABULARY");
        JsonObject vocabulary = new JsonObject();

        Iterator<HasLHS> ruleIter = RawSafety.castType(this.engine.listDefrules());
        Iterator<HasLHS> ruleIterCheck = RawSafety.castType(this.engine.listDefrules());
        int count = 0;
        while (ruleIter.hasNext()) {
            HasLHS ruleCheck = ruleIterCheck.next();
            if (ruleCheck instanceof Defquery) {
                ruleIter.next();
            }
            else if (ruleCheck instanceof Defrule) {
                Defrule rule = (Defrule)ruleIter.next();
                String moduleName = rule.getModule();
                String ruleFullName = rule.getName();
                String[] parts = ruleFullName.split("::");
                String ruleName = parts[1];
                JsonObject ruleJson = new JsonObject();
                ruleJson.addProperty("module", moduleName);
                ruleJson.addProperty("name", ruleName);
                vocabulary.add(Integer.toString(count), ruleJson);
                System.out.println("-- " + Integer.toString(count) + " " + moduleName + " " + ruleName);
                count++;
            }
        }

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        FileWriter jsonWriter = new FileWriter("/app/debug/datasets/vocabulary.json");
        String jsonString = gson.toJson(vocabulary);
        jsonWriter.write(jsonString);
        jsonWriter.flush();

    }




    public void parseModule(String module){

    }


}
