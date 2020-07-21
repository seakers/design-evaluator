package vassar.database;

// I/O

// JSON
import com.apollographql.apollo.ApolloSubscriptionCall;
import com.evaluator.ArchitectureCostInformationQuery;
import com.evaluator.CostMissionAttributeQuery;
import com.evaluator.GlobalInstrumentQuery;
import com.evaluator.InstrumentQuery;
import com.evaluator.type.*;
import vassar.database.service.DebugAPI;
import vassar.database.service.QueryAPI;
import vassar.database.template.TemplateRequest;
import vassar.database.template.TemplateResponse;
import vassar.problem.Problem;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class DatabaseClient {

    private boolean  debug;
    private QueryAPI queryAPI;
    private DebugAPI debugAPI;
    private ArrayList<ApolloSubscriptionCall> subscriptions;

    public static class Builder {

        private boolean  debug;
        private QueryAPI queryAPI;
        private DebugAPI debugAPI;
        private ArrayList<ApolloSubscriptionCall> subscriptions;

        public Builder(){
            this.debug = false;
            this.queryAPI = null;
        }
        public Builder debug(boolean debug){
            this.debug = debug;
            return this;
        }

        public Builder queryClient(QueryAPI qClient){
            this.queryAPI = qClient;
            return this;
        }

        public Builder debugClient(DebugAPI dClient){
            this.debugAPI = dClient;
            return this;
        }

        public Builder subscribe(){
            subscriptions = new ArrayList<>();
            subscriptions.add(
                    this.queryAPI.subscribeToInstruments()
            );
            subscriptions.add(
                    this.queryAPI.subscribeToOrbits()
            );
            subscriptions.add(
                    this.queryAPI.subscribeToStakeholders()
            );
            subscriptions.add(
                    this.queryAPI.subscribeToInstrumentCharacteristics()
            );
            subscriptions.add(
                    this.queryAPI.subscribeToLaunchVehicles()
            );
            return this;
        }

        public DatabaseClient build() {
            DatabaseClient api = new DatabaseClient();
            api.debug          = this.debug;
            api.queryAPI       = this.queryAPI;
            api.debugAPI       = this.debugAPI;
            api.subscriptions  = this.subscriptions;
            return api;
        }
    }

    public TemplateResponse processTemplateRequest(TemplateRequest request, Problem.Builder problemBuilder) {

        // PASS PROBLEM
        request.setProblemBuilder(problemBuilder);
        String key = request.getTemplateFilePath();

        // PROCESS REQUEST
        TemplateResponse response = request.processRequest(this.queryAPI);
        System.out.println(key);
        this.debugAPI.writeTemplateOutputFile(key, response.getTemplateString());

        return response;
    }




    // ---> Index Architecture
    public int indexArchitecture(String input, double science, double cost, boolean ga, boolean redo){
        if(redo){
            return this.queryAPI.updateArchitecture(input, science, cost, ga);
        }
        else{
            return this.queryAPI.insertArchitecture(input, science, cost, ga);
        }
    }
    public void deleteArchitectureScoreExplanations(int archID){
        this.queryAPI.deleteArchitectureScoreExplanations(archID);
    }
    public void deleteArchitectureCostInformation(int archID){
        List<ArchitectureCostInformationQuery.Item> items = this.queryAPI.queryArchitectureCostInformation(archID);
        for(ArchitectureCostInformationQuery.Item item: items){
            this.queryAPI.deleteArchitectureCostInformationFK(item.id());
            this.queryAPI.deleteArchitectureCostInformationPK(item.id());
        }
    }

    // ---> Index Explanations
    public void indexArchitectureScoreExplanation(String panelName, double satisfaction, double weight, int archID){
        System.out.println("---> Getting panel id: " + panelName);
        int panelId = this.queryAPI.getPanelID(panelName);
        this.queryAPI.insertArchitectureScoreExplanation(archID, panelId, satisfaction);
    }
    public void indexPanelScoreExplanation(String objName, double satisfaction, double weight, int archID){
        System.out.println("---> Getting objective id: " + objName);
        int objectiveId = this.queryAPI.getObjectiveID(objName);
        this.queryAPI.insertPanelScoreExplanation(archID, objectiveId, satisfaction);
    }
    public void indexObjectiveScoreExplanation(String subobjName, double satisfaction, double weight, int archID){
        System.out.println("---> Getting subobjective id: " + subobjName);
        int subobjectiveID = this.queryAPI.getSubobjectiveID(subobjName);
        this.queryAPI.insertObjectiveScoreExplanation(archID, subobjectiveID, satisfaction);
    }
    public void insertArchitectureScoreExplanationBatch(ArrayList<ArchitectureScoreExplanation_insert_input> items){
        this.queryAPI.insertArchitectureScoreExplanationBatch(items);
    }
    public void insertPanelScoreExplanationBatch(ArrayList<PanelScoreExplanation_insert_input> items){
        this.queryAPI.insertPanelScoreExplanationBatch(items);
    }
    public void insertObjectiveScoreExplanationBatch(ArrayList<ObjectiveScoreExplanation_insert_input> items){
        this.queryAPI.insertObjectiveScoreExplanationBatch(items);
    }

    // ---> Index Cost Information
    public HashMap<String, Integer> getMissionAttributeIDs(ArrayList<String> attributes){
        HashMap<String, Integer>              attrs = new HashMap<>();
        List<CostMissionAttributeQuery.Item>  items = this.queryAPI.costMissionAttributeQuery(attributes);
        for(CostMissionAttributeQuery.Item item: items){
            attrs.put(item.name(), item.id());
        }
        return attrs;
    }
    public HashMap<String, Integer> getInstrumentIDs(){
        HashMap<String, Integer>    insts = new HashMap<>();
        List<GlobalInstrumentQuery.Item>  items = this.queryAPI.globalInstrumentQuery();
        for(GlobalInstrumentQuery.Item item: items){
            insts.put(item.name(), item.id());
        }
        return insts;
    }
    public int insertArchitectureCostInformation(int architecture_id, String mission_name, String launch_vehicle, double mass, double power, double cost, double others){
        return this.queryAPI.insertArchitectureCostInformation(architecture_id, mission_name, launch_vehicle, mass, power, cost, others);
    }
    public void insertArchitecturePayloadBatch(ArrayList<ArchitecturePayload_insert_input> items){
        this.queryAPI.insertArchitecturePayloadBatch(items);
    }
    public void insertArchitectureBudgetBatch(ArrayList<ArchitectureBudget_insert_input> items){
        this.queryAPI.insertArchitectureBudgetBatch(items);
    }

    // ---> Index Critique
    public void updateArchitectureCritique(int archID, String critique){
        this.queryAPI.updateArchitectureCritique(archID, critique);
    }

    // ---> Getters
    public int getPanelID(String panelName){
        return this.queryAPI.getPanelID(panelName);
    }
    public int getObjectiveID(String objName){
        return this.queryAPI.getObjectiveID(objName);
    }
    public int getSubobjectiveID(String subobjName){
        return this.queryAPI.getSubobjectiveID(subobjName);
    }



    public void setProblemID(int id){
        this.queryAPI.problem_id = id;
    }

    public void setGroupID(int id){
        this.queryAPI.group_id = id;
    }

    public boolean doesArchitectureExist(String input){
        return this.queryAPI.doesArchitectureExist(input);
    }


    public void writeDebugInfo() {
        this.debugAPI.writeJson();
    }

}
