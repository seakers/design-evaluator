package vassar.jess;

import evaluator.EvaluatorApp;
import jess.*;
import vassar.database.service.DebugAPI;
import vassar.problem.Problem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;

public class QueryBuilder {

    private Rete r;
    private HashMap<String, HashMap<String, Fact>> precomputedQueries;
    private String   debug_dir;
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
            build.debug_dir = "/app/debug/QueryBuilder";

            build.debugAPI = new DebugAPI.Builder("/app/debug/QueryBuilder/output.json")
                    .newFile()
                    .setOutputPath("/app/debug/QueryBuilder")
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
            System.out.println(m.group(1));
            trans += (m.group() + "\n");
        }

        return trans;
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
