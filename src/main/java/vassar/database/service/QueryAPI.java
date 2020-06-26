package vassar.database.service;

import com.apollographql.apollo.ApolloClient;
import com.evaluator.*;
import okhttp3.OkHttpClient;


// APOLLO
import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.api.Response;

// RxJava2
import com.apollographql.apollo.rx2.Rx2Apollo;
import io.reactivex.Observable;

// QUERIES


import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class QueryAPI {

    private ApolloClient apollo;
    private OkHttpClient http;
    private String       apollo_url;     // = "http://graphql:8080/v1/graphql";
    public  int          group_id;
    public  int          problem_id;

    public static class Builder {

        private ApolloClient apollo;
        private OkHttpClient http;
        private String       apollo_url;     // = "http://graphql:8080/v1/graphql";
        private int          group_id;
        private int          problem_id;

        public Builder(String apollo_url){
            this.apollo_url = apollo_url;
            this.http       = new OkHttpClient.Builder().build();
            this.apollo     = ApolloClient.builder().serverUrl(this.apollo_url).okHttpClient(this.http).build();
        }

        public Builder groupID(int group_id){
            this.group_id = group_id;
            return this;
        }

        public Builder problemID(int problem_id){
            this.problem_id = problem_id;
            return this;
        }

        public QueryAPI build(){
            QueryAPI client = new QueryAPI();

            client.apollo      = this.apollo;
            client.http        = this.http;     // = "http://graphql:8080/v1/graphql";
            client.apollo_url  = this.apollo_url; // = "/app/logs/jessInitDB.json"; ???
            client.group_id    = this.group_id;
            client.problem_id  = this.problem_id;

            return client;
        }

    }


    // QUERIES
    public List<OrbitInformationQuery.Item> orbitQuery(){
        OrbitInformationQuery orbitQuery = OrbitInformationQuery.builder()
                                                                .problem_id(this.problem_id)
                                                                .build();
        ApolloCall<OrbitInformationQuery.Data>           apolloCall  = this.apollo.query(orbitQuery);
        Observable<Response<OrbitInformationQuery.Data>> observable  = Rx2Apollo.from(apolloCall);
        return observable.blockingFirst().getData().items();
    }

    public List<LaunchVehicleInformationQuery.Item> launchVehicleQuery(){
        LaunchVehicleInformationQuery lvQuery = LaunchVehicleInformationQuery.builder()
                                                                                .group_id(this.group_id)
                                                                                .problem_id(this.problem_id)
                                                                                .build();
        ApolloCall<LaunchVehicleInformationQuery.Data>           apolloCall  = this.apollo.query(lvQuery);
        Observable<Response<LaunchVehicleInformationQuery.Data>> observable  = Rx2Apollo.from(apolloCall);
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

    public List<OrbitAttributeQuery.Item> orbitAttributeQuery(){
        OrbitAttributeQuery iaQuery = OrbitAttributeQuery.builder()
                                                        .group_id(this.group_id)
                                                        .build();
        ApolloCall<OrbitAttributeQuery.Data>           apolloCall  = this.apollo.query(iaQuery);
        Observable<Response<OrbitAttributeQuery.Data>> observable  = Rx2Apollo.from(apolloCall);
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

    public List<InstrumentQuery.Item> instrumentQuery(){
        InstrumentQuery iaQuery = InstrumentQuery.builder()
                                                .problem_id(this.problem_id)
                                                .build();
        ApolloCall<InstrumentQuery.Data>           apolloCall  = this.apollo.query(iaQuery);
        Observable<Response<InstrumentQuery.Data>> observable  = Rx2Apollo.from(apolloCall);
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

    public List<RequirementRuleCaseQuery.Item> requirementRuleCaseQuery(){
        RequirementRuleCaseQuery iaQuery = RequirementRuleCaseQuery.builder()
                .problem_id(this.problem_id)
                .build();
        ApolloCall<RequirementRuleCaseQuery.Data>           apolloCall  = this.apollo.query(iaQuery);
        Observable<Response<RequirementRuleCaseQuery.Data>> observable  = Rx2Apollo.from(apolloCall);
        return observable.blockingFirst().getData().items();
    }

    public List<CapabilityRuleQuery.Item> capabilityRuleQuery(){
        CapabilityRuleQuery iaQuery = CapabilityRuleQuery.builder()
                .problem_id(this.problem_id)
                .group_id(this.group_id)
                .build();
        ApolloCall<CapabilityRuleQuery.Data>           apolloCall  = this.apollo.query(iaQuery);
        Observable<Response<CapabilityRuleQuery.Data>> observable  = Rx2Apollo.from(apolloCall);

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

    public List<AggregationRuleQuery.Item> aggregationRuleQuery(){
        AggregationRuleQuery iaQuery = AggregationRuleQuery.builder()
                .problem_id(this.problem_id)
                .build();
        ApolloCall<AggregationRuleQuery.Data>           apolloCall  = this.apollo.query(iaQuery);
        Observable<Response<AggregationRuleQuery.Data>> observable  = Rx2Apollo.from(apolloCall);

        return observable.blockingFirst().getData().items();
    }

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

    public int insertArchitecture(String input, double science, double cost){
        InsertArchitectureMutation archMutation = InsertArchitectureMutation.builder()
                .problem_id(this.problem_id)
                .input(input)
                .science(science)
                .cost(cost)
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


}
