package vassar.architecture;

import evaluator.ResourcePaths;
import jess.Fact;
import jess.Rete;
import vassar.jess.Resource;
import vassar.result.Result;

import java.io.FileWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ContextualizedDesign {

    public Resource resource;
    public Rete r;
    public Result result;
    public String id;
    public String context;
    public Path design_file;
    public Path design_dir;

    public ArrayList<String> insts = new ArrayList<>();


    public ContextualizedDesign(Result result, Resource resource){

        // --> Initialize instruments
        insts.add("BIOMASS");
        insts.add("CMIS");
        insts.add("VIIRS");
        insts.add("SMAP_MWR");
        insts.add("SMAP_RAD");

        // --> Initialize design file
        this.design_file = Paths.get(ResourcePaths.rootDirectory, "debug", "design.txt");
        this.design_dir = Paths.get(ResourcePaths.rootDirectory, "debug", "design");
        this.resource = resource;
        this.r = this.resource.getEngine();
        System.out.println("\n\n----- DESIGN CONTEXT -----");
        this.result = result;
        this.id = "1";


        // 1. Cost Components
        Path cost_file = Paths.get(this.design_dir.toString(), "cost.txt");
        this.writeContext(cost_file, this.buildCost());






        // 1. Measurements
        Path all_measurements_file = Paths.get(this.design_dir.toString(), "all_measurements.txt");
        this.writeContext(all_measurements_file, this.allMeasurements());
        Path applied_measurements_file = Paths.get(this.design_dir.toString(), "applied_measurements.txt");
        this.writeContext(applied_measurements_file, this.appliedMeasurements());

        // 2. Stakeholders
        Path stakeholder_file = Paths.get(this.design_dir.toString(), "stakeholders.txt");
        this.writeContext(stakeholder_file, this.buildStakeholders());

        // 3. Objectives
        Path objectives_file = Paths.get(this.design_dir.toString(), "objectives.txt");
        this.writeContext(objectives_file, this.buildObjectives());




        // --> Stakeholder values
//        context_builder.append(this.buildStakeholders());

        // --> Measurements
//        context_builder.append(this.buildMeasurements());



        // --> Mission Facts (instrument to orbit assignment)
//        context_builder.append(this.buildMissionFacts());

        // --> Measurements that satisfy stakeholder subobjectives


        // --> Critiques
//        context_builder.append(this.buildCritiques());






        this.writeContext();
        System.out.println(this.context);
    }
    
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






    // --------------------
    // --- Measurements ---
    // --------------------

    public String allMeasurements(){
        StringBuilder all_measurements = new StringBuilder();
        ArrayList<Fact> measurement_facts =  this.result.getCapabilities();
        for(Fact fact: measurement_facts){
            all_measurements.append("\n").append(fact.toStringWithParens());
        }
        return all_measurements.toString();
    }

    public String appliedMeasurements(){
        // This functions formats measurements that satisfy stakeholder subobjectives
        //    It details subobjective satisfaction levels with accompanying reasons
        // Sequence Diagram
        // 1. Iterate over subobjective satisfaction facts
        // - retrieve measurements that satisfy a subobjective
        // 2. Iterate over measurements





        // 1. Iterate over subobjective satisfaction facts
        // - retrieve measurements that satisfy a subobjective


        // --> 1. Sort subobjective satisfaction facts into a map, where mission products (measurements) map to
        LinkedHashMap<String, LinkedHashMap<String, Fact>> mission_products = new LinkedHashMap<>();
        for (Map.Entry<String, ArrayList<Fact>> entry : this.result.getExplanations().entrySet()) {
            String subobjective = entry.getKey();
            ArrayList<Fact> subobjective_facts = entry.getValue();
            for(Fact fact: subobjective_facts){
                try{
                    String sat_str = fact.getSlotValue("satisfaction").stringValue(this.r.getGlobalContext());
                    double satisfaction = Double.parseDouble(sat_str);
                    if(satisfaction >= 0.0){
                        String mission_product = fact.getSlotValue("satisfied-by").stringValue(this.r.getGlobalContext());
                        String subobj = fact.getSlotValue("id").stringValue(this.r.getGlobalContext());
                        String meas_id = fact.getSlotValue("requirement-id").stringValue(this.r.getGlobalContext());


                        // --> mission_products logic
                        if(!mission_products.containsKey(meas_id)){
                            mission_products.put(meas_id, new LinkedHashMap<>());
                        }
                        LinkedHashMap<String, Fact> product_map = mission_products.get(meas_id);
                        if(!product_map.containsKey(subobj)){
                            product_map.put(subobj, fact);
                        }
                        else{
                            Fact max_subobj = product_map.get(subobj);
                            String max_sat_str = max_subobj.getSlotValue("satisfaction").stringValue(this.r.getGlobalContext());
                            double max_sat = Double.parseDouble(max_sat_str);
                            if(satisfaction > max_sat){
                                product_map.put(subobj, fact);
                            }
                        }
                    }
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }


        // --> Get measurements
        ArrayList<Fact> capabilities = this.result.getCapabilities();
        StringBuilder measurements_builder = new StringBuilder();
        measurements_builder.append("\nDesign Measurements:");

        // --> 2. Iterate over measurements
        int count = 1;
        for(String meas_id: mission_products.keySet()){
            System.out.println("Meas ID: " + meas_id);
            if(meas_id.equals("nil")){
                continue;
            }
            String meas_num = "Meas" + count;
            count++;
            Fact measurement_fact = ContextualizedDesign.findMeasFact(capabilities, Integer.parseInt(meas_id));
            String meas_name = "";
            String flies_in = "";
            String instrument = "";
            String taken_by = "";
            String Id = "";
            int synergy_level = 0;
            try{
                meas_name = measurement_fact.getSlotValue("Parameter").stringValue(this.r.getGlobalContext()).replaceAll("\\d+\\.\\d+\\.\\d+", "");
                flies_in = measurement_fact.getSlotValue("flies-in").stringValue(this.r.getGlobalContext());
                instrument = measurement_fact.getSlotValue("Instrument").stringValue(this.r.getGlobalContext());
                synergy_level = measurement_fact.getSlotValue("synergy-level#").intValue(this.r.getGlobalContext());
                taken_by = measurement_fact.getSlotValue("taken-by").stringValue(this.r.getGlobalContext());
                Id = measurement_fact.getSlotValue("Id").stringValue(this.r.getGlobalContext());
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }


            // Extract instruments from taken_by string (if synergy_level > 0)
            // taken_by is of the form: SMAP_RAD-space-averaged or BIOMASS-CMIS-disaggregated
            if(synergy_level > 0){
                String[] instruments = taken_by.split("-");
                // Filter instruments such that only values in insts are included
                ArrayList<String> filtered_instruments = new ArrayList<>();
                for(String inst: instruments){
                    if(insts.contains(inst)){
                        filtered_instruments.add(inst);
                    }
                }
                if(filtered_instruments.size() > 1){
                    instrument = "";
                    for(int i = 0; i < filtered_instruments.size(); i++){
                        if(i == filtered_instruments.size() - 1 && filtered_instruments.size() > 2){
                            instrument += " and " + filtered_instruments.get(i);
                        }
                        else if(i == filtered_instruments.size() - 1 && filtered_instruments.size() == 2){
                            instrument = instrument.substring(0, instrument.length() - 2) + " and " + filtered_instruments.get(i);
                        }
                        else{
                            instrument += filtered_instruments.get(i) + ", ";
                        }
                    }
                }
                else{
                    instrument = filtered_instruments.get(0);
                }
            }

            // Example strings with synergies
            // 1. Two or more instruments
            //      - This synergistic measurement is a product of BIOMASS and CMIS instruments and partially or fully satisfies the following subobjectives:
            // 2. One instrument
            //      - This synergistic measurement taken by the BIOMASS instrument in orbit SSO-600-SSO-DD partially or fully satisfies the following subobjectives:
            // Example strings without synergies
            // - This measurement taken by the BIOMASS instrument in orbit SSO-600-SSO-DD partially or fully satisfies the following subobjectives:
            StringBuilder measurement_builder = new StringBuilder();
            measurement_builder.append("a");

            if(synergy_level > 0) {
                if (instrument.contains(",")) {
                    measurement_builder.append(" synergistic").append(meas_name).append(" measurement is a product of ").append(instrument).append(" instruments");

                } else {
                    measurement_builder.append(" synergistic").append(meas_name).append(" measurement taken by the ").append(instrument).append(" instrument in orbit ").append(flies_in);
                }
            }
            else{
                measurement_builder.append(meas_name).append(" measurement taken by the ").append(instrument).append(" instrument in orbit ").append(flies_in);
            }
            measurement_builder.append(". Subobjective Satisfaction:");



            // --> 3. Iterate over mission product subobjective satisfactions
            LinkedHashMap<String, Fact> subobj_map = mission_products.get(meas_id);
            if(subobj_map.size() == 0){
                continue;
            }
            measurements_builder.append("\n * ").append(taken_by).append(": ").append(measurement_builder.toString());
            for(String subobj: subobj_map.keySet()){
                Fact subobj_fact = subobj_map.get(subobj);
                try{
                    String satisfaction_str = subobj_fact.getSlotValue("satisfaction").stringValue(this.r.getGlobalContext());
                    double satisfaction = Double.parseDouble(satisfaction_str);
                    DecimalFormat df = new DecimalFormat("#.##");





                    String satisfied_by = subobj_fact.getSlotValue("satisfied-by").stringValue(this.r.getGlobalContext());
                    String reason_parsed = FactExtractor.extractReasonsNoCitations(subobj_fact.getSlotValue("reasons").listValue(r.getGlobalContext()), r);
                    measurements_builder.append("\n   ").append(subobj).append(": ").append(df.format(satisfaction));
                    if(satisfaction < 1.0 && reason_parsed.length() > 0){
                        measurements_builder.append(" ").append(reason_parsed);
                    }


                }
                catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }



        // --> Strings to replace
        LinkedHashMap<String, String> replacement_map = new LinkedHashMap<>();
        replacement_map.put("AM/PM SSO \\(DD required\\) - degraded quality", "is not in an SSO dawn dusk orbit resulting in degraded quality");
        replacement_map.put("SSO orbit does not provide adequate coverage of polar regions \\(polar orbit required\\)", "is in an SSO orbit which does not provide adequate coverage of the polar regions");
        replacement_map.put("has insufficient accuracy \\(High accuracy req'd, obtained through multispectral combination of sensors\\)", "has insufficient accuracy \\(obtained through multispectral combination of sensors\\)");
        replacement_map.put("insufficient", "has insufficient");




        // --> Reasons for not fully satisfied requirements
        StringBuilder reasons_builder = new StringBuilder();
        reasons_builder.append("Reference Reasons:");
        for(String reason: FactExtractor.reason_citations.keySet()){
            String citation = FactExtractor.reason_citations.get(reason);
            reasons_builder.append("\n * ").append(citation).append(" ").append(reason);
        }


        StringBuilder final_builder = new StringBuilder();
        final_builder.append(reasons_builder.toString());
        final_builder.append("\n");
        final_builder.append(measurements_builder.toString());


        String final_str = final_builder.toString();
        final_str = final_str.replaceAll("Insufficient", "insufficient");
        for(String key: replacement_map.keySet()){
            final_str = final_str.replaceAll(key, replacement_map.get(key));
        }
        final_str = FactExtractor.removeDuplicateLines(final_str);





        return final_str;
    }










    // -------------
    // --- Other ---
    // -------------

    public String buildCost(){
        StringBuilder cost_builder = new StringBuilder();
        String cost = Double.toString(this.result.getCost());
        cost = FactExtractor.convertDecimalVal(cost);
        cost_builder.append("Design Cost: ").append(cost);

        ArrayList<Fact> mission_facts = this.result.getCostFacts();
        int count = 1;
        for(Fact mission_fact: mission_facts){
            System.out.println(mission_fact.toStringWithParens());
            try{
                String orbit_str = mission_fact.getSlotValue("orbit-string").stringValue(this.r.getGlobalContext());
                String payloads = mission_fact.getSlotValue("instruments").listValue(this.r.getGlobalContext()).toString().replaceAll("SMAP_ANT", "");;
//                cost_builder.append("\n" + orbit_str).append(" has the following cost components: ");
                cost_builder.append(FactExtractor.extractMissionCostComponents(r, mission_fact, "D-1"));
                count++;
            }
            catch (Exception ex){
                ex.printStackTrace();
            }

        }

        return cost_builder.toString();
    }




    public String buildMissionFacts(){
        StringBuilder mission_builder = new StringBuilder();

        ArrayList<Fact> mission_facts = this.result.getCostFacts();
        int count = 1;
        for(Fact mission_fact: mission_facts){
            System.out.println(mission_fact.toStringWithParens());
            try{
                String orbit_str = mission_fact.getSlotValue("orbit-string").stringValue(this.r.getGlobalContext());
                String payloads = mission_fact.getSlotValue("instruments").listValue(this.r.getGlobalContext()).toString().replaceAll("SMAP_ANT", "");;
                mission_builder.append("\nSatellite " + count).append(" has the following cost components: ");
                mission_builder.append(FactExtractor.extractMissionCostComponents(r, mission_fact, "D-1"));
                count++;
            }
            catch (Exception ex){
                ex.printStackTrace();
            }

        }

        return mission_builder.toString();
    }

    public static Fact findMeasFact(ArrayList<Fact> facts, int id){
        for(Fact fact: facts){
            if(fact.getFactId() == id){
                return fact;
            }
        }
        return null;
    }

    public String formatMeasFact(Fact fact){
        StringBuilder fact_str = new StringBuilder();

        String slot_string = this.parseFactString(fact.toStringWithParens());



        return this.parseFactString(fact.toStringWithParens());
    }

    public String parseFactString(String factString) {
        ArrayList<String> blacklist_facts = new ArrayList<>();
        blacklist_facts.add("factHistory");
        blacklist_facts.add("science-multiplier");
        blacklist_facts.add("Temporal-resolution");
        blacklist_facts.add("Swath");
        blacklist_facts.add("spectral-bands");
        blacklist_facts.add("num-of-planes#");
        blacklist_facts.add("num-of-sats-per-plane#");
        blacklist_facts.add("mission-architecture");
        blacklist_facts.add("lifetime");
        blacklist_facts.add("launch-date");
        blacklist_facts.add("Id");
        blacklist_facts.add("Horizontal-Spatial-Resolution-Cross-track");
        blacklist_facts.add("Horizontal-Spatial-Resolution-Along-track");
        blacklist_facts.add("Horizontal-Spatial-Resolution");
        blacklist_facts.add("orbit-string");
        blacklist_facts.add("Accuracy");
//        blacklist_facts.add("factHistory");
//        blacklist_facts.add("factHistory");
//        blacklist_facts.add("factHistory");
//        blacklist_facts.add("factHistory");
//        blacklist_facts.add("factHistory");
//        blacklist_facts.add("factHistory");
//        blacklist_facts.add("factHistory");


        factString = factString.substring(1, factString.length()-1);
        StringBuilder sb = new StringBuilder();
        Pattern slotPattern = Pattern.compile("\\((.*?)\\)");
        Matcher slotMatcher = slotPattern.matcher(factString);
        while (slotMatcher.find()) {
            String slotString = slotMatcher.group(1);
            String[] slotParts = slotString.split("\\s+", 2);
            String slotName = slotParts[0];
            String slotValue = slotParts[1];
            if (!slotValue.equals("nil") && !blacklist_facts.contains(slotName)) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                if (slotValue.matches("-?\\d+(\\.\\d+)?")) {
                    double slotValueDouble = Double.parseDouble(slotValue);
                    slotValue = String.format("%.3f", slotValueDouble);
                }
                sb.append(slotName).append(": ").append(slotValue);
            }
        }
        return sb.toString();
    }






    public String buildObjectives(){
        StringBuilder objectives = new StringBuilder();
        DecimalFormat df = new DecimalFormat("0.###");
        objectives.append("\nScience: ").append(df.format(result.getScience()));
        objectives.append("\nCost: ").append(df.format(result.getCost()));
        return objectives.toString();
    }

    public String buildStakeholders(){
        /* Aggregation Facts
        (deftemplate AGGREGATION::VALUE (slot satisfaction) (slot fuzzy-value) (slot reason) (multislot weights) (multislot sh-scores) (multislot sh-fuzzy-scores)(slot factHistory))
        (deftemplate AGGREGATION::STAKEHOLDER (slot id) (slot fuzzy-value) (slot parent) (slot index) (slot satisfaction) (slot satisfied-by) (multislot obj-fuzzy-scores) (multislot obj-scores) (slot reason) (multislot weights)(slot factHistory))
        (deftemplate AGGREGATION::OBJECTIVE (slot id) (slot fuzzy-value) (slot index) (slot satisfaction) (slot reason) (multislot subobj-fuzzy-scores) (multislot subobj-scores) (slot satisfied-by) (slot parent) (multislot weights)(slot factHistory))
        (deftemplate AGGREGATION::SUBOBJECTIVE (slot id) (slot fuzzy-value) (slot index) (slot satisfaction) (multislot attributes) (multislot attrib-scores) (multislot reasons) (slot reason) (slot satisfied-by) (slot parent) (slot requirement-id) (slot factHistory))
         */

        StringBuilder stakeholders = new StringBuilder();

//        for(Fact fact: this.result.valueFacts){
//            stakeholders.append(fact.toStringWithParens()).append("\n");
//        }


        stakeholders.append("\n\n");

        ArrayList<Fact> stakeholder_facts = new ArrayList<>();
        stakeholder_facts.addAll(this.result.stakeholderFacts);
        stakeholder_facts.addAll(this.result.objectiveFacts);
        stakeholder_facts.addAll(this.result.subobjectiveFacts);
        HashMap<String, ArrayList<Fact>> stakeholder_map = FactExtractor.groupStakeholderFacts(this.r, stakeholder_facts);
        ArrayList<Fact> capabilities = this.result.getCapabilities();
        for(String stakeholder: stakeholder_map.keySet()){
            stakeholders.append(FactExtractor.extractStakeholder(this.r, stakeholder_map.get(stakeholder), capabilities, stakeholder)).append("\n");
        }



        return stakeholders.toString();
    }













    public String buildCritiques(){
        StringBuilder critique_builder = new StringBuilder();
        critique_builder.append("\nCritiques:");


        critique_builder.append("\n * Cost: ");
        Vector<String> cost_critique = this.result.getCostCritique();
        for(String critique: cost_critique){
            critique_builder.append(critique.substring(2)).append(" ");
        }

        critique_builder.append("\n * Science: ");
        Vector<String> performance_critique = this.result.getPerformanceCritique();
        for(String critique: performance_critique){
            critique_builder.append(critique.substring(2, critique.length()-1)).append(" ");
        }
        return critique_builder.toString();
    }


}
