package vassar.architecture;


import com.google.gson.*;
import evaluator.EvaluatorApp;
import jess.Fact;
import jess.JessException;
import jess.Rete;
import org.checkerframework.checker.nullness.Opt;
import org.checkerframework.checker.units.qual.A;
import vassar.evaluator.spacecraft.Orbit;
import vassar.jess.Resource;
import vassar.result.Result;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ADDArchitecture extends AbstractArchitecture {

    protected String          id;
    protected Gson            gson;
    protected JsonArray       design;

    public ArrayList<Fact> mission_fact_ordering;
    public ArrayList<Fact> synergy_fact_ordering;


    public ADDArchitecture(String design){
        super();
        this.gson   = new GsonBuilder().setPrettyPrinting().create();
        this.design = new JsonParser().parse(design).getAsJsonArray();
        this.mission_fact_ordering = new ArrayList<>();
        this.synergy_fact_ordering = new ArrayList<>();
    }

    public ADDArchitecture(JsonArray design){
        super();
        this.gson   = new GsonBuilder().setPrettyPrinting().create();
        this.design = design;
        this.mission_fact_ordering = new ArrayList<>();
        this.synergy_fact_ordering = new ArrayList<>();
    }

    public ADDArchitecture(){
        super();
    }




    public void optimizePartitioningArchitecture(Resource res){
        HashMap<Integer, ArrayList<String>> partitions = new HashMap<>();

        Iterator spacecraft_iterator = this.design.iterator();
        while(spacecraft_iterator.hasNext()) {
            ArrayList<String> instruments = new ArrayList<>();
            JsonObject spacecraft = ((JsonElement) spacecraft_iterator.next()).getAsJsonObject();
            JsonArray  spacecraft_elements = spacecraft.getAsJsonArray("elements");

            Iterator instrument_iterator = spacecraft_elements.iterator();
            while(instrument_iterator.hasNext()) {
                JsonObject instrument = ((JsonElement) instrument_iterator.next()).getAsJsonObject();
                instruments.add(instrument.get("name").getAsString());
            }
            partitions.put(partitions.size(), instruments);
        }

        ArrayList<Integer> ordering = Optimization.scheduleMissions(partitions, res);

        // REBUILD: this.designs
        JsonArray new_design = new JsonArray();
        for(Integer key: ordering){
            ArrayList<String> mission_insts = partitions.get(key);

            JsonObject mission = new JsonObject();
            JsonArray elements = new JsonArray();
            for(String inst: mission_insts){
                JsonObject mission_inst = new JsonObject();
                mission_inst.addProperty("name", inst);
                elements.add(mission_inst);
            }
            mission.add("elements", elements);
            new_design.add(mission);
        }

        this.design = new_design;
    }

    public void optimizeSelectingArchitecture(Resource res){

        ArrayList<String> instruments = new ArrayList<>();
        Iterator it = this.design.iterator();
        while(it.hasNext()){
            JsonObject inst = ((JsonElement) it.next()).getAsJsonObject();
            if(inst.get("active").getAsBoolean()){
                instruments.add(inst.get("name").getAsString());
            }
        }

        HashMap<Integer, ArrayList<String>> partitions = Optimization.partitionInstruments(instruments, res);

        ArrayList<Integer> ordering = Optimization.scheduleMissions(partitions, res);

        System.out.println("--> INSTRUMENTS: " + instruments);
        System.out.println("--> PARTITIONS: " + partitions);
        System.out.println("--> ORDERING: " + ordering);

        // REBUILD: this.designs
        JsonArray new_design = new JsonArray();
        for(Integer key: ordering){
            ArrayList<String> mission_insts = partitions.get(key);

            JsonObject mission = new JsonObject();
            JsonArray elements = new JsonArray();
            for(String inst: mission_insts){
                JsonObject mission_inst = new JsonObject();
                mission_inst.addProperty("name", inst);
                elements.add(mission_inst);
            }
            mission.add("elements", elements);
            new_design.add(mission);
        }

        System.out.println(this.gson.toJson(new_design));

        this.design = new_design;
        // EvaluatorApp.sleep(5);
    }








    public void setMissionOrbit(String orbit, ArrayList<String> mission_instruments){
        // 0.1 Sort mission instruments for comparison
        Collections.sort(mission_instruments);

        // 1. Find mission based on mission instruments
        ArrayList<ArrayList<String>> missions = this.get_array_info();
        int counter = 0;
        for(ArrayList<String> insts: missions){
            if(mission_instruments.equals(insts)){
                this.design.get(counter).getAsJsonObject().addProperty("orbit", orbit);
                return;
            }
            counter++;
        }
        System.out.println("---> WASNT ABLE TO SET MISSION ORBIT");
        // EvaluatorApp.sleep(10);
    }



    @Override
    public boolean isFeasibleAssignment(){
        return true;
    }

    @Override
    public String toString(String delimiter){
        return this.gson.toJson(this.design);
    }

    public String toString(String delimiter, double science, double cost, double data_continuity){
        JsonArray design_perf = this.design.deepCopy();

        JsonObject perf = new JsonObject();
        perf.addProperty("science", science);
        perf.addProperty("cost", cost);
        perf.addProperty("data_continuity", data_continuity);
        design_perf.add(perf);

        return this.gson.toJson(design_perf);
    }

    public String toString(Result result){
        JsonArray design_perf = this.design.deepCopy();

        JsonObject perf = new JsonObject();
        perf.addProperty("science", result.getScience());
        perf.addProperty("cost", result.getCost());
        perf.addProperty("data_continuity", result.getDataContinuityScore());
        design_perf.add(perf);

        design_perf.add(result.subobjectiveInfo);
        return this.gson.toJson(design_perf);
    }



    public ArrayList<ArrayList<String>> get_array_info(){
        ArrayList<ArrayList<String>> instrument_groups = new ArrayList<>();

        JsonArray satellites = this.design;

        // BUILD GROUPS OF INSTRUMENTS
        for(int x = 0; x < satellites.size(); x++){

            // JSON ELEMENTS
            JsonObject sat = satellites.get(x).getAsJsonObject();
            JsonArray instruments = sat.getAsJsonArray("elements");

            // NEW GROUP
            ArrayList<String> new_sat = new ArrayList<>();

            for(int y = 0; y < instruments.size(); y++){
                JsonObject inst = instruments.get(y).getAsJsonObject();
                String inst_name = inst.get("name").getAsString();
                new_sat.add(inst_name);
            }
            Collections.sort(new_sat);
            instrument_groups.add(new_sat);
        }

        return instrument_groups;
    }


    public Set<Orbit> assignArchitecture(Rete engine){

        System.out.println("\n\n-------> ASSIGNING ARCHITECTURE");
        System.out.println(this.gson.toJson(this.design));

        JsonArray satellites = this.design;

        ArrayList<ArrayList<String>> instrument_groups = new ArrayList<>();

        // BUILD GROUPS OF INSTRUMENTS
        for(int x = 0; x < satellites.size(); x++){

            // JSON ELEMENTS
            JsonObject sat = satellites.get(x).getAsJsonObject();
            JsonArray instruments = sat.getAsJsonArray("elements");

            // NEW GROUP
            instrument_groups.add(new ArrayList<String>());

            for(int y = 0; y < instruments.size(); y++){
                JsonObject inst = instruments.get(y).getAsJsonObject();
                String inst_name = inst.get("name").getAsString();
                instrument_groups.get(x).add(inst_name);
            }
        }

        System.out.println(instrument_groups);

        // ADD INSTRUMENTS TO ENGINE
        String mission_name = "";
        int counter = 0;
        for(ArrayList<String> group: instrument_groups){

            if(group.isEmpty()){
                System.out.println("-----> SATELLITE INSTRUMENTS IS EMPTY");
                System.exit(0);
            }

            // MISSION DEFINITION
            mission_name = "mission" + Integer.toString(counter);
            String mission = "(assert (MANIFEST::Mission ";
            String payload = "";
            for(String instrument: group){
                payload += (" " + instrument);
            }

            // ASSERTIONS: synergies now asserted after orbit is selected (in ADDEvaluator)
            String mission_fact_str = "(MANIFEST::Mission (Name "+mission_name+") (instruments " + payload + ") (lifetime 5) (launch-date 2015) (select-orbit yes) (order-index "+counter+") (factHistory F0))";
            String synergy_fact_str = "(SYNERGIES::cross-registered-instruments (instruments " + payload + ") (degree-of-cross-registration spacecraft) (factHistory F0))";

            System.out.println(mission_fact_str);
            // ASSERT
            try {

                Fact mission_fact = engine.assertString(mission_fact_str);
                // Fact synergy_fact = engine.assertString(synergy_fact_str);
                this.mission_fact_ordering.add(mission_fact);
                // this.synergy_fact_ordering.add(synergy_fact);

            } catch (JessException e) {
                e.printStackTrace();
            }
            counter++;
        }

        return null;
    }

    public ArrayList<String> getOrbitsUsed(){
        return (new ArrayList<>());
    }


    public void printMissionFactIdOrdering(){
        System.out.println("\n--> MISSION FACT ORDERING");
        for(Fact fact: this.mission_fact_ordering){
            System.out.println(fact.getFactId());
        }
        System.out.println("\n");
    }

    public static void printMissionFactIdOrdering(ArrayList<Fact> facts){
        System.out.println("\n--> MISSION FACT ORDERING");
        for(Fact fact: facts){
            System.out.println(fact.getFactId());
        }
        System.out.println("\n");
    }


}
