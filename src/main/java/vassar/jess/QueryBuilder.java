package vassar.jess;

import evaluator.EvaluatorApp;
import evaluator.ResourcePaths;
import jess.*;
import vassar.database.service.DebugAPI;
import vassar.problem.Problem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.regex.Matcher;

public class QueryBuilder {

    private Rete r;
    private HashMap<String, HashMap<String, Fact>> precomputedQueries;
    private String   debugDir;
    private DebugAPI debugAPI;

    public static class Builder{

        private Rete r;
        private HashMap<String, HashMap<String, Fact>> precomputedQueries;

        public Builder(Rete r) {
            this.r = r;
            precomputedQueries = new HashMap<>();
        }

        public QueryBuilder build(){
            QueryBuilder build = new QueryBuilder();
            build.r = this.r;
            build.precomputedQueries = this.precomputedQueries;
            build.debugDir = ResourcePaths.rootDirectory + "/debug/QueryBuilder";

            build.debugAPI = new DebugAPI.Builder(ResourcePaths.rootDirectory + "/debug/QueryBuilder/output.json")
                    .setOutputPath(ResourcePaths.rootDirectory + "/debug/QueryBuilder")        
                    .newFile()
                    .build();

            return build;
        }

    }

    public void addPrecomputedQuery(String key, HashMap<String, Fact> hm) {
        this.precomputedQueries.put(key, hm);
    }

    public ArrayList<Fact> makeQuery(String template) {
        ArrayList<Fact> facts = new ArrayList<>();

        String call = "(defquery TempArchitecture-query ?f <- (" + template + "))";

        try {
            r.eval(call);
            QueryResult q_result = r.runQueryStar("TempArchitecture-query", new ValueVector());

            while(q_result.next())
                facts.add((Fact) q_result.getObject("f"));

            r.removeDefrule("TempArchitecture-query");

        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }

        return facts;
    }

    public ArrayList<String> getMissionInstruments(String mission_name){
        ArrayList<Fact> facts = new ArrayList<>();
        ArrayList<String> instruments = new ArrayList<>();

        String query_call = "(defquery modify-query ?f <- (MANIFEST::Mission (Name "+mission_name+")))";

        try{
            this.r.eval(query_call);
            QueryResult q_result = r.runQueryStar("modify-query", new ValueVector());
            while(q_result.next())
                facts.add((Fact) q_result.getObject("f"));
            this.r.removeDefrule("modify-query");

            if(facts.isEmpty()){
                System.out.println("---> MISSION FACT NOT FOUND");
                EvaluatorApp.sleep(3);
                return null;
            }
            else if(facts.size() > 1){
                System.out.println("---> MULTIPLE MISSION FACTS FOUND, CAN ONLY MODIFY ONE");
                EvaluatorApp.sleep(3);
                return null;
            }

            Fact mission = facts.get(0);
            String insts_string = mission.getSlotValue("instruments").listValue(this.r.getGlobalContext()).toString();
            String[] insts = insts_string.split("\\s+");

            for(String inst: insts){
                instruments.add(inst.trim());
            }
            return instruments;
        }
        catch(JessException e){

        }
        return instruments;
    }


    public ArrayList<Fact> getMeasurementFacts(String measurement){
        ArrayList<Fact> facts = new ArrayList<>();
        ArrayList<String> instruments = new ArrayList<>();

        String query_call = "(defquery modify-query ?f <- (REQUIREMENTS::Measurement (Parameter "+measurement+")))";
        try{
            this.r.eval(query_call);
            QueryResult q_result = r.runQueryStar("modify-query", new ValueVector());
            while(q_result.next())
                facts.add((Fact) q_result.getObject("f"));
            this.r.removeDefrule("modify-query");

            if(facts.isEmpty()){
                System.out.println("---> MEASUREMENT FACT NOT FOUND: " + measurement);
                // EvaluatorApp.sleep(1);
                return facts;
            }

            return facts;
        }
        catch(JessException e){
            e.printStackTrace();
        }
        return facts;
    }



    // Assumes there is only one mission !!!
    public String getMissionSlotValue(String slot_name){
        ArrayList<Fact> facts = new ArrayList<>();

        String query_call = "(defquery modify-query ?f <- (MANIFEST::Mission))";

        try{
            this.r.eval(query_call);
            QueryResult q_result = r.runQueryStar("modify-query", new ValueVector());
            while(q_result.next())
                facts.add((Fact) q_result.getObject("f"));
            this.r.removeDefrule("modify-query");

            if(facts.isEmpty()){
                System.out.println("---> MISSION FACT NOT FOUND");
                EvaluatorApp.sleep(3);
                return null;
            }
            else if(facts.size() > 1){
                System.out.println("---> MULTIPLE MISSION FACTS FOUND, ASSUMES ONE FACT");
                EvaluatorApp.sleep(3);
                return null;
            }

            Fact mission = facts.get(0);
            String slot_value = mission.getSlotValue(slot_name).stringValue(this.r.getGlobalContext());
            return slot_value;
        }
        catch(JessException e){

        }
        return "";
    }





    public ArrayList<Fact> saveQuery(String fileName, String template){

        ArrayList<Fact> facts = new ArrayList<>();

        String call = "(defquery TempArchitecture-query ?f <- (" + template + "))";

        try {
            r.eval(call);
            QueryResult q_result = r.runQueryStar("TempArchitecture-query", new ValueVector());

            while(q_result.next())
                facts.add((Fact) q_result.getObject("f"));

            r.removeDefrule("TempArchitecture-query");

        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }

        if(ResourcePaths.writeFiles){
            String debug = "empty query";
            if(!facts.isEmpty()){
                debug = "";
                int counter = 1;

                // Sort facts based on an attribute value
                Collections.sort(facts, this.getFactComparator());

                for(Fact fct: facts){
                    debug += "\n--------------- " + template + " - " + counter + " ---------------\n";


                    debug += this.transformFactString(fct.toStringWithParens());
                    counter++;
                }
            }
            this.debugAPI.writeTemplateOutputFileName(fileName, debug);
        }

        return facts;
    }

    public ArrayList<Fact> saveDirectQuery(String fileName, String template){

        ArrayList<Fact> facts = new ArrayList<>();

        String call = "(defquery TempArchitecture-query ?f <- (" + template + "))";

        try {
            r.eval(call);
            QueryResult q_result = r.runQueryStar("TempArchitecture-query", new ValueVector());

            while(q_result.next())
                facts.add((Fact) q_result.getObject("f"));

            r.removeDefrule("TempArchitecture-query");

        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }

        String debug = "empty query";
        if(!facts.isEmpty()){
            debug = "";
            int counter = 1;
            for(Fact fct: facts){
                debug += "\n--------------- " + template + " - " + counter + " ---------------\n";


                debug += this.transformFactString(fct.toStringWithParens());
                counter++;
            }
        }

        this.debugAPI.writeTemplateOutputFileName(fileName, debug);
        return facts;
    }

    public String transformFactString(String fact){
        String trans = "";
        String clipped = fact.substring(1, fact.length()-1);

        Matcher m = java.util.regex.Pattern.compile("\\((.*?)\\)").matcher(clipped);
        while(m.find()) {
            trans += (m.group() + "\n");
        }

        return trans;
    }




    public Comparator<Fact> getFactComparator(){
        Rete engine = this.r;
        return new Comparator<Fact>() {
            @Override
            public int compare(Fact o1, Fact o2) {
                Deftemplate f1 = o1.getDeftemplate();
                String[] slot_names = f1.getSlotNames();
                String true_slot = "";
                boolean slot_found = false;
                for(String slot_name: slot_names) {


                    // Comparison on Name attribute
                    if (slot_name.equals("Name")) {
                        true_slot = slot_name;
                        slot_found = true;
                    } else if (slot_name.equals("Parameter")) {
                        true_slot = slot_name;
                        slot_found = true;
                    }

                    // Evaluate and return
                    if (slot_found) {
                        try {
                            String v1 = o1.getSlotValue(true_slot).stringValue(engine.getGlobalContext());
                            String v2 = o2.getSlotValue(true_slot).stringValue(engine.getGlobalContext());
                            return v1.compareTo(v2);
                        } catch (Exception e) {
                            e.printStackTrace();
                            System.exit(0);
                        }
                    }
                }
                return 0;
            }
        };
    }





    public ArrayList<Fact> missionFactQuery(String fileName){

        ArrayList<Fact> facts = new ArrayList<>();

        String call = "(defquery TempArchitecture-query ?f <- (MANIFEST::Mission))";

        try {
            r.eval(call);
            QueryResult q_result = r.runQueryStar("TempArchitecture-query", new ValueVector());

            while(q_result.next())
                facts.add((Fact) q_result.getObject("f"));

            r.removeDefrule("TempArchitecture-query");

        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }

        String debug = "empty query";
        if(!facts.isEmpty()){
            debug = "";
            int counter = 1;
            for(Fact fct: facts){
                debug+="\n--------------- RESULT " + counter + " ---------------\n";
                debug+=fct.toStringWithParens();
                counter++;
            }
        }

        this.debugAPI.writeTemplateOutputFileName(fileName, debug);
        return facts;
    }



    public boolean modifyMissionFact(String query_slot, String query_slot_value, String slot_modify, String slot_modify_value){
        ArrayList<Fact> facts = new ArrayList<>();
        String fact_id = "";


        try{

            String query_call = "(defquery modify-query ?f <- (MANIFEST::Mission ("+query_slot+" "+query_slot_value+")))";
            this.r.eval(query_call);
            QueryResult q_result = r.runQueryStar("modify-query", new ValueVector());
            while(q_result.next())
                facts.add((Fact) q_result.getObject("f"));
            this.r.removeDefrule("modify-query");

            if(facts.isEmpty()){
                System.out.println("---> MISSION FACT NOT FOUND");
                EvaluatorApp.sleep(3);
                return false;
            }
            else if(facts.size() > 1){
                System.out.println("---> MULTIPLE MISSION FACTS FOUND, CAN ONLY MODIFY ONE");
                EvaluatorApp.sleep(3);
                return false;
            }

            Fact mission = facts.get(0);
            fact_id = Integer.toString(mission.getFactId());



            // Modify the mission fact
            String modify_call = "(modify " + fact_id + " (" + slot_modify + " " + slot_modify_value + "))";
            System.out.println("---> MISSION MOD CALL: " + modify_call);
            this.r.eval(modify_call);
        }
        catch(JessException e){
            e.printStackTrace();
            return false;
        }
        return true;
    }



}
