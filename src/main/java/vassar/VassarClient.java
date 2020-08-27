package vassar;



// -  -  -   ____   ____                                     ______  __    _                  _
// -  -  -  |_  _| |_  _|                                  .' ___  |[  |  (_)                / |_
// -  -  -    \ \   / /,--.   .--.   .--.   ,--.   _ .--. / .'   \_| | |  __  .---.  _ .--. `| |-'
// -  -  -     \ \ / /`'_\ : ( (`\] ( (`\] `'_\ : [ `/'`\]| |        | | [  |/ /__\\[ `.-. | | |
// -  -  -      \ ' / // | |, `'.'.  `'.'. // | |, | |    \ `.___.'\ | |  | || \__., | | | | | |,
// -  -  -       \_/  \'-;__/[\__) )[\__) )\'-;__/[___]    `.____ .'[___][___]'.__.'[___||__]\__/
// -  -  -  - Gabe: take it easy m8

import com.evaluator.type.*;
import jess.Fact;
import jess.JessException;
import jess.Rete;
import jess.ValueVector;
import vassar.architecture.AbstractArchitecture;
import vassar.architecture.Architecture;
import vassar.evaluator.AbstractArchitectureEvaluator;
import vassar.evaluator.ArchitectureEvaluator;
import vassar.jess.Requests;
import vassar.jess.Resource;
import vassar.result.Result;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class VassarClient {

    private Resource engine;

    public static class Builder {

        private Resource engine;

        public Builder() {

        }

        public Builder setEngine(Resource engine) {
            this.engine = engine;
            return this;
        }

        public VassarClient build() {
            VassarClient build = new VassarClient();
            build.engine = this.engine;
            return build;
        }

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

    public Result evaluateArchitecture(String bitString, boolean ga, boolean redo, boolean fast){

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

        this.indexArchitecture(result, bitString, ga, redo, fast);

        return result;
    }

    public void indexArchitecture(Result result, String bitString, boolean ga, boolean redo, boolean fast){

        double cost    = result.getCost();
        double science = result.getScience();

        int archID = this.engine.dbClient.indexArchitecture(bitString, science, cost, ga, redo);

        if (redo){
            this.engine.dbClient.deleteArchitectureScoreExplanations(archID);
            this.engine.dbClient.deleteArchitectureCostInformation(archID);
        }

        if (!fast){
            this.indexArchitectureScoreExplanations(result, archID);
            this.indexArchitectureCostInformation(result, archID);
            this.indexArchitectureCritique(result, archID);
        }
    }

    private void indexArchitectureScoreExplanations(Result result, int archID){
        System.out.println("---> indexing architecture score explanations");
        ArrayList<ArchitectureScoreExplanation_insert_input> archExplanations      = new ArrayList<>();
        ArrayList<PanelScoreExplanation_insert_input>        panelExplanations     = new ArrayList<>();
        ArrayList<ObjectiveScoreExplanation_insert_input>    objectiveExplanations = new ArrayList<>();

        for (int i = 0; i < this.engine.problem.panelNames.size(); ++i) {

            // getArchitectureScoreExplanation
            archExplanations.add(
                    ArchitectureScoreExplanation_insert_input.builder()
                            .architecture_id(archID)
                            .panel_id(this.engine.dbClient.getPanelID(this.engine.problem.panelNames.get(i)))
                            .satisfaction(result.getPanelScores().get(i))
                            .build()
            );

            for (int j = 0; j < this.engine.problem.objNames.get(i).size(); ++j) {

                // getPanelScoreExplanation
                panelExplanations.add(
                        PanelScoreExplanation_insert_input.builder()
                                .architecture_id(archID)
                                .objective_id(this.engine.dbClient.getObjectiveID(this.engine.problem.objNames.get(i).get(j)))
                                .satisfaction(result.getObjectiveScores().get(i).get(j))
                                .build()
                );


                for (int k = 0; k < this.engine.problem.subobjectives.get(i).get(j).size(); ++k) {

                    // getObjectiveScoreExplanation
                    objectiveExplanations.add(
                            ObjectiveScoreExplanation_insert_input.builder()
                                    .architecture_id(archID)
                                    .subobjective_id(this.engine.dbClient.getSubobjectiveID(this.engine.problem.subobjectives.get(i).get(j).get(k)))
                                    .satisfaction(result.getSubobjectiveScores().get(i).get(j).get(k))
                                    .build()
                    );

                }
            }
        }

        this.engine.dbClient.insertArchitectureScoreExplanationBatch(archExplanations);
        this.engine.dbClient.insertPanelScoreExplanationBatch(panelExplanations);
        this.engine.dbClient.insertObjectiveScoreExplanationBatch(objectiveExplanations);
    }

    private void indexArchitectureCostInformation(Result result, int archID){
        System.out.println("---> Indexing architecture cost information");
        ArrayList<String> attributes = new ArrayList<>();
        String[] powerBudgetSlots    = { "payload-peak-power#", "satellite-BOL-power#" };
        String[] costBudgetSlots     = { "payload-cost#", "bus-cost#", "launch-cost#", "program-cost#", "IAT-cost#", "operations-cost#" };
        String[] massBudgetSlots     = { "adapter-mass", "propulsion-mass#", "structure-mass#", "avionics-mass#", "ADCS-mass#", "EPS-mass#", "propellant-mass-injection", "propellant-mass-ADCS", "thermal-mass#", "payload-mass#" };
        for(String atr: powerBudgetSlots){attributes.add(atr);}
        for(String atr: costBudgetSlots){attributes.add(atr);}
        for(String atr: massBudgetSlots){attributes.add(atr);}
        HashMap<String, Integer> attrKeys = this.engine.dbClient.getMissionAttributeIDs(attributes);
        HashMap<String, Integer> instKeys = this.engine.dbClient.getInstrumentIDs();

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
                int arch_cost_id = this.engine.dbClient.insertArchitectureCostInformation(
                        archID,
                        mission_name,
                        launch_vehicle,
                        mass,
                        power,
                        cost,
                        others
                );

                // --> 2. Index ArchitectureBudget with arch_cost_id
                ArrayList<ArchitectureBudget_insert_input> budget_inserts = new ArrayList<>();
                // power
                for(Integer mission_attribute_id: powerBudget.keySet()){
                    budget_inserts.add(
                            ArchitectureBudget_insert_input.builder()
                                .value(powerBudget.get(mission_attribute_id))
                                .arch_cost_id(arch_cost_id)
                                .mission_attribute_id(mission_attribute_id)
                                .build()
                    );
                }
                // cost
                for(Integer mission_attribute_id: costBudget.keySet()){
                    budget_inserts.add(
                            ArchitectureBudget_insert_input.builder()
                                    .value(costBudget.get(mission_attribute_id))
                                    .arch_cost_id(arch_cost_id)
                                    .mission_attribute_id(mission_attribute_id)
                                    .build()
                    );
                }
                // mass
                for(Integer mission_attribute_id: massBudget.keySet()){
                    budget_inserts.add(
                            ArchitectureBudget_insert_input.builder()
                                    .value(massBudget.get(mission_attribute_id))
                                    .arch_cost_id(arch_cost_id)
                                    .mission_attribute_id(mission_attribute_id)
                                    .build()
                    );
                }
                this.engine.dbClient.insertArchitectureBudgetBatch(budget_inserts);

                // --> 3. Index ArchitecturePayload with arch_cost_id
                ArrayList<ArchitecturePayload_insert_input> payload_inserts = new ArrayList<>();
                for(Integer instrument_id: payloads){
                    payload_inserts.add(
                            ArchitecturePayload_insert_input.builder()
                                .instrument_id(instrument_id)
                                .arch_cost_id(arch_cost_id)
                                .build()
                    );
                }
                this.engine.dbClient.insertArchitecturePayloadBatch(payload_inserts);

            }
            catch (JessException e) {
                System.err.println(e.toString());
            }
        }
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

        String jessGlobalTempPath = rootPath + "/app/src/main/java/vassar/database/template/defs";
        String jessGlobalFuncPath = rootPath + "/app/src/main/java/vassar/jess/utils/clp";
        String jessAppPath        = rootPath + "/app/problems/smap/clp";
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
