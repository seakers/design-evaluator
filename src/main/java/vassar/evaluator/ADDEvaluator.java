package vassar.evaluator;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import evaluator.EvaluatorApp;
import jess.*;
import org.checkerframework.checker.units.qual.C;
import org.hipparchus.util.FastMath;
import org.orekit.frames.TopocentricFrame;
import seakers.orekit.coverage.access.TimeIntervalArray;
import seakers.orekit.event.EventIntervalMerger;
import vassar.architecture.ADDArchitecture;
import vassar.architecture.AbstractArchitecture;
import vassar.evaluator.coverage.CoverageAnalysis;
import vassar.evaluator.spacecraft.Orbit;
import vassar.jess.QueryBuilder;
import vassar.jess.Resource;
import vassar.jess.utils.Interval;
import vassar.matlab.MatlabFunctions;
import vassar.problem.Problem;
import vassar.result.FuzzyValue;
import vassar.result.Result;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class ADDEvaluator implements Callable<Result> {

    private Resource        resource;
    private Problem         problem;
    private Rete            engine;
    private QueryBuilder    q_builder;
    private ADDArchitecture arch;
    private Set<Orbit>      orbitsUsed;

    private ArrayList<Fact> arch_measurements;

    public boolean evalPerformance;
    public boolean evalCost;
    public boolean evalScheduling;

    public boolean orbitSelection;
    public boolean synergyDeclaration;

    public boolean extractLaunchMass;

    public static class Builder{

        public Resource        res;
        public ADDArchitecture arch;
        public boolean evalPerformance;
        public boolean evalCost;
        public boolean evalScheduling;
        public boolean orbitSelection;
        public boolean synergyDeclaration;
        public boolean extractLaunchMass;

        public Builder(Resource res){
            this.res = res;
            this.evalPerformance = false;
            this.evalCost        = false;
            this.evalScheduling  = false;
            this.orbitSelection  = false;
            this.synergyDeclaration = false;
            this.extractLaunchMass  = false;
        }

        public Builder setArchitecture(ADDArchitecture arch){
            this.arch = arch;
            return this;
        }

        public Builder evalCost(boolean evalCost){
            this.evalCost = evalCost;
            return this;
        }

        public Builder evalScheduling(boolean evalScheduling){
            this.evalScheduling = evalScheduling;
            return this;
        }

        public Builder orbitSelection(boolean orbitSelection){
            this.orbitSelection = orbitSelection;
            return this;
        }

        public Builder synergyDeclaration(boolean synergyDeclaration){
            this.synergyDeclaration = synergyDeclaration;
            return this;
        }

        public Builder evalPerformance(boolean evalPerformance){
            this.evalPerformance = evalPerformance;
            return this;
        }

        public Builder extractLaunchMass(boolean extractLaunchMass){
            this.extractLaunchMass = extractLaunchMass;
            return this;
        }

        public ADDEvaluator build(){
            ADDEvaluator build = new ADDEvaluator();

            build.resource  = this.res;
            build.engine    = this.res.getEngine();
            build.q_builder = this.res.getQueryBuilder();
            build.problem   = this.res.problem;
            build.arch      = this.arch;
            build.evalCost  = this.evalCost;
            build.evalPerformance = this.evalPerformance;
            build.evalScheduling  = this.evalScheduling;
            build.orbitsUsed      = new HashSet<>();
            build.arch_measurements  = new ArrayList<>();
            build.orbitSelection     = this.orbitSelection;
            build.synergyDeclaration = this.synergyDeclaration;
            build.extractLaunchMass  = this.extractLaunchMass;

            return build;
        }

    }






//    _____      _ _
//   / ____|    | | |
//  | |     __ _| | |
//  | |    / _` | | |
//  | |___| (_| | | |
//   \_____\__,_|_|_|



    @Override
    public Result call(){
        boolean reset_engine_globals = true;

        // --> Result
        Result result = new Result();

        // 1. Check Feasibility
        if(!this.arch.isFeasibleAssignment()){
            return new Result(this.arch, 0.0, 1E5);
        }

        // 2. ADD COMPARATORS
        try{
            engine.eval("(defadvice before (create$ >= <= < >) (foreach ?xxx $?argv (if (eq ?xxx nil) then (return FALSE))))");
            engine.eval("(defadvice before (create$ sqrt + * **) (foreach ?xxx $?argv (if (eq ?xxx nil) then (bind ?xxx 0))))");
        }
        catch (Exception e){
            e.printStackTrace();
        }


        // 3. Evaluate Performance
        if(this.evalPerformance){
            if(reset_engine_globals){
                this.resetEngineGlobals(true);
            }
            result = this.evaluatePerformance(result);
        }

        // 4. Evaluate Cost
        if(this.evalCost){
            if(reset_engine_globals){
                this.resetEngineGlobals(true);
            }
            this.evaluateCost(result);
            if(this.evalScheduling){
                this.evaluateScheduling(result);
            }
        }

        // 5. Return result
        return result;
    }



//  _____           __
// |  __ \         / _|
// | |__) |__ _ __| |_ ___  _ __ _ __ ___   __ _ _ __   ___ ___
// |  ___/ _ \ '__|  _/ _ \| '__| '_ ` _ \ / _` | '_ \ / __/ _ \
// | |  |  __/ |  | || (_) | |  | | | | | | (_| | | | | (_|  __/
// |_|   \___|_|  |_| \___/|_|  |_| |_| |_|\__,_|_| |_|\___\___|


    public Result evaluatePerformance(Result result){
        System.out.println("----- EVALUATING PERFORMANCE");

        // 1. Initialize Engine
        Rete    engine = this.initializeEngine(true);
        Problem params = this.resource.getProblem();


        try{
            engine.eval("(bind ?*science-multiplier* 1.0)");
//            engine.eval("(defadvice before (create$ >= <= < >) (foreach ?xxx $?argv (if (eq ?xxx nil) then (return FALSE))))");
//            engine.eval("(defadvice before (create$ sqrt + * **) (foreach ?xxx $?argv (if (eq ?xxx nil) then (bind ?xxx 0))))");
            engine.eval("(watch rules)");
            engine.eval("(facts)");


            if(this.orbitSelection){
                this.selectMissionOrbits();
            }
            else{
                // Fill this.orbitsUsed
                ArrayList<String> arch_orbits = this.arch.getOrbitsUsed();
                for(String orbit: arch_orbits){
                    this.orbitsUsed.add(new Orbit(orbit, 1, 1));
                }
            }

            this.q_builder.saveQuery("performance/1-MISSION-FACTS", "MANIFEST::Mission");

            if(this.synergyDeclaration){
                System.out.println("---> SYNERGY EVAL");
                // EvaluatorApp.sleep(3);
                this.declareMissionSynergies();
            }
            else{
                System.out.println("---> NO SYNERGY EVAL");
                // EvaluatorApp.sleep(3);
            }

            this.q_builder.saveQuery("performance/2-SYNERGY-FACTS", "SYNERGIES::cross-registered-instruments");

            // MANIFEST INSTRUMENTS
            engine.setFocus("MANIFEST0");
            engine.run();
            this.q_builder.saveQuery("performance/3-MANIFESTED-INSTRUMENTS", "CAPABILITIES::Manifested-instrument");
            this.q_builder.saveQuery("performance/3-REVISIT-TIMES", "DATABASE::Revisit-time-of");

            // MEASUREMENT CAPABILITIES / DESIGN PROPULTION SYSTEM
            engine.setFocus("MANIFEST");
            engine.run();
            this.q_builder.saveQuery("performance/4-REVISIT-TIMES", "DATABASE::Revisit-time-of");
            this.q_builder.saveQuery("performance/4-CAN-MEASURE", "CAPABILITIES::can-measure");
            this.q_builder.saveQuery("performance/4-PROPULTION-SYSTEM", "MANIFEST::Mission");
            this.q_builder.saveQuery("performance/4-MANIFESTED-INSTRUMENTS", "CAPABILITIES::Manifested-instrument");

            // Sets
            // - power-duty-cycle
            // - data-rate-duty-cycle
            this.designSpacecraft(this.engine, this.arch, this.q_builder);

            // New module that applies the min of (power-duty-cycle / data-rate-duty-cycle)



            // Modifies: CAPABILITIES::can-measure
            engine.setFocus("CAPABILITIES");
            engine.run();
            this.q_builder.saveQuery("performance/5-SOLVE-CAN-MEASURE", "CAPABILITIES::can-measure");
            this.q_builder.saveQuery("performance/5-SOLVE-MANIFESTED-INSTRUMENTS", "CAPABILITIES::Manifested-instrument");
            this.q_builder.saveQuery("performance/5-MISSION-CAPABILITIES", "MANIFEST::Mission");

            engine.setFocus("CAPABILITIES-REMOVE-OVERLAPS");
            engine.run();

            // Asserts: REQUIREMENT::Measurement
            engine.setFocus("CAPABILITIES-GENERATE");
            engine.run();
            // EvaluatorApp.sleep(10);

            this.q_builder.saveQuery("performance/6-GENERATE-REQUIREMENTS", "REQUIREMENTS::Measurement");
            this.q_builder.saveQuery("performance/6-GENERATE-SYNERGIES", "SYNERGIES::cross-registered");
            this.q_builder.saveQuery("performance/6-GENERATE-CAPABILITY-LIMITATIONS", "CAPABILITIES::resource-limitations");
            // SYNERGIES::cross-registered
            // CAPABILITIES::resource-limitations

            engine.setFocus("CAPABILITIES-CROSS-REGISTER");
            engine.run();

            engine.setFocus("CAPABILITIES-UPDATE");
            engine.run();

            engine.setFocus("SYNERGIES");
            engine.run();
            // EvaluatorApp.sleep(10);

            // this.evaluateCoverage();

            engine.setFocus("ASSIMILATION2");
            engine.run();

            this.q_builder.saveQuery("performance/9-MEASUREMENT-REVISIT-TIME", "REQUIREMENTS::Measurement");

            engine.setFocus("ASSIMILATION");
            engine.run();

            engine.setFocus("FUZZY");
            engine.run();

            this.q_builder.saveQuery("performance/10-MEASUREMENT-AFTER-FUZZY", "REQUIREMENTS::Measurement");

            engine.setFocus("SYNERGIES");
            engine.run();

            engine.setFocus("SYNERGIES-ACROSS-ORBITS");
            engine.run();

            this.q_builder.saveQuery("requirements/requirement_facts", "REQUIREMENTS::Measurement");

            // REQUIREMENTS
            if ((params.getRequestMode().equalsIgnoreCase("FUZZY-CASES")) || (params.getRequestMode().equalsIgnoreCase("FUZZY-ATTRIBUTES"))) {
                engine.setFocus("FUZZY-REQUIREMENTS");
            }
            else {
                engine.setFocus("REQUIREMENTS");
            }
            engine.run();

            // AGGREGATION
            if ((params.getRequestMode().equalsIgnoreCase("FUZZY-CASES")) || (params.getRequestMode().equalsIgnoreCase("FUZZY-ATTRIBUTES"))) {
                engine.setFocus("FUZZY-AGGREGATION");
            }
            else {
                engine.setFocus("AGGREGATION");
            }
            engine.run();

            // --> Record all measurement facts for data continuity calculations
            this.q_builder.saveQuery("performance/11-MEASUREMENT-FINAL", "REQUIREMENTS::Measurement");
            this.arch_measurements = this.q_builder.makeQuery("REQUIREMENTS::Measurement");


            if ((params.getRequestMode().equalsIgnoreCase("CRISP-ATTRIBUTES")) || (params.getRequestMode().equalsIgnoreCase("FUZZY-ATTRIBUTES"))) {
                result = this.aggregate_performance_score_facts();
            }
            else{
                result = this.aggregate_performance_score();
            }

            this.q_builder.saveQuery("performance/END-PERFORMANCE", "MANIFEST::Mission");



        }
        catch(JessException e){
            System.out.println(e.getMessage() + " " + e.getClass() + " ");
            e.printStackTrace();
        }

        return result;
    }


    private void declareMissionSynergies(){

        ArrayList<Fact> missions = this.q_builder.makeQuery("MANIFEST::Mission");

        try{
            for(Fact mission: missions){
                String orbit   = mission.getSlotValue("orbit-string").stringValue(this.engine.getGlobalContext());
                String payload = mission.getSlotValue("instruments").listValue(this.engine.getGlobalContext()).toString();
                String synergy_assertion = "(SYNERGIES::cross-registered-instruments (instruments "+payload+") (degree-of-cross-registration spacecraft) (platform "+orbit+") (factHistory F0))";

                System.out.println("---> SYNERGY ASSERTION");
                System.out.println(synergy_assertion);
                // EvaluatorApp.sleep(5);
                Fact synergy_fact = engine.assertString(synergy_assertion);
            }
        }
        catch (JessException e){
            e.printStackTrace();
        }
    }

    private void selectMissionOrbits(){
        System.out.println("--> SELECTING MISSION ORBITS");
        this.q_builder.saveQuery("orbit-selection/1-STARTING-MISSIONS", "MANIFEST::Mission");
        HashMap<String, String> mission_to_selected_orbit = new HashMap<>();

        try{

            // Create all potential mission orbits
            this.engine.setFocus("ORBIT-SELECTION");
            this.engine.run();

            this.q_builder.saveQuery("orbit-selection/2-ENUMERATE-MISSIONS", "MANIFEST::Mission");
            this.q_builder.saveQuery("orbit-selection/3-ENUMERATE-ORBITS", "ORBIT-SELECTION::orbit");

            // Save a mapping each mission's potential orbits to their penalty values
            HashMap<String, HashMap<String, Double>> mission_orbits = new HashMap<>();
            ArrayList<Fact> orbit_facts = this.q_builder.makeQuery("ORBIT-SELECTION::orbit");
            for(Fact fct: orbit_facts){

                String mission_name          = fct.getSlotValue("in-mission").stringValue(this.engine.getGlobalContext());
                String mission_orbit         = fct.getSlotValue("orb").stringValue(this.engine.getGlobalContext());
                String prnalty_var_name      = fct.getSlotValue("penalty-var").stringValue(this.engine.getGlobalContext());
                Double mission_orbit_penalty = this.engine.eval(prnalty_var_name).floatValue(this.engine.getGlobalContext());

                HashMap<String, Double> orbit_penalties;
                if(!mission_orbits.containsKey(mission_name)){
                    orbit_penalties = new HashMap<>();
                    mission_orbits.put(mission_name, orbit_penalties);
                }
                else{
                    orbit_penalties = mission_orbits.get(mission_name);
                }

                if(orbit_penalties.containsKey(mission_orbit)){
                    Double pen = orbit_penalties.get(mission_orbit);
                    orbit_penalties.put(mission_orbit, pen + mission_orbit_penalty);
                }
                else{
                    orbit_penalties.put(mission_orbit, mission_orbit_penalty);
                }
            }
            System.out.println(mission_orbits);

            // Remove the mission orbits with the highest penalties
            Set<String> mission_keys = mission_orbits.keySet();
            HashMap<String, ArrayList<String>> best_orbits = new HashMap<>();

            // Find the best orbits for each mission
            for(String mission_key: mission_keys){
                Double init_penalty = 100000.0;

                // Get the penalties for each orbit
                HashMap<String, Double> penalties = mission_orbits.get(mission_key);
                ArrayList<String> best_orbit = new ArrayList<>();
                best_orbits.put(mission_key, best_orbit);
                for(String penalty_key: penalties.keySet()){
                    // The orbit's penalty
                    Double single_penalty = penalties.get(penalty_key);
                    if(single_penalty < init_penalty){
                        best_orbit = new ArrayList<>();
                        init_penalty = single_penalty;
                        best_orbit.add(penalty_key);
                        best_orbits.put(mission_key, best_orbit);
                    }
                    else if(single_penalty.equals(init_penalty)){
                        best_orbit.add(penalty_key);
                        best_orbits.put(mission_key, best_orbit);
                    }
                }
            }
            System.out.println("\n\n  BEST ORBITS: " + best_orbits);


            // Arbitrarily choose the first of top orbits and retract all unnecessary Mission facts
            for(String mission_key: best_orbits.keySet()){
                ArrayList<String> orbits = best_orbits.get(mission_key);
                Random rand = new Random();
                String random_orbit = orbits.get(orbits.size()-1);
                int start_idx = random_orbit.indexOf('-') + 1;
                String random_orbit_trm = random_orbit.substring(start_idx);
                System.out.println(random_orbit_trm);
                mission_to_selected_orbit.put(mission_key, random_orbit_trm);

                String call = "(defrule ORBIT-SELECTION::remove-suboptimal-rules-" + mission_key +
                " \"Remove Mission facts that have suboptimal orbits\" " +
                " ?miss <- (MANIFEST::Mission (Name " + mission_key + ") " +
                " (in-orbit ?orb&:(neq ?orb " + random_orbit_trm + ")))" +
                " =>" +
                " (retract ?miss)" + " )";
                System.out.println(call);
                this.engine.eval(call);
            }

            this.engine.setFocus("ORBIT-SELECTION");
            this.engine.run();

            this.q_builder.saveQuery("orbit-selection/4-FINAL-MISSIONS", "MANIFEST::Mission");

            // Remove added rules
            Iterator rule_itr = this.engine.listDefrules();
            while(rule_itr.hasNext()){
                Object rule = rule_itr.next();
                if(rule.getClass().getName() == "jess.Defrule"){
                    Defrule rulec = (Defrule) rule;
                    if(rulec.getName().contains("ORBIT-SELECTION::remove-suboptimal-rules")){
                        this.engine.removeDefrule(rulec.getName());
                    }
                }
            }

            // Remove all ORBIT-SELECTION::orbit facts
            Iterator fact_itr = this.engine.listFacts();
            while(fact_itr.hasNext()){
                Object fact = fact_itr.next();
                if(fact.getClass().getName() == "jess.Fact"){
                    Fact factc = (Fact) fact;
                    // System.out.println(factc.getDeftemplate().toString());
                    if(factc.getDeftemplate().toString().contains("deftemplate ORBIT-SELECTION::orbit")){
                        this.engine.retract(factc);
                    }
                }
            }

        }
        catch (JessException e){
            e.printStackTrace();
        }

        this.orbitsUsed = new HashSet<>();
        for(String mission_name: mission_to_selected_orbit.keySet()){
            String            orbit_name          = mission_to_selected_orbit.get(mission_name);
            ArrayList<String> mission_instruments = this.q_builder.getMissionInstruments(mission_name);
            this.arch.setMissionOrbit(orbit_name, mission_instruments);
            this.orbitsUsed.add(new Orbit(orbit_name, 1, 1));
        }
    }

    private Result aggregate_performance_score_facts(){
        QueryBuilder qb = this.resource.getQueryBuilder();
        Problem params = this.resource.getProblem();
        ArrayList<ArrayList<Double>> obj_scores = new ArrayList<>();
        ArrayList<ArrayList<ArrayList<Double>>> subobj_scores = new ArrayList<>();
        ArrayList<Double> panel_scores = new ArrayList<>();
        double science = 0.0;
        double cost = 0.0;
        FuzzyValue fuzzy_science = null;
        FuzzyValue fuzzy_cost = null;
        TreeMap<String, ArrayList<Fact>> explanations = new TreeMap<>();
        TreeMap<String, Double> subobj_scores_map = new TreeMap<>();

        // aggregated_info: contains all subobjective info
        JsonObject aggregated_info = new JsonObject();
        try {
            // General and panel scores
            ArrayList<Fact> vals = qb.makeQuery("AGGREGATION::VALUE");
            Fact val = vals.get(0);
            science = val.getSlotValue("satisfaction").floatValue(engine.getGlobalContext());
            if (params.getRequestMode().equalsIgnoreCase("FUZZY-ATTRIBUTES") || params.getRequestMode().equalsIgnoreCase("FUZZY-CASES")) {
                fuzzy_science = (FuzzyValue)val.getSlotValue("fuzzy-value").javaObjectValue(engine.getGlobalContext());
            }
            for (String str_val: MatlabFunctions.jessList2ArrayList(val.getSlotValue("sh-scores").listValue(engine.getGlobalContext()), engine)) {
                panel_scores.add(Double.parseDouble(str_val));
            }

            ArrayList<Fact> subobj_facts = qb.makeQuery("AGGREGATION::SUBOBJECTIVE");
            for (Fact f: subobj_facts) {
                String subobj = f.getSlotValue("id").stringValue(engine.getGlobalContext());
                Double subobj_score = f.getSlotValue("satisfaction").floatValue(engine.getGlobalContext());
                Double current_subobj_score = subobj_scores_map.get(subobj);
                if(current_subobj_score == null || subobj_score > current_subobj_score) {
                    subobj_scores_map.put(subobj, subobj_score);
                }
                if (!explanations.containsKey(subobj)) {
                    explanations.put(subobj, qb.makeQuery("AGGREGATION::SUBOBJECTIVE (id " + subobj + ")"));
                }
            }

            JsonArray all_subobjectives = new JsonArray();
            aggregated_info.add("subobjectives", all_subobjectives);

            for (int p = 0; p < params.numPanels; p++) {
                int nob = params.numObjectivesPerPanel.get(p);
                ArrayList<ArrayList<Double>> subobj_scores_p = new ArrayList<>(nob);
                for (int o = 0; o < nob; o++) {
                    ArrayList<ArrayList<String>> subobj_p = params.subobjectives.get(p);
                    ArrayList<String> subobj_o = subobj_p.get(o);
                    int nsubob = subobj_o.size();
                    ArrayList<Double> subobj_scores_o = new ArrayList<>(nsubob);

                    for (String subobj : subobj_o) {
                        JsonObject subobjective_info = this.resource.dbClient.getSubobjectiveAttributeInformation(subobj);
                        subobjective_info.addProperty("score", subobj_scores_map.get(subobj));
                        all_subobjectives.add(subobjective_info);

                        subobj_scores_o.add(subobj_scores_map.get(subobj));
                    }
                    subobj_scores_p.add(subobj_scores_o);
                }
                subobj_scores.add(subobj_scores_p);
            }

            //Objective scores
            for (int p = 0; p < params.numPanels; p++) {
                int nob = params.numObjectivesPerPanel.get(p);
                ArrayList<Double> obj_scores_p = new ArrayList<>(nob);
                for (int o = 0; o < nob; o++) {

                    ArrayList<ArrayList<Double>> subobj_weights_p = params.subobjWeights.get(p);
                    ArrayList<Double>            subobj_weights_o = subobj_weights_p.get(o);
                    ArrayList<ArrayList<Double>> subobj_scores_p  = subobj_scores.get(p);
                    ArrayList<Double>            subobj_scores_o  = subobj_scores_p.get(o);

                    try {
                        obj_scores_p.add(Result.sumProduct(subobj_weights_o, subobj_scores_o));
                    }
                    catch (Exception e) {
                        System.out.println(e.getMessage());
                    }
                }
                obj_scores.add(obj_scores_p);
            }
        }
        catch (Exception e) {
            System.out.println(e.getMessage() + " " + e.getClass());
            e.printStackTrace();
        }
        Result result = new Result(arch, science, cost, fuzzy_science, fuzzy_cost, subobj_scores, obj_scores, panel_scores, subobj_scores_map);
        result.setSubobjectiveInfo(aggregated_info);

        if (true) {
            result.setCapabilities(qb.makeQuery("REQUIREMENTS::Measurement"));
            result.setExplanations(explanations);
        }

        return result;
    }

    protected Result aggregate_performance_score() {
        JsonObject aggregated_info = new JsonObject();

        ArrayList<ArrayList<ArrayList<Double>>> subobj_scores = new ArrayList<>();
        ArrayList<ArrayList<Double>> obj_scores = new ArrayList<>();
        ArrayList<Double> panel_scores = new ArrayList<>();
        TreeMap<String, ArrayList<Fact>> explanations = new TreeMap<>();
        TreeMap<String, ArrayList<Fact>> capabilities = new TreeMap<>();
        double science = 0.0;
        double cost = 0.0;
        ArrayList<Fact> temp = new ArrayList<>();
        System.out.println("aggregating performance score");



        long startTime = System.nanoTime();


        JsonArray all_subobjectives = new JsonArray();
        aggregated_info.add("subobjectives", all_subobjectives);

        //Subobjective scores
        for (int p = 0; p < this.problem.numPanels; p++) {
            int nob = this.problem.numObjectivesPerPanel.get(p);
            ArrayList<ArrayList<Double>> subobj_scores_p = new ArrayList<>(nob);
            for (int o = 0; o < nob; o++) {
                ArrayList<ArrayList<String>> subobj_p = this.problem.subobjectives.get(p);
                ArrayList<String> subobj_o = subobj_p.get(o);
                int nsubob = subobj_o.size();
                ArrayList<Double> subobj_scores_o = new ArrayList<>(nsubob);
                for (String subobj : subobj_o) {
                    // JsonObject subobjective_info = this.resource.dbClient.getSubobjectiveAttributeInformation(subobj);
                    JsonObject subobjective_info = this.problem.subobjectiveDetails.get(subobj);
                    String var_name = "?*subobj-" + subobj + "*";
                    try {
                        subobjective_info.addProperty("score", this.engine.eval(var_name).floatValue(this.engine.getGlobalContext()));
                        all_subobjectives.add(subobjective_info);

                        subobj_scores_o.add(this.engine.eval(var_name).floatValue(this.engine.getGlobalContext()));
//                        System.out.println(this.engine.eval(var_name).floatValue(this.engine.getGlobalContext()));
                        if (!explanations.containsKey(subobj)) {
                            temp = this.q_builder.makeQuery("REASONING::fully-satisfied (subobjective " + subobj + ")");
                            temp.addAll(this.q_builder.makeQuery("REASONING::partially-satisfied (subobjective " + subobj + ")"));
                            explanations.put(subobj,temp);
                        }
                    }
                    catch (Exception e) {
                        System.out.println(e.getMessage());
                    }
                    capabilities.put(subobj, this.q_builder.makeQuery("REQUIREMENTS::Measurement (Parameter " + this.problem.subobjectivesToMeasurements.get(subobj) + ")"));
                }
                subobj_scores_p.add(subobj_scores_o);
            }
            subobj_scores.add(subobj_scores_p);
        }
        System.out.println("---> SUBOBJECTIVE TIME: " + (System.nanoTime() - startTime));


        //Objective scores
        for (int p = 0; p < this.problem.numPanels; p++) {
            int nob = this.problem.numObjectivesPerPanel.get(p);
            ArrayList<Double> obj_scores_p = new ArrayList<>(nob);
            for (int o = 0; o < nob; o++) {
                ArrayList<ArrayList<Double>> subobj_weights_p = this.problem.subobjWeights.get(p);
                ArrayList<Double> subobj_weights_o = subobj_weights_p.get(o);
                ArrayList<ArrayList<Double>> subobj_scores_p = subobj_scores.get(p);
                ArrayList<Double> subobj_scores_o = subobj_scores_p.get(o);
                try {
                    obj_scores_p.add(Result.sumProduct(subobj_weights_o, subobj_scores_o));
                }
                catch (Exception e) {
                    System.out.println(e.getMessage());
                }
            }
            obj_scores.add(obj_scores_p);
        }
        System.out.println("---> OBJECTIVE TIME: " + (System.nanoTime() - startTime));

        //Stakeholder and final score
        for (int p = 0; p < this.problem.numPanels; p++) {
            try {
                panel_scores.add(Result.sumProduct(this.problem.objWeights.get(p), obj_scores.get(p)));
            }
            catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
        System.out.println("---> STAKEHOLDER TIME: " + (System.nanoTime() - startTime));

        try {
            science = Result.sumProduct(this.problem.panelWeights, panel_scores);
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
        }

        // The final science score must be multiplied by the science score multiplier
        try{
            Value val = this.engine.eval("?*science-multiplier*");
            // Double science_multiplier = Double.parseDouble(val.toString());
            // science *= science_multiplier;
        }
        catch (Exception e){
            e.printStackTrace();
        }



        Result theResult = new Result(arch, science, cost, subobj_scores, obj_scores, panel_scores,null);
        theResult.setSubobjectiveInfo(aggregated_info);
        theResult.setExplanations(explanations);
        theResult.setCapabilityList(capabilities);
        theResult.setCapabilities(this.q_builder.makeQuery("REQUIREMENTS::Measurement"));

        System.out.println("---> RETURN: " + (System.nanoTime() - startTime));
        return theResult;
    }



//  _____
// / ____|
//| |      ___ __   __ ___  _ __  __ _   __ _   ___
//| |     / _ \\ \ / // _ \| '__|/ _` | / _` | / _ \
//| |____| (_) |\ V /|  __/| |  | (_| || (_| ||  __/
// \_____|\___/  \_/  \___||_|   \__,_| \__, | \___|
//                                       __/ |
//                                      |___/

    private void evaluateCoverage(){

        try{
            int javaAssertedFactID = 1;


            // 1. ITERATE OVER MEASUREMENTS
            for (String measurement: this.problem.measurementsToInstruments.keySet()){

                // 2. GET MEASUREMENT FOVS
                HashMap<String, ArrayList<Integer>> measurement_fovs = this.getMeasurementFOVs(measurement);

                // If this measurement is taken by the architecture
                if(!measurement_fovs.isEmpty()){

                    HashMap<String, Double> revisit_times = this.get_revisit_time_metrics(measurement_fovs);

                    Double therevtimesGlobal = revisit_times.get("global");
                    Double therevtimesUS     = revisit_times.get("us");

                    System.out.println("\n-------- REVISIT TIMES: " + measurement);
                    System.out.println("-- GLOBAL: " + therevtimesGlobal);
                    System.out.println("------ US: " + therevtimesUS);
                    System.out.println("---- FOVS: " + measurement_fovs);
                    System.out.println("-------------------\n");
                    // EvaluatorApp.sleep(5);

                    String call = "(assert (ASSIMILATION2::UPDATE-REV-TIME (parameter " +  measurement + ") "
                            + "(avg-revisit-time-global# " + therevtimesGlobal + ") "
                            + "(avg-revisit-time-US# " + therevtimesUS + ")"
                            + "(factHistory J" + javaAssertedFactID + ")))";

                    javaAssertedFactID++;
                    engine.eval(call);
                }
            }

        }
        catch (JessException e){
            e.printStackTrace();
        }

    }

    private HashMap<String, ArrayList<Integer>> getMeasurementFOVs(String measurement){
        ArrayList<ArrayList<String>> satellites = this.arch.get_array_info();


        // key: orbit
        // value: list of fovs
        HashMap<String, ArrayList<Integer>> orb_fov_map  = new HashMap<>();
        HashMap<String, ArrayList<String>>  orb_inst_map = new HashMap<>();


        // FACTS
        try{
            ArrayList<Fact>    measurement_facts = this.q_builder.getMeasurementFacts(measurement);
            if(measurement_facts.isEmpty()){
                return (new HashMap<String, ArrayList<Integer>>());
            }

            ArrayList<Integer> sat_idx_used      = new ArrayList<>();
            for(Fact fact: measurement_facts){

                int    fov        = fact.getSlotValue("Field-of-view#").intValue(this.engine.getGlobalContext());
                String orbit      = fact.getSlotValue("orbit-string").stringValue(this.engine.getGlobalContext());
                String instrument = fact.getSlotValue("Instrument").stringValue(this.engine.getGlobalContext());

                // orb_inst_map
                if(!orb_inst_map.containsKey(orbit)){
                    orb_inst_map.put(orbit, new ArrayList<>());
                }
                if(!orb_inst_map.get(orbit).contains(instrument)){
                    orb_inst_map.get(orbit).add(instrument);
                }

                // orb_fov_map
                if(!orb_fov_map.containsKey(orbit)){
                    orb_fov_map.put(orbit, new ArrayList<>());
                }
                orb_fov_map.get(orbit).add(fov);

            }
        }
        catch (Exception e){
            e.printStackTrace();
        }

        // DETERMINE THE NUMBER OF SATELLITES PER-ORBIT
        HashMap<String, Integer> orb_num_sat_map = new HashMap<>();
        for(String orbit: orb_inst_map.keySet()){
            ArrayList<Integer> used_missions_idx = new ArrayList<>();
            ArrayList<String>  instruments       = orb_inst_map.get(orbit);
            for(String instrument: instruments){
                int idx = this.getSatelliteIndexForInstrument(satellites, instrument);
                if(!used_missions_idx.contains(idx)){
                    used_missions_idx.add(idx);
                }
            }
            orb_num_sat_map.put(orbit, used_missions_idx.size());
        }

        // DETERMINE THE FOVS FOR EACH ORBIT
        HashMap<String, Integer> orb_fov_map_final = new HashMap<>();
        for(String orbit: orb_fov_map.keySet()){
            ArrayList<Integer> fovs = orb_fov_map.get(orbit);
            orb_fov_map_final.put(orbit, Collections.max(fovs));
        }

        System.out.println("--> ORBIT TO NUM SATELLITES " + orb_num_sat_map);
        System.out.println("--> ORBIT TO FOVS " + orb_fov_map_final);

        // AGGREGATE RESULTS
        HashMap<String, ArrayList<Integer>> results  = new HashMap<>();
        for(String orbit: orb_fov_map_final.keySet()){
            Integer num_sats = orb_num_sat_map.get(orbit);
            Integer fov      = orb_fov_map_final.get(orbit);
            ArrayList<Integer> orbit_data = new ArrayList<>();
            orbit_data.add(num_sats);
            orbit_data.add(fov);
            results.put(orbit, orbit_data);
        }

        return results;
    }

    private int getSatelliteIndexForInstrument(ArrayList<ArrayList<String>> satellites, String instrument){

        int idx = -1;
        for(ArrayList<String> sat: satellites){
            idx++;
            if(sat.contains(instrument)){
                return idx;
            }
        }
        return idx;
    }

    private HashMap<String, Double> get_revisit_time_metrics(HashMap<String, ArrayList<Integer>> orbit_fov_map){


        // COVERAGE GRANULARITY
        int coverageGranularity = 20;

        // ANALYSIS
        CoverageAnalysis coverageAnalysis = new CoverageAnalysis(1, coverageGranularity, true, true, this.problem.orekitResourcesPath);

        // EVENTS
        List<Map<TopocentricFrame, TimeIntervalArray>> all_access_events = new ArrayList<>();

        // AREAS OF INTEREST
        double[] latBounds = new double[]{FastMath.toRadians(-70), FastMath.toRadians(70)};
        double[] lonBounds = new double[]{FastMath.toRadians(-180), FastMath.toRadians(180)};
        double[] latBoundsUS = new double[]{FastMath.toRadians(25), FastMath.toRadians(50)};
        double[] lonBoundsUS = new double[]{FastMath.toRadians(-125), FastMath.toRadians(-66)};

        for(String orbit_str: orbit_fov_map.keySet()){
            ArrayList<Integer> orbit_data = orbit_fov_map.get(orbit_str);
            Integer num_sats = orbit_data.get(0);
            Integer fov      = orbit_data.get(1);
            Orbit orbit = new Orbit(orbit_str, 1, num_sats);

            // COVERAGE PARAMETERS
            double fieldOfView = fov;         // CHOOSE FIRST FOV TO USE
            double inclination = orbit.getInclinationNum(); // [deg]
            double altitude    = orbit.getAltitudeNum();    // [m]
            String raanLabel   = orbit.getRaan();
            int numSats        = Integer.parseInt(orbit.getNum_sats_per_plane());
            int numPlanes      = Integer.parseInt(orbit.getNplanes());

            // ACCESSES EVENTS
            Map<TopocentricFrame, TimeIntervalArray> access_events = coverageAnalysis.getAccesses(fieldOfView, inclination, altitude, numSats, numPlanes, raanLabel);
            all_access_events.add(access_events);
        }

        // INSTIANTIATE MERGED EVENTS
        Map<TopocentricFrame, TimeIntervalArray> mergedEvents = new HashMap<>(all_access_events.get(0));

        // MERGE EVENTS
        for(int i = 1; i < all_access_events.size(); ++i) {
            Map<TopocentricFrame, TimeIntervalArray> event = all_access_events.get(i);
            mergedEvents = EventIntervalMerger.merge(mergedEvents, event, false);
        }

        Double therevtimesGlobal = coverageAnalysis.getRevisitTime(mergedEvents, latBounds, lonBounds)/3600;
        Double therevtimesUS     = coverageAnalysis.getRevisitTime(mergedEvents, latBoundsUS, lonBoundsUS)/3600;

        HashMap<String, Double> results = new HashMap<>();
        results.put("global", therevtimesGlobal);
        results.put("us", therevtimesUS);

        return results;
    }



//    _____      _              _       _ _
//  / ____|    | |            | |     | (_)
// | (___   ___| |__   ___  __| |_   _| |_ _ __   __ _
//  \___ \ / __| '_ \ / _ \/ _` | | | | | | '_ \ / _` |
//  ____) | (__| | | |  __/ (_| | |_| | | | | | | (_| |
// |_____/ \___|_| |_|\___|\__,_|\__,_|_|_|_| |_|\__, |
//                                                __/ |
//                                               |___/


    private void evaluateScheduling(Result result){

        this.evaluateDataContinuityScore(result);
        this.evaluateFairnessScore(result);
    }

    private void evaluateDataContinuityScore(Result result){

        this.q_builder.saveQuery("data-continuity/1-INITIAL-MEASUREMENTS", "REQUIREMENTS::Measurement");

        // SCORE
        int score = 0;
        int overlap_counter = 0;

        // HARDCODE
        int    t_zero           = 2000; // 2010 for SMAP, 2000 for DECADAL
        double yearly_budget    = 200;  // In Millions
        int    mission_duration = 5; // 5 year default mission duration


        // 1. Use QueryBuilder to get asserted Mission Facts - parse mission start / end date
        ArrayList<Fact> missions  = this.get_ordered_mission();


        // 2. Get historical missions
        ArrayList<HashMap<String, ArrayList<Date>>> historical_missions = this.resource.dbClient.getDataContinuityInformation();
        // DatabaseClient.printHistoricalMeasurements(historical_missions);
        // EvaluatorApp.sleep(10);


        int counter = 0;
        for(Fact mission_fact: missions){
            try{
                // 1. Get measurements associated with mission: measurements
                HashMap<String, Integer> measurements = this.get_mission_fact_measurements(mission_fact);
                measurements = this.transform_measurement_names(measurements);



                // 2. Calculate mission start / end with via cost
                double lifecycle_cost   = Double.parseDouble(mission_fact.getSlotValue("lifecycle-cost#").stringValue(this.engine.getGlobalContext()));         // millions
                double operations_cost  = Double.parseDouble(mission_fact.getSlotValue("operations-cost#").stringValue(this.engine.getGlobalContext())) / 1000; // millions
                double development_cost = lifecycle_cost - operations_cost;

                double mission_cost = Double.parseDouble(mission_fact.getSlotValue("mission-cost#").stringValue(this.engine.getGlobalContext()));

                t_zero += Math.ceil(development_cost / yearly_budget);
//                System.out.println("---> TIME INBETWEEN MISSIONS: " + Math.ceil(development_cost / yearly_budget));
//                System.out.println("---> PARAMETERS: " + t_zero + " " + lifecycle_cost + " " + operations_cost);
                // EvaluatorApp.sleep(3);
                int    start_year   = t_zero;
                int    end_year     = start_year + mission_duration; // Each mission flies for 5 years

                // 3. Iterate over mission measurements
                // EvaluatorApp.sleep(6);
                for(String mission_meas: measurements.keySet()){
                    int meas_multiplier = measurements.get(mission_meas);
                    meas_multiplier = 1;

                    // 4. Iterate over historical missions
                    for(HashMap<String, ArrayList<Date>> historical_mission: historical_missions){

                        // 5. Iterate over historical mission measurements
                        for(String measurement: historical_mission.keySet()){

                            ArrayList<Date> measurement_timeline = historical_mission.get(measurement);
                            Calendar calendar = new GregorianCalendar();

                            // Measurement Start Year
                            Date measurement_start   = measurement_timeline.get(0);
                            calendar.setTime(measurement_start);
                            int measurement_start_yr = calendar.get(Calendar.YEAR);

                            // Measurement End Year
                            Date measurement_end   = measurement_timeline.get(1);
                            calendar.setTime(measurement_end);
                            int measurement_end_yr = calendar.get(Calendar.YEAR);

                            // Transformed Name
                            String trans_name = this.transform_historical_measurement_name(measurement);

                            // if(trans_name.equalsIgnoreCase(mission_meas)){
//                            if(trans_name.toLowerCase().contains(mission_meas.toLowerCase())){
                            if(ADDEvaluator.compareMeasurementNames(trans_name, mission_meas)){
                                overlap_counter++;
                                if(end_year < measurement_start_yr || start_year > measurement_end_yr){
                                    score = score;
                                }
                                else if(start_year > measurement_start_yr && end_year < measurement_end_yr){
                                    score = score + ((end_year - start_year) * meas_multiplier);
                                }
                                else if(start_year > measurement_start_yr && end_year > measurement_end_yr){
                                    score = score + ((measurement_end_yr - start_year) * meas_multiplier);
                                }
                                else if(start_year < measurement_start_yr && end_year < measurement_end_yr){
                                    score = score + ((end_year - measurement_start_yr) * meas_multiplier);
                                }
                                else{
                                    score = score;
                                }
                            }
                        }
                    }
                }
            }
            catch(JessException e){
                e.printStackTrace();
            }
            counter++;
        }
        System.out.println("--> DATA SCORES: " + score + " | " + overlap_counter);

        result.setDataContinuityScore(score);
    }

    private HashMap<String, Integer> get_mission_fact_measurements(Fact mission){

        // Maps measurement name to the number of occurrences for this missions
        HashMap<String, Integer> measurement_map = new HashMap<>();

        try{
            // 1. Get all measurement facts for the specific mission
            String mission_name = mission.getSlotValue("Name").stringValue(this.engine.getGlobalContext());
            for(Fact measurement_fact: this.arch_measurements){
                String meas_mission = measurement_fact.getSlotValue("flies-in").stringValue(this.engine.getGlobalContext());
                String measurement_name = measurement_fact.getSlotValue("Parameter").stringValue(this.engine.getGlobalContext());

                if(mission_name.equals(meas_mission)){
                    if(!measurement_map.containsKey(measurement_name)){
                        measurement_map.put(measurement_name, 0);
                    }
                    Integer occurances = measurement_map.get(measurement_name) + 1;
                    measurement_map.put(measurement_name, occurances);
                }
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }

        for(String meas: measurement_map.keySet()){
            System.out.println("--> " + meas + ": " + measurement_map.get(meas));
        }
        // EvaluatorApp.sleep(10);



//        ArrayList<String> measurements = new ArrayList<>();
//
//        try{
//            String insts_string = mission.getSlotValue("instruments").listValue(this.engine.getGlobalContext()).toString();
//            String[] insts = insts_string.split("\\s+");
//
//            ArrayList<String> instruments = new ArrayList<>();
//            for(String inst: insts){
//                instruments.add(inst.trim());
//            }
//            instruments.remove("");
//
//
//            for(String inst: instruments){
//
//                // This won't work, because not all instrument measurements will always be able to be taken
//                ArrayList<String> inst_measurements = this.resource.dbClient.getInstrumentMeasurements(inst, false); // TRIM FOR SMAP
//                for(String meas: inst_measurements){
//
//                    // This is wrong because if a missions takes 2 of the same measurement, the data continuity score should reflect both measurements !!!
//                    if(!measurements.contains(meas)){
//                        measurements.add(meas);
//                    }
//                }
//            }
//
//        }
//        catch (JessException e){
//            e.printStackTrace();
//        }

        return measurement_map;
    }

    private HashMap<String, Integer> transform_measurement_names(HashMap<String, Integer> measurements_orig){
        ConcurrentHashMap<String, Integer> measurements = new ConcurrentHashMap<>(measurements_orig);

        System.out.println("---> MEASUREMENT NAMES " + measurements);
        HashMap<String, String> transform = new HashMap<>();
        transform.put("1.6.2 cloud ice particle size distribution", "Cloud ice (column/profile)");
        transform.put("1.7.2 Cloud droplet size", "Cloud drop effective radius");
        transform.put("1.1.4 aerosol extinction profiles/vertical concentration", "Aerosol Extinction / Backscatter (column/profile)");
        transform.put("1.8.5 CO", "CO2 Mole Fraction");
        transform.put("1.8.4 CH4", "CH4 Mole Fraction");
        transform.put("1.2.1 Atmospheric temperature fields", "Atmospheric temperature (column/profile)");
        transform.put("1.5.4 cloud mask", "Cloud mask");

        for(String meas: measurements.keySet()){
            if(transform.containsKey(meas)){
                Integer num = measurements.remove(meas);
                measurements.put(transform.get(meas), num);
            }
        }

        // System.out.println("---> MEASUREMENT FIXED " + measurements);
        return (new HashMap<>(measurements));
    }

    private String transform_historical_measurement_name(String measurement){
        if(measurement.equals("Sea-ice cover")) {
            return "Sea ice cover";
        }
        else if(measurement.equals("Soil moisture at the surface")) {
            return "Soil moisture";
        }
        else if(measurement.equals("Wind vector over sea surface (horizontal)") || measurement.equals("Wind speed over sea surface (horizontal)")) {
            return "Ocean surface wind speed";
        }
        // Decadal 2007 Transformations
        else if(measurement.equals("Aerosol optical depth (column/profile)")) {
            return "aerosol height/optical depth";
        }
        else if(measurement.equals("Aerosol absorption optical depth (column/profile)")) {
            return "aerosol absorption optical thickness and profiles";
        }
        else if(measurement.equals("Ocean imagery and water leaving spectral radiance")) {
            return "Spectrally resolved SW radiance";
        }
        return measurement;
    }

    private ArrayList<Fact> get_ordered_mission(){
        ArrayList<Fact> ordered_missions = new ArrayList<>();

        QueryBuilder    q_builder = this.resource.getQueryBuilder();
        ArrayList<Fact> missions  = this.q_builder.missionFactQuery("beforeScheduling");

        System.out.println("--> mission query: " + missions);

        HashMap<Integer, Fact> order_map = new HashMap<>();
        for(Fact mission_fact: missions){
            try{
                int key = Integer.parseInt(mission_fact.getSlotValue("order-index").stringValue(this.engine.getGlobalContext()));
                order_map.put(key, mission_fact);
            }
            catch (JessException e){
                e.printStackTrace();
            }
        }

        System.out.println("--> mission order map: " + order_map);

        for(int x = 0; x < order_map.keySet().size(); x++){
            ordered_missions.add(order_map.get(x));
        }

        System.out.println("--> ordered missions: " + ordered_missions);

//        for(Fact mission_fact: ordered_missions){
//            try{
//                System.out.println(mission_fact.getSlotValue("order-index").stringValue(this.engine.getGlobalContext()));
//            }
//            catch (JessException e){
//                e.printStackTrace();
//            }
//        }
        return ordered_missions;
    }

    private void evaluateFairnessScore(Result result){
        int score = 0;


    }

    public static boolean compareMeasurementNames(String meas1, String meas2){
        // System.out.println(meas1 + " | " + meas2);
        return (meas1.toLowerCase().contains(meas2.toLowerCase()) || meas2.toLowerCase().contains(meas1.toLowerCase()));
    }


//    _____          _
//  / ____|        | |
// | |     ___  ___| |_
// | |    / _ \/ __| __|
// | |___| (_) \__ \ |_
//  \_____\___/|___/\__|


    public void evaluateCostDecadal2007(Result res){



        Problem params = this.resource.getProblem();

        // 1. Initialize Engine
        Rete engine = this.initializeEngine(true);

        // 2. Select mission orbits if needed
        if(this.orbitSelection){
            this.selectMissionOrbits();
        }

        // NOT NEEDED IN COST CALCULATION
//        if(this.synergyDeclaration){
//            this.declareMissionSynergies();
//        }

        // 3. Run Modules
        try{
            this.engine.setFocus("LV-SELECTION");
            this.engine.run();

            if(true){
                this.engine.setFocus("BUS-SELECTION");
                this.engine.run();
            }

            this.engine.setFocus("EPS-DESIGN");
            this.engine.run();

            this.engine.setFocus("MASS-BUDGET");
            this.engine.run();

            // Clean LV selection from mission facts
            this.engine.setFocus("CLEAN");
            this.engine.run();

            this.engine.setFocus("LV-SELECTION");
            this.engine.run();

            this.engine.setFocus("COST-ESTIMATION");
            this.engine.run();
        }
        catch (Exception e){
            e.printStackTrace();
        }

        // Now get costs from MANIFEST::Mission facts



    }



    public void evaluateCost(Result res){

        System.out.println("---> EVALUATING COST");

        Problem params = this.resource.getProblem();

        // 1. Initialize Engine
        Rete engine = this.initializeEngine(true);

        // 2. Select mission orbits if needed
        if(this.orbitSelection){
            this.selectMissionOrbits();
        }

        if(this.synergyDeclaration){
            this.declareMissionSynergies();
        }

        try{
            this.engine.setFocus("MANIFEST0");
            this.engine.run();

            this.engine.eval("(focus MANIFEST)");
            this.engine.eval("(run)");



            designSpacecraft(this.engine, arch, this.q_builder);

            if(this.extractLaunchMass){
                // 1. Set the spacecraft launch mass in result
                res.mission_launch_mass = this.q_builder.getMissionSlotValue("satellite-launch-mass");
                // 2. Return result
                return;
            }



            this.engine.eval("(focus SAT-CONFIGURATION)");
            this.engine.eval("(run)");


            this.engine.eval("(focus LV-SELECTION0)");
            this.engine.eval("(run)");

            this.engine.eval("(focus LV-SELECTION1)");
            this.engine.eval("(run)");
            this.engine.eval("(focus LV-SELECTION2)");
            this.engine.eval("(run)");
            this.engine.eval("(focus LV-SELECTION3)");
            this.engine.eval("(run)");

            this.q_builder.saveQuery("cost/END-COST", "MANIFEST::Mission");



            if ((params.getRequestMode().equalsIgnoreCase("FUZZY-CASES")) || (params.getRequestMode().equalsIgnoreCase("FUZZY-ATTRIBUTES"))) {
                this.engine.eval("(focus FUZZY-COST-ESTIMATION)");
            }
            else {
                this.engine.eval("(focus COST-ESTIMATION)");
            }
            this.engine.eval("(run)");

            double cost = 0.0;
            FuzzyValue fzcost = new FuzzyValue("Cost", new Interval("delta",0,0),"FY04$M");
            ArrayList<Fact> missions = this.q_builder.makeQuery("MANIFEST::Mission");
            for (Fact mission: missions)  {
                cost = cost + mission.getSlotValue("lifecycle-cost#").floatValue(this.engine.getGlobalContext());
                if (params.getRequestMode().equalsIgnoreCase("FUZZY-ATTRIBUTES") || params.getRequestMode().equalsIgnoreCase("FUZZY-CASES")) {
                    fzcost = fzcost.add((FuzzyValue)mission.getSlotValue("lifecycle-cost").javaObjectValue(this.engine.getGlobalContext()));
                }
            }

            res.setCost(cost);
            res.setFuzzyCost(fzcost);
            res.setCostFacts(missions);
        }
        catch(JessException e){
            e.printStackTrace();
        }




    }


    protected void designSpacecraft(Rete r, AbstractArchitecture arch, QueryBuilder qb) {
        try {

            this.q_builder.saveQuery("spacecraft-design/1-INITIAL-MISSION-FACTS", "MANIFEST::Mission");


            r.eval("(focus PRELIM-MASS-BUDGET)");
            r.eval("(run)");

            this.q_builder.saveQuery("spacecraft-design/2-MISSION-FACTS-POST-PRELIM-MASS-BUDGET", "MANIFEST::Mission");




            ArrayList<Fact> missions = qb.makeQuery("MANIFEST::Mission");
            Double[] oldmasses = new Double[missions.size()];
            for (int i = 0; i < missions.size(); i++) {
                oldmasses[i] = missions.get(i).getSlotValue("satellite-dry-mass").floatValue(r.getGlobalContext());
            }
            Double[] diffs = new Double[missions.size()];
            double tolerance = 10*missions.size();
            boolean converged = false;
            while (!converged) {
                r.eval("(focus CLEAN1)");
                r.eval("(run)");

                r.eval("(focus MASS-BUDGET)");
                r.eval("(run)");

                r.eval("(focus CLEAN2)");
                r.eval("(run)");

                r.eval("(focus UPDATE-MASS-BUDGET)");
                r.eval("(run)");

                Double[] drymasses = new Double[missions.size()];
                double sumdiff = 0.0;
                double summasses = 0.0;
                for (int i = 0; i < missions.size(); i++) {
                    drymasses[i] = missions.get(i).getSlotValue("satellite-dry-mass").floatValue(r.getGlobalContext());
                    diffs[i] = Math.abs(drymasses[i] - oldmasses[i]);
                    sumdiff += diffs[i];
                    summasses += drymasses[i];
                }
                converged = sumdiff < tolerance || summasses == 0;
                oldmasses = drymasses;

            }
        }
        catch (Exception e) {
            System.out.println("EXC in evaluateCost: " + e.getClass() + " " + e.getMessage());
            e.printStackTrace();
        }


        this.q_builder.saveQuery("spacecraft-design/3-FINAL-MISSION-SPACECRAFT", "MANIFEST::Mission");
    }




//  _                     _   _____            _
// | |                   | | |  __ \          (_)
// | |     ___   __ _  __| | | |  | | ___  ___ _  __ _ _ __
// | |    / _ \ / _` |/ _` | | |  | |/ _ \/ __| |/ _` | '_ \
// | |___| (_) | (_| | (_| | | |__| |  __/\__ \ | (_| | | | |
// |______\___/ \__,_|\__,_| |_____/ \___||___/_|\__, |_| |_|
//                                                __/ |
//                                               |___/


    public Rete initializeEngine(boolean reset){

        if(reset){
            this.resource.resetEngine();
        }

        Rete engine = this.resource.getEngine();

        // AssertMissions equivalent
        try{

            Set<Orbit> func_return = this.arch.assignArchitecture(engine);
            if(func_return != null){
                // Orbits have been decided upon
                this.orbitsUsed = func_return;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return engine;
    }


    public void resetEngineGlobals(boolean globals){
        try{
            if(globals){
                this.engine.eval("(set-reset-globals TRUE)");
            }
            this.engine.eval("(reset)");
        }
        catch (JessException e){

            e.printStackTrace();
        }
    }




//  _    _ _   _ _ _ _
// | |  | | | (_) (_) |
// | |  | | |_ _| |_| |_ _   _
// | |  | | __| | | | __| | | |
// | |__| | |_| | | | |_| |_| |
//  \____/ \__|_|_|_|\__|\__, |
//                        __/ |
//                       |___/


    // ---> THREAD SLEEP
    public void consumerSleep(int seconds){
        try                            { TimeUnit.SECONDS.sleep(seconds); }
        catch (InterruptedException e) { e.printStackTrace(); }
    }



}
