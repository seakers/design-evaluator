package vassar;



import com.evaluator.InsertArchitectureCostInformationMutation;
import com.evaluator.InsertArchitectureSlowMutation;
import com.evaluator.RequirementRulesForSubobjectiveQuery;

// -  -  -   ____   ____                                     ______  __    _                  _
// -  -  -  |_  _| |_  _|                                  .' ___  |[  |  (_)                / |_
// -  -  -    \ \   / /,--.   .--.   .--.   ,--.   _ .--. / .'   \_| | |  __  .---.  _ .--. `| |-'
// -  -  -     \ \ / /`'_\ : ( (`\] ( (`\] `'_\ : [ `/'`\]| |        | | [  |/ /__\\[ `.-. | | |
// -  -  -      \ ' / // | |, `'.'.  `'.'. // | |, | |    \ `.___.'\ | |  | || \__., | | | | | |,
// -  -  -       \_/  \'-;__/[\__) )[\__) )\'-;__/[___]    `.____ .'[___][___]'.__.'[___||__]\__/
// -  -  -  - Gabe: take it easy m8
// -  -  -  - Antoni: you shouldn't have asked if you could, but rather if you should

import com.evaluator.type.*;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import evaluator.EvaluatorApp;
import evaluator.ResourcePaths;
import jess.Fact;
import jess.JessException;
import jess.Rete;
import jess.Value;
import jess.ValueVector;
import vassar.architecture.*;
import vassar.combinatorics.Combinatorics;
import vassar.evaluator.ADDEvaluator;
import vassar.evaluator.AbstractArchitectureEvaluator;
import vassar.evaluator.ArchitectureEvaluator;
import vassar.jess.Requests;
import vassar.jess.Resource;
import vassar.result.Result;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class VassarClient {

    private Resource engine;

    private HashMap<ArrayList<ArrayList<String>>, Result> add_result_store;

    public static class Builder {

        private Resource engine;

        public Builder() {

        }

        public Builder setEngine(Resource engine) {
            this.engine = engine;
            return this;
        }

        public VassarClient build() {
            VassarClient build     = new VassarClient();
            build.engine           = this.engine;
            build.add_result_store = new HashMap<>();
            return build;
        }

    }

    public void setUserID(int id) {
        this.engine.dbClient.setUserID(id);
    }

    public boolean doesArchitectureExist(String input){
        return this.engine.dbClient.doesArchitectureExist(input);
    }



//
//  ______          _             _                           _     _ _            _
// |  ____|        | |           | |           /\            | |   (_) |          | |
// | |____   ____ _| |_   _  __ _| |_ ___     /  \   _ __ ___| |__  _| |_ ___  ___| |_ _   _ _ __ ___
// |  __\ \ / / _` | | | | |/ _` | __/ _ \   / /\ \ | '__/ __| '_ \| | __/ _ \/ __| __| | | | '__/ _ \
// | |___\ V / (_| | | |_| | (_| | ||  __/  / ____ \| | | (__| | | | | ||  __/ (__| |_| |_| | | |  __/
// |______\_/ \__,_|_|\__,_|\__,_|\__\___| /_/    \_\_|  \___|_| |_|_|\__\___|\___|\__|\__,_|_|  \___|
//

    public Result evaluateArchitecture(String bitString, Integer datasetId, boolean ga, boolean redo, boolean fast){

        AbstractArchitecture arch = new Architecture(bitString, 1, this.engine.getProblem());

        System.out.println(arch.isFeasibleAssignment());

        AbstractArchitectureEvaluator t = new ArchitectureEvaluator(this.engine, arch, "Slow");

        ExecutorService executorService = Executors.newFixedThreadPool(1);

        Future<Result> future = executorService.submit(t);

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

        this.indexArchitecture(result, bitString, datasetId, ga, redo, fast);

        return result;
    }

    public Result evaluateADDArchitecture(String json_design){

        // ARCHITECTURE
        ADDArchitecture arch = new ADDArchitecture(json_design);


        if(this.add_result_store.containsKey(arch.get_array_info())){
            return this.add_result_store.get(arch.get_array_info());
        }
        else{
            System.out.println(arch.isFeasibleAssignment());

            ADDEvaluator process = new ADDEvaluator.Builder(this.engine)
                    .setArchitecture(arch)
                    .evalCost(true)
                    .evalPerformance(true)
                    .evalScheduling(true)
                    .orbitSelection(true)
                    .synergyDeclaration(true)
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


            result.setDesignString(
                    arch.toString(result)
            );
            // this.add_result_store.put(arch.get_array_info(), result);
            return result;
        }
    }



    public Result evaluateSELECTINGArchitecture(String json_design){

        // ARCHITECTURE
        ADDArchitecture arch = new ADDArchitecture(json_design);
        arch.optimizeSelectingArchitecture(this.engine);


        if(this.add_result_store.containsKey(arch.get_array_info())){
            return this.add_result_store.get(arch.get_array_info());
        }
        else{

            System.out.println(arch.isFeasibleAssignment());

            ADDEvaluator process = new ADDEvaluator.Builder(this.engine)
                    .setArchitecture(arch)
                    .evalCost(true)
                    .evalPerformance(true)
                    .evalScheduling(true)
                    .orbitSelection(true)
                    .synergyDeclaration(true)
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

            // System.out.println(arch.toString(""));
            // EvaluatorApp.sleep(10);

            result.setDesignString(
                    arch.toString(result)
            );
            this.add_result_store.put(arch.get_array_info(), result);
            return result;

        }
    }

    public Result evaluatePARTITIONINGArchitecture(String json_design){

        // ARCHITECTURE
        ADDArchitecture arch = new ADDArchitecture(json_design);
        arch.optimizePartitioningArchitecture(this.engine);


        if(this.add_result_store.containsKey(arch.get_array_info())){
            return this.add_result_store.get(arch.get_array_info());
        }
        else{

            System.out.println(arch.isFeasibleAssignment());

            ADDEvaluator process = new ADDEvaluator.Builder(this.engine)
                    .setArchitecture(arch)
                    .evalCost(true)
                    .evalPerformance(true)
                    .evalScheduling(true)
                    .orbitSelection(true)
                    .synergyDeclaration(true)
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

            // System.out.println(arch.toString(""));
            // EvaluatorApp.sleep(10);

            result.setDesignString(
                    arch.toString(result)
            );
            this.add_result_store.put(arch.get_array_info(), result);
            return result;

        }
    }

    public Result evaluateTESTArchitecture(){

        // TEST ORBIT
//        SSO-700-SSO-PM
//        SSO-500-SSO-PM
        String orbit = "SSO-500-SSO-PM";

        // TEST INSTRUMENTS
        ArrayList<String> instruments = new ArrayList<>();
        instruments.add("ACE-CPR");

        SingleSat arch = new SingleSat(instruments, orbit);

        // EVALUATE
        ADDEvaluator process = new ADDEvaluator.Builder(this.engine)
                .setArchitecture(arch)
                .evalCost(true)
                .evalPerformance(true)
                .evalScheduling(true)
                .orbitSelection(false)
                .synergyDeclaration(true)
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


        return result;
    }

//  _   _ _____   _____ __  __
// | \ | |  __ \ / ____|  \/  |
// |  \| | |  | | (___ | \  / |
// | . ` | |  | |\___ \| |\/| |
// | |\  | |__| |____) | |  | |
// |_| \_|_____/|_____/|_|  |_|

    public void computeNDSMs(){

        // 2 DIMENSIONAL
        Combinatorics.computeNDSM(this.engine, this.engine.problem.instrumentList, 2);
        // 3 DIMENSIONAL
//        Combinatorics.computeNDSM(this.engine, this.engine.problem.instrumentList, 3);
//        // N DIMENSIONAL
//        Combinatorics.computeNDSM(this.engine, this.engine.problem.instrumentList, this.engine.problem.numInstr);

    }

    public void computeContinuityMatrix(){


        ArrayList<String> mission1 = new ArrayList<>();
        mission1.add("BIOMASS");
        mission1.add("SMAP_RAD");
        mission1.add("SMAP_MWR");

        ArrayList<String> mission2 = new ArrayList<>();
        mission2.add("CMIS");
        mission2.add("SMAP_RAD");
        mission2.add("SMAP_MWR");

        HashMap<Integer, ArrayList<String>> missions = new HashMap<>();
        missions.put(0, mission1);
        missions.put(1, mission2);

        ArrayList<Integer> ordering = Optimization.scheduleMissions(missions, this.engine);
        System.out.println("---> MISSION ORDERING " + ordering);





        // Optimization.computeDataContinuityMatrix(this.engine);
    }


//  _____           _                               _     _ _            _
// |_   _|         | |               /\            | |   (_) |          | |
//   | |  _ __   __| | _____  __    /  \   _ __ ___| |__  _| |_ ___  ___| |_ _   _ _ __ ___
//   | | | '_ \ / _` |/ _ \ \/ /   / /\ \ | '__/ __| '_ \| | __/ _ \/ __| __| | | | '__/ _ \
//  _| |_| | | | (_| |  __/>  <   / ____ \| | | (__| | | | | ||  __/ (__| |_| |_| | | |  __/
// |_____|_| |_|\__,_|\___/_/\_\ /_/    \_\_|  \___|_| |_|_|\__\___|\___|\__|\__,_|_|  \___|



    public void indexArchitecture(Result result, String bitString, Integer datasetId, boolean ga, boolean redo, boolean fast){

        double cost    = result.getCost();
        double science = result.getScience();

        if (redo) {
            this.engine.dbClient.deleteArchitectureScoreExplanations(archID);
            this.engine.dbClient.deleteArchitectureCostInformation(archID);
            // TODO: Update Arch builder
            this.engine.dbClient.indexArchitecture(bitString, datasetId, science, cost, ga, redo);
        }
        else {
            if (fast) {
                // TODO: Build Fast Arch
                this.engine.dbClient.indexArchitecture(bitString, datasetId, science, cost, ga, redo);
            }
            else {
                InsertArchitectureSlowMutation.Builder archBuilder = InsertArchitectureSlowMutation.builder();
                this.fillArchitectureInformation(archBuilder, datasetId, bitString, science, cost, ga);
                this.fillArchitectureScoreExplanations(result, archBuilder);
                this.fillArchitectureCostInformations(result, archBuilder);
                this.indexArchitectureCritique(result, archID);
                int archID = this.engine.dbClient.insertArchitectureSlow(archBuilder);
            }
        }
    }

    private void fillArchitectureInformation(InsertArchitectureSlowMutation.Builder builder, Integer datasetId, String input, double science, double cost, boolean ga) {
        builder
            .dataset_id(datasetId)
            .input(input)
            .science(science)
            .cost(cost)
            .eval_status(true)
            .ga(ga)
            .improve_hv(false);
    }

    private void fillArchitectureScoreExplanations(Result result, InsertArchitectureSlowMutation.Builder archBuilder){
        System.out.println("---> indexing architecture score explanations");
        ArrayList<ArchitectureScoreExplanation_insert_input> archExplanations         = new ArrayList<>();
        ArrayList<PanelScoreExplanation_insert_input>        panelExplanations        = new ArrayList<>();
        ArrayList<ObjectiveScoreExplanation_insert_input>    objectiveExplanations    = new ArrayList<>();
        ArrayList<SubobjectiveScoreExplanation_insert_input> subobjectiveExplanations = new ArrayList<>();

        for (int panel_idx = 0; panel_idx < this.engine.problem.panelNames.size(); ++panel_idx) {

            // getArchitectureScoreExplanation
            archExplanations.add(
                    ArchitectureScoreExplanation_insert_input.builder()
                            .architecture_id(archID)
                            .panel_id(this.engine.dbClient.getPanelID(this.engine.problem.panelNames.get(panel_idx)))
                            .satisfaction(result.getPanelScores().get(panel_idx))
                            .build()
            );

            for (int obj_idx = 0; obj_idx < this.engine.problem.objNames.get(panel_idx).size(); ++obj_idx) {

                // getPanelScoreExplanation
                panelExplanations.add(
                        PanelScoreExplanation_insert_input.builder()
                                .architecture_id(archID)
                                .objective_id(this.engine.dbClient.getObjectiveID(this.engine.problem.objNames.get(panel_idx).get(obj_idx)))
                                .satisfaction(result.getObjectiveScores().get(panel_idx).get(obj_idx))
                                .build()
                );


                for (int subobj_idx = 0; subobj_idx < this.engine.problem.subobjectives.get(panel_idx).get(obj_idx).size(); ++subobj_idx) {
                    String subobjectiveName = this.engine.problem.subobjectives.get(panel_idx).get(obj_idx).get(subobj_idx);
                    // getObjectiveScoreExplanation
                    objectiveExplanations.add(
                            ObjectiveScoreExplanation_insert_input.builder()
                                    .architecture_id(archID)
                                    .subobjective_id(this.engine.dbClient.getSubobjectiveID(subobjectiveName))
                                    .satisfaction(result.getSubobjectiveScores().get(panel_idx).get(obj_idx).get(subobj_idx))
                                    .build()
                    );

                    subobjectiveExplanations.addAll(getSubobjectiveExplanations(result, archID, subobjectiveName));
                }
            }
        }

        this.engine.dbClient.insertArchitectureScoreExplanationBatch(archExplanations);
        this.engine.dbClient.insertPanelScoreExplanationBatch(panelExplanations);
        this.engine.dbClient.insertObjectiveScoreExplanationBatch(objectiveExplanations);
        this.engine.dbClient.insertSubobjectiveScoreExplanationBatch(subobjectiveExplanations);
    }

    private List<SubobjectiveScoreExplanation_insert_input> getSubobjectiveExplanations(Result result, int archID, String subobj) {
        String measurement = this.engine.problem.subobjectivesToMeasurements.get(subobj);

        // Obtain list of attributes for this measurement
        ArrayList<String> attrNames = new ArrayList<>();
        HashMap<String, String> attrTypes = new HashMap<>();
        List<RequirementRulesForSubobjectiveQuery.Item> requirementRules = this.engine.dbClient.getRequirementRulesForSubobjective(subobj);
        Integer subobjectiveId = requirementRules.get(0).subobjective().id();
        requirementRules.forEach((reqRule) -> attrNames.add(reqRule.measurement_attribute().name()));
        requirementRules.forEach((reqRule) -> attrTypes.put(reqRule.measurement_attribute().name(), reqRule.type()));
        HashMap<String, Integer> numDecimals = new HashMap<>();
        numDecimals.put("Horizontal-Spatial-Resolution#", 0);
        numDecimals.put("Temporal-resolution#", 0);
        numDecimals.put("Swath#", 0);

        // Loop to get rows of details for each data product
        ArrayList<SubobjectiveScoreExplanation_insert_input> subobjectiveExplanations = new ArrayList<>();
        for (Fact explanation: result.getExplanations().get(subobj)) {
            try {
                // Try to find the requirement fact!
                int measurementId = explanation.getSlotValue("requirement-id").intValue(null);
                if (measurementId == -1) {
                    continue;
                }
                Fact measurementFact = null;
                for (Fact capability: result.getCapabilities()) {
                    if (capability.getFactId() == measurementId) {
                        measurementFact = capability;
                        break;
                    }
                }
                // Start by putting all attribute values into list
                HashMap<String, String> attributes = new HashMap<>();
                for (String attrName: attrNames) {
                    String attrType = attrTypes.get(attrName);
                    // Check type and convert to String if needed
                    Value attrValue = measurementFact.getSlotValue(attrName);
                    switch (attrType) {
                        case "SIB":
                        case "LIB": {
                            Double value = attrValue.floatValue(null);
                            double scale = 100;
                            if (numDecimals.containsKey(attrName)) {
                                scale = Math.pow(10, numDecimals.get(attrName));
                            }
                            value = Math.round(value * scale) / scale;
                            attributes.put(attrName, value.toString());
                            break;
                        }
                        default: {
                            attributes.put(attrName, attrValue.toString());
                            break;
                        }
                    }
                }
                JSONObject attributesJson = new JSONObject(attributes);

                // Get information from explanation fact
                Double score = explanation.getSlotValue("satisfaction").floatValue(null);
                String satisfiedBy = explanation.getSlotValue("satisfied-by").stringValue(null);
                ArrayList<String> rowJustifications = new ArrayList<>();
                ValueVector reasons = explanation.getSlotValue("reasons").listValue(null);
                for (int i = 0; i < reasons.size(); ++i) {
                    String reason = reasons.get(i).stringValue(null);
                    if (!reason.equals("N-A")) {
                        rowJustifications.add(reason);
                    }
                }
                JSONArray justificationsJson = new JSONArray();
                justificationsJson.addAll(rowJustifications);

                // Put everything in their respective object
                SubobjectiveScoreExplanation_insert_input scoreExplanation = SubobjectiveScoreExplanation_insert_input.builder()
                    .architecture_id(archID)
                    .subobjective_id(subobjectiveId)
                    .measurement_attribute_values(attributesJson)
                    .score(score)
                    .taken_by(satisfiedBy)
                    .justifications(justificationsJson)
                    .build();
                subobjectiveExplanations.add(scoreExplanation);
            }
            catch (JessException e) {
                System.err.println(e.toString());
            }
        }

        return subobjectiveExplanations;
    }

    private void fillArchitectureCostInformations(Result result, InsertArchitectureSlowMutation.Builder archBuilder) {
        //System.out.println("---> Indexing architecture cost information");
        ArrayList<String> attributes = new ArrayList<>();
        String[] powerBudgetSlots    = { "payload-peak-power#", "satellite-BOL-power#" };
        String[] costBudgetSlots     = { "payload-cost#", "bus-cost#", "launch-cost#", "program-cost#", "IAT-cost#", "operations-cost#" };
        String[] massBudgetSlots     = { "adapter-mass", "propulsion-mass#", "structure-mass#", "avionics-mass#", "ADCS-mass#", "EPS-mass#", "propellant-mass-injection", "propellant-mass-ADCS", "thermal-mass#", "payload-mass#" };
        for(String atr: powerBudgetSlots){attributes.add(atr);}
        for(String atr: costBudgetSlots){attributes.add(atr);}
        for(String atr: massBudgetSlots){attributes.add(atr);}
        HashMap<String, Integer> attrKeys = this.engine.dbClient.getMissionAttributeIDs(attributes);
        HashMap<String, Integer> instKeys = this.engine.dbClient.getInstrumentIDs();

        ArrayList<ArchitectureCostInformation_insert_input> costInserts = new ArrayList<>();
        for(Fact costFact: result.getCostFacts()){
            try {
                // --> ArchitectureCostInformation
                String mission_name   = costFact.getSlotValue("Name").stringValue(null);                     // ArchitectureCostInformation!!!
                String launch_vehicle = costFact.getSlotValue("launch-vehicle").stringValue(null);           // ArchitectureCostInformation!!!
                Double cost           = costFact.getSlotValue("mission-cost#").floatValue(null);             // ArchitectureCostInformation!!!
                Double mass           = costFact.getSlotValue("satellite-launch-mass").floatValue(null);     // ArchitectureCostInformation!!!
                Double power          = 0.0;                                                                            // ArchitectureCostInformation!!!
                Double others         = 0.0;                                                                            // ArchitectureCostInformation!!!
                
                // ArchitectureBudget: power
                HashMap<Integer, Double> powerBudget = new HashMap<>();                                                  // ArchitectureBudget!!!
                for (String powerSlot: powerBudgetSlots) {
                    Double value = costFact.getSlotValue(powerSlot).floatValue(null);
                    power += value;                                                                                     // SET POWER - ArchitectureCostInformation
                    powerBudget.put(attrKeys.get(powerSlot), value);
                }

                // ArchitectureBudget: cost
                double[] costMultipliers = { 1e-3, 1e-3, 1.0, 1e-3, 1e-3, 1e-3 };
                HashMap<Integer, Double> costBudget = new HashMap<>();                                                   // ArchitectureBudget!!!
                Double sumCost = 0.0;
                for (int i = 0; i < costBudgetSlots.length; ++i) {
                    String costSlot = costBudgetSlots[i];
                    Double multiplier = costMultipliers[i];
                    Double value = costFact.getSlotValue(costSlot).floatValue(null);
                    sumCost += value*multiplier;
                    costBudget.put(attrKeys.get(costSlot), value*multiplier);
                }
                others = cost - sumCost;                                                                                // SET OTHERS - ArchitectureCostInformation

                // ArchitectureBudget: mass
                HashMap<Integer, Double> massBudget = new HashMap<>();                                                   // ArchitectureBudget!!!
                for (String massSlot: massBudgetSlots) {
                    Double value = costFact.getSlotValue(massSlot).floatValue(null);
                    massBudget.put(attrKeys.get(massSlot), value);
                }

                // ArchitecturePayload
                ArrayList<Integer> payloads = new ArrayList<>();                                                         // ArchitecturePayload!!!
                ValueVector instruments = costFact.getSlotValue("instruments").listValue(null);
                for (int i = 0; i < instruments.size(); ++i) {
                    System.out.println("--> " + instruments.get(i).stringValue(null));
                    payloads.add(instKeys.get(instruments.get(i).stringValue(null)));
                }

                // --> 1. Index ArchitectureCostInformation - get arch_cost_id
                ArchitectureCostInformation_insert_input.Builder costInsertBuilder = ArchitectureCostInformation_insert_input.builder()
                    .mission_name(mission_name)
                    .launch_vehicle(launch_vehicle)
                    .mass(mass)
                    .power(power)
                    .cost(cost)
                    .others(others);
                
                // --> 2. Index ArchitectureBudget with arch_cost_id
                ArrayList<ArchitectureBudget_insert_input> budget_inserts = new ArrayList<>();
                // power
                for(Integer mission_attribute_id: powerBudget.keySet()){
                    budget_inserts.add(
                            ArchitectureBudget_insert_input.builder()
                                .value(powerBudget.get(mission_attribute_id))
                                .mission_attribute_id(mission_attribute_id)
                                .build()
                    );
                }
                // cost
                for(Integer mission_attribute_id: costBudget.keySet()){
                    budget_inserts.add(
                            ArchitectureBudget_insert_input.builder()
                                    .value(costBudget.get(mission_attribute_id))
                                    .mission_attribute_id(mission_attribute_id)
                                    .build()
                    );
                }
                // mass
                for(Integer mission_attribute_id: massBudget.keySet()){
                    budget_inserts.add(
                            ArchitectureBudget_insert_input.builder()
                                    .value(massBudget.get(mission_attribute_id))
                                    .mission_attribute_id(mission_attribute_id)
                                    .build()
                    );
                }
                ArchitectureBudget_arr_rel_insert_input budgets_input = ArchitectureBudget_arr_rel_insert_input.builder().data(budget_inserts).build();
                //this.engine.dbClient.insertArchitectureBudgetBatch(budget_inserts);

                // --> 3. Index ArchitecturePayload with arch_cost_id
                ArrayList<ArchitecturePayload_insert_input> payload_inserts = new ArrayList<>();
                for(Integer instrument_id: payloads){
                    payload_inserts.add(
                            ArchitecturePayload_insert_input.builder()
                                .instrument_id(instrument_id)
                                .build()
                    );
                }
                ArchitecturePayload_arr_rel_insert_input payloads_input = ArchitecturePayload_arr_rel_insert_input.builder().data(payload_inserts).build();

                costInsertBuilder
                    .architectureBudgets(budgets_input)
                    .architecturePayloads(payloads_input);

                costInserts.add(costInsertBuilder.build());

                //this.engine.dbClient.insertArchitecturePayloadBatch(payload_inserts);

            }
            catch (JessException e) {
                System.err.println(e.toString());
            }
        }
        archBuilder.cost_informations(costInserts);
    }

    private void indexArchitectureCritique(Result result, int archID){
        System.out.println("---> Indexing architecture critique");
        Vector<String> performanceCritique = result.getPerformanceCritique();
        Vector<String> costCritique = result.getCostCritique();
        String critique = "";
        for(String crit: performanceCritique){
            critique = critique + crit + " | ";
        }
        for(String crit: costCritique){
            critique = critique + crit + " | ";
        }
        System.out.println(critique);
        this.engine.dbClient.updateArchitectureCritique(archID, critique);
    }


//  _____      _           _ _     _   _____
// |  __ \    | |         (_) |   | | |  __ \
// | |__) |___| |__  _   _ _| | __| | | |__) |___  ___  ___  _   _ _ __ ___ ___
// |  _  // _ \ '_ \| | | | | |/ _` | |  _  // _ \/ __|/ _ \| | | | '__/ __/ _ \
// | | \ \  __/ |_) | |_| | | | (_| | | | \ \  __/\__ \ (_) | |_| | | | (_|  __/
// |_|  \_\___|_.__/ \__,_|_|_|\__,_| |_|  \_\___||___/\___/ \__,_|_|  \___\___|


    public void rebuildResource(int group_id, int problem_id){

        // String rootPath = "/Users/gabeapaza/repositories/seakers/design_evaluator";
        String rootPath = ""; // DOCKER

        String jessGlobalTempPath = ResourcePaths.resourcesRootDir + "/vassar/templates";
        String jessGlobalFuncPath = ResourcePaths.resourcesRootDir + "/vassar/functions";
        String jessAppPath        = ResourcePaths.resourcesRootDir + "/vassar/problems/SMAP/clp";
        String requestMode        = "CRISP-ATTRIBUTES";
        Requests newRequests = new Requests.Builder()
                                           .setGlobalTemplatePath(jessGlobalTempPath)
                                           .setGlobalFunctionPath(jessGlobalFuncPath)
                                           .setFunctionTemplates()
                                           .setRequestMode(requestMode)
                                           .setJessAppPath(jessAppPath)
                                           .build();

        Resource newResource = this.engine.rebuild(group_id, problem_id, newRequests.getRequests());
        this.engine          = newResource;
    }


}
