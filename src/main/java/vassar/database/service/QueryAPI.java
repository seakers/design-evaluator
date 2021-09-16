package vassar.database.service;

import com.apollographql.apollo.ApolloClient;
import com.apollographql.apollo.ApolloSubscriptionCall;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.subscription.WebSocketSubscriptionTransport;
import com.evaluator.*;
import com.evaluator.type.*;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

// APOLLO
import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.api.Response;

// RxJava2
import com.apollographql.apollo.rx2.Rx2Apollo;
import io.reactivex.Observable;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.io.IOException;

// QUERIES


import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class QueryAPI {

    private ApolloClient apollo;
    private OkHttpClient http;
    private String       apolloUrl;     // = "http://graphql:8080/v1/graphql";
    public  int          groupId;
    public  int          problemId;
    public  int          userId;

    private ConcurrentLinkedQueue<Map<String, String>> privateQueue;
    private final SqsClient client;

    public QueryAPI(ConcurrentLinkedQueue<Map<String, String>> privateQueue, SqsClient client) {
        this.privateQueue = privateQueue;
        this.client = client;
    }

    public static class Builder {

        private ApolloClient                               apollo;
        private OkHttpClient                               http;
        private SqsClient                                  client;
        private String                                     apolloUrl;     // = "http://graphql:8080/v1/graphql";
        private ConcurrentLinkedQueue<Map<String, String>> privateQueue;
        private int                                        groupId;
        private int                                        problemId;

        public Builder(String apolloUrl, String apolloWsUrl){
            this.apolloUrl = apolloUrl;
            this.http       = new OkHttpClient.Builder().build();
            this.apollo     = ApolloClient.builder()
                    .serverUrl(this.apolloUrl)
                    .subscriptionTransportFactory(new WebSocketSubscriptionTransport.Factory(apolloWsUrl, this.http)) // ws://graphql:8080/v1/graphql
                    .okHttpClient(this.http)
                    .build();
        }

        public Builder groupID(int groupId){
            this.groupId = groupId;
            return this;
        }

        public Builder privateQueue(ConcurrentLinkedQueue<Map<String, String>> privateQueue){
            this.privateQueue = privateQueue;
            return this;
        }

        public Builder sqsClient(SqsClient client){
            this.client = client;
            return this;
        }

        public Builder problemID(int problemId){
            this.problemId = problemId;
            return this;
        }

        public QueryAPI build(){
            QueryAPI client = new QueryAPI(this.privateQueue, this.client);

            client.apollo            = this.apollo;
            client.http              = this.http;       
            client.apolloUrl         = this.apolloUrl;  // = "http://graphql:8080/v1/graphql";
            client.groupId           = this.groupId;
            client.problemId         = this.problemId;

            return client;
        }

    }


//   ____                  _
//  / __ \                (_)
// | |  | |_   _  ___ _ __ _  ___  ___
// | |  | | | | |/ _ \ '__| |/ _ \/ __|
// | |__| | |_| |  __/ |  | |  __/\__ \
//  \___\_\\__,_|\___|_|  |_|\___||___/
//



    // ---> INSTRUMENTS
    public List<InstrumentQuery.Item> instrumentQuery(){
        InstrumentQuery iaQuery = InstrumentQuery.builder()
                .problem_id(this.problemId)
                .build();
        ApolloCall<InstrumentQuery.Data>           apolloCall  = this.apollo.query(iaQuery);
        Observable<Response<InstrumentQuery.Data>> observable  = Rx2Apollo.from(apolloCall);
        return observable.blockingFirst().getData().items();
    }

    public List<GlobalInstrumentQuery.Item> globalInstrumentQuery(){
        GlobalInstrumentQuery iaQuery = GlobalInstrumentQuery.builder()
                .group_id(this.groupId)
                .build();
        ApolloCall<GlobalInstrumentQuery.Data>           apolloCall  = this.apollo.query(iaQuery);
        Observable<Response<GlobalInstrumentQuery.Data>> observable  = Rx2Apollo.from(apolloCall);
        return observable.blockingFirst().getData().items();
    }

    public List<ProblemInstrumentsQuery.Item> problemInstrumentQuery(){
        ProblemInstrumentsQuery iaQuery = ProblemInstrumentsQuery.builder()
                .problem_id(this.problemId)
                .build();
        ApolloCall<ProblemInstrumentsQuery.Data>           apolloCall  = this.apollo.query(iaQuery);
        Observable<Response<ProblemInstrumentsQuery.Data>> observable  = Rx2Apollo.from(apolloCall);
        return observable.blockingFirst().getData().items();
    }

    public List<EnabledInstrumentsQuery.Item> enabledInstrumentQuery(){
        EnabledInstrumentsQuery iaQuery = EnabledInstrumentsQuery.builder()
                .problem_id(this.problemId)
                .build();
        ApolloCall<EnabledInstrumentsQuery.Data>           apolloCall  = this.apollo.query(iaQuery);
        Observable<Response<EnabledInstrumentsQuery.Data>> observable  = Rx2Apollo.from(apolloCall);

        return observable.blockingFirst().getData().items();
    }

    public List<MeasurementAttributeQuery.Item> measurementAttributeQuery(){
        MeasurementAttributeQuery maQuery = MeasurementAttributeQuery.builder()
                .group_id(this.groupId)
                .build();
        ApolloCall<MeasurementAttributeQuery.Data>           apolloCall  = this.apollo.query(maQuery);
        Observable<Response<MeasurementAttributeQuery.Data>> observable  = Rx2Apollo.from(apolloCall);
        return Objects.requireNonNull(observable.blockingFirst().getData()).items();
    }

    public List<InstrumentAttributeQuery.Item> instrumentAttributeQuery(){
        InstrumentAttributeQuery iaQuery = InstrumentAttributeQuery.builder()
                .group_id(this.groupId)
                .build();
        ApolloCall<InstrumentAttributeQuery.Data>           apolloCall  = this.apollo.query(iaQuery);
        Observable<Response<InstrumentAttributeQuery.Data>> observable  = Rx2Apollo.from(apolloCall);
        return observable.blockingFirst().getData().items();
    }

    public List<InstrumentMeasurementsQuery.Item> instrumentMeasurementQuery(String instrument){
        InstrumentMeasurementsQuery iaQuery = InstrumentMeasurementsQuery.builder()
                .problem_id(this.problemId)
                .instrument_name(instrument)
                .build();
        ApolloCall<InstrumentMeasurementsQuery.Data>           apolloCall  = this.apollo.query(iaQuery);
        Observable<Response<InstrumentMeasurementsQuery.Data>> observable  = Rx2Apollo.from(apolloCall);
        return observable.blockingFirst().getData().items();
    }

    public List<WalkerMissionAnalysisQuery.Item> walkerMissionAnalysisQuery(){
        WalkerMissionAnalysisQuery iaQuery = WalkerMissionAnalysisQuery.builder()
                .problem_id(this.problemId)
                .build();
        ApolloCall<WalkerMissionAnalysisQuery.Data>           apolloCall  = this.apollo.query(iaQuery);
        Observable<Response<WalkerMissionAnalysisQuery.Data>> observable  = Rx2Apollo.from(apolloCall);
        return observable.blockingFirst().getData().items();
    }



    // ----> ORBIT
    public List<ProblemOrbitsQuery.Item> problemOrbitQuery(){
        ProblemOrbitsQuery orbitQuery = ProblemOrbitsQuery.builder()
                .problem_id(this.problemId)
                .build();
        ApolloCall<ProblemOrbitsQuery.Data>           apolloCall  = this.apollo.query(orbitQuery);
        Observable<Response<ProblemOrbitsQuery.Data>> observable  = Rx2Apollo.from(apolloCall);
        return observable.blockingFirst().getData().items();
    }

    public List<ProblemOrbitJoinQuery.Item> problemOrbitJoin(){
        ProblemOrbitJoinQuery orbitQuery = ProblemOrbitJoinQuery.builder()
                .problem_id(this.problemId)
                .build();
        ApolloCall<ProblemOrbitJoinQuery.Data>           apolloCall  = this.apollo.query(orbitQuery);
        Observable<Response<ProblemOrbitJoinQuery.Data>> observable  = Rx2Apollo.from(apolloCall);
        return observable.blockingFirst().getData().items();
    }

    public List<OrbitAttributeQuery.Item> orbitAttributeQuery(){
        OrbitAttributeQuery iaQuery = OrbitAttributeQuery.builder()
                .group_id(this.groupId)
                .build();
        ApolloCall<OrbitAttributeQuery.Data>           apolloCall  = this.apollo.query(iaQuery);
        Observable<Response<OrbitAttributeQuery.Data>> observable  = Rx2Apollo.from(apolloCall);
        return observable.blockingFirst().getData().items();
    }





    // ---> LAUNCH VEHICLE

    // remove
    public List<ProblemLaunchVehicleQuery.Item> problemLaunchVehicleQuery(){
        ProblemLaunchVehicleQuery iaQuery = ProblemLaunchVehicleQuery.builder()
                .problem_id(this.problemId)
                .build();
        ApolloCall<ProblemLaunchVehicleQuery.Data>           apolloCall  = this.apollo.query(iaQuery);
        Observable<Response<ProblemLaunchVehicleQuery.Data>> observable  = Rx2Apollo.from(apolloCall);
        return observable.blockingFirst().getData().items();
    }

    public List<LaunchVehicleAttributeQuery.Item> launchVehicleAttributeQuery(){
        LaunchVehicleAttributeQuery iaQuery = LaunchVehicleAttributeQuery.builder()
                                                                        .group_id(this.groupId)
                                                                        .build();
        ApolloCall<LaunchVehicleAttributeQuery.Data>           apolloCall  = this.apollo.query(iaQuery);
        Observable<Response<LaunchVehicleAttributeQuery.Data>> observable  = Rx2Apollo.from(apolloCall);
        return observable.blockingFirst().getData().items();
    }





    // ---> RULES
    public List<AggregationRuleQuery.Item> aggregationRuleQuery(){
        AggregationRuleQuery iaQuery = AggregationRuleQuery.builder()
                .problem_id(this.problemId)
                .build();
        ApolloCall<AggregationRuleQuery.Data>           apolloCall  = this.apollo.query(iaQuery);
        Observable<Response<AggregationRuleQuery.Data>> observable  = Rx2Apollo.from(apolloCall);

        return observable.blockingFirst().getData().items();
    }

    public List<RequirementRuleCaseQuery.Item> requirementRuleCaseQuery(){
        RequirementRuleCaseQuery iaQuery = RequirementRuleCaseQuery.builder()
                .problem_id(this.problemId)
                .build();
        ApolloCall<RequirementRuleCaseQuery.Data>           apolloCall  = this.apollo.query(iaQuery);
        Observable<Response<RequirementRuleCaseQuery.Data>> observable  = Rx2Apollo.from(apolloCall);
        return observable.blockingFirst().getData().items();
    }

    public List<RequirementRuleAttributeQuery.Item> requirementRuleAttributeQuery(){
        RequirementRuleAttributeQuery iaQuery = RequirementRuleAttributeQuery.builder()
                .problem_id(this.problemId)
                .build();
        ApolloCall<RequirementRuleAttributeQuery.Data>           apolloCall  = this.apollo.query(iaQuery);
        Observable<Response<RequirementRuleAttributeQuery.Data>> observable  = Rx2Apollo.from(apolloCall);

        return observable.blockingFirst().getData().items();
    }

    public List<CapabilityRuleQuery.Item> capabilityRuleQuery(List<Integer> instList){
        CapabilityRuleQuery iaQuery = CapabilityRuleQuery.builder()
        //        .problem_id(this.problem_id)
                .group_id(this.groupId)
                .inst_ids(instList)
                .build();
        ApolloCall<CapabilityRuleQuery.Data>           apolloCall  = this.apollo.query(iaQuery);
        Observable<Response<CapabilityRuleQuery.Data>> observable  = Rx2Apollo.from(apolloCall);

        return observable.blockingFirst().getData().items();
    }

    public List<SubobjectiveAttributeInformationQuery.Item> querySubobjectiveAttributeInformation(String name){
        SubobjectiveAttributeInformationQuery iaQuery = SubobjectiveAttributeInformationQuery.builder()
                .problem_id(this.problemId)
                .name(name)
                .build();
        ApolloCall<SubobjectiveAttributeInformationQuery.Data>           apolloCall  = this.apollo.query(iaQuery);
        Observable<Response<SubobjectiveAttributeInformationQuery.Data>> observable  = Rx2Apollo.from(apolloCall);

        return observable.blockingFirst().getData().items();
    }





    // ---> MISSION
    public List<MissionAttributeQuery.Item> missionAttributeQuery(){
        MissionAttributeQuery iaQuery = MissionAttributeQuery.builder()
                                                            .problem_id(this.problemId)
                                                            .build();
        ApolloCall<MissionAttributeQuery.Data>           apolloCall  = this.apollo.query(iaQuery);
        Observable<Response<MissionAttributeQuery.Data>> observable  = Rx2Apollo.from(apolloCall);
        return observable.blockingFirst().getData().items();
    }

    public List<AttributeInheritanceQuery.Item> attributeInheritanceQuery(){
        AttributeInheritanceQuery iaQuery = AttributeInheritanceQuery.builder()
                .problem_id(this.problemId)
                .build();
        ApolloCall<AttributeInheritanceQuery.Data>           apolloCall  = this.apollo.query(iaQuery);
        Observable<Response<AttributeInheritanceQuery.Data>> observable  = Rx2Apollo.from(apolloCall);
        return observable.blockingFirst().getData().items();
    }

    public List<FuzzyAttributeQuery.Item> fuzzyAttributeQuery(){
        FuzzyAttributeQuery iaQuery = FuzzyAttributeQuery.builder()
                .problem_id(this.problemId)
                .build();
        ApolloCall<FuzzyAttributeQuery.Data>           apolloCall  = this.apollo.query(iaQuery);
        Observable<Response<FuzzyAttributeQuery.Data>> observable  = Rx2Apollo.from(apolloCall);
        return observable.blockingFirst().getData().items();
    }

    public List<CostMissionAttributeQuery.Item> costMissionAttributeQuery(ArrayList<String> attributes){
        CostMissionAttributeQuery iaQuery = CostMissionAttributeQuery.builder()
                .problem_id(this.problemId)
                .attributes(attributes)
                .build();
        ApolloCall<CostMissionAttributeQuery.Data>           apolloCall  = this.apollo.query(iaQuery);
        Observable<Response<CostMissionAttributeQuery.Data>> observable  = Rx2Apollo.from(apolloCall);
        return observable.blockingFirst().getData().items();
    }


    // ---> STAKEHOLDERS
    public int getPanelID(String panel){
        PanelIdQuery idQuery = PanelIdQuery.builder()
                .problem_id(this.problemId)
                .name(panel)
                .build();
        ApolloCall<PanelIdQuery.Data>           apolloCall  = this.apollo.query(idQuery);
        Observable<Response<PanelIdQuery.Data>> observable  = Rx2Apollo.from(apolloCall);

        return observable.blockingFirst().getData().items().get(0).id();
    }

    public int getObjectiveID(String objective){
        ObjectiveIdQuery idQuery = ObjectiveIdQuery.builder()
                .problem_id(this.problemId)
                .name(objective)
                .build();
        ApolloCall<ObjectiveIdQuery.Data>           apolloCall  = this.apollo.query(idQuery);
        Observable<Response<ObjectiveIdQuery.Data>> observable  = Rx2Apollo.from(apolloCall);

        return observable.blockingFirst().getData().items().get(0).id();
    }

    public int getSubobjectiveID(String subobjective){
        SubobjectiveIdQuery idQuery = SubobjectiveIdQuery.builder()
                .problem_id(this.problemId)
                .name(subobjective)
                .build();
        ApolloCall<SubobjectiveIdQuery.Data>           apolloCall  = this.apollo.query(idQuery);
        Observable<Response<SubobjectiveIdQuery.Data>> observable  = Rx2Apollo.from(apolloCall);

        return observable.blockingFirst().getData().items().get(0).id();
    }

    // ---> HISTORICAL
    public List<HistoricalMissionMeasurementContinuityQuery.Item> getHistoricalMissionMeasurementContinuity(){
        HistoricalMissionMeasurementContinuityQuery iaQuery = HistoricalMissionMeasurementContinuityQuery.builder()
                .build();
        ApolloCall<HistoricalMissionMeasurementContinuityQuery.Data>           apolloCall  = this.apollo.query(iaQuery);
        Observable<Response<HistoricalMissionMeasurementContinuityQuery.Data>> observable  = Rx2Apollo.from(apolloCall);
        return observable.blockingFirst().getData().items();
    }




    // ---> ARCHITECTURE
    public int updateArchitecture(String input, Integer datasetId, double science, double cost, boolean ga){
        UpdateArchitectureMutation archMutation = UpdateArchitectureMutation.builder()
                .user_id(this.userId)
                .problem_id(this.problemId)
                .dataset_id(datasetId)
                .input(input)
                .science(science)
                .cost(cost)
                .eval_status(true)
                .improve_hv(false)
                .build();
        ApolloCall<UpdateArchitectureMutation.Data>           apolloCall  = this.apollo.mutate(archMutation);
        Observable<Response<UpdateArchitectureMutation.Data>> observable  = Rx2Apollo.from(apolloCall);

        return observable.blockingFirst().getData().items().returning().get(0).id();
    }

    public int insertArchitecture(String input, Integer datasetId, double science, double cost, boolean ga){
        InsertArchitectureMutation archMutation = InsertArchitectureMutation.builder()
                .user_id(this.userId)
                .problem_id(this.problemId)
                .dataset_id(datasetId)
                .input(input)
                .science(science)
                .cost(cost)
                .eval_status(true)
                .ga(ga)
                .improve_hv(false)
                .build();
        ApolloCall<InsertArchitectureMutation.Data>           apolloCall  = this.apollo.mutate(archMutation);
        Observable<Response<InsertArchitectureMutation.Data>> observable  = Rx2Apollo.from(apolloCall);

        return observable.blockingFirst().getData().items().id();
    }

    public int insertArchitectureScoreExplanation(int archID, int panelID, double satisfaction){
        InsertArchitectureScoreExplanationMutation mutation = InsertArchitectureScoreExplanationMutation.builder()
                .architecture_id(archID)
                .panel_id(panelID)
                .satisfaction(satisfaction)
                .build();
        ApolloCall<InsertArchitectureScoreExplanationMutation.Data>           apolloCall  = this.apollo.mutate(mutation);
        Observable<Response<InsertArchitectureScoreExplanationMutation.Data>> observable  = Rx2Apollo.from(apolloCall);

        return observable.blockingFirst().getData().items().id();
    }

    public int insertPanelScoreExplanation(int archID, int objectiveID, double satisfaction){
        InsertPanelScoreExplanationMutation mutation = InsertPanelScoreExplanationMutation.builder()
                .architecture_id(archID)
                .objective_id(objectiveID)
                .satisfaction(satisfaction)
                .build();
        ApolloCall<InsertPanelScoreExplanationMutation.Data>           apolloCall  = this.apollo.mutate(mutation);
        Observable<Response<InsertPanelScoreExplanationMutation.Data>> observable  = Rx2Apollo.from(apolloCall);

        return observable.blockingFirst().getData().items().id();
    }

    public int insertObjectiveScoreExplanation(int archID, int subobjectiveID, double satisfaction){
        InsertObjectiveScoreExplanationMutation mutation = InsertObjectiveScoreExplanationMutation.builder()
                .architecture_id(archID)
                .subobjective_id(subobjectiveID)
                .satisfaction(satisfaction)
                .build();
        ApolloCall<InsertObjectiveScoreExplanationMutation.Data>           apolloCall  = this.apollo.mutate(mutation);
        Observable<Response<InsertObjectiveScoreExplanationMutation.Data>> observable  = Rx2Apollo.from(apolloCall);

        return observable.blockingFirst().getData().items().id();
    }

    public int insertArchitectureScoreExplanationBatch(ArrayList<ArchitectureScoreExplanation_insert_input> items){
        InsertArchitectureScoreExplanationBatchMutation mutation = InsertArchitectureScoreExplanationBatchMutation.builder()
                .objects(items)
                .build();
        ApolloCall<InsertArchitectureScoreExplanationBatchMutation.Data>           apolloCall  = this.apollo.mutate(mutation);
        Observable<Response<InsertArchitectureScoreExplanationBatchMutation.Data>> observable  = Rx2Apollo.from(apolloCall);

        return observable.blockingFirst().getData().items().affected_rows();
    }

    public int insertPanelScoreExplanationBatch(ArrayList<PanelScoreExplanation_insert_input> items){
        InsertPanelScoreExplanationBatchMutation mutation = InsertPanelScoreExplanationBatchMutation.builder()
                .objects(items)
                .build();
        ApolloCall<InsertPanelScoreExplanationBatchMutation.Data>           apolloCall  = this.apollo.mutate(mutation);
        Observable<Response<InsertPanelScoreExplanationBatchMutation.Data>> observable  = Rx2Apollo.from(apolloCall);
        return observable.blockingFirst().getData().items().affected_rows();
    }

    public int insertObjectiveScoreExplanationBatch(ArrayList<ObjectiveScoreExplanation_insert_input> items){
        InsertObjectiveScoreExplanationBatchMutation mutation = InsertObjectiveScoreExplanationBatchMutation.builder()
                .objects(items)
                .build();
        ApolloCall<InsertObjectiveScoreExplanationBatchMutation.Data>           apolloCall  = this.apollo.mutate(mutation);
        Observable<Response<InsertObjectiveScoreExplanationBatchMutation.Data>> observable  = Rx2Apollo.from(apolloCall);

        return observable.blockingFirst().getData().items().affected_rows();
    }

    public int insertSubobjectiveScoreExplanationBatch(ArrayList<SubobjectiveScoreExplanation_insert_input> items){
        InsertSubobjectiveScoreExplanationBatchMutation mutation = InsertSubobjectiveScoreExplanationBatchMutation.builder()
                .objects(items)
                .build();
        ApolloCall<InsertSubobjectiveScoreExplanationBatchMutation.Data>           apolloCall  = this.apollo.mutate(mutation);
        Observable<Response<InsertSubobjectiveScoreExplanationBatchMutation.Data>> observable  = Rx2Apollo.from(apolloCall);

        return observable.blockingFirst().getData().items().affected_rows();
    }

    public List<RequirementRulesForSubobjectiveQuery.Item> getRequirementRulesForSubobjective(String subobjectiveName){
        RequirementRulesForSubobjectiveQuery query = RequirementRulesForSubobjectiveQuery.builder()
                .problem_id(this.problemId)
                .subobjective_name(subobjectiveName)
                .build();
        ApolloCall<RequirementRulesForSubobjectiveQuery.Data>           apolloCall  = this.apollo.query(query);
        Observable<Response<RequirementRulesForSubobjectiveQuery.Data>> observable  = Rx2Apollo.from(apolloCall);
        return observable.blockingFirst().getData().items();
    }

    public int insertArchitectureCostInformation(int architecture_id, String mission_name, String launch_vehicle, double mass, double power, double cost, double others){
        InsertArchitectureCostInformationMutation mutation = InsertArchitectureCostInformationMutation.builder()
                .architecture_id(architecture_id)
                .mission_name(mission_name)
                .launch_vehicle(launch_vehicle)
                .mass(mass)
                .power(power)
                .cost(cost)
                .others(others)
                .build();
        ApolloCall<InsertArchitectureCostInformationMutation.Data>           apolloCall  = this.apollo.mutate(mutation);
        Observable<Response<InsertArchitectureCostInformationMutation.Data>> observable  = Rx2Apollo.from(apolloCall);
        return observable.blockingFirst().getData().items().id();
    }

    public int insertArchitecturePayloadBatch(ArrayList<ArchitecturePayload_insert_input> items){
        InsertArchitecturePayloadBatchMutation mutation = InsertArchitecturePayloadBatchMutation.builder()
                .objects(items)
                .build();
        ApolloCall<InsertArchitecturePayloadBatchMutation.Data>           apolloCall  = this.apollo.mutate(mutation);
        Observable<Response<InsertArchitecturePayloadBatchMutation.Data>> observable  = Rx2Apollo.from(apolloCall);
        return observable.blockingFirst().getData().items().affected_rows();
    }

    public int insertArchitectureBudgetBatch(ArrayList<ArchitectureBudget_insert_input> items){
        InsertArchitectureBudgetBatchMutation mutation = InsertArchitectureBudgetBatchMutation.builder()
                .objects(items)
                .build();
        ApolloCall<InsertArchitectureBudgetBatchMutation.Data>           apolloCall  = this.apollo.mutate(mutation);
        Observable<Response<InsertArchitectureBudgetBatchMutation.Data>> observable  = Rx2Apollo.from(apolloCall);
        return observable.blockingFirst().getData().items().affected_rows();
    }

    public void deleteArchitectureScoreExplanations(int archID){
        DeleteScoreExplanationsMutation mutation = DeleteScoreExplanationsMutation.builder()
                .arch_id(archID)
                .build();
        ApolloCall<DeleteScoreExplanationsMutation.Data>           apolloCall  = this.apollo.mutate(mutation);
        Observable<Response<DeleteScoreExplanationsMutation.Data>> observable  = Rx2Apollo.from(apolloCall);
        observable.blockingFirst().getData();
    }

    public List<ArchitectureCostInformationQuery.Item> queryArchitectureCostInformation(int archID){
        ArchitectureCostInformationQuery query = ArchitectureCostInformationQuery.builder()
                .arch_id(archID)
                .build();
        ApolloCall<ArchitectureCostInformationQuery.Data>           apolloCall  = this.apollo.query(query);
        Observable<Response<ArchitectureCostInformationQuery.Data>> observable  = Rx2Apollo.from(apolloCall);
        return observable.blockingFirst().getData().items();
    }

    public void deleteArchitectureCostInformationFK(int arch_cost_id){
        DeleteArchitecturePayloadBudgetMutation mutation = DeleteArchitecturePayloadBudgetMutation.builder()
                .arch_cost_id(arch_cost_id)
                .build();
        ApolloCall<DeleteArchitecturePayloadBudgetMutation.Data>           apolloCall  = this.apollo.mutate(mutation);
        Observable<Response<DeleteArchitecturePayloadBudgetMutation.Data>> observable  = Rx2Apollo.from(apolloCall);
        observable.blockingFirst().getData();
    }

    public void deleteArchitectureCostInformationPK(int arch_cost_id){
        DeleteArchitectureCostInformationPKMutation mutation = DeleteArchitectureCostInformationPKMutation.builder()
                .arch_cost_id(arch_cost_id)
                .build();
        ApolloCall<DeleteArchitectureCostInformationPKMutation.Data>           apolloCall  = this.apollo.mutate(mutation);
        Observable<Response<DeleteArchitectureCostInformationPKMutation.Data>> observable  = Rx2Apollo.from(apolloCall);
        observable.blockingFirst().getData();
    }

    public void updateArchitectureCritique(int archID, String critique){
        UpdateArchitectureCritiqueMutation mutation = UpdateArchitectureCritiqueMutation.builder()
                .arch_id(archID)
                .critique(critique)
                .build();
        ApolloCall<UpdateArchitectureCritiqueMutation.Data>           apolloCall  = this.apollo.mutate(mutation);
        Observable<Response<UpdateArchitectureCritiqueMutation.Data>> observable  = Rx2Apollo.from(apolloCall);
        observable.blockingFirst().getData();
    }



    public boolean doesArchitectureExist(String input){
        ArchitectureCountQuery countQuery = ArchitectureCountQuery.builder()
                .problem_id(this.problemId)
                .input(input)
                .build();
        ApolloCall<ArchitectureCountQuery.Data>           apolloCall  = this.apollo.query(countQuery);
        Observable<Response<ArchitectureCountQuery.Data>> observable  = Rx2Apollo.from(apolloCall);

        int count = observable.blockingFirst().getData().items().aggregate().count();
        if(count > 0){
            return true;
        }
        else{
            return false;
        }
    }





    // ---> SUBSCRIPTIONS
    public ApolloSubscriptionCall<InstrumentSubscription.Data> subscribeToInstruments(){
        System.out.println("-----> subscribeToInstruments");

        InstrumentSubscription sub = InstrumentSubscription.builder()
                .problem_id(this.problemId)
                .build();

        ApolloSubscriptionCall<InstrumentSubscription.Data> subCall = this.apollo.subscribe(sub);

        subCall.execute( new ApolloSubscriptionCall.Callback<>() {
            @Override
            public void onResponse(@NotNull Response<InstrumentSubscription.Data> response) {
                System.out.println("-----> INSTRUMENT CHANGE: REBUILD");
                final Map<String, String> messageAttributes = new HashMap<>();
                messageAttributes.put("msgType", "build");
                messageAttributes.put("group_id", "-1");
                messageAttributes.put("problem_id", "-1");
                privateQueue.add(messageAttributes);
            }

            @Override
            public void onFailure(@NotNull ApolloException e) {
                System.out.println("-----> FAILURE");
            }

            @Override
            public void onCompleted() {
                System.out.println("-----> COMPLETED");
            }

            @Override
            public void onTerminated() {
                System.out.println("-----> TERMINATED");
            }

            @Override
            public void onConnected() {
                System.out.println("-----> CONNECTED");
            }
        });
        return subCall;
    }

    public ApolloSubscriptionCall<InstrumentCharacteristicSubscription.Data> subscribeToInstrumentCharacteristics(){
        System.out.println("-----> subscribeToInstrumentCharacteristics");

        InstrumentCharacteristicSubscription sub = InstrumentCharacteristicSubscription.builder()
                .problem_id(this.problemId)
                .build();

        ApolloSubscriptionCall<InstrumentCharacteristicSubscription.Data> subCall = this.apollo.subscribe(sub);

        subCall.execute( new ApolloSubscriptionCall.Callback<>() {
            @Override
            public void onResponse(@NotNull Response<InstrumentCharacteristicSubscription.Data> response) {
                System.out.println("-----> INSTRUMENT CHARACTERISTIC CHANGE: REBUILD");
                final Map<String, String> messageAttributes = new HashMap<>();
                messageAttributes.put("msgType", "build");
                messageAttributes.put("group_id", "-1");
                messageAttributes.put("problem_id", "-1");
                privateQueue.add(messageAttributes);
            }

            @Override
            public void onFailure(@NotNull ApolloException e) {
                System.out.println("-----> FAILURE");
            }

            @Override
            public void onCompleted() {
                System.out.println("-----> COMPLETED");
            }

            @Override
            public void onTerminated() {
                System.out.println("-----> TERMINATED");
            }

            @Override
            public void onConnected() {
                System.out.println("-----> CONNECTED");
            }
        });
        return subCall;
    }

    public ApolloSubscriptionCall<OrbitSubscription.Data> subscribeToOrbits(){
        System.out.println("-----> subscribeToOrbits");

        OrbitSubscription sub = OrbitSubscription.builder()
                .problem_id(this.problemId)
                .build();

        ApolloSubscriptionCall<OrbitSubscription.Data> subCall = this.apollo.subscribe(sub);

        subCall.execute( new ApolloSubscriptionCall.Callback<>() {
            @Override
            public void onResponse(@NotNull Response<OrbitSubscription.Data> response) {
                System.out.println("-----> ORBIT CHANGE: REBUILD");
                final Map<String, String> messageAttributes = new HashMap<>();
                messageAttributes.put("msgType", "build");
                messageAttributes.put("group_id", "-1");
                messageAttributes.put("problem_id", "-1");
                privateQueue.add(messageAttributes);
            }

            @Override
            public void onFailure(@NotNull ApolloException e) {
                System.out.println("-----> FAILURE");
            }

            @Override
            public void onCompleted() {
                System.out.println("-----> COMPLETED");
            }

            @Override
            public void onTerminated() {
                System.out.println("-----> TERMINATED");
            }

            @Override
            public void onConnected() {
                System.out.println("-----> CONNECTED");
            }
        });
        return subCall;
    }

    public ApolloSubscriptionCall<AggregationRuleSubscription.Data> subscribeToStakeholders(){
        System.out.println("-----> subscribeToStakeholders");

        AggregationRuleSubscription sub = AggregationRuleSubscription.builder()
                .problem_id(this.problemId)
                .build();

        ApolloSubscriptionCall<AggregationRuleSubscription.Data> subCall = this.apollo.subscribe(sub);

        subCall.execute( new ApolloSubscriptionCall.Callback<>() {
            @Override
            public void onResponse(@NotNull Response<AggregationRuleSubscription.Data> response) {
                System.out.println("-----> STEAKHOLDER CHANGE: REBUILD");
                final Map<String, String> messageAttributes = new HashMap<>();
                messageAttributes.put("msgType", "build");
                messageAttributes.put("group_id", "-1");
                messageAttributes.put("problem_id", "-1");
                privateQueue.add(messageAttributes);
            }

            @Override
            public void onFailure(@NotNull ApolloException e) {
                System.out.println("-----> FAILURE");
            }

            @Override
            public void onCompleted() {
                System.out.println("-----> COMPLETED");
            }

            @Override
            public void onTerminated() {
                System.out.println("-----> TERMINATED");
            }

            @Override
            public void onConnected() {
                System.out.println("-----> CONNECTED");
            }
        });
        return subCall;
    }

    public ApolloSubscriptionCall<LaunchVehicleSubscription.Data> subscribeToLaunchVehicles(){
        System.out.println("-----> subscribeToLaunchVehicles");

        LaunchVehicleSubscription sub = LaunchVehicleSubscription.builder()
                .problem_id(this.problemId)
                .build();

        ApolloSubscriptionCall<LaunchVehicleSubscription.Data> subCall = this.apollo.subscribe(sub);

        subCall.execute( new ApolloSubscriptionCall.Callback<>() {
            @Override
            public void onResponse(@NotNull Response<LaunchVehicleSubscription.Data> response) {
                System.out.println("-----> LAUNCH VEHICLE CHANGE: REBUILD");
                final Map<String, String> messageAttributes = new HashMap<>();
                messageAttributes.put("msgType", "build");
                messageAttributes.put("group_id", "-1");
                messageAttributes.put("problem_id", "-1");
                privateQueue.add(messageAttributes);
            }

            @Override
            public void onFailure(@NotNull ApolloException e) {
                System.out.println("-----> FAILURE");
            }

            @Override
            public void onCompleted() {
                System.out.println("-----> COMPLETED");
            }

            @Override
            public void onTerminated() {
                System.out.println("-----> TERMINATED");
            }

            @Override
            public void onConnected() {
                System.out.println("-----> CONNECTED");
            }
        });
        return subCall;
    }

    public void cancelAllSubscriptions() {
        this.apollo.disableSubscriptions();
    }
}
