package vassar.jess;

import evaluator.EvaluatorApp;
import vassar.database.template.TemplateRequest;
import vassar.database.template.functions.JessExtension;
import vassar.database.template.request.*;

import java.util.ArrayList;
import java.util.HashMap;

public class Requests {

    private ArrayList<TemplateRequest> requests;
    public HashMap<String, TemplateRequest> requests_map;

    public static class Builder {

        private String jessAppPath;
        private String globalTemplatePath;
        private String globalFunctionPath;
        private String requestMode;
        private ArrayList<String> funcTemplates;

        public Builder() {

        }

        public Builder setGlobalTemplatePath(String globalTemplatePath) {
            this.globalTemplatePath = globalTemplatePath;
            return this;
        }

        public Builder setGlobalFunctionPath(String globalFunctionPath) {
            this.globalFunctionPath = globalFunctionPath;
            return this;
        }

        public Builder setJessAppPath(String jessAppPath) {
            this.jessAppPath = jessAppPath;
            return this;
        }

        public Builder setRequestMode(String requestMode) {
            this.requestMode = requestMode;
            return this;
        }

        public Builder setFunctionTemplates() {
            this.funcTemplates = new ArrayList<>() {{
                add("/update-objective-variable.clp");
                add("/ContainsRegion.clp");
                add("/ContainsBands.clp");
                add("/NumericalToFuzzy.clp");
                add("/RevisitTimeToTemporalResolution.clp");
                add("/FuzzyMax.clp");
                add("/FuzzyMin.clp");
                add("/FuzzyAvg.clp");
                add("/Member.clp");
                add("/ValidOrbit.clp");
                add("/WorthImprovingMeasurement.clp");
                add("/MeasGroup.clp");
                add("/GetMeasGroup.clp");
            }};
            return this;
        }


        public Requests buildOrbits(){
            Requests build = new Requests();

            build.requests_map = new HashMap<>();

            // ---------- ORBIT RULES
            build.requests_map.put("orbit_rules",
                    new BatchFileTemplateRequest.Builder()
                            .templateFilePath(this.jessAppPath + "/orbit_rules.clp")
                            .build()
            );
            build.requests_map.put("orbit_selection_rules",
                    new BatchFileTemplateRequest.Builder()
                            .templateFilePath(this.jessAppPath + "/orbit_selection_rules.clp")
                            .build()
            );
            build.requests.add(
                    new OrbitAttributeTemplateRequest.Builder()
                            .applyExtension(new JessExtension())
                            .templateFilePath(this.globalTemplatePath + "/AttributeTemplateRequest.clp")
                            .templateHeader("DATABASE::Orbit")
                            .build()
            );
            build.requests_map.put("OrbitTemplateRequest",
                    new OrbitTemplateRequest.Builder()
                            .applyExtension(new JessExtension())
                            .templateFilePath(this.globalTemplatePath + "/OrbitTemplateRequest.clp")
                            .orbitHeader("DATABASE::Orbit")
                            .templateHeader("orbit-information-facts")
                            .build()
            );

            return build;
        }



        public Requests build() {
            Requests build = new Requests();




            build.requests = new ArrayList<>();

            // ---------- GLOBALS AND IMPORTS
            build.requests.add(
                    new HeaderTemplateRequest.Builder()
                            .templateFilePath(this.globalTemplatePath + "/HeaderTemplateRequest.clp")
                            .setAppPath(this.jessAppPath)
                            .build()
            );

            // ---------- MODULES
            build.requests.add(
                    new BatchFileTemplateRequest.Builder()
                            .templateFilePath(this.jessAppPath + "/modules.clp")
                            .build()
            );

            // ---------- TEMPLATES
            build.requests.add(
                    new MeasurementTemplateRequest.Builder()
                            .applyExtension(new JessExtension())
                            .templateFilePath(this.globalTemplatePath + "/MeasurementTemplate.clp")
                            .templateHeader("REQUIREMENTS::Measurement")
                            .build()
            );
            build.requests.add(
                    new InstrumentAttributeTemplateRequest.Builder()
                            .applyExtension(new JessExtension())
                            .templateFilePath(this.globalTemplatePath + "/InstrumentAttributeTemplate.clp")
                            .templateHeader("CAPABILITIES::Manifested-instrument")
                            .build()
            );
            build.requests.add(
                    new InstrumentAttributeTemplateRequest.Builder()
                            .applyExtension(new JessExtension())
                            .templateFilePath(this.globalTemplatePath + "/InstrumentAttributeTemplate.clp")
                            .templateHeader("DATABASE::Instrument")
                            .build()
            );
            build.requests.add(
                    new MissionAttributeTemplateRequest.Builder()
                            .applyExtension(new JessExtension())
                            .templateFilePath(this.globalTemplatePath + "/MissionAttributeTemplateRequest.clp")
                            .templateHeader("MANIFEST::Mission")
                            .build()
            );
            build.requests.add(
                    new OrbitAttributeTemplateRequest.Builder()
                            .applyExtension(new JessExtension())
                            .templateFilePath(this.globalTemplatePath + "/AttributeTemplateRequest.clp")
                            .templateHeader("DATABASE::Orbit")
                            .build()
            );
            build.requests.add(
                    new LaunchVehicleAttributeTemplateRequest.Builder()
                            .applyExtension(new JessExtension())
                            .templateFilePath(this.globalTemplatePath + "/AttributeTemplateRequest.clp")
                            .templateHeader("DATABASE::Launch-vehicle")
                            .build()
            );
            build.requests.add(
                    new BatchFileTemplateRequest.Builder()
                            .templateFilePath(this.jessAppPath + "/templates.clp")
                            .build()
            );
//            build.requests.add(
//                    new BatchFileTemplateRequest.Builder()
//                            .templateFilePath(this.jessAppPath + "/mission_template.clp")
//                            .build()
//            );

            // ---------- FUNCTIONS
            build.requests.add(
                    new BatchFileTemplateRequest.Builder()
                            .templateFilePath(this.jessAppPath + "/jess_functions.clp")
                            .build()
            );
            build.requests.add(
                    new BatchFileTemplateRequest.Builder()
                            .templateFilePath(this.jessAppPath + "/functions.clp")
                            .build()
            );
            for (String funcFile : this.funcTemplates) {
                build.requests.add(
                        new TemplateRequest.Builder()
                                .templateFilePath(this.globalFunctionPath + funcFile)
                                .build()
                );
            }

            // ---------- ORDERED DEFFACTS
            build.requests.add(
                    new OrbitTemplateRequest.Builder()
                            .applyExtension(new JessExtension())
                            .templateFilePath(this.globalTemplatePath + "/OrbitTemplateRequest.clp")
                            .orbitHeader("DATABASE::Orbit")
                            .templateHeader("orbit-information-facts")
                            .build()
            );
            build.requests.add(
                    new LaunchVehicleTemplateRequest.Builder()
                            .applyExtension(new JessExtension())
                            .templateFilePath(this.globalTemplatePath + "/LaunchVehicleTemplateRequest.clp")
                            .launchVehicleHeader("DATABASE::Launch-vehicle")
                            .templateHeader("DATABASE::launch-vehicle-information-facts")
                            .build()
            );

            // ---------- UNORDERED DEFFACTS
            build.requests.add(
                    new InstrumentTemplateRequest.Builder()
                            .applyExtension(new JessExtension())
                            .templateFilePath(this.globalTemplatePath + "/InstrumentTemplate.clp")
                            .templateHeader("instrument-database-facts")
                            .instrumentHeader("DATABASE::Instrument")
                            .build()
            );

            // ---------- ATTRIBUTE INHERITANCE RULES
            build.requests.add(
                    new BatchFileTemplateRequest.Builder()
                            .templateFilePath(this.jessAppPath + "/attribute_inheritance_rules_not_scuffed.clp")
                            .build()
            );
//            build.requests.add(
//                    new BatchFileTemplateRequest.Builder()
//                            .templateFilePath(this.jessAppPath + "/attribute_inheritance_rules.clp")
//                            .build()
//            );
            build.requests.add(
                    new AttributeInheritanceTemplateRequest.Builder()
                            .applyExtension(new JessExtension())
                            .templateFilePath(this.globalTemplatePath + "/AttributeInheritanceTemplate.clp")
                            .build()
            );

            // ---------- ORBIT RULES
            build.requests.add(
                    new BatchFileTemplateRequest.Builder()
                            .templateFilePath(this.jessAppPath + "/orbit_rules.clp")
                            .build()
            );
            build.requests.add(
                    new BatchFileTemplateRequest.Builder()
                            .templateFilePath(this.jessAppPath + "/orbit_selection_rules.clp")
                            .build()
            );

            // ----------- MATLAB
//            build.requests.add(
//                    new BatchFileTemplateRequest.Builder()
//                            .templateFilePath(this.jessAppPath + "/matlab_rules.clp")
//                            .build()
//            );

            // ---------- MASS BUDGET RULES
            build.requests.add(
                    new BatchFileTemplateRequest.Builder()
                            .templateFilePath(this.jessAppPath + "/mass_budget_rules.clp")
                            .build()
            );
            build.requests.add(
                    new BatchFileTemplateRequest.Builder()
                            .templateFilePath(this.jessAppPath + "/subsystem_mass_budget_rules.clp")
                            .build()
            );
            build.requests.add(
                    new BatchFileTemplateRequest.Builder()
                            .templateFilePath(this.jessAppPath + "/deltaV_budget_rules.clp")
                            .build()
            );

            // ---------- SPACECRAFT DESIGN RULES
            build.requests.add(
                    new BatchFileTemplateRequest.Builder()
                            .templateFilePath(this.jessAppPath + "/eps_design_rules.clp")
                            .build()
            );
            build.requests.add(
                    new BatchFileTemplateRequest.Builder()
                            .templateFilePath(this.jessAppPath + "/adcs_design_rules.clp")
                            .build()
            );
            build.requests.add(
                    new BatchFileTemplateRequest.Builder()
                            .templateFilePath(this.jessAppPath + "/propulsion_design_rules.clp")
                            .build()
            );

            // ---------- COST ESTIMATION RULES
            if ( this.requestMode.equalsIgnoreCase("FUZZY-CASES") || this.requestMode.equalsIgnoreCase("FUZZY-ATTRIBUTES")) {
                build.requests.add(
                        new BatchFileTemplateRequest.Builder()
                                .templateFilePath(this.jessAppPath + "/fuzzy_cost_estimation_rules.clp")
                                .build()
                );
            }
            else {
                build.requests.add(
                        new BatchFileTemplateRequest.Builder()
                                .templateFilePath(this.jessAppPath + "/cost_estimation_rules.clp")
                                .build()
                );
                build.requests.add(
                        new BatchFileTemplateRequest.Builder()
                                .templateFilePath(this.jessAppPath + "/fuzzy_cost_estimation_rules.clp")
                                .build()
                );
            }
            build.requests.add(
                    new BatchFileTemplateRequest.Builder()
                            .templateFilePath(this.jessAppPath + "/cost_estimation_rules.clp")
                            .build()
            );

            // ---------- LAUNCH VEHICLE SELECTION RULES
            build.requests.add(
                    new BatchFileTemplateRequest.Builder()
                            .templateFilePath(this.jessAppPath + "/launch_cost_estimation_rules.clp")
                            .build()
            );

            // ---------- FUZZY ATTRIBUTE RULES
            build.requests.add(
                    new FuzzyAttributeTemplateRequest.Builder()
                            .applyExtension(new JessExtension())
                            .templateFilePath(this.globalTemplatePath + "/FuzzyAttributeTemplate.clp")
                            .build()
            );

            // 14 ---------- REQUIREMENT RULES
            if ( this.requestMode.equalsIgnoreCase("CRISP-ATTRIBUTES") ) {
                build.requests.add(
                        new RequirementRuleAttributeTemplateRequest.Builder()
                                .templateFilePath(this.globalTemplatePath + "/RequirementRuleAttributeTemplate.clp")
                                .setWrapperTemplatePath(this.globalTemplatePath + "/RequirementRuleAttributeTemplateWrapper.clp")
                                .build()
                );
            }
            if ( this.requestMode.equalsIgnoreCase("CRISP-CASES") ) {
                build.requests.add(
                        new RequirementRuleCaseTemplateRequest.Builder()
                                .templateFilePath(this.globalTemplatePath + "/RequirementRuleCaseTemplate.clp")
                                .build()
                );
            }
//            else if ( this.requestMode.equalsIgnoreCase("FUZZY-ATTRIBUTES") ) {
//                build.requests.add(
//                        new RequirementRuleAttributeTemplateRequest.Builder()
//                                .templateFilePath(this.globalTemplatePath + "/FuzzyRequirementRuleAttributeTemplate.clp")
//                                .setWrapperTemplatePath(this.globalTemplatePath + "/FuzzyRequirementRuleAttributeTemplateWrapper.clp")
//                                .build()
//                );
//            }




            // 14.5 MISSION ANALYSIS DATABASE
            build.requests.add(
                    new WalkerMissionAnalysisTemplateRequest.Builder()
                            .templateFilePath(this.globalTemplatePath + "/WalkerMissionAnalysisTemplate.clp")
                            .build()
            );



            // 15 ---------- CAPABILITY RULES
            build.requests.add(
                    new BatchFileTemplateRequest.Builder()
                            .templateFilePath(this.jessAppPath + "/capability_rules.clp")
                            .build()
            );
            build.requests.add(
                    new CapabilityRuleTemplateRequest.Builder()
                            .templateFilePath(this.globalTemplatePath + "/CapabilityRuleTemplate.clp")
                            .build()
            );

            // 15.1 DUTY CYCLE CONSTRAINTS
//            build.requests.add(
//                    new BatchFileTemplateRequest.Builder()
//                            .templateFilePath(this.jessAppPath + "/duty_cycle_constraints.clp")
//                            .build()
//            );


            // 16 ---------- SYNERGY RULES
            build.requests.add(
                    new BatchFileTemplateRequest.Builder()
                            .templateFilePath(this.jessAppPath + "/synergy_rules.clp")
                            .build()
            );
            build.requests.add(
                    new SynergyRuleTemplateRequest.Builder()
                            .templateFilePath(this.globalTemplatePath + "/SynergyRuleTemplate.clp")
                            .build()
            );

            // 17 ---------- ASSIMILATION RULES
            build.requests.add(
                    new BatchFileTemplateRequest.Builder()
                            .templateFilePath(this.jessAppPath + "/assimilation_rules.clp")
                            .build()
            );

            // 18 ---------- AD-HOC RULES: instruments
            build.requests.add(
                    new AdHocRuleTemplateRequest.Builder()
                            .templateFilePath(this.globalTemplatePath + "/AdHocRuleTemplate.clp")
                            .build()
            );
            build.requests.add(
                    new BatchFileTemplateRequest.Builder()
                            .templateFilePath(this.jessAppPath + "/smap_rules_test.clp")
                            .build()
            );
//            build.requests.add(
//                    new BatchFileTemplateRequest.Builder()
//                            .templateFilePath(this.jessAppPath + "/Decadal_specific_rules.clp")
//                            .build()
//            );

            // 19 ---------- DOWN SELECTION RULES
//            build.requests.add(
//                    new BatchFileTemplateRequest.Builder()
//                            .templateFilePath(this.jessAppPath + "/down_selection_rules_smap.clp")
//                            .build()
//            );

            // 20 ---------- SEARCH RULES
//            build.requests.add(
//                    new BatchFileTemplateRequest.Builder()
//                            .templateFilePath(this.globalTemplatePath + "/SearchRuleTemplate.clp")
//                            .build()
//            );
//            build.requests.add(
//                    new BatchFileTemplateRequest.Builder()
//                            .templateFilePath(this.jessAppPath + "/search_heuristic_rules_smap_improveOrbit.clp")
//                            .build()
//            );
            build.requests.add(
                    new ResetRequest.Builder()
                            .setFocus("DATABASE")
                            .build()
            );

            // 21 ---------- EXPLANATION RULES
            build.requests.add(
                    new BatchFileTemplateRequest.Builder()
                            .templateFilePath(this.jessAppPath + "/explanation_rules.clp")
                            .build()
            );
            build.requests.add(
                    new BatchFileTemplateRequest.Builder()
                            .templateFilePath(this.globalTemplatePath + "/ExplanationRuleTemplate.clp")
                            .build()
            );

            // 22 ---------- AGGREGATION RULES
            build.requests.add(
                    new BatchFileTemplateRequest.Builder()
                            .templateFilePath(this.jessAppPath + "/aggregation_rules.clp")
                            .build()
            );
            build.requests.add(
                    new BatchFileTemplateRequest.Builder()
                            .templateFilePath(this.jessAppPath + "/fuzzy_aggregation_rules.clp")
                            .build()
            );
            build.requests.add(
                    new AggregationRuleTemplateRequest.Builder()
                            .templateFilePath(this.globalTemplatePath + "/AggregationRuleTemplate.clp")
                            .build()
            );

            // 23 ---------- CRITIC RULES
            build.requests.add(
                    new BatchFileTemplateRequest.Builder()
                            .templateFilePath(this.jessAppPath + "/critique/critique_performance_precalculation.clp")
                            .build()
            );
            build.requests.add(
                    new BatchFileTemplateRequest.Builder()
                            .templateFilePath(this.jessAppPath + "/critique/critique_performance.clp")
                            .build()
            );
            build.requests.add(
                    new BatchFileTemplateRequest.Builder()
                            .templateFilePath(this.jessAppPath + "/critique/critique_cost_precalculation.clp")
                            .build()
            );
            build.requests.add(
                    new BatchFileTemplateRequest.Builder()
                            .templateFilePath(this.jessAppPath + "/critique/critique_cost.clp")
                            .build()
            );




            return build;
        }
    }

    public ArrayList<TemplateRequest> getRequests() {
        return this.requests;
    }







}
