package vassar.jess.modules.orbits;

import evaluator.EvaluatorApp;
import jess.Defrule;
import jess.Fact;
import jess.Rete;
import okhttp3.Handshake;
import okhttp3.Request;
import vassar.architecture.ADDArchitecture;
import vassar.database.template.TemplateRequest;
import vassar.database.template.TemplateResponse;
import vassar.evaluator.spacecraft.Orbit;
import vassar.jess.QueryBuilder;
import vassar.jess.Requests;
import vassar.jess.Resource;

import java.util.*;

public class Orbits {



    private Resource resource;



    // --> CONTAINS RULE CHROMOSOME STRUCTURE
    public static class Builder{

        private Resource resource;
        private HashMap<String, TemplateResponse> chromosome_map;

        public Builder(Resource resource){
            this.resource       = resource;
            this.chromosome_map = new HashMap<>();
        }


        // This clears all the current rules
        public Builder clear(){
            if(this.chromosome_map.isEmpty()){
                System.out.println("---> CAN'T CLEAR: CHROMOSOME MAP IS NULL");
                return this;
            }

            // Code to clear rules
            return this;
        }



        public Orbits build(){
            Orbits   build          = new Orbits(this.resource);
            Requests orbit_requests = new Requests.Builder().buildOrbits();


            // Add re-computed orbit rules
            for(String key: orbit_requests.requests_map.keySet()){
                TemplateRequest  request  = orbit_requests.requests_map.get(key);
                TemplateResponse response = this.resource.dbClient.processTemplateRequest(request, this.resource.problemBuilder);
                this.chromosome_map.put(key, response);
            }


            return build;
        }



        // Rebuild the module with the new chromosome structure
        // Structure: ArrayList<Integer>
        public Orbits rebuild(String rules, ArrayList<Integer> chromosome){
            Orbits   build          = new Orbits(this.resource);

            if(this.chromosome_map.isEmpty()){
                System.out.println("---> CAN'T REBUILD: CHROMOSOME MAP IS NULL");
                EvaluatorApp.sleep(50);
                return null;
            }

            TemplateResponse response = this.chromosome_map.get(rules);
            response.evaluateChromosome(chromosome);

            return build;
        }



    }


    private QueryBuilder   q_builder;
    private Rete           engine;
    private HashSet<Orbit> orbitsUsed;
    private String         output_path;

    public Orbits(Resource resource){
        this.resource  = resource;
        this.q_builder = resource.getQueryBuilder();
        this.engine    = resource.getEngine();
        this.output_path = "/app/src/main/java/vassar/jess/modules/orbits/output/";
    }






    public HashSet<Orbit> evaluate(ADDArchitecture arch){

        this.q_builder.saveQuery("orbit-selection/1-STARTING-MISSIONS", "MANIFEST::Mission");
        HashMap<String, String> mission_to_selected_orbit = new HashMap<>();

        try {

            // Create all potential mission orbits
            this.engine.setFocus("ORBIT-SELECTION");
            this.engine.run();

            this.q_builder.saveDirectQuery(this.output_path + "2-ENUMERATE-MISSIONS.txt", "MANIFEST::Mission");
            this.q_builder.saveDirectQuery(this.output_path + "3-ENUMERATE-ORBITS.txt", "ORBIT-SELECTION::orbit");

            // key: mission name
            // value: mapping orbit to penalty value
            HashMap<String, HashMap<String, Double>> mission_orbits = this.get_mission_orbit_penalties();

            // key: mission name
            // value: array of best orbits
            HashMap<String, ArrayList<String>> best_orbits = this.find_best_orbits(mission_orbits);

            // key: mission name
            // value: best orbit
            mission_to_selected_orbit = this.choose_best_orbits(best_orbits);

            // - removes dominated orbit facts
            this.engine.setFocus("ORBIT-SELECTION");
            this.engine.run();

            this.q_builder.saveQuery(this.output_path + "4-FINAL-MISSIONS.txt", "MANIFEST::Mission");

            // - remove all orbit rules
            this.remove_rules();

        }
        catch (Exception e){
            e.printStackTrace();
        }

        // Create Orbit objects for each design
        this.build_orbits_used(mission_to_selected_orbit, arch);

        return this.orbitsUsed;
    }

    private HashMap<String, HashMap<String, Double>> get_mission_orbit_penalties(){
        HashMap<String, HashMap<String, Double>> mission_orbits = new HashMap<>();

        try{
            ArrayList<Fact> orbit_facts = this.q_builder.makeQuery("ORBIT-SELECTION::orbit");
            for(Fact fct: orbit_facts){

                String mission_name          = fct.getSlotValue("in-mission").stringValue(this.engine.getGlobalContext());
                String mission_orbit         = fct.getSlotValue("orb").stringValue(this.engine.getGlobalContext());
                String prnalty_var_name      = fct.getSlotValue("penalty-var").stringValue(this.engine.getGlobalContext());
                Double mission_orbit_penalty = this.engine.eval(prnalty_var_name).floatValue(this.engine.getGlobalContext());

                HashMap<String, Double> orbit_penalties;
                if(!mission_orbits.containsKey(mission_name)){
                    orbit_penalties = new HashMap<>();
                    mission_orbits.put(mission_name, orbit_penalties);
                }
                else{
                    orbit_penalties = mission_orbits.get(mission_name);
                }

                if(orbit_penalties.containsKey(mission_orbit)){
                    Double pen = orbit_penalties.get(mission_orbit);
                    orbit_penalties.put(mission_orbit, pen + mission_orbit_penalty);
                }
                else{
                    orbit_penalties.put(mission_orbit, mission_orbit_penalty);
                }
            }
            System.out.println(mission_orbits);
        }
        catch(Exception e){
            e.printStackTrace();
        }

        return mission_orbits;
    }

    // Finds the best orbit for each mission
    private HashMap<String, ArrayList<String>> find_best_orbits(HashMap<String, HashMap<String, Double>> mission_orbits){
        Set<String> mission_keys = mission_orbits.keySet();
        HashMap<String, ArrayList<String>> best_orbits = new HashMap<>();

        try{
            for(String mission_key: mission_keys){
                Double init_penalty = 100000.0;
                HashMap<String, Double> penalties = mission_orbits.get(mission_key);
                ArrayList<String> best_orbit = new ArrayList<>();
                best_orbits.put(mission_key, best_orbit);
                for(String penalty_key: penalties.keySet()){
                    Double single_penalty = penalties.get(penalty_key);
                    if(single_penalty < init_penalty){
                        best_orbit = new ArrayList<>();
                        init_penalty = single_penalty;
                        best_orbit.add(penalty_key);
                        best_orbits.put(mission_key, best_orbit);
                    }
                    else if(single_penalty == init_penalty){
                        best_orbit.add(penalty_key);
                        best_orbits.put(mission_key, best_orbit);
                    }
                }
            }
            System.out.println("\n\n" + best_orbits);
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return best_orbits;
    }

    private HashMap<String, String> choose_best_orbits(HashMap<String, ArrayList<String>> best_orbits){
        HashMap<String, String> mission_to_selected_orbit = new HashMap<>();

        try{
            for(String mission_key: best_orbits.keySet()){
                ArrayList<String> orbits = best_orbits.get(mission_key);
                Random rand = new Random();
                String random_orbit = orbits.get(rand.nextInt(orbits.size()));
                int start_idx = random_orbit.indexOf('-') + 1;
                String random_orbit_trm = random_orbit.substring(start_idx);
                System.out.println(random_orbit_trm);
                mission_to_selected_orbit.put(mission_key, random_orbit_trm);

                String call = "(defrule ORBIT-SELECTION::remove-suboptimal-rules-" + mission_key +
                        " \"Remove Mission facts that have suboptimal orbits\" " +
                        " ?miss <- (MANIFEST::Mission (Name " + mission_key + ") " +
                        " (in-orbit ?orb&:(neq ?orb " + random_orbit_trm + ")))" +
                        " =>" +
                        " (retract ?miss)" + " )";
                System.out.println(call);
                this.engine.eval(call);
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }

        return mission_to_selected_orbit;
    }

    private void remove_rules(){

        try{
            // Remove added rules
            Iterator rule_itr = this.engine.listDefrules();
            while(rule_itr.hasNext()){
                Object rule = rule_itr.next();
                if(rule.getClass().getName() == "jess.Defrule"){
                    Defrule rulec = (Defrule) rule;
                    if(rulec.getName().contains("ORBIT-SELECTION::remove-suboptimal-rules")){
                        this.engine.removeDefrule(rulec.getName());
                    }
                }
            }

            // Remove all ORBIT-SELECTION::orbit facts
            Iterator fact_itr = this.engine.listFacts();
            while(fact_itr.hasNext()){
                Object fact = fact_itr.next();
                if(fact.getClass().getName() == "jess.Fact"){
                    Fact factc = (Fact) fact;
                    // System.out.println(factc.getDeftemplate().toString());
                    if(factc.getDeftemplate().toString().contains("deftemplate ORBIT-SELECTION::orbit")){
                        this.engine.retract(factc);
                    }
                }
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    // Sets: this.orbits_used
    private void build_orbits_used(HashMap<String, String> mission_to_selected_orbit, ADDArchitecture arch){
        this.orbitsUsed = new HashSet<>();

        try{
            for(String mission_name: mission_to_selected_orbit.keySet()){
                String            orbit_name          = mission_to_selected_orbit.get(mission_name);
                ArrayList<String> mission_instruments = this.q_builder.getMissionInstruments(mission_name);
                arch.setMissionOrbit(orbit_name, mission_instruments);
                orbitsUsed.add(new Orbit(orbit_name, 1, 1));
            }

        }
        catch (Exception e){
            e.printStackTrace();
        }
    }













}
