package vassar.architecture;


import com.evaluator.*;
import com.evaluator.type.*;
import jess.Fact;
import jess.JessException;
import jess.ValueVector;
import vassar.database.DatabaseClient;
import vassar.database.service.QueryAPI;
import vassar.jess.Resource;
import vassar.problem.Problem;
import vassar.result.Result;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;


/*
    -- For the GA, we want to be able to send evaluated architectures to the GA queue with only a string representation
        of the objects...
    -- The idea is to create an ArchitectureWrapper class capable of being built from both the Result obj
        and the String sent over the GA queue.
    -- This class is capable of indexing the architecture into the database of course


 */

public class ArchitectureDB {

    public class ScoreExplanations {

        public ScoreExplanations(){
            archExplanations = new ArrayList<>();
            panelExplanations = new ArrayList<>();
            objectiveExplanations = new ArrayList<>();
            subobjectiveExplanations = new ArrayList<>();
        }

        ArrayList<ArchitectureScoreExplanation_insert_input> archExplanations;
        ArrayList<PanelScoreExplanation_insert_input>        panelExplanations;
        ArrayList<ObjectiveScoreExplanation_insert_input>    objectiveExplanations;
        ArrayList<SubobjectiveScoreExplanation_insert_input> subobjectiveExplanations;
    }

    public class CostInformation {
        ArrayList<ArchitectureCostInformation_insert_input> costInformation;
    }

    private DatabaseClient db;
    private Problem problem;
    private int problem_id;
    private int dataset_id;

    public ArchitectureDB(Resource engine, int dataset_id){
        this.db = engine.dbClient;
        this.problem = engine.problem;

        this.problem_id = db.getCurrentProblemID();
        this.dataset_id = dataset_id;

        this.print();
    }

    private void print(){
        System.out.println("--> " + this.problem_id);
        System.out.println("--> " + this.dataset_id);
    }



//     _____                     _
//    |_   _|                   | |
//      | |  _ __  ___  ___ _ __| |_
//      | | | '_ \/ __|/ _ \ '__| __|
//     _| |_| | | \__ \  __/ |  | |_
//    |_____|_| |_|___/\___|_|   \__|
//


    public void insert_architecture(String input, Result result){
        System.out.println("--> INSERTING ARCHITECTURE");

        // --> 1. Create builder object
        InsertArchitectureWrapperFullMutation.Builder builder = InsertArchitectureWrapperFullMutation.builder();

        // --> 2. Insert base info
        builder
                .dataset_id(this.dataset_id)
                .input(input)
                .science(result.getScience())
                .cost(result.getCost())
                .programmatic_risk(result.getProgrammaticRisk())
                .fairness(result.getFairnessScore())
                .data_continuity(result.getDataContinuityScore())
                .critique(result.getCritique())
                .eval_idx(result.getEvalIdx())
                .ga(false)
                .improve_hv(false)
                .eval_status(true);

        // --> 3. Insert score information
        ScoreExplanations explanations = this.get_score_info(result);
        builder.arch_scores(explanations.archExplanations)
                .panel_scores(explanations.panelExplanations)
                .objective_scores(explanations.objectiveExplanations)
                .subobjective_scores(explanations.subobjectiveExplanations);

        // --> 4. Insert cost info
        CostInformation cost_info = this.get_cost_info(result);
        builder.cost_informations(cost_info.costInformation);

        // --> 5. Finally insert into db
        int id = this.db.insertArchitectureWrapperFull(builder);
    }


    public void insert_architecture_ga(String input, Result result){

        // --> 1. Create builder object
        InsertArchitectureWrapperFullMutation.Builder builder = InsertArchitectureWrapperFullMutation.builder();

        // --> 2. Insert base info
        builder
                .dataset_id(this.dataset_id)
                .input(input)
                .science(result.getScience())
                .cost(result.getCost())
                .programmatic_risk(result.getProgrammaticRisk())
                .fairness(result.getFairnessScore())
                .data_continuity(result.getDataContinuityScore())
                .critique(result.getCritique())
                .eval_idx(result.getEvalIdx())
                .ga(true)
                .improve_hv(false)
                .eval_status(true);

        // --> 3. Insert score information
        ScoreExplanations explanations = this.get_score_info(result);
        builder.arch_scores(explanations.archExplanations)
                .panel_scores(explanations.panelExplanations)
                .objective_scores(explanations.objectiveExplanations)
                .subobjective_scores(explanations.subobjectiveExplanations);

        // --> 4. Insert cost info
        CostInformation cost_info = this.get_cost_info(result);
        builder.cost_informations(cost_info.costInformation);

        // --> 5. Finally insert into db
        int id = this.db.insertArchitectureWrapperFull(builder);
    }

    public void insert_architecture_fast(String input, Result result){

        // --> 1. Create builder object
        InsertArchitectureWrapperFastMutation.Builder builder = InsertArchitectureWrapperFastMutation.builder();

        // --> 2. Insert base info
        builder
                .dataset_id(this.dataset_id)
                .input(input)
                .science(result.getScience())
                .cost(result.getCost())
                .programmatic_risk(result.getProgrammaticRisk())
                .fairness(result.getFairnessScore())
                .data_continuity(result.getDataContinuityScore())
                .ga(false)
                .eval_status(true)
                .improve_hv(false)
                .critique(result.getCritique())
                .eval_idx(result.getEvalIdx())
                .build();

        // --> 3. Insert into DB
        this.db.queryAPI.insertArchitectureWrapper(builder);
    }


    private ScoreExplanations get_score_info(Result result){

        // --> 1. Create score explanation objects
        ArrayList<ArchitectureScoreExplanation_insert_input> arch_explanations         = new ArrayList<>();
        ArrayList<PanelScoreExplanation_insert_input>        panel_explanations        = new ArrayList<>();
        ArrayList<ObjectiveScoreExplanation_insert_input>    objective_explanations    = new ArrayList<>();
        ArrayList<SubobjectiveScoreExplanation_insert_input> subobjective_explanations = new ArrayList<>();

        // --> 2. Iterate recursively through: Panels -> Objectives -> Subobjectives
        System.out.println("--> PANEL NAMES: " + this.problem.panelNames);
        int panel_idx = 0;
        for(String panel: this.problem.panelNames){

            System.out.println("--> PANEL: " + this.db.getPanelID(panel) + " --- " + panel);

            arch_explanations.add(
                    ArchitectureScoreExplanation_insert_input.builder()
                            .panel_id(this.db.getPanelID(panel))
                            .satisfaction(result.getPanelScore(panel_idx))
                            .build()
            );

            // --> RECUR (objectives)
            ArrayList<String> objectives = this.problem.objNames.get(panel_idx);
            int objective_idx = 0;
            for(String objective: objectives){

                panel_explanations.add(
                        PanelScoreExplanation_insert_input.builder()
                                .objective_id(this.db.getObjectiveID(objective))
                                .satisfaction(result.getObjectiveScore(panel_idx, objective_idx))
                                .build()
                );


                // --> RECUR (subobjectives)
                ArrayList<String> subobjectives = this.problem.subobjectives.get(panel_idx).get(objective_idx);
                int subobjective_idx = 0;
                for(String subobjective: subobjectives){

                    objective_explanations.add(
                            ObjectiveScoreExplanation_insert_input.builder()
                                    .subobjective_id(this.db.getSubobjectiveID(subobjective))
                                    .satisfaction(result.getSubobjectiveScore(panel_idx, objective_idx, subobjective_idx))
                                    .build()

                    );

                    subobjective_idx++;
                }

                objective_idx++;
            }

            panel_idx++;
        }


        ScoreExplanations explanations = new ScoreExplanations();
        explanations.archExplanations = arch_explanations;
        explanations.panelExplanations = panel_explanations;
        explanations.objectiveExplanations = objective_explanations;
        explanations.subobjectiveExplanations = subobjective_explanations;

        return explanations;
    }

    private ScoreExplanations get_new_score_info(Result result, int arch_id){

        // --> 1. Create score explanation objects
        ArrayList<ArchitectureScoreExplanation_insert_input> arch_explanations         = new ArrayList<>();
        ArrayList<PanelScoreExplanation_insert_input>        panel_explanations        = new ArrayList<>();
        ArrayList<ObjectiveScoreExplanation_insert_input>    objective_explanations    = new ArrayList<>();
        ArrayList<SubobjectiveScoreExplanation_insert_input> subobjective_explanations = new ArrayList<>();

        // --> 2. Iterate recursively through: Panels -> Objectives -> Subobjectives
        System.out.println("--> PANEL NAMES: " + this.problem.panelNames);
        int panel_idx = 0;
        for(String panel: this.problem.panelNames){

            System.out.println("--> PANEL: " + this.db.getPanelID(panel) + " --- " + panel);

            arch_explanations.add(
                    ArchitectureScoreExplanation_insert_input.builder()
                            .architecture_id(arch_id)
                            .panel_id(this.db.getPanelID(panel))
                            .satisfaction(result.getPanelScore(panel_idx))
                            .build()
            );

            // --> RECUR (objectives)
            ArrayList<String> objectives = this.problem.objNames.get(panel_idx);
            int objective_idx = 0;
            for(String objective: objectives){

                panel_explanations.add(
                        PanelScoreExplanation_insert_input.builder()
                                .architecture_id(arch_id)
                                .objective_id(this.db.getObjectiveID(objective))
                                .satisfaction(result.getObjectiveScore(panel_idx, objective_idx))
                                .build()
                );


                // --> RECUR (subobjectives)
                ArrayList<String> subobjectives = this.problem.subobjectives.get(panel_idx).get(objective_idx);
                int subobjective_idx = 0;
                for(String subobjective: subobjectives){

                    objective_explanations.add(
                            ObjectiveScoreExplanation_insert_input.builder()
                                    .architecture_id(arch_id)
                                    .subobjective_id(this.db.getSubobjectiveID(subobjective))
                                    .satisfaction(result.getSubobjectiveScore(panel_idx, objective_idx, subobjective_idx))
                                    .build()

                    );

                    subobjective_idx++;
                }

                objective_idx++;
            }

            panel_idx++;
        }


        ScoreExplanations explanations = new ScoreExplanations();
        explanations.archExplanations = arch_explanations;
        explanations.panelExplanations = panel_explanations;
        explanations.objectiveExplanations = objective_explanations;
        explanations.subobjectiveExplanations = subobjective_explanations;

        return explanations;
    }


    private CostInformation get_cost_info(Result result){
        ArrayList<String> attributes = new ArrayList<>();
        String[] powerBudgetSlots    = { "payload-peak-power#", "satellite-BOL-power#" };
        String[] costBudgetSlots     = { "payload-cost#", "bus-cost#", "launch-cost#", "program-cost#", "IAT-cost#", "operations-cost#" };
        String[] massBudgetSlots     = { "adapter-mass", "propulsion-mass#", "structure-mass#", "avionics-mass#", "ADCS-mass#", "EPS-mass#", "propellant-mass-injection", "propellant-mass-ADCS", "thermal-mass#", "payload-mass#" };
        for(String atr: powerBudgetSlots){attributes.add(atr);}
        for(String atr: costBudgetSlots){attributes.add(atr);}
        for(String atr: massBudgetSlots){attributes.add(atr);}
        HashMap<String, Integer> attrKeys = this.db.getMissionAttributeIDs(attributes);
        HashMap<String, Integer> instKeys = this.db.getInstrumentIDs();

        ArrayList<ArchitectureCostInformation_insert_input> costInserts = new ArrayList<>();
        for(Fact costFact: result.getCostFacts()){
            try{
                // --> ArchitectureCostInformation
                String mission_name   = costFact.getSlotValue("Name").stringValue(null);                     // ArchitectureCostInformation!!!
                String launch_vehicle = costFact.getSlotValue("launch-vehicle").stringValue(null);           // ArchitectureCostInformation!!!
                Double cost           = costFact.getSlotValue("mission-cost#").floatValue(null);             // ArchitectureCostInformation!!!
                Double mass           = costFact.getSlotValue("satellite-launch-mass").floatValue(null);     // ArchitectureCostInformation!!!
                Double power          = 0.0;                                                                            // ArchitectureCostInformation!!!
                Double others         = 0.0;

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
            }
            catch (JessException e) {
                System.err.println(e.toString());
            }
        }

        CostInformation costInfo = new CostInformation();
        costInfo.costInformation = costInserts;
        return costInfo;
    }

    private CostInformation get_new_cost_info(Result result, int arch_id){
        ArrayList<String> attributes = new ArrayList<>();
        String[] powerBudgetSlots    = { "payload-peak-power#", "satellite-BOL-power#" };
        String[] costBudgetSlots     = { "payload-cost#", "bus-cost#", "launch-cost#", "program-cost#", "IAT-cost#", "operations-cost#" };
        String[] massBudgetSlots     = { "adapter-mass", "propulsion-mass#", "structure-mass#", "avionics-mass#", "ADCS-mass#", "EPS-mass#", "propellant-mass-injection", "propellant-mass-ADCS", "thermal-mass#", "payload-mass#" };
        for(String atr: powerBudgetSlots){attributes.add(atr);}
        for(String atr: costBudgetSlots){attributes.add(atr);}
        for(String atr: massBudgetSlots){attributes.add(atr);}
        HashMap<String, Integer> attrKeys = this.db.getMissionAttributeIDs(attributes);
        HashMap<String, Integer> instKeys = this.db.getInstrumentIDs();

        ArrayList<ArchitectureCostInformation_insert_input> costInserts = new ArrayList<>();
        for(Fact costFact: result.getCostFacts()){
            try{
                // --> ArchitectureCostInformation
                String mission_name   = costFact.getSlotValue("Name").stringValue(null);                     // ArchitectureCostInformation!!!
                String launch_vehicle = costFact.getSlotValue("launch-vehicle").stringValue(null);           // ArchitectureCostInformation!!!
                Double cost           = costFact.getSlotValue("mission-cost#").floatValue(null);             // ArchitectureCostInformation!!!
                Double mass           = costFact.getSlotValue("satellite-launch-mass").floatValue(null);     // ArchitectureCostInformation!!!
                Double power          = 0.0;                                                                            // ArchitectureCostInformation!!!
                Double others         = 0.0;

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
                        .architecture_id(arch_id)
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
            }
            catch (JessException e) {
                System.err.println(e.toString());
            }
        }

        CostInformation costInfo = new CostInformation();
        costInfo.costInformation = costInserts;
        return costInfo;
    }


//     _    _           _       _
//    | |  | |         | |     | |
//    | |  | |_ __   __| | __ _| |_ ___
//    | |  | | '_ \ / _` |/ _` | __/ _ \
//    | |__| | |_) | (_| | (_| | ||  __/
//     \____/| .__/ \__,_|\__,_|\__\___|
//           | |
//           |_|


    public void update_architecture(String input, Result result, int arch_id){
        System.out.println("--> UPDATING: " + arch_id + " --- " + input);


        // --> 1. Query all architecture component tables and get their ids
        FullArchitectureByPkQuery.Architecture architecture = this.db.queryAPI.fullArchitectureQuery(arch_id);

        // --> 2. Create update architecture mutation
        UpdateArchitectureFullMutation.Builder build = UpdateArchitectureFullMutation.builder();

        // --> 3. Set base information
        this.update_base_info(build, architecture, input, result);

        // --> 4. Set cost information
        this.update_cost_info(build, architecture, result);

        // --> 5. Set score information
        this.update_score_info(build, architecture, result);

        // --> 6. Execute built mutation
        this.db.queryAPI.updateArchitectureFull(build.build());
    }

    private void update_base_info(UpdateArchitectureFullMutation.Builder build, FullArchitectureByPkQuery.Architecture architecture, String input, Result result){
        build.arch_id(architecture.id())
                .dataset_id(architecture.dataset_id())
                .cost(result.getCost())
                .critique(result.getCritique())
                .data_continuity(result.getDataContinuityScore())
                .science(result.getScience())
                .programmatic_risk(result.getProgrammaticRisk())
                .input(input)
                .fairness(result.getFairnessScore())
                .eval_status(true);
    }

    private void update_cost_info(UpdateArchitectureFullMutation.Builder build, FullArchitectureByPkQuery.Architecture architecture, Result result){
        // --> Delete
        build.del_paylod_ids(this.getArchitecturePayloadIDs(architecture))
                .del_budget_ids(this.getArchitectureBudgetIDs(architecture))
                .del_cost_info_ids(this.getArchitectureCostInfoIDs(architecture));

        // --> Insert
        CostInformation cost_info = this.get_new_cost_info(result, architecture.id());
        build.new_arch_cost_info(cost_info.costInformation);
    }

    private void update_score_info(UpdateArchitectureFullMutation.Builder build, FullArchitectureByPkQuery.Architecture architecture, Result result){
        // --> Delete
        build.del_arch_score_ids(this.getArchitectureScoreIDs(architecture))
                .del_panel_score_ids(this.getPanelScoreIDs(architecture))
                .del_objective_score_ids(this.getObjectiveScoreIDs(architecture))
                .del_subobjective_score_ids(this.getSubobjectiveScoreIDs(architecture));

        // --> Insert
        ScoreExplanations explanations = this.get_new_score_info(result, architecture.id());
        build.new_arch_scores(explanations.archExplanations)
                .new_panel_scores(explanations.panelExplanations)
                .new_objective_scores(explanations.objectiveExplanations)
                .new_subobjective_scores(explanations.subobjectiveExplanations);
    }




    private List<Integer> getArchitectureScoreIDs(FullArchitectureByPkQuery.Architecture architecture){
        ArrayList<Integer> ids = new ArrayList<>();
        for(FullArchitectureByPkQuery.ArchitectureScoreExplanation entry: architecture.ArchitectureScoreExplanations()){
            ids.add(entry.id());
        }
        return ids;
    }

    private List<Integer> getPanelScoreIDs(FullArchitectureByPkQuery.Architecture architecture){
        ArrayList<Integer> ids = new ArrayList<>();
        for(FullArchitectureByPkQuery.PanelScoreExplanation entry: architecture.PanelScoreExplanations()){
            ids.add(entry.id());
        }
        return ids;
    }

    private List<Integer> getObjectiveScoreIDs(FullArchitectureByPkQuery.Architecture architecture){
        ArrayList<Integer> ids = new ArrayList<>();
        for(FullArchitectureByPkQuery.ObjectiveScoreExplanation entry: architecture.ObjectiveScoreExplanations()){
            ids.add(entry.id());
        }
        return ids;
    }

    private List<Integer> getSubobjectiveScoreIDs(FullArchitectureByPkQuery.Architecture architecture){
        ArrayList<Integer> ids = new ArrayList<>();
        for(FullArchitectureByPkQuery.SubobjectiveScoreExplanation entry: architecture.SubobjectiveScoreExplanations()){
            ids.add(entry.id());
        }
        return ids;
    }

    private List<Integer> getArchitectureBudgetIDs(FullArchitectureByPkQuery.Architecture architecture){
        ArrayList<Integer> ids = new ArrayList<>();
        for(FullArchitectureByPkQuery.ArchitectureCostInformation entry: architecture.ArchitectureCostInformations()){
            for(FullArchitectureByPkQuery.ArchitectureBudget budget: entry.ArchitectureBudgets()){
                ids.add(budget.id());
            }
        }
        return ids;
    }

    private List<Integer> getArchitecturePayloadIDs(FullArchitectureByPkQuery.Architecture architecture){
        ArrayList<Integer> ids = new ArrayList<>();
        for(FullArchitectureByPkQuery.ArchitectureCostInformation entry: architecture.ArchitectureCostInformations()){
            for(FullArchitectureByPkQuery.ArchitecturePayload payload: entry.ArchitecturePayloads()){
                ids.add(payload.id());
            }
        }
        return ids;
    }

    private List<Integer> getArchitectureCostInfoIDs(FullArchitectureByPkQuery.Architecture architecture){
        ArrayList<Integer> ids = new ArrayList<>();
        for(FullArchitectureByPkQuery.ArchitectureCostInformation entry: architecture.ArchitectureCostInformations()){
            ids.add(entry.id());
        }
        return ids;
    }











}
