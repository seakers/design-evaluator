package vassar.architecture;

import evaluator.ResourcePaths;
import jess.Fact;
import jess.Rete;
import vassar.jess.Resource;
import vassar.result.Result;

import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DesignWriter {

    public Resource resource;
    public Rete r;
    public Result result;
    public String id;
    public String context;
    public Path design_file;
    public Path design_store;
    public Path design_dir;
    public String design_tag;

    public ArrayList<String> insts = new ArrayList<>();
    
    
    public Path objectives_file;
    public Path architecture_file;
    public Path cost_components_dir;
    public Path satellite_design_dir;
    public Path stakeholder_dir;
    public Path measurements_dir;


    public DesignWriter(Result result, Resource resource){

        // --> Initialize instruments
        insts.add("BIOMASS");
        insts.add("CMIS");
        insts.add("VIIRS");
        insts.add("SMAP_MWR");
        insts.add("SMAP_RAD");


        // --> Initialize design directory
        this.design_store = Paths.get(ResourcePaths.rootDirectory, "design");
        int dir_count = 0;
        try{
            dir_count = (int) Files.list(this.design_store).count();
        }
        catch (Exception ex){
            ex.printStackTrace();
        }
        this.design_tag = "D" + dir_count;
        this.design_dir = Paths.get(ResourcePaths.rootDirectory, "design", this.design_tag);
        this.createDirectory(this.design_dir);
        this.resource = resource;
        this.r = this.resource.getEngine();
        System.out.println("\n\n----- DESIGN CONTEXT -----");
        this.result = result;
        this.id = "1";
        
        // --> Initialize Context Files
        System.out.println("DesignWriter: Initializing Context Files / Directories");
        this.objectives_file = Paths.get(this.design_dir.toString(), "objectives.txt");
        this.writeContext(this.objectives_file, this.buildObjectives());

        // Cost
        this.cost_components_dir = Paths.get(this.design_dir.toString(), "cost_components");
        this.createDirectory(this.cost_components_dir);
        this.buildCost();

        // Satellite Design
        this.satellite_design_dir = Paths.get(this.design_dir.toString(), "satellite_design");
        this.createDirectory(this.satellite_design_dir);
        this.buildSats();

        // Stakeholders
        this.stakeholder_dir = Paths.get(this.design_dir.toString(), "stakeholders");
        this.createDirectory(this.stakeholder_dir);
        this.buildStakeholders();

        // Measurements
        this.measurements_dir = Paths.get(this.design_dir.toString(), "measurements");
        this.createDirectory(this.measurements_dir);
        this.buildMeasurements();



    }

    // -----------------------
    // --- Cost Components ---
    // -----------------------

    public void buildCost(){
        String cost = Double.toString(this.result.getCost());
        cost = FactExtractor.convertDecimalVal(cost);

        ArrayList<Fact> mission_facts = this.result.getCostFacts();
        int count = 1;
        ArrayList<String> mission_costs = new ArrayList<>();
        for(Fact mission_fact: mission_facts){
            StringBuilder mission_cost_builder = new StringBuilder();
//            System.out.println(mission_fact.toStringWithParens());
            try{
                String orbit_str = mission_fact.getSlotValue("orbit-string").stringValue(this.r.getGlobalContext());
                String payloads = mission_fact.getSlotValue("instruments").listValue(this.r.getGlobalContext()).toString().replaceAll("SMAP_ANT", "");
                String file_name = orbit_str + ".txt";
                String mission_text = FactExtractor.extractMissionCostComponents(r, mission_fact, this.design_tag);
                mission_cost_builder.append(mission_text);
                Path cost_file = Paths.get(this.cost_components_dir.toString(), file_name);
                this.writeContext(cost_file, mission_cost_builder.toString());
                count++;
            }
            catch (Exception ex){
                ex.printStackTrace();
            }
            mission_costs.add(mission_cost_builder.toString());
        }

    }


    // ------------------------
    // --- Satellite Design ---
    // ------------------------

    public void buildSats(){
        ArrayList<Fact> mission_facts = this.result.getCostFacts();
        try{
            for(Fact mission_fact: mission_facts){
                String fact_str = FactExtractor.extractMissionDesignComponents(r, mission_fact, this.design_tag);
                String orbit_str = mission_fact.getSlotValue("orbit-string").stringValue(this.r.getGlobalContext());
                // String payloads = mission_fact.getSlotValue("instruments").listValue(this.r.getGlobalContext()).toString().replaceAll("SMAP_ANT", "");
                String file_name = orbit_str + ".txt";
                Path sat_file = Paths.get(this.satellite_design_dir.toString(), file_name);
                this.writeContext(sat_file, fact_str);
            }
        }
        catch (Exception ex){
            ex.printStackTrace();
        }
    }

    // ------------------------
    // --- Objective Values ---
    // ------------------------

    public String buildObjectives(){
        StringBuilder objectives = new StringBuilder();
        DecimalFormat df = new DecimalFormat("0.###");
        objectives.append("Design "+this.design_tag+" Objective Scores:");
        objectives.append("\n\tScience: ").append(df.format(result.getScience()));
        objectives.append("\n\tCost: ").append(df.format(result.getCost()));
        return objectives.toString();
    }

    // --------------------------------
    // --- Stakeholder Satisfaction ---
    // --------------------------------

    public void buildStakeholders(){

        ArrayList<Fact> stakeholder_facts = new ArrayList<>();
        stakeholder_facts.addAll(this.result.stakeholderFacts);
        stakeholder_facts.addAll(this.result.objectiveFacts);
        stakeholder_facts.addAll(this.result.subobjectiveFacts);
        HashMap<String, ArrayList<Fact>> stakeholder_map = FactExtractor.groupStakeholderFacts(this.r, stakeholder_facts);
        ArrayList<Fact> capabilities = this.result.getCapabilities();
        try{
            for(String stakeholder: stakeholder_map.keySet()){
                String file_name = stakeholder + ".txt";
                Path stakeholder_file = Paths.get(this.stakeholder_dir.toString(), file_name);
//                String stakeholder_str = FactExtractor.extractStakeholder(this.r, stakeholder_map.get(stakeholder), capabilities, stakeholder);
                String stakeholder_str = StakeholderExtractor.extract(this.r, stakeholder_map.get(stakeholder), capabilities, stakeholder, this.design_tag);
                this.writeContext(stakeholder_file, stakeholder_str);
            }
        }
        catch (Exception ex){
            ex.printStackTrace();
        }

    }


    // --------------------
    // --- Measurements ---
    // --------------------

    public void buildMeasurements(){

        ArrayList<Fact> capabilities = this.result.getCapabilities();
        try{
            int idx = 0;
            for(Fact capability: capabilities){
                String fact_str = FactExtractor.extractMeasurementComponents(r, capability, this.design_tag);

                String file_name = "M" + capability.getFactId() + ".txt";
                Path sat_file = Paths.get(this.measurements_dir.toString(), file_name);
                this.writeContext(sat_file, fact_str);
                idx++;
            }
        }
        catch (Exception ex){
            ex.printStackTrace();
        }
    }







    // ------------------------
    // --- Helper Functions ---
    // ------------------------


    public void writeContext(Path write_file, String context){
        this.context += "\n\n" + context;
        try{
            FileWriter writer = new FileWriter(write_file.toFile(), false); // 'false' flag overwrites existing contents
            writer.write(context);
            writer.close();
        }
        catch (Exception ex){
            ex.printStackTrace();
        }
    }

    public void writeContext(){
        try{
            FileWriter writer = new FileWriter(this.design_file.toFile(), false); // 'false' flag overwrites existing contents
            writer.write(this.context);
            writer.close();
        }
        catch (Exception ex){
            ex.printStackTrace();
        }
    }

    public void createDirectory(Path path){
        try{
            Files.createDirectories(path);
        }
        catch (Exception ex){
            ex.printStackTrace();
        }
    }

}
