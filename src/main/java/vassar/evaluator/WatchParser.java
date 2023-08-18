package vassar.evaluator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jess.*;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WatchParser {

    Rete engine;
    StringWriter watch_writer;
    ArrayList<JsonObject> facts;
    ArrayList<JsonObject> activations;
    JsonObject rules;


    public WatchParser(Rete engine, StringWriter watch_writer){
        System.out.println("--> CREATING WATCH PARSER");


        this.engine = engine;

        this.facts = new ArrayList<>();
        this.activations = new ArrayList<>();
        this.rules = new JsonObject();

        this.watch_writer = watch_writer;



    }


    public void runParsing(){
        String[] lines = this.watch_writer.toString().split("\\n");
        JsonArray rule_names = new JsonArray();


        try{
            BufferedWriter writer = new BufferedWriter(new FileWriter("/app/debug/datasets/rules_fired.txt"));
            for (String line : lines) {
                writer.write(line);
                writer.newLine(); // This adds a new line after each line
            }
        }
        catch (Exception ex){
            ex.printStackTrace();
        }

        // --> Parse lines into: facts, activations, rules
        int rule_cnt = 0;
        for (String str : lines) {
//            if (str.startsWith(" ==> f")) {
//                JsonObject clean_str = this.parseFact(str);
//                this.facts.add(clean_str);
//            }
//            if (str.startsWith("==> Activation")) {
//                JsonObject clean_str = this.parseActivation(str);
//                this.activations.add(clean_str);
//            }
            if (str.startsWith("FIRE")) {
                JsonObject clean_str = this.parseRule(str);
                this.rules.add(Integer.toString(rule_cnt), clean_str);
                rule_names.add(clean_str.get("name"));
                rule_cnt++;
            }
        }

        try{
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            FileWriter jsonWriter = new FileWriter("/app/debug/datasets/rules_fired_full.json");
            String jsonString = gson.toJson(this.rules);
            jsonWriter.write(jsonString);
            jsonWriter.flush();

            FileWriter jsonWriter2 = new FileWriter("/app/debug/datasets/rules_fired.json");
            String jsonString2 = gson.toJson(rule_names);
            jsonWriter2.write(jsonString2);
            jsonWriter2.flush();
        }
        catch (Exception ex){
            ex.printStackTrace();
        }




    }




    public void clearBuffer(){
        this.watch_writer.getBuffer().setLength(0);
    }


    public JsonObject parseFact(String fact_str){
        JsonArray slot_pairs = new JsonArray();
        String fact_doc = "";
        String fact_module = "";
        String fact_name = "";


        // --> 1. Get Fact ID
        int index = fact_str.indexOf("f-") + 2;
        int endIndex = fact_str.indexOf(" ", index);
        String numberString = fact_str.substring(index, endIndex);
        int fact_id = Integer.parseInt(numberString);

        // --> 2. Parse important fact info
        // - doc string (description)
        // - slot name / value pairs
        try{
            Fact fact = this.engine.findFactByID(fact_id);
            Deftemplate fact_def = fact.getDeftemplate();

            // doc string
            fact_doc = fact_def.getDocstring();
            fact_module = fact_def.getName().split("::")[0];
            fact_name = fact_def.getName().split("::")[1];

            // slot name / value pairs
            String[] slot_names = fact_def.getSlotNames();
            for(String slot_name: slot_names){
                String pair = slot_name + " " + fact.getSlotValue(slot_name).stringValue(this.engine.getGlobalContext());
                slot_pairs.add(pair);
            }
        }
        catch (Exception ex){
            ex.printStackTrace();
        }

//        System.out.println(this.getFactContext(fact_id));


        // --> 3. Combine elements into json object and return
        String formatted = fact_module + " | " + fact_name + " | " + fact_doc + " | " + slot_pairs.toString();
//        System.out.println("(FACT) " + formatted);
        JsonObject fact_object = new JsonObject();
        fact_object.addProperty("module", fact_module);
        fact_object.addProperty("name", fact_name);
        fact_object.addProperty("description", fact_doc);
        fact_object.addProperty("fact_id", fact_id);
        fact_object.add("slots", slot_pairs);
        return fact_object;
    }

    public JsonObject parseActivation(String activation){
        System.out.println(activation);
        String[] splitString = activation.split(" ");
        String module = splitString[2].split("::")[0];
        String ruleName = splitString[2].split("::")[1];
        JsonArray fact_numbers = new JsonArray();

        for (int i = 4; i < splitString.length; i++) {
            String activationLine = splitString[i];
            String factNumber = "";
            if (activationLine.contains("f-")) {
                int startIndex = activationLine.indexOf("f-") + 2;
                int endIndex = activationLine.indexOf(",", startIndex);
                if (endIndex == -1) {
                    endIndex = activationLine.length();
                }
                factNumber = activationLine.substring(startIndex, endIndex);
            }
            if(!factNumber.equals("")){
                fact_numbers.add(Integer.parseInt(factNumber));
            }
        }

        // --> Get rule description
        String description = "";
        try{
            System.out.println(ruleName);
            HasLHS lhs = this.engine.findDefrule(ruleName);
            description = lhs.getDocstring();
            if(description.equals("")){
                description = "no description";
            }
        }
        catch (Exception ex){
            ex.printStackTrace();
        }


        String formatted = module + " | " + ruleName + " | " + fact_numbers;
//        System.out.println("(ACTIVATION) " + formatted);
        JsonObject fact_object = new JsonObject();
        fact_object.addProperty("module", module);
        fact_object.addProperty("name", ruleName);
        fact_object.addProperty("description", description);
        fact_object.add("facts", fact_numbers);
        return fact_object;
    }

    public JsonObject parseRule(String rule){

        // --> Get rule module, name, and facts
        String description = "";
        String[] items = new String[3];
        String[] words = rule.split(" ");
        String module = words[2].split("::")[0]; // Extract "MATH" from "MATH::generate-combos"
        String rule_name = words[2].split("::")[1]; // Extract "generate-combos" from "MATH::generate-combos"
        String[] factNumbers = Arrays.copyOfRange(words, 3, words.length); // Extract fact numbers
        JsonArray fact_numbers = new JsonArray();
        for(String str: factNumbers){
            fact_numbers.add(Integer.parseInt(str.replaceAll("[^0-9\\s]", "")));
        }

        // --> Get rule description
        try{
            HasLHS lhs = this.engine.findDefrule(rule_name);
            description = lhs.getDocstring();
            if(description.equals("")){
                description = "no description";
            }
        }
        catch (Exception ex){
            System.out.println("--> RULE HAS NO DESCRIPTION");
//            ex.printStackTrace();
        }

        String formatted = module + " | " + rule_name + " | " + description + " | " + fact_numbers;
//        System.out.println("(RULE) " + formatted);
        // --> 3. Combine elements into json object and return
        JsonObject rule_object = new JsonObject();
        rule_object.addProperty("module", module);
        rule_object.addProperty("name", rule_name);
        rule_object.addProperty("description", description);
        rule_object.add("facts", fact_numbers);
        return rule_object;
    }





    public String getModuleContext(String module){
        StringBuilder module_info = new StringBuilder();
        module_info.append(" * ").append(module).append(" Module: ");

        // --> 1. Describe the module (evaluation step)
        String module_description = "";
        try{
            Defmodule mod = this.engine.findModule(module);
            module_description = mod.getDocstring();
        }
        catch (Exception ex){
            ex.printStackTrace();
        }

        // --> 2. Print facts involved with fact IDs
        for(JsonObject fact: this.facts){

        }




        // --> 3. Print rules fired and fact IDs involved for each rule (unique on fact IDs)



        // --> 4. Print facts asserted from module rules








        return module_info.toString();
    }




    public String getRuleContext(String rule_name){





        return "";
    }


    // --> This will likely not be used
    public String getFiredRuleContext(String rule_name, JsonArray fact_ids){




        return "";
    }








    // --> Finished
    // - Replaces words (including hyphenated) preceded by _ with it's corresponding slot value.
    public String getFactContext(int fact_id){
        HashMap<String, String> slot_values = new HashMap<>();
        JsonArray fact_slots = new JsonArray();
        String fact_doc = "";
        String fact_module = "";
        String fact_name = "";

        try{
            Fact fact = this.engine.findFactByID(fact_id);
            Deftemplate fact_def = fact.getDeftemplate();

            // doc string
            fact_doc = fact_def.getDocstring();
            fact_module = fact_def.getName().split("::")[0];
            fact_name = fact_def.getName().split("::")[1];

            // slot name / value pairs
            String[] slot_names = fact_def.getSlotNames();
            for(String slot_name: slot_names){
                String slot_value = fact.getSlotValue(slot_name).stringValue(this.engine.getGlobalContext());
                slot_values.put(slot_name, slot_value);
                String pair = slot_name + " " + slot_value;
                fact_slots.add(pair);
            }

            Pattern pattern = Pattern.compile("_[\\w-]+");
            Matcher matcher = pattern.matcher(fact_doc);
            StringBuffer buffer = new StringBuffer();
            while (matcher.find()) {
                String slotName = matcher.group().substring(1);
                String slotValue = slot_values.remove(slotName);
                matcher.appendReplacement(buffer, slotValue != null ? slotValue : "");
            }
            matcher.appendTail(buffer);
            fact_doc = buffer.toString();
        }
        catch (Exception ex){
            ex.printStackTrace();
        }

        StringBuilder builder = new StringBuilder();
        builder.append("    - ").append(fact_name).append(": ");
        if(!fact_doc.equals("")){
            builder.append("This fact ").append(fact_doc).append(". ");
        }
        else{
            builder.append("(no description). ");
        }
        if(fact_slots.size() == 0){
            builder.append("(no values). ");
        }
        else{
            if(slot_values.size() != 0){
                builder.append("It has following values: ");
            }
            int idx = 0;
            for(String key: slot_values.keySet()){
                String value = slot_values.get(key);
                builder.append(key).append(" ").append(value);
                if(idx == slot_values.size()-1){
                    builder.append(".");
                }
                else{
                    builder.append(", ");
                }
            }
        }
        return builder.toString();
    }






}