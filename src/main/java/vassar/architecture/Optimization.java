package vassar.architecture;


import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import evaluator.EvaluatorApp;
import evaluator.ResourcePaths;
import org.checkerframework.checker.nullness.Opt;
import org.checkerframework.checker.units.qual.A;
import vassar.combinatorics.Combinatorics;
import vassar.combinatorics.NDSM;
import vassar.combinatorics.Nto1pair;
import vassar.database.DatabaseClient;
import vassar.evaluator.ADDEvaluator;
import vassar.jess.Resource;
import vassar.result.Result;

import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.*;

public class Optimization {


//    A rule on the maximum number of subsets that a partition can have (Code 41 in the Appendix).
//            - A rule on the maximum number of elements that any subset in a partition can have. This avoids
//    the exploration of architectures that would include subsets that are non-sensical for being too
//    large. For example, if the elements are instruments and the subsets are satellites, this avoids
//    looking at architectures that have satellites that cannot be launched by any current launch vehicle
//            (Code 42 in the Appendix).
//            -
//    One or more rules to force that an element is assigned to a subset of size k (typically, k = 1).
//    This dramatically reduces the size of the tradespace. For k = 1 for example, this is equivalent to
//    considering a PaP with m-1 elements (Code 43 in the Appendix)
//-
//    One or more rules to enforce that two or more elements are grouped in the same subset (Code
//44 in the Appendix).
//            -
//    One or more rules to enforce that two or more elements are assigned to different subsets (Code
//45 in the Appendix).


    // Partitioning based on synergy NDSM
    public static HashMap<Integer, ArrayList<String>> partitionInstruments(ArrayList<String> items, Resource engine){
        // int max_size = 3;

        ConcurrentHashMap<Integer, ArrayList<String>> partitions = new ConcurrentHashMap<>();

        // CONSTRAINTS
        if(items.size() == 1){
            partitions.put(0, items);
            return (new HashMap<>(partitions));
        }

        // LATERAL SYNERGIES
        TreeMap<Nto1pair,Double> dimen_2 = Combinatorics.combineNDSM_File(ResourcePaths.ndsm2_Decadal, "SDSM");
        // TreeMap<Nto1pair,Double> dimen_3 = Combinatorics.combineNDSM_File(Files.ndsm3, "SDSM");
        // TreeMap<Nto1pair,Double> dimen_N = Combinatorics.combineNDSM_File(Files.ndsmN, "SDSM");


        // OPTIMIZE
        for(int x = 0; x < 5; x++){
            partitions = Optimization.improveSynergyScore(partitions, dimen_2, engine, items);
        }
        System.out.println(dimen_2);
        System.out.println("---> improveSynergyScore");
        Optimization.printPartitions(partitions);
//        EvaluatorApp.sleep(5);

        // REPAIR LONE ELEMENTS
        partitions = Optimization.repairLoneSynergisticElement(partitions, dimen_2, engine);
        System.out.println("---> repairLoneSynergisticElement");
        Optimization.printPartitions(partitions);
//        EvaluatorApp.sleep(5);

        // PUT REMAINING INSTS IN NEW MISSION
        partitions = Optimization.addRemainingElements(partitions, items);
        System.out.println("---> addRemainingElements");
        Optimization.printPartitions(partitions);
//        EvaluatorApp.sleep(5);

        // REPAIR LONE ELEMENTS
        partitions = Optimization.repairLoneSynergisticElement(partitions, dimen_2, engine);
        System.out.println("---> repairLoneSynergisticElement");
        Optimization.printPartitions(partitions);
//        EvaluatorApp.sleep(5);

        // REMOVE DUPLICATE ELEMENTS
        partitions = Optimization.removeDuplicateElements(partitions, items);
        System.out.println("---> removeDuplicateElements");
        Optimization.printPartitions(partitions);
//        EvaluatorApp.sleep(5);

        return (new HashMap<>(partitions));
    }


    public static ConcurrentHashMap<Integer, ArrayList<String>> removeDuplicateElements(ConcurrentHashMap<Integer, ArrayList<String>> partitions, ArrayList<String> items){

        for(String inst: items){
            boolean remove = false;

            // For each instrument, determine how many times it shows up in the chromosome
            for(Integer idx: partitions.keySet()){
                for(String group_inst: partitions.get(idx)){
                    if(group_inst.equals(inst)){
                        if(!remove){
                            remove = true;
                        }
                        else{
                            partitions.get(idx).remove(inst);
                        }
                    }
                }
            }

        }
        partitions = Optimization.repairPartitions(partitions);
        return partitions;
    }

    public static ConcurrentHashMap<Integer, ArrayList<String>> addRemainingElements(ConcurrentHashMap<Integer, ArrayList<String>> partitions, ArrayList<String> items){

        // PUT REMAINING INSTS IN NEW MISSION
        for(Integer idx: partitions.keySet()){
            for(String inst: partitions.get(idx)){
                items.remove(inst);
            }
        }

        for(String inst: items){
            ArrayList<String> new_mission = new ArrayList<>();
            new_mission.add(inst);
            partitions.put(partitions.size(), new_mission);
        }

        return partitions;
    }

    public static ConcurrentHashMap<Integer, ArrayList<String>> deepClonePartitions(ConcurrentHashMap<Integer, ArrayList<String>> item){
        Gson gson = new Gson();
        String obj_string = gson.toJson(item);
        Type type = new TypeToken<ConcurrentHashMap<Integer, ArrayList<String>>>(){}.getType();
        ConcurrentHashMap<Integer, ArrayList<String>> clone = gson.fromJson(obj_string, type);

        return clone;
    }

    public static ArrayList<String> deepCloneGroup(ArrayList<String> group){
        Gson gson = new Gson();
        String obj_string = gson.toJson(group);
        Type type = new TypeToken<ArrayList<String>>(){}.getType();
        ArrayList<String> clone = gson.fromJson(obj_string, type);

        return clone;
    }




    // CHANGE MAX SIZE
    public static ConcurrentHashMap<Integer, ArrayList<String>> improveSynergyScore(ConcurrentHashMap<Integer, ArrayList<String>> partitions_orig, TreeMap<Nto1pair,Double> dimen_2, Resource engine, ArrayList<String> items){
        ConcurrentHashMap<Integer, ArrayList<String>> partitions = Optimization.deepClonePartitions(partitions_orig);

        // Iterate through synergies and try to improve overall synergy score
        Iterator synergy_iterator = dimen_2.keySet().iterator();
        while(synergy_iterator.hasNext()) {

            Object synergy_key = synergy_iterator.next();
            Nto1pair nt = (Nto1pair) synergy_key;
            String inst_check_synergy = nt.getAdded();
            String has_synergy_with = nt.getBase()[0];


            ConcurrentHashMap<Integer, ArrayList<String>> new_partitions = Optimization.makeSynergisticChange(partitions, has_synergy_with, inst_check_synergy, engine, items);

            double current_score = Optimization.calcualteSynergyScore(partitions, dimen_2);
            double change_score  = Optimization.calcualteSynergyScore(new_partitions, dimen_2);

//            System.out.println("\n--> CURRENT " + current_score);
//            Optimization.printPartitions(partitions);
//            System.out.println("\n--> CHANGE " + change_score);
//            Optimization.printPartitions(new_partitions);


            if(change_score >= current_score){
                partitions = Optimization.deepClonePartitions(new_partitions);
            }

            partitions = Optimization.repairPartitions(partitions);
            // EvaluatorApp.sleep(1);
        }

        return partitions;
    }

    public static void printPartitions(ConcurrentHashMap<Integer, ArrayList<String>> partitions){
        for(Integer key: partitions.keySet()){
            System.out.println("--> " + key + " " + partitions.get(key));
        }
    }

    public static ConcurrentHashMap<Integer, ArrayList<String>> makeSynergisticChange(ConcurrentHashMap<Integer, ArrayList<String>> partitions_orig, String has_synergy_with, String inst_check_synergy, Resource engine, ArrayList<String> items){
        ConcurrentHashMap<Integer, ArrayList<String>> partitions = Optimization.deepClonePartitions(partitions_orig);


        // Make sure synergy is feasible
        if(!items.contains(has_synergy_with) || !items.contains(inst_check_synergy)){
            return partitions;
        }

        // Try to move inst_check_synergy to the group containing has_synergy_with
        int has_synergy_with_group   = Optimization.findGroupContainingElement(partitions, has_synergy_with);
        int inst_check_synergy_group = Optimization.findGroupContainingElement(partitions, inst_check_synergy);



        // If they are already in the same group, return
        if(has_synergy_with_group == inst_check_synergy_group && inst_check_synergy_group != -1){
//            System.out.println("--> END 1");
            return partitions;
        }

        // if 'synergy with' inst isn't in partitions
        if(has_synergy_with_group == -1){
            // Create a new group for the pair
            ArrayList<String> new_group = new ArrayList<>();
            new_group.add(has_synergy_with);
            if(inst_check_synergy_group == -1){
                new_group.add(inst_check_synergy);
                partitions.put(partitions.size(), new_group);
//                System.out.println("--> END 2");
            }
            else{
                partitions.get(inst_check_synergy_group).remove(inst_check_synergy);
                new_group.add(inst_check_synergy);
                partitions.put(partitions.size(), new_group);
                partitions = Optimization.repairPartitions(partitions);
//                System.out.println("--> END 3");
            }

        }
        // if 'synergy with' inst is in partitions
        else{

            // Check not to violate size constraint - if it does, create new group for it and put alone
            ArrayList<String> clone_group = Optimization.deepCloneGroup(partitions.get(has_synergy_with_group));
            clone_group.add(inst_check_synergy);
            boolean group_feasible = Optimization.evaluateGroupFeasibility(clone_group, engine);
            if(!group_feasible){
                // Add instrument to new group if not already in design
                if(inst_check_synergy_group == -1){
                    ArrayList<String> new_group = new ArrayList<>();
                    new_group.add(inst_check_synergy);
                    partitions.put(partitions.size(), new_group);
//                    System.out.println("--> END 4");
                    return partitions;
                }
                return partitions;
            }

            // Move inst_check_synergy to that group
            if(inst_check_synergy_group == -1){
                partitions.get(has_synergy_with_group).add(inst_check_synergy);
//                System.out.println("--> END 5");
            }
            else{
                partitions.get(inst_check_synergy_group).remove(inst_check_synergy);
                partitions.get(has_synergy_with_group).add(inst_check_synergy);
                partitions = Optimization.repairPartitions(partitions);
//                System.out.println("--> END 6");
            }
        }
        return partitions;
    }

    public static ConcurrentHashMap<Integer, ArrayList<String>> repairPartitions(ConcurrentHashMap<Integer, ArrayList<String>> partitions_orig){
        ConcurrentHashMap<Integer, ArrayList<String>> partitions = Optimization.deepClonePartitions(partitions_orig);


        // Eliminate empty groups
        Set<Integer> keys2 = partitions.keySet();

        Gson gson = new Gson();
        String obj_string = gson.toJson(keys2);
        Type type = new TypeToken<Set<Integer>>(){}.getType();
        Set<Integer> keys = gson.fromJson(obj_string, type);



        for(Integer key: keys){
            if(partitions.get(key).size() == 0){
                partitions.remove(key);
            }
        }

        // Re-index
        ConcurrentHashMap<Integer, ArrayList<String>> fixed_indicies = new ConcurrentHashMap<>();
        int counter = 0;
        for(Integer key: partitions.keySet()){
            fixed_indicies.put(counter, partitions.get(key));
            counter++;
        }


        return fixed_indicies;
    }

    public static Integer findGroupContainingElement(ConcurrentHashMap<Integer, ArrayList<String>> partitions, String instrument){

        Iterator group_iterator = partitions.keySet().iterator();
        while(group_iterator.hasNext()) {
            Integer group_key = (Integer) group_iterator.next();
            ArrayList<String> group = partitions.get(group_key);
            if(group.contains(instrument)){
                return group_key;
            }
        }
        return -1;
    }

    public static double calcualteSynergyScore(ConcurrentHashMap<Integer, ArrayList<String>> partitions, TreeMap<Nto1pair,Double> dimen_2){
        double total_score = 0;

        Iterator group_iterator = partitions.keySet().iterator();
        while(group_iterator.hasNext()) {
            Integer group_key = (Integer) group_iterator.next();
            ArrayList<String> group = partitions.get(group_key);
            double group_score = 0;


            Iterator synergy_entry_iterator = dimen_2.entrySet().iterator();
            while(synergy_entry_iterator.hasNext()){
                Map.Entry pair = (Map.Entry) synergy_entry_iterator.next();
                Object synergy_key = pair.getKey();
                Nto1pair nt = (Nto1pair) synergy_key;
                String has_synergy_with = nt.getBase()[0];
                String inst_check_synergy = nt.getAdded();
                if(group.contains(has_synergy_with) && group.contains(inst_check_synergy)){
                    group_score += ((Double) pair.getValue());
                }
            }
            total_score += group_score;
        }

        return total_score;
    }




    // --> SIZE CONSTRAINT
    // CHANGE MAX SIZE
    public static ConcurrentHashMap<Integer, ArrayList<String>> repairLoneSynergisticElement(ConcurrentHashMap<Integer, ArrayList<String>> partitions_orig, TreeMap<Nto1pair,Double> dimen_2, Resource engine){
        ConcurrentHashMap<Integer, ArrayList<String>> partitions = Optimization.deepClonePartitions(partitions_orig);

        Iterator group_iterator = partitions.keySet().iterator();
        while(group_iterator.hasNext()) {
            Integer group_key = (Integer) group_iterator.next();
            ArrayList<String> group = partitions.get(group_key);

            // Find group with single instrument
            if(group.size() == 1){
                String instrument = group.get(0);
                // Find if instrument has any synergies
                if(Optimization.doesElementHaveSynergy(instrument, dimen_2)){
                    // Remove group
                    partitions.remove(group_key);
                    // Add to most synergistic group
                    partitions = Optimization.addInstrumentToSynergisticGroup(partitions, dimen_2, instrument, engine);
                }
            }
        }

        return partitions;
    }

    public static boolean doesElementHaveSynergy(String instrument, TreeMap<Nto1pair,Double> dimen_2){

        Iterator synergy_iterator = dimen_2.keySet().iterator();
        while(synergy_iterator.hasNext()) {
            Object synergy_key = synergy_iterator.next();
            Nto1pair nt = (Nto1pair) synergy_key;
            String has_synergy_with = nt.getBase()[0];
            String inst_check_synergy = nt.getAdded();
            if(has_synergy_with.equals(instrument) || inst_check_synergy.equals(instrument)){
                return true;
            }
        }
        return false;
    }


    // --> SIZE CONSTRAINT: duplicate group to test for scoring
//    public static ConcurrentHashMap<Integer, ArrayList<String>> groupSizeConstraint(ConcurrentHashMap<Integer, ArrayList<String>> partitions_orig, TreeMap<Nto1pair,Double> dimen_2, int max_size){
//        ConcurrentHashMap<Integer, ArrayList<String>> partitions = Optimization.deepClonePartitions(partitions_orig);
//
//        Iterator group_iterator = partitions.keySet().iterator();
//        while(group_iterator.hasNext()){
//            Integer group_key = (Integer) group_iterator.next();
//            ArrayList<String> group = partitions.get(group_key);
//
//            // Remove the instrument from the group that lowers the group synergy level the least
//            if(group.size() >= max_size){
//                // Find the lowest synergy instrument
//                String inst_remove = Optimization.findLowestSynergyInstrument(group, dimen_2);
//                // Add instrument to other group
//                partitions = Optimization.addInstrumentToSynergisticGroup(partitions, dimen_2, inst_remove, max_size);
//                // Remove instrument from this group
//                group.remove(inst_remove);
//            }
//        }
//
//        return partitions;
//    }


    public static ConcurrentHashMap<Integer, ArrayList<String>> addInstrumentToSynergisticGroup(ConcurrentHashMap<Integer, ArrayList<String>> partitions_orig, TreeMap<Nto1pair,Double> dimen_2, String instrument, Resource engine){
        ConcurrentHashMap<Integer, ArrayList<String>> partitions = Optimization.deepClonePartitions(partitions_orig);

        // Find feasible groups to add instrument to
        ArrayList<Integer> feasible_groups = new ArrayList<>();
        Iterator group_iterator = partitions.keySet().iterator();
        while(group_iterator.hasNext()){
            Integer group_key = (Integer) group_iterator.next();

            // For each group, add the instrument and calculate the mass of the satellite
            // -- if the mass < 2000kg, the group is feasible
            ArrayList<String> cloned_group = Optimization.deepCloneGroup(partitions.get(group_key));
            cloned_group.add(instrument);
            if(Optimization.evaluateGroupFeasibility(cloned_group, engine)){
                feasible_groups.add(group_key);
            }
        }







        // If no feasible groups, create new mission for instrument
        if(feasible_groups.isEmpty()){
            ArrayList<String> new_group = new ArrayList<>();
            new_group.add(instrument);
            partitions.put(partitions.size(), new_group);
        }
        // If only one feasible group, add to that group
        else if(feasible_groups.size() == 1){
            partitions.get(feasible_groups.get(0)).add(instrument);
        }
        // If multiple feasible groups, find the group that the inst would add the most synergy to and add it to that
        else{

            // Iterate over groups and find highest synergy score
            double top_synergy_score = 0;
            Integer top_group_key = feasible_groups.get(0);
            for(Integer group_key: feasible_groups){
                ArrayList<String> group = partitions.get(group_key);
                double synergy_score = Optimization.calcGroupSynergyScoreAddingInstrument(group, instrument, dimen_2);
                if(synergy_score >= top_synergy_score){
                    top_synergy_score = synergy_score;
                    top_group_key = group_key;
                }
            }
            // Add instrument to found group
            partitions.get(top_group_key).add(instrument);
        }

        return partitions;
    }



    // Group feasibility is based on a satellite-launch-mass threshold
    public static boolean evaluateGroupFeasibility(ArrayList<String> group, Resource engine){
        double mass_threshold = 2000; // 2000kg
        boolean feasible = false;

        // Do a single sat evaluation that exits once the satellite-launch-mass has been found
        SingleSat arch = new SingleSat(group);

        ADDEvaluator process = new ADDEvaluator.Builder(engine)
                .setArchitecture(arch)
                .evalCost(true)
                .evalPerformance(false)
                .evalScheduling(false)
                .orbitSelection(true)
                .synergyDeclaration(true)
                .extractLaunchMass(true)
                .build();

        ExecutorService executorService = Executors.newFixedThreadPool(1);

        Future<Result> future = executorService.submit(process);

        Result result = null;
        try {
            result = future.get();
        }
        catch (ExecutionException e) {
            System.out.println("Exception when evaluating an architecture");
            e.printStackTrace();
            System.exit(-1);
        }
        catch (InterruptedException e) {
            System.out.println("Execution got interrupted while evaluating an architecture");
            e.printStackTrace();
            System.exit(-1);
        }

        System.out.println("--> SATELLITE LAUNCH MASS: " + group + " " +  result.mission_launch_mass);
        return (Double.parseDouble(result.mission_launch_mass) < mass_threshold);
    }








    public static double calcGroupSynergyScoreAddingInstrument(ArrayList<String> group, String instrument, TreeMap<Nto1pair,Double> dimen_2){
        double score = 0;

        ArrayList<String> new_group = new ArrayList<>(group);
        new_group.add(instrument);

        Iterator synergy_entry_iterator = dimen_2.entrySet().iterator();
        while(synergy_entry_iterator.hasNext()) {
            Map.Entry pair = (Map.Entry) synergy_entry_iterator.next();
            Object synergy_key = pair.getKey();
            Nto1pair nt = (Nto1pair) synergy_key;
            String has_synergy_with = nt.getBase()[0];
            String inst_check_synergy = nt.getAdded();
            if(new_group.contains(has_synergy_with) && new_group.contains(inst_check_synergy)){
                score += ((Double) pair.getValue());
            }
        }

        return score;
    }









    public static String findLowestSynergyInstrument(ArrayList<String> group, TreeMap<Nto1pair,Double> dimen_2){
        if(group.size() == 1){
            return group.get(0);
        }

        // For each instrument, check how much synergy it adds to the group
        ConcurrentHashMap<String, Double> instrument_score_map = new ConcurrentHashMap<>();
        for(String instrument: group){
            double   synergy_score    = 0;
            Iterator synergy_iterator = dimen_2.keySet().iterator();
            while(synergy_iterator.hasNext()){
                Object   synergy_key = synergy_iterator.next();
                Nto1pair nt          = (Nto1pair) synergy_key;
                String has_synergy_with   = nt.getBase()[0];
                String inst_check_synergy = nt.getAdded();

                // If synergy concerns the instrument we are checking && the group contains the other instrument
                if(inst_check_synergy.equals(instrument) && group.contains(has_synergy_with)){
                    // Summate this instrument's synergy score
                    synergy_score += dimen_2.get(synergy_key);
                }
            }
            instrument_score_map.put(instrument, synergy_score);
        }

        double max_score = 0;
        String max_inst  = "";
        for(String instrument: instrument_score_map.keySet()){
            double score = instrument_score_map.get(instrument);
            if(score >= max_score){
                max_score = score;
                max_inst  = instrument;

            }
        }

        return max_inst;
    }


    public static int checkPartitions(ConcurrentHashMap<Integer, ArrayList<String>> partitions, String instrument){

        for(Integer key: partitions.keySet()){
            ArrayList<String> group = partitions.get(key);
            if(group.contains(instrument)){
                return key;
            }
        }
        return -1;
    }



    // Schedule missions based on Data Continuity penalties
    public static ArrayList<Integer> scheduleMissions(HashMap<Integer, ArrayList<String>> missions, Resource res) throws Exception {

        // Get data continuity matrix
        HashMap<Integer, HashMap<String, Double>> matrix = Optimization.computeDataContinuityMatrix(res, missions.size());

        // Map missions to measurements
        HashMap<Integer, ArrayList<String>> mission_measurement_map = new HashMap<>();
        for(Integer key: missions.keySet()){
            ArrayList<String> mission_insts = missions.get(key);

            // Get instrument measurements
            ArrayList<String> mission_measurements = new ArrayList<>();
            for(String inst: mission_insts){
                ArrayList<String> inst_meas = res.dbClient.getInstrumentMeasurements(inst, false);
                for(String meas: inst_meas){
                    if(!mission_measurements.contains(meas)){
                        mission_measurements.add(meas);
                    }
                }
            }
            mission_measurement_map.put(key, mission_measurements);
        }


        // Iterate over mission slots
        ArrayList<Integer> mission_ordering = new ArrayList<>();
        int slot_counter = 0;
        for(Integer key: matrix.keySet()){
            System.out.println("\n-------- SLOT " + slot_counter + " PENALTIES");
            HashMap<String, Double> measurement_score_map = matrix.get(key);

            // For slot (key), determine which mission has the lowest penalty
            int lowest_penalty = 100000000;
            int slot_mission_key = 0;
            // Iterate over missions
            for(Integer mission_key: mission_measurement_map.keySet()){
                int mission_penalty = 0;
                ArrayList<String> mission_measurements = mission_measurement_map.get(mission_key);

                for(String mission_measurement: mission_measurements){
                    for(String historical_measurement: measurement_score_map.keySet()){
                        if(historical_measurement.toLowerCase().contains(mission_measurement.toLowerCase()) || mission_measurement.toLowerCase().contains(historical_measurement.toLowerCase())){
                            mission_penalty += measurement_score_map.get(historical_measurement);
                        }
                    }
                }
                if(mission_penalty < lowest_penalty){
                    lowest_penalty = mission_penalty;
                    slot_mission_key = mission_key;
                }
                System.out.println("---> MISSION PENALTY " + mission_key + " " + mission_penalty);
            }
            mission_ordering.add(slot_mission_key);
            mission_measurement_map.remove(slot_mission_key);
            slot_counter++;
        }

        return mission_ordering;
    }



    public static HashMap<Integer, HashMap<String, Double>> computeDataContinuityMatrix(Resource res, int num_mission_slots) throws Exception {
        // HashMap<Integer                         -- mission slots
        // HashMap<Integer, HashMap                -- measurements mapped to penalty
        // HashMap<Integer, HashMap<String         -- measurement
        // HashMap<Integer, HashMap<String, Double -- penalty
        HashMap<Integer, HashMap<String, Double>> matrix = new HashMap<>();

        // Assume the first mission is launched here
        int t_zero = 2000;

        // Assume 5 years to develop each mission
        int time_between_missions = 3;

        // Assume each mission lasts 2 years
        int mission_duration = 5;

        ArrayList<ArrayList<Integer>> mission_slots = new ArrayList<>();
        for(int x = 0; x < num_mission_slots; x++){
            ArrayList<Integer> slot = new ArrayList<>();
            slot.add(t_zero);
            slot.add(t_zero + mission_duration);
            mission_slots.add(slot);
            t_zero += (time_between_missions);
        }



        ArrayList<HashMap<String, ArrayList<Date>>> historical_missions = res.dbClient.getDataContinuityInformation();

        int slot_index = 0;
        for(ArrayList<Integer> mission_slot: mission_slots){
            int start_year = mission_slot.get(0);
            int end_year = mission_slot.get(1);

            // measurement to penalty map for this mission slot
            HashMap<String, Double> penalty_map = new HashMap<>();

            // Iterate over historical missions
            for(HashMap<String, ArrayList<Date>> historical_mission: historical_missions){
                // Iterate over historical mission measurements
                for(String measurement: historical_mission.keySet()){
                    ArrayList<Date> measurement_timeline = historical_mission.get(measurement);

                    // Get measurement Date data
                    Calendar calendar = new GregorianCalendar();
                    Date measurement_start   = measurement_timeline.get(0);
                    calendar.setTime(measurement_start);
                    int measurement_start_yr = calendar.get(Calendar.YEAR);
                    Date measurement_end   = measurement_timeline.get(1);
                    calendar.setTime(measurement_end);
                    int measurement_end_yr = calendar.get(Calendar.YEAR);

                    String trans_name = DatabaseClient.transformHistoricalMeasurementName(measurement);

                    int score = 0;

                    if(end_year < measurement_start_yr || start_year > measurement_end_yr){
                        score = score;
                    }
                    else if(start_year > measurement_start_yr && end_year < measurement_end_yr){
                        score = end_year - start_year;
                    }
                    else if(start_year > measurement_start_yr && end_year > measurement_end_yr){
                        score = measurement_end_yr - start_year;
                    }
                    else if(start_year < measurement_start_yr && end_year < measurement_end_yr){
                        score = end_year - measurement_start_yr;
                    }
                    else{
                        score = score;
                    }

                    // Check to see if the measurement is in the penalty map
                    if(penalty_map.containsKey(trans_name)){
                        Double current_score = penalty_map.get(trans_name);
                        penalty_map.put(trans_name, score + current_score);
                    }
                    else{
                        penalty_map.put(trans_name, (double) score);
                    }
                }
            }
            matrix.put(slot_index, penalty_map);
            slot_index++;
        }

//        System.out.println("------------> PENALTY MAP");
//        for(Integer key: matrix.keySet()){
//            System.out.println("-----> SLOT " + key);
//            HashMap<String, Double> penalty_map = matrix.get(key);
//            for(String meas_key: penalty_map.keySet()){
//                System.out.println("---> MEASUREMENT " + meas_key + " " + penalty_map.get(meas_key));
//            }
//        }
//        EvaluatorApp.sleep(30);


        return matrix;
    }


}














































//        for(int x = 0; x < 5; x++){
//        Iterator it = dimen_2.keySet().iterator();
//
//        // ITERATE OVER SYNERGIES
//        while(it.hasNext()){
//        Nto1pair nt = (Nto1pair)it.next();
//
//        ArrayList<String> al = new ArrayList<>();
//        Collections.addAll(al,nt.getBase());
//        al.add(nt.getAdded());
//        System.out.println("---> al: " + al);
//
//
//        String[] has_synergy_with   = nt.getBase();
//        String   inst_check_synergy = nt.getAdded();
//
//        ArrayList<String> combined = new ArrayList<>();
//        for(String inst: has_synergy_with){
//        combined.add(inst);
//        }
//        combined.add(inst_check_synergy);
//        Random rand = new Random();
//
//        // 1. Iterate through synergies to find one that is in the set of instruments
//        if(items.contains(combined.get(0)) && items.contains(combined.get(1))){
//        int idx_inst_check_synergy = Optimization.checkPartitions(partitions, inst_check_synergy);
//
//        // inst_check_synergy not already in a group
//        if(idx_inst_check_synergy == -1){
//        int idx_has_synergy_with = Optimization.checkPartitions(partitions, has_synergy_with[0]);
//        // If has_synergy_with is not in a group, create a new satellite for them
//        if(idx_has_synergy_with == -1){
//        ArrayList<String> group = new ArrayList<>();
//        group.add(inst_check_synergy);
//        group.add(has_synergy_with[0]);
//        partitions.put(partitions.size(), group);
//        }
//        // If has_synergy_with is in a group, add inst_check_synergy to the group with size constraints
//        else{
//        // If the group already has 3 instruments, add to a new mission
//        int size_lim = 3;
//
//        if(partitions.get(idx_has_synergy_with).size() >= size_lim){
//        ArrayList<String> group = new ArrayList<>();
//        group.add(inst_check_synergy);
//        partitions.put(partitions.size(), group);
//        }
//        }
//        }
//        }
//
//        }
//        }
//
//        // 2-lateral synergies
//        System.out.println("---> TWO LATERAL SYNERGIES");
//        Iterator it2 = dimen_2.keySet().iterator();
//
//        // ITERATE OVER SYNERGIES
//        while(it2.hasNext()){
//        Nto1pair nt = (Nto1pair)it2.next();
//
//        ArrayList<String> al = new ArrayList<>();
//        Collections.addAll(al,nt.getBase());
//        al.add(nt.getAdded());
//        System.out.println("---> al: " + al);
//
//
//        String[] has_synergy_with   = nt.getBase();
//        String   inst_check_synergy = nt.getAdded();
//
//        ArrayList<String> combined = new ArrayList<>();
//        for(String inst: has_synergy_with){
//        combined.add(inst);
//        }
//        combined.add(inst_check_synergy);
//        Random rand = new Random();
//
//        // 1. Iterate through synergies to find one that is in the set of instruments
//        if(items.contains(combined.get(0)) && items.contains(combined.get(1))){
//        int idx_inst_check_synergy = Optimization.checkPartitions(partitions, inst_check_synergy);
//
//        // inst_check_synergy not already in a group
//        if(idx_inst_check_synergy == -1){
//        int idx_has_synergy_with = Optimization.checkPartitions(partitions, has_synergy_with[0]);
//        // If has_synergy_with is not in a group, create a new satellite for them
//        if(idx_has_synergy_with == -1){
//        ArrayList<String> group = new ArrayList<>();
//        group.add(inst_check_synergy);
//        group.add(has_synergy_with[0]);
//        partitions.put(partitions.size(), group);
//        }
//        // If has_synergy_with is in a group, add inst_check_synergy to the group with size constraints
//        else{
//        // If the group already has 3 instruments, add to a new mission
//        int size_lim = 3;
//
//        if(partitions.get(idx_has_synergy_with).size() >= size_lim){
//        ArrayList<String> group = new ArrayList<>();
//        group.add(inst_check_synergy);
//        partitions.put(partitions.size(), group);
//        }
//        else{
//        partitions.get(idx_has_synergy_with).add(inst_check_synergy);
//        }
//        }
//        }
//        }
//        }
//
//        // remove already allocated instruments from the set
//        for(Integer idx: partitions.keySet()){
//        for(String inst: partitions.get(idx)){
//        items.remove(inst);
//        }
//        }
//
//        // For instruments with no synergies, place in a separate mission
//        for(String inst: items){
//        ArrayList<String> new_mission = new ArrayList<>();
//        new_mission.add(inst);
//        partitions.put(partitions.size(), new_mission);
//        }
//
//        return (new HashMap<>(partitions));



