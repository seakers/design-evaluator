package vassar.database.service;

import com.apollographql.apollo.ApolloClient;
import com.apollographql.apollo.ApolloSubscriptionCall;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.subscription.WebSocketSubscriptionTransport;
import com.evaluator.*;
import okhttp3.OkHttpClient;


// APOLLO
import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.api.Response;

// RxJava2
import com.apollographql.apollo.rx2.Rx2Apollo;
import io.reactivex.Observable;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

// QUERIES


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class QueryAPI {

    private ApolloClient apollo;
    private OkHttpClient http;
    private String       apollo_url;     // = "http://graphql:8080/v1/graphql";
    public  int          group_id;
    public  int          problem_id;

    private final String    private_queue_url;
    private final SqsClient client;

    public QueryAPI(String private_queue_url, SqsClient client) {
        this.private_queue_url = private_queue_url;
        this.client = client;
    }

    public static class Builder {

        private ApolloClient apollo;
        private OkHttpClient http;
        private SqsClient    client;
        private String       apollo_url;     // = "http://graphql:8080/v1/graphql";
        private String       private_queue_url;
        private int          group_id;
        private int          problem_id;

        public Builder(String apollo_url){
            this.apollo_url = apollo_url;
            this.http       = new OkHttpClient.Builder().build();
            this.apollo     = ApolloClient.builder()
                    .serverUrl(this.apollo_url)
                    .subscriptionTransportFactory(new WebSocketSubscriptionTransport.Factory("ws://graphql:8080/v1/graphql", this.http))
                    .okHttpClient(this.http)
                    .build();
        }

        public Builder groupID(int group_id){
            this.group_id = group_id;
            return this;
        }

        public Builder privateQueue(String private_queue_url){
            this.private_queue_url = private_queue_url;
            return this;
        }

        public Builder sqsClient(SqsClient client){
            this.client = client;
            return this;
        }

        public Builder problemID(int problem_id){
            this.problem_id = problem_id;
            return this;
        }

        public QueryAPI build(){
            QueryAPI client = new QueryAPI(this.private_queue_url, this.client);

            client.apollo            = this.apollo;
            client.http              = this.http;       // = "http://graphql:8080/v1/graphql";
            client.apollo_url        = this.apollo_url; // = "/app/logs/jessInitDB.json"; ???
            client.group_id          = this.group_id;
            client.problem_id        = this.problem_id;

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
                .problem_id(this.problem_id)
                .build();
        ApolloCall<InstrumentQuery.Data>           apolloCall  = this.apollo.query(iaQuery);
        Observable<Response<InstrumentQuery.Data>> observable  = Rx2Apollo.from(apolloCall);
        return observable.blockingFirst().getData().items();
    }

    public List<ProblemInstrumentsQuery.Item> problemInstrumentQuery(){
        ProblemInstrumentsQuery iaQuery = ProblemInstrumentsQuery.builder()
                .problem_id(this.problem_id)
                .build();
        ApolloCall<ProblemInstrumentsQuery.Data>           apolloCall  = this.apollo.query(iaQuery);
        Observable<Response<ProblemInstrumentsQuery.Data>> observable  = Rx2Apollo.from(apolloCall);
        return observable.blockingFirst().getData().items();
    }

    public List<EnabledInstrumentsQuery.Item> enabledInstrumentQuery(){
        EnabledInstrumentsQuery iaQuery = EnabledInstrumentsQuery.builder()
                .problem_id(this.problem_id)
                .build();
        ApolloCall<EnabledInstrumentsQuery.Data>           apolloCall  = this.apollo.query(iaQuery);
        Observable<Response<EnabledInstrumentsQuery.Data>> observable  = Rx2Apollo.from(apolloCall);

        return observable.blockingFirst().getData().items();
    }

    public List<MeasurementAttributeQuery.Item> measurementAttributeQuery(){
        MeasurementAttributeQuery maQuery = MeasurementAttributeQuery.builder()
                .group_id(this.group_id)
                .build();
        ApolloCall<MeasurementAttributeQuery.Data>           apolloCall  = this.apollo.query(maQuery);
        Observable<Response<MeasurementAttributeQuery.Data>> observable  = Rx2Apollo.from(apolloCall);
        return Objects.requireNonNull(observable.blockingFirst().getData()).items();
    }

    public List<InstrumentAttributeQuery.Item> instrumentAttributeQuery(){
        InstrumentAttributeQuery iaQuery = InstrumentAttributeQuery.builder()
                .group_id(this.group_id)
                .build();
        ApolloCall<InstrumentAttributeQuery.Data>           apolloCall  = this.apollo.query(iaQuery);
        Observable<Response<InstrumentAttributeQuery.Data>> observable  = Rx2Apollo.from(apolloCall);
        return observable.blockingFirst().getData().items();
    }





    // ----> ORBIT
    public List<ProblemOrbitsQuery.Item> problemOrbitQuery(){
        ProblemOrbitsQuery orbitQuery = ProblemOrbitsQuery.builder()
                .problem_id(this.problem_id)
                .build();
        ApolloCall<ProblemOrbitsQuery.Data>           apolloCall  = this.apollo.query(orbitQuery);
        Observable<Response<ProblemOrbitsQuery.Data>> observable  = Rx2Apollo.from(apolloCall);
        return observable.blockingFirst().getData().items();
    }

    public List<ProblemOrbitJoinQuery.Item> problemOrbitJoin(){
        ProblemOrbitJoinQuery orbitQuery = ProblemOrbitJoinQuery.builder()
                .problem_id(this.problem_id)
                .build();
        ApolloCall<ProblemOrbitJoinQuery.Data>           apolloCall  = this.apollo.query(orbitQuery);
        Observable<Response<ProblemOrbitJoinQuery.Data>> observable  = Rx2Apollo.from(apolloCall);
        return observable.blockingFirst().getData().items();
    }

    public List<OrbitAttributeQuery.Item> orbitAttributeQuery(){
        OrbitAttributeQuery iaQuery = OrbitAttributeQuery.builder()
                .group_id(this.group_id)
                .build();
        ApolloCall<OrbitAttributeQuery.Data>           apolloCall  = this.apollo.query(iaQuery);
        Observable<Response<OrbitAttributeQuery.Data>> observable  = Rx2Apollo.from(apolloCall);
        return observable.blockingFirst().getData().items();
    }





    // ---> LAUNCH VEHICLE

    // remove
    public List<ProblemLaunchVehicleQuery.Item> problemLaunchVehicleQuery(){
        ProblemLaunchVehicleQuery iaQuery = ProblemLaunchVehicleQuery.builder()
                .problem_id(this.problem_id)
                .build();
        ApolloCall<ProblemLaunchVehicleQuery.Data>           apolloCall  = this.apollo.query(iaQuery);
        Observable<Response<ProblemLaunchVehicleQuery.Data>> observable  = Rx2Apollo.from(apolloCall);
        return observable.blockingFirst().getData().items();
    }

    public List<LaunchVehicleAttributeQuery.Item> launchVehicleAttributeQuery(){
        LaunchVehicleAttributeQuery iaQuery = LaunchVehicleAttributeQuery.builder()
                                                                        .group_id(this.group_id)
                                                                        .build();
        ApolloCall<LaunchVehicleAttributeQuery.Data>           apolloCall  = this.apollo.query(iaQuery);
        Observable<Response<LaunchVehicleAttributeQuery.Data>> observable  = Rx2Apollo.from(apolloCall);
        return observable.blockingFirst().getData().items();
    }





    // ---> RULES
    public List<AggregationRuleQuery.Item> aggregationRuleQuery(){
        AggregationRuleQuery iaQuery = AggregationRuleQuery.builder()
                .problem_id(this.problem_id)
                .build();
        ApolloCall<AggregationRuleQuery.Data>           apolloCall  = this.apollo.query(iaQuery);
        Observable<Response<AggregationRuleQuery.Data>> observable  = Rx2Apollo.from(apolloCall);

        return observable.blockingFirst().getData().items();
    }

    public List<RequirementRuleCaseQuery.Item> requirementRuleCaseQuery(){
        RequirementRuleCaseQuery iaQuery = RequirementRuleCaseQuery.builder()
                .problem_id(this.problem_id)
                .build();
        ApolloCall<RequirementRuleCaseQuery.Data>           apolloCall  = this.apollo.query(iaQuery);
        Observable<Response<RequirementRuleCaseQuery.Data>> observable  = Rx2Apollo.from(apolloCall);
        return observable.blockingFirst().getData().items();
    }

    public List<RequirementRuleAttributeQuery.Item> requirementRuleAttributeQuery(){
        RequirementRuleAttributeQuery iaQuery = RequirementRuleAttributeQuery.builder()
                .problem_id(this.problem_id)
                .build();
        ApolloCall<RequirementRuleAttributeQuery.Data>           apolloCall  = this.apollo.query(iaQuery);
        Observable<Response<RequirementRuleAttributeQuery.Data>> observable  = Rx2Apollo.from(apolloCall);

        return observable.blockingFirst().getData().items();
    }

    public List<CapabilityRuleQuery.Item> capabilityRuleQuery(List<Integer> instList){
        CapabilityRuleQuery iaQuery = CapabilityRuleQuery.builder()
        //        .problem_id(this.problem_id)
                .group_id(this.group_id)
                .inst_ids(instList)
                .build();
        ApolloCall<CapabilityRuleQuery.Data>           apolloCall  = this.apollo.query(iaQuery);
        Observable<Response<CapabilityRuleQuery.Data>> observable  = Rx2Apollo.from(apolloCall);

        return observable.blockingFirst().getData().items();
    }





    // ---> MISSION
    public List<MissionAttributeQuery.Item> missionAttributeQuery(){
        MissionAttributeQuery iaQuery = MissionAttributeQuery.builder()
                                                            .problem_id(this.problem_id)
                                                            .build();
        ApolloCall<MissionAttributeQuery.Data>           apolloCall  = this.apollo.query(iaQuery);
        Observable<Response<MissionAttributeQuery.Data>> observable  = Rx2Apollo.from(apolloCall);
        return observable.blockingFirst().getData().items();
    }

    public List<AttributeInheritanceQuery.Item> attributeInheritanceQuery(){
        AttributeInheritanceQuery iaQuery = AttributeInheritanceQuery.builder()
                .problem_id(this.problem_id)
                .build();
        ApolloCall<AttributeInheritanceQuery.Data>           apolloCall  = this.apollo.query(iaQuery);
        Observable<Response<AttributeInheritanceQuery.Data>> observable  = Rx2Apollo.from(apolloCall);
        return observable.blockingFirst().getData().items();
    }

    public List<FuzzyAttributeQuery.Item> fuzzyAttributeQuery(){
        FuzzyAttributeQuery iaQuery = FuzzyAttributeQuery.builder()
                .problem_id(this.problem_id)
                .build();
        ApolloCall<FuzzyAttributeQuery.Data>           apolloCall  = this.apollo.query(iaQuery);
        Observable<Response<FuzzyAttributeQuery.Data>> observable  = Rx2Apollo.from(apolloCall);
        return observable.blockingFirst().getData().items();
    }




    // ---> STAKEHOLDERS
    public int getPanelID(String panel){
        PanelIdQuery idQuery = PanelIdQuery.builder()
                .problem_id(this.problem_id)
                .name(panel)
                .build();
        ApolloCall<PanelIdQuery.Data>           apolloCall  = this.apollo.query(idQuery);
        Observable<Response<PanelIdQuery.Data>> observable  = Rx2Apollo.from(apolloCall);

        return observable.blockingFirst().getData().items().get(0).id();
    }

    public int getObjectiveID(String objective){
        ObjectiveIdQuery idQuery = ObjectiveIdQuery.builder()
                .problem_id(this.problem_id)
                .name(objective)
                .build();
        ApolloCall<ObjectiveIdQuery.Data>           apolloCall  = this.apollo.query(idQuery);
        Observable<Response<ObjectiveIdQuery.Data>> observable  = Rx2Apollo.from(apolloCall);

        return observable.blockingFirst().getData().items().get(0).id();
    }

    public int getSubobjectiveID(String subobjective){
        SubobjectiveIdQuery idQuery = SubobjectiveIdQuery.builder()
                .problem_id(this.problem_id)
                .name(subobjective)
                .build();
        ApolloCall<SubobjectiveIdQuery.Data>           apolloCall  = this.apollo.query(idQuery);
        Observable<Response<SubobjectiveIdQuery.Data>> observable  = Rx2Apollo.from(apolloCall);

        return observable.blockingFirst().getData().items().get(0).id();
    }




    // ---> ARCHITECTURE
    public int updateArchitecture(String input, double science, double cost, boolean ga){
        UpdateArchitectureMutation archMutation = UpdateArchitectureMutation.builder()
                .problem_id(this.problem_id)
                .input(input)
                .science(science)
                .cost(cost)
                .eval_status(true)
                .build();
        ApolloCall<UpdateArchitectureMutation.Data>           apolloCall  = this.apollo.mutate(archMutation);
        Observable<Response<UpdateArchitectureMutation.Data>> observable  = Rx2Apollo.from(apolloCall);

        return observable.blockingFirst().getData().items().returning().get(0).id();
    }

    public int insertArchitecture(String input, double science, double cost, boolean ga){
        InsertArchitectureMutation archMutation = InsertArchitectureMutation.builder()
                .problem_id(this.problem_id)
                .input(input)
                .science(science)
                .cost(cost)
                .eval_status(true)
                .ga(ga)
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

    public boolean doesArchitectureExist(String input){
        ArchitectureCountQuery countQuery = ArchitectureCountQuery.builder()
                .problem_id(this.problem_id)
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
    public ApolloSubscriptionCall subscribeToInstruments(){
        System.out.println("\n\n-----> subscribeToInstruments");

        InstrumentSubscription sub = InstrumentSubscription.builder()
                .problem_id(this.problem_id)
                .build();

        ApolloSubscriptionCall<InstrumentSubscription.Data> subCall = this.apollo.subscribe(sub);

        subCall.execute( new ApolloSubscriptionCall.Callback<>() {
            @Override
            public void onResponse(@NotNull Response<InstrumentSubscription.Data> response) {
                System.out.println("\n\n\n-----> INSTRUMENT CHANGE: REBUILD");
                final Map<String, MessageAttributeValue> messageAttributes = new HashMap<>();
                messageAttributes.put("msgType",
                        MessageAttributeValue.builder()
                                .dataType("String")
                                .stringValue("build")
                                .build()
                );
                messageAttributes.put("group_id",
                        MessageAttributeValue.builder()
                                .dataType("String")
                                .stringValue("-1")
                                .build()
                );
                messageAttributes.put("problem_id",
                        MessageAttributeValue.builder()
                                .dataType("String")
                                .stringValue("-1")
                                .build()
                );
                client.sendMessage(SendMessageRequest.builder()
                        .queueUrl(private_queue_url)
                        .messageBody("")
                        .messageAttributes(messageAttributes)
                        .delaySeconds(1)
                        .build());
            }

            @Override
            public void onFailure(@NotNull ApolloException e) {
                System.out.println("\n\n\n\n-----> FAILURE");
            }

            @Override
            public void onCompleted() {
                System.out.println("\n\n\n\n-----> COMPLETED");
            }

            @Override
            public void onTerminated() {
                System.out.println("\n\n\n\n-----> TERMINATED");
            }

            @Override
            public void onConnected() {
                System.out.println("\n\n\n\n-----> CONNECTED");
            }
        });
        return subCall;
    }

    public ApolloSubscriptionCall subscribeToOrbits(){
        System.out.println("\n\n-----> subscribeToOrbits");

        OrbitSubscription sub = OrbitSubscription.builder()
                .problem_id(this.problem_id)
                .build();

        ApolloSubscriptionCall<OrbitSubscription.Data> subCall = this.apollo.subscribe(sub);

        subCall.execute( new ApolloSubscriptionCall.Callback<>() {
            @Override
            public void onResponse(@NotNull Response<OrbitSubscription.Data> response) {
                System.out.println("-----> ORBIT CHANGE: REBUILD");
                final Map<String, MessageAttributeValue> messageAttributes = new HashMap<>();
                messageAttributes.put("msgType",
                        MessageAttributeValue.builder()
                                .dataType("String")
                                .stringValue("build")
                                .build()
                );
                messageAttributes.put("group_id",
                        MessageAttributeValue.builder()
                                .dataType("String")
                                .stringValue("-1")
                                .build()
                );
                messageAttributes.put("problem_id",
                        MessageAttributeValue.builder()
                                .dataType("String")
                                .stringValue("-1")
                                .build()
                );
                client.sendMessage(SendMessageRequest.builder()
                        .queueUrl(private_queue_url)
                        .messageBody("")
                        .messageAttributes(messageAttributes)
                        .delaySeconds(1)
                        .build());
            }

            @Override
            public void onFailure(@NotNull ApolloException e) {
                System.out.println("\n\n\n\n-----> FAILURE");
            }

            @Override
            public void onCompleted() {
                System.out.println("\n\n\n\n-----> COMPLETED");
            }

            @Override
            public void onTerminated() {
                System.out.println("\n\n\n\n-----> TERMINATED");
            }

            @Override
            public void onConnected() {
                System.out.println("\n\n\n\n-----> CONNECTED");
            }
        });
        return subCall;
    }


}
