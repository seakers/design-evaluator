package vassar.database;

// I/O

// JSON
import com.apollographql.apollo.ApolloSubscriptionCall;
import com.evaluator.*;
import com.evaluator.type.*;
import com.google.gson.JsonObject;
import evaluator.EvaluatorApp;
import vassar.database.service.DebugAPI;
import vassar.database.service.QueryAPI;
import vassar.database.template.TemplateRequest;
import vassar.database.template.TemplateResponse;
import vassar.problem.Problem;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.temporal.ChronoUnit;
import java.util.*;


public class DatabaseClient {

    private boolean  debug;
    private QueryAPI queryAPI;
    private DebugAPI debugAPI;
    private ArrayList<ApolloSubscriptionCall> subscriptions;

    ArrayList<HashMap<String, ArrayList<Date>>> historical_info;
    HashMap<String, JsonObject> subobjective_objects;

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
            api.historical_info = new ArrayList<>();
            api.subobjective_objects = new HashMap<>();
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



    // ---> Data Continuity Information
    public ArrayList<HashMap<String, ArrayList<Date>>> getDataContinuityInformation(){
        ArrayList<String> all_measurements = new ArrayList<>();

        if(this.historical_info.isEmpty()){
            // --> Each HashMap<String, ArrayList<String>> represents a mission
            // -----> Key: measurement name
            // -----> ArrayList<String>: [0] mission start - [1] mission end
            ArrayList<HashMap<String, ArrayList<Date>>> all_missions = new ArrayList<>();
            List<HistoricalMissionMeasurementContinuityQuery.Item> missions = this.queryAPI.getHistoricalMissionMeasurementContinuity();

            for(HistoricalMissionMeasurementContinuityQuery.Item mission: missions){
                HashMap<String, ArrayList<Date>> new_mission = new HashMap<>();

                // Format: 2010-07-12T00:00:00
                // Format: yyyy-MM-ddTHH:mm:ss
                if(mission.start_date() == null || mission.end_date() == null){
                    continue;
                }
                String start_date = mission.start_date().toString();
                String end_date   = mission.end_date().toString();

                Date s_date = new Date();
                Date e_date = new Date();
                try{
                    s_date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(start_date);
                    e_date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(end_date);
                    ChronoUnit.SECONDS.between(s_date.toInstant(), e_date.toInstant());
                }
                catch (ParseException e){
                    e.printStackTrace();
                    System.out.println("---- ERORR PARSING MISSION DATE");
                    System.out.println(start_date);
                    System.out.println(end_date);
                    System.exit(0);
                }

                // Iterate over mission instruments
                for(HistoricalMissionMeasurementContinuityQuery.Instrument inst: mission.instruments()){
                    for(HistoricalMissionMeasurementContinuityQuery.Measurement meas: inst.item().measurements()){
                        String meas_name = meas.item().name();
                        if(!all_measurements.contains(meas_name)){
                            all_measurements.add(meas_name);
                        }

                        ArrayList<Date> dates = new ArrayList<>();
                        dates.add(s_date);
                        dates.add(e_date);
                        new_mission.put(meas_name, dates);
                    }
                }
                all_missions.add(new_mission);
            }
            this.historical_info = all_missions;
        }

        return this.historical_info;
    }


    public List<WalkerMissionAnalysisQuery.Item> getWalkerMissionAnalysis(){
        return this.queryAPI.walkerMissionAnalysisQuery();
    }


    // ---> Index Architecture
    public int updateArchitecture(String input, Integer datasetId, double science, double cost, boolean ga, double programmatic_risk, double fairness, double data_continuity){
        return this.queryAPI.updateArchitecture(input, datasetId, science, cost, ga, programmatic_risk, fairness, data_continuity);
    }

    public int insertArchitectureFast(String input, Integer datasetId, double science, double cost, boolean ga){
        return this.queryAPI.insertArchitecture(input, datasetId, science, cost, ga);
    }

    public int insertArchitectureSlow(InsertArchitectureSlowMutation.Builder archBuilder){
        return this.queryAPI.insertArchitectureSlow(archBuilder);
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
    public void insertSubobjectiveScoreExplanationBatch(ArrayList<SubobjectiveScoreExplanation_insert_input> items){
        this.queryAPI.insertSubobjectiveScoreExplanationBatch(items);
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
    public void insertArchitectureCostInformationBatch(ArrayList<ArchitectureCostInformation_insert_input> items){
        this.queryAPI.insertArchitectureCostInformationBatch(items);
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

    public ArrayList<String> getInstrumentMeasurements(String instrument, boolean trim){
        ArrayList<String> measurements = new ArrayList<>();

        List<InstrumentMeasurementsQuery.Item> items = this.queryAPI.instrumentMeasurementQuery(instrument);
        for(InstrumentMeasurementsQuery.Item item: items){
            for(InstrumentMeasurementsQuery.Join__Instrument_Capability cap: item.Join__Instrument_Capabilities()){
                String meas = cap.Measurement().name();
                if(trim){
                    String[] segments = meas.split("\\s+");
                    ArrayList<String> shorter = new ArrayList<>();
                    for(int x = 0; x < segments.length; x++){
                        if(x == 0){
                            continue;
                        }
                        shorter.add(segments[x]);
                    }
                    meas = String.join(" ", shorter);
                }
                measurements.add(meas);
            }
        }
        return measurements;
    }

    public JsonObject getSubobjectiveAttributeInformation(String name){

        if(this.subobjective_objects.containsKey(name)){
            return this.subobjective_objects.get(name);
        }
        else{
            List<SubobjectiveAttributeInformationQuery.Item> items = this.queryAPI.querySubobjectiveAttributeInformation(name);

            if(items.size() == 0){
                System.out.println("---> ERROR, ONE SUBOBJECTIVE SHOULD BE RETURNED");
                EvaluatorApp.sleep(45);
            }

            JsonObject subjective_info = new JsonObject();
            subjective_info.addProperty("name", name);
            subjective_info.addProperty("description", items.get(0).description());
            subjective_info.addProperty("weight", items.get(0).weight().toString());

            this.subobjective_objects.put(name, subjective_info);
            return subjective_info;
        }
    }

    public List<RequirementRulesForSubobjectiveQuery.Item> getRequirementRulesForSubobjective(String subobjectiveName) {
        return this.queryAPI.getRequirementRulesForSubobjective(subobjectiveName);
    }




    public void setProblemID(int id){
        this.queryAPI.problemId = id;
    }

    public void setGroupID(int id){
        this.queryAPI.groupId = id;
    }

    public void setUserID(int id){
        this.queryAPI.userId = id;
    }

    public void resubscribe(){
        // 1. Cancel all subscriptions
        for(ApolloSubscriptionCall sub: this.subscriptions){
            sub.cancel();
        }

        // 2. Create new subscriptions
        ArrayList<ApolloSubscriptionCall> new_subs = new ArrayList<>();
        new_subs.add(
                this.queryAPI.subscribeToInstruments()
        );
        new_subs.add(
                this.queryAPI.subscribeToOrbits()
        );
        new_subs.add(
                this.queryAPI.subscribeToStakeholders()
        );
        new_subs.add(
                this.queryAPI.subscribeToInstrumentCharacteristics()
        );
        new_subs.add(
                this.queryAPI.subscribeToLaunchVehicles()
        );
        this.subscriptions = new_subs;
    }

    public boolean doesArchitectureExist(String input){
        return this.queryAPI.doesArchitectureExist(input);
    }


    public void writeDebugInfo() {
        this.debugAPI.writeJson();
    }



    public static String transformHistoricalMeasurementName(String measurement){
        if(measurement.equals("Sea-ice cover")) {
            return "Sea ice cover";
        }
        else if(measurement.equals("Soil moisture at the surface")) {
            return "Soil moisture";
        }
        else if(measurement.equals("Wind vector over sea surface (horizontal)") || measurement.equals("Wind speed over sea surface (horizontal)")) {
            return "Ocean surface wind speed";
        }
        return measurement;
    }


    public static void printHistoricalMeasurements(ArrayList<HashMap<String, ArrayList<Date>>> historical_missions){
        ArrayList<String> all_measurements = new ArrayList<>();
        for(HashMap<String, ArrayList<Date>> mission: historical_missions){
            for(String measurement: mission.keySet()){
                if(!all_measurements.contains(measurement)){
                    all_measurements.add(measurement);
                }
            }
        }
        Collections.sort(all_measurements);
        System.out.println("\n---> ALL HISTORICAL MEASUREMENTS");
        System.out.println(all_measurements);
    }



}
