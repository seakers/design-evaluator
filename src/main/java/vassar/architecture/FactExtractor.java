package vassar.architecture;

import jess.Fact;
import jess.Rete;
import jess.ValueVector;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FactExtractor {

    public static HashMap<String, ArrayList<Fact>> groupStakeholderFacts(Rete r, ArrayList<Fact> facts){
        HashMap<String, ArrayList<Fact>> fact_map = new HashMap<>();
        try{
            for(Fact fact: facts){
                String key = fact.getSlotValue("id").stringValue(r.getGlobalContext()).substring(0, 3);
                if(!fact_map.containsKey(key)){
                    ArrayList<Fact> fact_list = new ArrayList<>();
                    fact_list.add(fact);
                    fact_map.put(key, fact_list);
                }
                else{
                    fact_map.get(key).add(fact);
                }
            }
        }
        catch (Exception ex){
            ex.printStackTrace();
        }
        return fact_map;
    }







    static class ECOComparator implements Comparator<String> {
        Pattern pattern = Pattern.compile("(ECO)(\\d+)?(-\\d+)?");

        @Override
        public int compare(String o1, String o2) {
            Matcher m1 = pattern.matcher(o1);
            Matcher m2 = pattern.matcher(o2);

            if (m1.matches() && m2.matches()) {
                int primary1 = m1.group(2) != null ? Integer.parseInt(m1.group(2)) : 0;
                int primary2 = m2.group(2) != null ? Integer.parseInt(m2.group(2)) : 0;

                if (primary1 != primary2) {
                    return primary1 - primary2;
                } else {
                    int secondary1 = m1.group(3) != null ? Integer.parseInt(m1.group(3).substring(1)) : 0;
                    int secondary2 = m2.group(3) != null ? Integer.parseInt(m2.group(3).substring(1)) : 0;

                    return secondary1 - secondary2;
                }
            }
            return o1.compareTo(o2);
        }
    }


    public static String replaceFirstThreeLetters(String original, String replacement) {
        if (original.length() < 3) {
            return replacement;
        } else {
            return replacement + original.substring(3);
        }
    }

    public static String extractStakeholder(Rete r, ArrayList<Fact> facts, ArrayList<Fact> capabilities, String stakeholder_name){
        ArrayList<String> insts = new ArrayList<>();
        insts.add("BIOMASS");
        insts.add("CMIS");
        insts.add("VIIRS");
        insts.add("SMAP_MWR");
        insts.add("SMAP_RAD");

        DecimalFormat df = new DecimalFormat("#.##");
        StringBuilder stakeholder_str = new StringBuilder();
        StringBuilder header_str = new StringBuilder();

        try{

            // --> Sort stakeholders, remove duplicate ids
            ArrayList<String> ordered_facts = new ArrayList<>();
            for(Fact fact: facts){
                ordered_facts.add(fact.getSlotValue("id").stringValue(r.getGlobalContext()));
            }
            ordered_facts.sort(new ECOComparator());
            Set<String> setWithoutDuplicates = new LinkedHashSet<>(ordered_facts);
            ordered_facts = new ArrayList<>(setWithoutDuplicates);
            boolean first_subobjective = true;
            String curr_subobj = "";
            float curr_subobj_sat = 0;
            String curr_subobj_str = "";

            // --> Add stakeholder info
            for(String header: ordered_facts){
                for(Fact fact: facts){
                    String fact_id = fact.getSlotValue("id").stringValue(r.getGlobalContext());
                    if(fact_id.equals(header)){

                        String satisfaction = fact.getSlotValue("satisfaction").stringValue(r.getGlobalContext());
                        if(!satisfaction.equals("1")){
                            int decimal_idx = satisfaction.indexOf(".");
                            if(satisfaction.length() > 3){
                                satisfaction = satisfaction.substring(0, decimal_idx+3);
                            }
                        }

                        // If the fact is a subobjective, extract the satisfied-by and reasons
                        if(fact.getName().equals("AGGREGATION::SUBOBJECTIVE")){


                            // Handel Subobjective Header
                            String obj = FactExtractor.replaceFirstThreeLetters(header, "");
                            if(!curr_subobj.equals(obj)){
                                first_subobjective = true;
                                curr_subobj = obj;
                                curr_subobj_sat = Float.parseFloat(satisfaction);
                            }
                            String new_header = FactExtractor.replaceFirstThreeLetters(header, "Subobjective ");
                            String first_subobj_string = "\t\t" + new_header + ": " + satisfaction + "\n";
                            if(first_subobjective){
                                curr_subobj_str = first_subobj_string;
                                stakeholder_str.append(first_subobj_string);
                                first_subobjective = false;
                            }
//                            if(Float.parseFloat(satisfaction) > curr_subobj_sat){
//                                curr_subobj_sat = Float.parseFloat(satisfaction);
//                                int startIdx = stakeholder_str.indexOf(curr_subobj_str);
//                                if(startIdx != -1) {
//                                    stakeholder_str.replace(startIdx, startIdx + curr_subobj_str.length(), first_subobj_string);
//                                }
//                                curr_subobj_str = first_subobj_string;
//                            }





                            // The measurement that satisfies this subobjective fact
                            String meas_id = fact.getSlotValue("meas-id").stringValue(r.getGlobalContext());
                            String meas_id_str = "M" + meas_id.strip();
                            if(meas_id.equals("nil")){
                                continue;
                            }
                            Fact meas_fact = ContextualizedDesign.findMeasFact(capabilities, Integer.parseInt(meas_id));


                            System.out.println("FACT ID: " + meas_id);
                            System.out.println("Measurement fact: " + meas_fact.toStringWithParens() + "\n\n");


//                            System.out.println("Subobjective: "+ fact.toStringWithParens() + "\n\n");
//                            String meas_id = fact.getSlotValue("requirement-id").stringValue(r.getGlobalContext());
//                            Fact measurement_fact = ContextualizedDesign.findMeasFact(capabilities, Integer.parseInt(meas_id));
//                            String measurement_id = measurement_fact.getSlotValue("Id").stringValue(r.getGlobalContext());

                            String satisfied_by = fact.getSlotValue("satisfied-by").stringValue(r.getGlobalContext());
                            if(satisfied_by.equals("nil")){
                                continue;
                            }
                            stakeholder_str.append("\t\t\t").append("Measurement ").append(meas_id_str).append(": ").append(satisfaction);


                            String reason = FactExtractor.extractReasonsNoCitations(fact.getSlotValue("reasons").listValue(r.getGlobalContext()), r);
                            if(!reason.equals(" []")){
                                stakeholder_str.append(reason);
                            }

//                            if(!reason.equals("")){
////                                stakeholder_str.append(satisfaction_str.toString()).append(reason);
//                                stakeholder_str.append(" partially satisfied by "+meas_id_str);
//                            }
//                            else{
//                                stakeholder_str.append(" fully satisfied by "+meas_id_str);
//                            }
                            stakeholder_str.append("\n");

                        }


                        else if(fact.getName().equals("AGGREGATION::OBJECTIVE")){
                            String new_header = FactExtractor.replaceFirstThreeLetters(header, "Objective ");
                            stakeholder_str.append("\t").append(new_header).append(": ").append(satisfaction);
                            stakeholder_str.append("\n");
                            first_subobjective = true;
                        }
                        else if(fact.getName().equals("AGGREGATION::STAKEHOLDER")){
                            first_subobjective = true;
                            if(stakeholder_name.equals("CLI")){
                                header_str.append("Climate Stakeholder (CLI) satisfaction info. ");
                                header_str.append("Satisfaction levels are provided for stakeholder objectives, subobjectives, and measurements. A value of 1 indicates full satisfaction\n");
                                header_str.append("Climate Satisfaction: ").append(satisfaction).append("\n");
                            }
                            else if(stakeholder_name.equals("WEA")){
                                header_str.append("Weather Stakeholder (WEA) satisfaction info. ");
                                header_str.append("Satisfaction levels are provided for stakeholder objectives, subobjectives, and measurements. A value of 1 indicates full satisfaction\n");
                                header_str.append("Weather Satisfaction: ").append(satisfaction).append("\n");
                            }
                            else if(stakeholder_name.equals("WAT")){
                                header_str.append("Water Stakeholder (WAT) satisfaction info. ");
                                header_str.append("Satisfaction levels are provided for stakeholder objectives, subobjectives, and measurements. A value of 1 indicates full satisfaction\n");
                                header_str.append("Water Satisfaction: ").append(satisfaction).append("\n");
                            }
                            else if(stakeholder_name.equals("HEA")){
                                header_str.append("Applications Stakeholder (WAT) satisfaction info. ");
                                header_str.append("Satisfaction levels are provided for stakeholder objectives, subobjectives, and measurements. A value of 1 indicates full satisfaction\n");
                                header_str.append("Applications Satisfaction: ").append(satisfaction).append("\n");
                            }
                            else if(stakeholder_name.equals("ECO")){
                                header_str.append("Ecosystems Stakeholder (ECO) satisfaction info. ");
                                header_str.append("Satisfaction levels are provided for stakeholder objectives, subobjectives, and measurements. A value of 1 indicates full satisfaction\n");
                                header_str.append("Ecosystems Satisfaction: ").append(satisfaction).append("\n");
                            }
                        }
                        // break;
                    }
                }
                // stakeholder.append("\n");
            }


        }
        catch (Exception ex){
            ex.printStackTrace();
        }


        HashMap<String, String> replacement_map = new HashMap<>();
        replacement_map.put("AM/PM SSO \\(DD required\\) - degraded quality", "does not meet SSO dawn dusk orbit requirement");
        replacement_map.put("SSO orbit does not provide adequate coverage of polar regions \\(polar orbit required\\)", "SSO orbit does not meet polar region coverage requirements");
        replacement_map.put("has insufficient accuracy \\(High accuracy req'd, obtained through multispectral combination of sensors\\)", "insufficient accuracy");
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



        String final_str = header_str.toString() + stakeholder_str.toString();



        final_str = final_str.replaceAll("Insufficient", "insufficient");
        for(String key: replacement_map.keySet()){
            final_str = final_str.replaceAll(key, replacement_map.get(key));
        }
        final_str = FactExtractor.removeDuplicateLines(final_str);
        return final_str;
    }


    public static LinkedHashMap<String, String> reason_citations = new LinkedHashMap<>();

    public static String extractReasons(ValueVector vec, Rete r) {
        String reason = "";
        try{
            for(int x = 0; x < vec.size(); x++){
                String reas = vec.get(x).stringValue(r.getGlobalContext());
                if(!reas.equals("N-A")){
                    if(!FactExtractor.reason_citations.containsKey(reas)){
                        String citation = "["+FactExtractor.reason_citations.keySet().size()+"]";
                        FactExtractor.reason_citations.put(reas, citation);
                    }
                    String reas_citation = FactExtractor.reason_citations.get(reas);
                    reason += reas_citation;
                    reason += ", ";
                }
            }
            if(reason.length() > 2){
                reason = reason.substring(0, reason.length()-2);
            }
        }
        catch (Exception ex){
            ex.printStackTrace();
        }
        return reason;
    }

    public static String extractReasonsNoCitations(ValueVector vec, Rete r){
        String reason = "";
        try{
            for(int x = 0; x < vec.size(); x++){
                String reas = vec.get(x).stringValue(r.getGlobalContext());
                if(!reas.equals("N-A")){
                    reason += reas;
                    reason += ", ";
                }
            }
            if(reason.length() > 2){
                reason = reason.substring(0, reason.length()-2);
            }
        }
        catch (Exception ex){
            ex.printStackTrace();
        }
        reason = " ["+reason+"]";
        return reason;
    }

    public static String removeDuplicateLines(String input) {
        String[] lines = input.split("\n");
        Set<String> uniqueLines = new LinkedHashSet<>();
        for (String line : lines) {
            uniqueLines.add(line);
        }
        StringBuilder sb = new StringBuilder();
        for (String line : uniqueLines) {
            sb.append(line);
            sb.append("\n");
        }
        return sb.toString();
    }








    public static String extractObjective(Rete r, Fact fact){
        StringBuilder stakeholder = new StringBuilder();
        try{
            stakeholder.append(fact.getSlotValue("id").stringValue(r.getGlobalContext()));
            stakeholder.append(": ").append(fact.getSlotValue("satisfaction").longValue(r.getGlobalContext())).append("\n");
        }
        catch (Exception ex){
            ex.printStackTrace();
        }
        return stakeholder.toString();
    }



    public static String processSlotValue(String str) {
        String[] items = str.split(" ");

        for (int i = 0; i < items.length; i++) {
            String item = items[i];

            // Remove surrounding quotes if present
            if (item.startsWith("\"") && item.endsWith("\"")) {
                items[i] = item.substring(1, item.length() - 1);
                continue;
            }

            // Check if item is a float, if yes then round it
            try {
                float value = Float.parseFloat(item);
                BigDecimal bd = BigDecimal.valueOf(value);
                bd = bd.setScale(2, RoundingMode.HALF_UP); // round to 2 decimal places
                items[i] = bd.toString();
            } catch (NumberFormatException ignored) {}
        }

        String final_str = "";
        for (String item : items) {
            final_str += item + " ";
        }

        return final_str;
    }



    public static HashMap<String, String> getFactSlots(Rete r, Fact f){
        HashMap<String, String> slots = new HashMap<>();
        try {
            // get slot names
            for(String slot: f.getDeftemplate().getSlotNames()){
                String slot_str = f.getSlotValue(slot).toString().trim();
                String slot_key = slot.trim();
                // Replace all - characters with spaces

                String slot_val = slot_str.trim();
                slot_val = FactExtractor.processSlotValue(slot_val).trim();
                if(slot_val.equals("nil") || slot_val.equals("")){
                    continue;
                }
                slots.put(slot_key, slot_val);
            }

        }
        catch (Exception ex){
            ex.printStackTrace();
        }
        return slots;
    }


    public static String extractMissionDesignComponents(Rete r, Fact fact, String design_tag){
        StringBuilder fact_str = new StringBuilder();

        // Get Slots
        HashMap<String, String> slots = FactExtractor.getFactSlots(r, fact);

        // Get Components to discard
        ArrayList<String> components = new ArrayList<>();
        // Add the following components: Isp injection, satellite BOL power, depth of discharge, moments of inertia, delta V ADCS, delta V, ADCS mass, payload mass, satellite volume, payload dimensions,
        components.add("lifecycle-cost#");
        components.add("launch-cost#");
        components.add("mission-cost#");
        components.add("mission-non-recurring-cost#");
        components.add("mission-recurring-cost#");
        components.add("bus-cost#");
        components.add("bus-non-recurring-cost#");
        components.add("bus-recurring-cost#");
        components.add("IAT-cost#");
        components.add("IAT-non-recurring-cost#");
        components.add("IAT-recurring-cost#");
        components.add("payload-non-recurring-cost#");
        components.add("payload-cost#");
        components.add("payload-recurring-cost#");
        components.add("operations-cost#");
        components.add("program-cost#");
        components.add("program-non-recurring-cost#");
        components.add("program-recurring-cost#");
        components.add("satellite-cost#");
        components.add("spacecraft-non-recurring-cost#");
        components.add("spacecraft-recurring-cost#");
        components.add("mission-architecture");
        components.add("updated2");
        components.add("launch-cost");
        components.add("updated");
        components.add("select-orbit");
        components.add("factHistory");


        String sat_name = slots.get("Name");
        fact_str.append("Design "+design_tag+" Satellite ").append(sat_name).append(" Specifications:\n");


        // Build String
        for(String key: slots.keySet()){
            if(components.contains(key)){
                continue;
            }
            String clean_key = key.replace("-", " ");
            clean_key = clean_key.replace("#", "");
            fact_str.append("\t").append(clean_key).append(": ").append(slots.get(key)).append("\n");
        }

        // Remove the last newline character
        if(fact_str.length() > 0){
            fact_str.deleteCharAt(fact_str.length()-1);
        }


        return fact_str.toString();
    }


    public static String extractMeasurementComponents(Rete r, Fact fact, String design_tag){

        StringBuilder fact_str = new StringBuilder();
        String fact_id_str = "M" + fact.getFactId();
        fact_str.append("Detailed info on Measurement ").append(fact_id_str).append(" for design "+design_tag+":\n");

        // Get Slots
        HashMap<String, String> slots = FactExtractor.getFactSlots(r, fact);

        // Component Discard
        ArrayList<String> components = new ArrayList<>();
        components.add("factHistory");

        // Build Meas Str
        for(String key: slots.keySet()){
            if(components.contains(key)){
                continue;
            }
            String clean_key = key.replace("-", " ");
            clean_key = clean_key.replace("#", "");
            fact_str.append("\t").append(clean_key).append(": ").append(slots.get(key)).append("\n");
        }

        return fact_str.toString();
    }




    public static String extractMissionCostComponents(Rete r, Fact fact, String design_tag){
        StringBuilder fact_str = new StringBuilder();

        // Cost Components
        ArrayList<String> components_mil = new ArrayList<>();
        components_mil.add("lifecycle-cost#");
        components_mil.add("launch-cost#");
//        components_mil.add("mission-cost#");
        components_mil.add("mission-non-recurring-cost#");
        components_mil.add("mission-recurring-cost#");

        ArrayList<String> components = new ArrayList<>();
        components.add("bus-cost#");
        components.add("bus-non-recurring-cost#");
        components.add("bus-recurring-cost#");
        components.add("IAT-cost#");
        components.add("IAT-non-recurring-cost#");
        components.add("IAT-recurring-cost#");
        components.add("payload-non-recurring-cost#");
        components.add("payload-cost#");
        components.add("payload-recurring-cost#");
        components.add("operations-cost#");
        components.add("program-cost#");
        components.add("program-non-recurring-cost#");
        components.add("program-recurring-cost#");
        components.add("satellite-cost#");
        components.add("spacecraft-non-recurring-cost#");
        components.add("spacecraft-recurring-cost#");

        String final_str = "";
        try{
            String orbit_str = fact.getSlotValue("orbit-string").stringValue(r.getGlobalContext());
            String mission_cost = fact.getSlotValue("mission-cost#").stringValue(r.getGlobalContext());
            mission_cost = convertDecimalVal(mission_cost);
            fact_str.append("\n\ttotal cost: ").append(mission_cost);

            for(String comp: components_mil){
                String value = fact.getSlotValue(comp).stringValue(r.getGlobalContext());
                value = convertDecimalVal(value);
                fact_str.append("\n\t").append(comp).append(": ").append(value);
            }
            for(String comp: components){
                String value = fact.getSlotValue(comp).stringValue(r.getGlobalContext());
                value = moveDecimalLeft(value, 3);
                value = convertDecimalVal(value);
                fact_str.append("\n\t").append(comp).append(": ").append(value);
            }

            final_str = fact_str.toString().replaceAll("#", "").replaceAll("-", " ");
            String header = "Design "+design_tag+" Satellite " + orbit_str + " Cost Components: \n\ttotal cost: " + mission_cost;
            final_str = header + final_str;
        }
        catch (Exception ex){
            ex.printStackTrace();
        }


        return final_str;
    }


    public static String convertDecimalVal(String val){
        int decimal_idx = val.indexOf(".");
        if(val.length() > (decimal_idx+3)){
            val = val.substring(0, decimal_idx+3);
        }
        return val + "M";
    }

    // This function takes a decimal string and moves the decimal point to the left by three digits
    public static String moveDecimalLeft(String decimal, int positions) {
        if (decimal == null || positions <= 0) {
            return decimal;
        }

        int decimalIndex = decimal.indexOf(".");
        if (decimalIndex < 0) {
            return decimal;
        }

        int newIndex = decimalIndex - positions;
        if (newIndex <= 0) {
            StringBuilder result = new StringBuilder();
            result.append("0.");
            for (int i = newIndex; i < 0; i++) {
                result.append("0");
            }
            result.append(decimal.substring(0, decimalIndex));
            result.append(decimal.substring(decimalIndex + 1));
            return result.toString();
        } else {
            String integerPart = decimal.substring(0, newIndex);
            String fractionalPart = decimal.substring(newIndex, decimalIndex) + decimal.substring(decimalIndex + 1);
            return integerPart + "." + fractionalPart;
        }
    }






}
