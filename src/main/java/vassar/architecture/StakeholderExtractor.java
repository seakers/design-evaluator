package vassar.architecture;

import jess.Fact;
import jess.Rete;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Set;

public class StakeholderExtractor {


    public static ArrayList<String> getInstruments(){
        ArrayList<String> insts = new ArrayList<>();
        insts.add("BIOMASS");
        insts.add("CMIS");
        insts.add("VIIRS");
        insts.add("SMAP_MWR");
        insts.add("SMAP_RAD");
        return insts;
    }


    public static String extract(Rete r, ArrayList<Fact> facts, ArrayList<Fact> capabilities, String stakeholder_name, String design_tag) throws Exception{
        StringBuilder stakeholder_builder = new StringBuilder();
        String stakeholder_header = "";

        ArrayList<String> insts = getInstruments();
        ArrayList<String> ordered_facts = getOrderedFacts(r, facts);



        // We need to keep track of the current subobjective we are building
        // If null, there is not a current subobjective
        String curr_subobj = null;


        // Builders
        StringBuilder subobj_body = new StringBuilder();


        int idx = 0;
        for(String header: ordered_facts){
            String subobj_block = "";
            float subobj_max_sat = 0;
            for(Fact fact: facts){
                String fact_id = fact.getSlotValue("id").stringValue(r.getGlobalContext());
                if(fact_id.equals(header)){


                    // 1. Get Satisfaction
                    String satisfaction = getFactSatisfaction(r, fact);


                    // Subobjective Fact
                    if(fact.getName().equals("AGGREGATION::SUBOBJECTIVE")){

                        // 1. Get Measurement that satisfies subobjective
                        String meas_id = fact.getSlotValue("meas-id").stringValue(r.getGlobalContext());
                        if(meas_id.equals("nil")){
                            continue;
                        }

                        // 2. Update max satisfaction
                        if(Float.parseFloat(satisfaction) > subobj_max_sat){
                            subobj_max_sat = Float.parseFloat(satisfaction);
                        }

                        // 3. Add Measurement String
                        String subobj_str = "";
                        String meas_id_str = "M" + meas_id.strip();
                        subobj_str += "\t\t\t" + "Measurement " + meas_id_str + ": " + satisfaction;
                        String reason = FactExtractor.extractReasonsNoCitations(fact.getSlotValue("reasons").listValue(r.getGlobalContext()), r);
                        if(!reason.equals(" []")){
//                    subobj_body.append(reason);
                            subobj_str += reason;
                        }
                        subobj_str += "\n";
                        subobj_block += subobj_str;
                    }

                    // Objective Fact
                    else if(fact.getName().equals("AGGREGATION::OBJECTIVE")){
                        String new_header = FactExtractor.replaceFirstThreeLetters(header, "Objective ");
                        stakeholder_builder.append("\t").append(new_header).append(": ").append(satisfaction);
                        stakeholder_builder.append("\n");
                    }

                    // Stakeholder Fact
                    else if(fact.getName().equals("AGGREGATION::STAKEHOLDER")){
                        stakeholder_header = getStakeholderHeader(stakeholder_name, satisfaction, design_tag);
                    }

                }
            }

            if(subobj_block.length() > 0){
                String subobj_id = getEntryID(header);
                String subobj_header = "\t\tSubobjective "+subobj_id+": " + subobj_max_sat;
                stakeholder_builder.append(subobj_header).append("\n").append(subobj_block);
            }
            else{
                String entry_id = getEntryID(header);
                if(entry_id.length() == 3){
                    String subobj_header = "\t\tSubobjective " + entry_id + ": " + subobj_max_sat;
                    stakeholder_builder.append(subobj_header).append("\n").append(subobj_block);
                }
            }
        }




        // Create final string and do final replacements
        String final_str = stakeholder_header + stakeholder_builder.toString();
        final_str = replaceFinalString(final_str);
        return final_str;
    }





























    public static boolean isSubobjective(Fact fact){
        if(fact.getName().equals("AGGREGATION::SUBOBJECTIVE")){
            return true;
        }
        return false;
    }

    public static boolean isObjective(Fact fact){
        if(fact.getName().equals("AGGREGATION::OBJECTIVE")){
            return true;
        }
        return false;
    }

    public static boolean isStakeholder(Fact fact){
        if(fact.getName().equals("AGGREGATION::STAKEHOLDER")){
            return true;
        }
        return false;
    }



    public static String replaceFinalString(String final_str){
        HashMap<String, String> replacement_map = new HashMap<>();
        replacement_map.put("AM/PM SSO \\(DD required\\) - degraded quality", "does not meet SSO dawn dusk orbit requirement");
        replacement_map.put("SSO orbit does not provide adequate coverage of polar regions \\(polar orbit required\\)", "SSO orbit does not meet polar region coverage requirements");
        replacement_map.put("insufficient accuracy \\(High accuracy req'd, obtained through multispectral combination of sensors\\)", "insufficient accuracy");
//        replacement_map.put("insufficient", "has insufficient");
//        replacement_map.put("WEA1-1", "soil moisture");
//        replacement_map.put("WEA2-1", "soil moisture");
//        replacement_map.put("WEA3-1", "soil moisture");
//        replacement_map.put("CLI1-1", "soil moisture");
//        replacement_map.put("CLI1-2", "freeze/thaw state");
//        replacement_map.put("CLI2-1", "ocean salinity");
//        replacement_map.put("CLI2-2", "ocean surface wind speed");
//        replacement_map.put("ECO1-1", "freeze/thaw state");
//        replacement_map.put("ECO2-1", "carbon net ecosystem exchange");
//        replacement_map.put("WAT1-1", "soil moisture");
//        replacement_map.put("WAT1-2", "freeze/thaw state");
//        replacement_map.put("WAT2-1", "rain rate and tropical storms");
//        replacement_map.put("WAT3-1", "snow cover");
//        replacement_map.put("WAT4-1", "sea ice cover");
//        replacement_map.put("HEA1-1", "soil moisture");
//        replacement_map.put("HEA2-1", "soil moisture");
//        replacement_map.put("HEA3-1", "soil moisture");
//        replacement_map.put("HEA4-1", "soil moisture");
//        replacement_map.put("HEA5-1", "soil moisture");
        final_str = final_str.replaceAll("Insufficient", "insufficient");
        for(String key: replacement_map.keySet()){
            final_str = final_str.replaceAll(key, replacement_map.get(key));
        }
        final_str = FactExtractor.removeDuplicateLines(final_str);
        return final_str;
    }

    public static String getStakeholderHeader(String stakeholder_name, String satisfaction, String design_tag){
        StringBuilder header_str = new StringBuilder();
        if(stakeholder_name.equals("CLI")){
            header_str.append("Design " + design_tag + " Climate Stakeholder (CLI) satisfaction info. ");
            header_str.append("Satisfaction levels are provided for stakeholder objectives, subobjectives, and measurements. A value of 1 indicates full satisfaction\n");
            header_str.append("Climate Satisfaction: ").append(satisfaction).append("\n");
        }
        else if(stakeholder_name.equals("WEA")){
            header_str.append("Design " + design_tag + " Weather Stakeholder (WEA) satisfaction info. ");
            header_str.append("Satisfaction levels are provided for stakeholder objectives, subobjectives, and measurements. A value of 1 indicates full satisfaction\n");
            header_str.append("Weather Satisfaction: ").append(satisfaction).append("\n");
        }
        else if(stakeholder_name.equals("WAT")){
            header_str.append("Design " + design_tag + " Water Stakeholder (WAT) satisfaction info. ");
            header_str.append("Satisfaction levels are provided for stakeholder objectives, subobjectives, and measurements. A value of 1 indicates full satisfaction\n");
            header_str.append("Water Satisfaction: ").append(satisfaction).append("\n");
        }
        else if(stakeholder_name.equals("HEA")){
            header_str.append("Design " + design_tag + " Applications Stakeholder (WAT) satisfaction info. ");
            header_str.append("Satisfaction levels are provided for stakeholder objectives, subobjectives, and measurements. A value of 1 indicates full satisfaction\n");
            header_str.append("Applications Satisfaction: ").append(satisfaction).append("\n");
        }
        else if(stakeholder_name.equals("ECO")){
            header_str.append("Design " + design_tag + " Ecosystems Stakeholder (ECO) satisfaction info. ");
            header_str.append("Satisfaction levels are provided for stakeholder objectives, subobjectives, and measurements. A value of 1 indicates full satisfaction\n");
            header_str.append("Ecosystems Satisfaction: ").append(satisfaction).append("\n");
        }
        return header_str.toString();
    }

    public static String getFactSatisfaction(Rete r, Fact fact) throws Exception{
        String satisfaction = fact.getSlotValue("satisfaction").stringValue(r.getGlobalContext());
        if(!satisfaction.equals("1")){
            int decimal_idx = satisfaction.indexOf(".");
            if(satisfaction.length() > 3){
                satisfaction = satisfaction.substring(0, decimal_idx+3);
            }
        }
        return satisfaction;
    }

    public static Fact findAggregationFact(String header, ArrayList<Fact> facts, Rete r) throws Exception{
        for(Fact fact: facts){
            String fact_id = fact.getSlotValue("id").stringValue(r.getGlobalContext());
            if(fact_id.equals(header)){
                return fact;
            }
        }
        return null;
    }

    public static ArrayList<String> getOrderedFacts(Rete r, ArrayList<Fact> facts) throws Exception{
        ArrayList<String> ordered_facts = new ArrayList<>();
        for(Fact fact: facts){
            ordered_facts.add(fact.getSlotValue("id").stringValue(r.getGlobalContext()));
        }
        ordered_facts.sort(new FactExtractor.ECOComparator());
        Set<String> setWithoutDuplicates = new LinkedHashSet<>(ordered_facts);
        ordered_facts = new ArrayList<>(setWithoutDuplicates);
        return ordered_facts;
    }


    public static String getEntryID(String header){
        return FactExtractor.replaceFirstThreeLetters(header, "").trim();
    }



}
