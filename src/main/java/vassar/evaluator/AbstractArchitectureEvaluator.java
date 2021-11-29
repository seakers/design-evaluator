package vassar.evaluator;

import jess.*;


import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.frames.TopocentricFrame;

import evaluator.ResourcePaths;
import vassar.jess.QueryBuilder;
import vassar.jess.Resource;
import vassar.jess.func.RawSafety;
import vassar.result.FuzzyValue;
import vassar.result.Result;
import vassar.jess.utils.Interval;
import vassar.matlab.MatlabFunctions;

import vassar.evaluator.coverage.CoverageAnalysis;
import seakers.orekit.coverage.access.TimeIntervalArray;
import seakers.orekit.event.EventIntervalMerger;

import vassar.architecture.AbstractArchitecture;
import vassar.problem.Problem;
import vassar.evaluator.spacecraft.Orbit;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author Ana-Dani
 */

public abstract class AbstractArchitectureEvaluator implements Callable<Result> {

    protected AbstractArchitecture arch;
    protected Rete r;
    protected String type;
    protected boolean debug;
    protected Set<Orbit> orbitsUsed;
    protected Resource res;

    private ArrayList<Fact> arch_measurements;

    public AbstractArchitectureEvaluator() {
        this.res = null;
        this.arch = null;
        this.type = null;
        this.debug = false;
        this.orbitsUsed = new HashSet<>();
    }

    public AbstractArchitectureEvaluator(Resource resource, AbstractArchitecture arch, String type) {
        this.res = resource;
        this.arch = arch;
        this.type = type;
        this.debug = false;
        this.orbitsUsed = new HashSet<>();
        this.arch_measurements = new ArrayList<>();
    }

    public abstract AbstractArchitectureEvaluator getNewInstance();
    public abstract AbstractArchitectureEvaluator getNewInstance(Resource resource, AbstractArchitecture arch, String type);

    public void checkInit(){
        if(this.res == null || this.arch == null || this.type == null){
            throw new IllegalStateException(AbstractArchitectureEvaluator.class.getName() + " not initialized. " +
                    "Either set class attributes resource, arch, and type from a constructor, " +
                    "or use getNewInstance() method to initialize this class.");
        }
    }


    @Override
    public Result call() {

        checkInit();

        if (!arch.isFeasibleAssignment()) {
            return new Result(arch, 0.0, 1E5);
        }

        Problem params    = res.getProblem();
        Rete r            = res.getEngine();
        QueryBuilder qb   = res.getQueryBuilder();
        // MatlabFunctions m = MatlabFunctions();

        Result result     = new Result();

        try {
            if (type.equalsIgnoreCase("Slow")) {
                result = evaluatePerformance(params, r, arch, qb);

                // TODO: critique design on performance
                result.setPerformanceCritique(this.critiquePerformance(r, result, qb));

                r.eval("(reset)");
                assertMissions(params, r, arch);
            }
            else {
                throw new Exception("Wrong type of task");
            }


            evaluateCost(params, r, arch, result, qb);

            this.evaluate_data_continuity(result, qb, r);

            // TODO: critique design on cost
            result.setCostCritique(this.critiqueCost(r, result));

            result.setTaskType(type);
        }
        catch (Exception e) {
            System.out.println("EXC in Task:call: " + e.getClass() + " " + e.getMessage());
            e.printStackTrace();
        }

        return result;
    }

    public void evaluate_data_continuity(Result result, QueryBuilder qb, Rete r){
        qb.saveQuery("data-continuity/1-INITIAL-MEASUREMENTS", "REQUIREMENTS::Measurement");

        // SCORE
        int score = 0;
        int overlap_counter = 0;

        // HARDCODE
        int    t_zero           = 2010; // 2010 for SMAP, 2000 for DECADAL
        double yearly_budget    = 200;  // In Millions
        int    mission_duration = 5; // 5 year default mission duration

        ArrayList<Fact> missions  = qb.missionFactQuery("beforeScheduling");

        // 2. Get historical missions
        ArrayList<HashMap<String, ArrayList<Date>>> historical_missions = this.res.dbClient.getDataContinuityInformation();

        int counter = 0;

        for(Fact mission_fact: missions){
            try{
                // 1. Get measurements associated with mission: measurements
                HashMap<String, Integer> measurements = this.get_mission_fact_measurements(mission_fact, r);
                measurements = this.transform_measurement_names(measurements);

                // 2. Calculate mission start / end with via cost
                double lifecycle_cost   = Double.parseDouble(mission_fact.getSlotValue("lifecycle-cost#").stringValue(r.getGlobalContext()));         // millions
                double operations_cost  = Double.parseDouble(mission_fact.getSlotValue("operations-cost#").stringValue(r.getGlobalContext())) / 1000; // millions
                double development_cost = lifecycle_cost - operations_cost;

                double mission_cost = Double.parseDouble(mission_fact.getSlotValue("mission-cost#").stringValue(r.getGlobalContext()));

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

    private HashMap<String, Integer> get_mission_fact_measurements(Fact mission, Rete r){

        // Maps measurement name to the number of occurrences for this missions
        HashMap<String, Integer> measurement_map = new HashMap<>();

        try{
            // 1. Get all measurement facts for the specific mission
            String mission_name = mission.getSlotValue("Name").stringValue(r.getGlobalContext());
            for(Fact measurement_fact: this.arch_measurements){
                String meas_mission = measurement_fact.getSlotValue("flies-in").stringValue(r.getGlobalContext());
                String measurement_name = measurement_fact.getSlotValue("Parameter").stringValue(r.getGlobalContext());

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

        return measurement_map;
    }

    public Vector<String> critiquePerformance(Rete r, Result res, QueryBuilder qb){
        System.out.println("--> Critiquing Performance");
        Vector<String> critique = new Vector<>();
        // AGGREGATION::STAKEHOLDER
        // 1. Load initial performance critique facts
        try {
            r.eval("(bind ?*p* (new java.util.Vector))");
            r.batch(ResourcePaths.resourcesRootDir + "/vassar/problems/SMAP/clp/critique/critique_performance_initialize_facts.clp");
            qb.saveQuery("performance/fairness", "CRITIQUE-PERFORMANCE-PARAM::fairness");
            qb.saveQuery("performance/stakeholder", "AGGREGATION::STAKEHOLDER");
            r.setFocus("CRITIQUE-PERFORMANCE-PRECALCULATION");
            r.run();
            qb.saveQuery("performance/fairness2", "CRITIQUE-PERFORMANCE-PARAM::fairness");
            r.setFocus("CRITIQUE-PERFORMANCE");
            r.run();
            res.setFairnessScore(qb.getFairnessScore());
            critique = RawSafety.castVector(r.getGlobalContext().getVariable("*p*").javaObjectValue(null));
            for(String crit: critique){
                System.out.println(crit);
            }
            r.reset();
        }
        catch(Exception e) {
            System.out.println(e.getMessage()+" "+e.getClass());
            e.printStackTrace();
        }
        return critique;
    }

    public Vector<String> critiqueCost(Rete r, Result res){
        System.out.println("--> Critiquing Cost");
        Vector<String> critique = new Vector<>();;

        // 1. Load initial cost critique facts
        try {
            r.eval("(bind ?*q* (new java.util.Vector))");
            r.batch(ResourcePaths.resourcesRootDir + "/vassar/problems/SMAP/clp/critique/critique_cost_initialize_facts.clp");
            r.setFocus("CRITIQUE-COST-PRECALCULATION");
            r.run();
            r.setFocus("CRITIQUE-COST");
            r.run();
            critique = RawSafety.castVector(r.getGlobalContext().getVariable("*q*").javaObjectValue(null));
            for(String crit: critique){
                System.out.println(crit);
            }
        }
        catch(Exception e) {
            System.out.println(e.getMessage()+" "+e.getClass());
            e.printStackTrace();
        }
        return critique;
    }

    protected Result evaluatePerformance(Problem params, Rete r, AbstractArchitecture arch, QueryBuilder qb) {
        System.out.println("----- EVALUATING PERFORMANCE");

        Result result = new Result();
        try {

            r.reset();

            r.eval("(bind ?*science-multiplier* 1.0)");
            r.eval("(defadvice before (create$ >= <= < >) (foreach ?xxx $?argv (if (eq ?xxx nil) then (return FALSE))))");
            r.eval("(defadvice before (create$ sqrt + * **) (foreach ?xxx $?argv (if (eq ?xxx nil) then (bind ?xxx 0))))");

            assertMissions(params, r, arch);
            
            //r.eval("(watch rules)");
            //r.eval("(facts)");


            // ----------- MANIFEST0 -----------
            System.out.println("--> MANIFEST0");
            r.setFocus("MANIFEST0");
            r.run();
            // ---------------------------------



            // ----------- MANIFEST -----------
            System.out.println("--> MANIFEST");
            r.setFocus("MANIFEST");
            r.run();
            // ---------------------------------



            // ----------- CAPABILITIES -----------
            System.out.println("--> CAPABILITIES");
            r.setFocus("CAPABILITIES");
            r.run();
            // ------------------------------------




            r.setFocus("CAPABILITIES-REMOVE-OVERLAPS");
            r.run();

            r.setFocus("CAPABILITIES-GENERATE");
            r.run();
//            qb.saveQuery("performance/6-GENERATE-REQUIREMENTS", "REQUIREMENTS::Measurement");
//            qb.saveQuery("performance/6-GENERATE-SYNERGIES", "SYNERGIES::cross-registered");
//            qb.saveQuery("performance/6-GENERATE-CAPABILITY-LIMITATIONS", "CAPABILITIES::resource-limitations");

            r.setFocus("CAPABILITIES-CROSS-REGISTER");
            r.run();

            r.setFocus("CAPABILITIES-UPDATE");
            r.run();

            System.out.println("--> SYNERGIES");
            r.setFocus("SYNERGIES");
            r.run();
            System.out.println("--> SYNERGIES FINISHED");

            int javaAssertedFactID = 1;








            // Check if all of the orbits in the original formulation are used




            // revTimePrecomputedIndex: integer list with as many indicies as problem orbits
            // revTimePrecomputedIndex: index mapping from the problem orbit list to the precomputed orbit list
            int[] revTimePrecomputedIndex = new int[params.getOrbitList().length];



            String[] revTimePrecomputedOrbitList = {"LEO-600-polar-NA","SSO-600-SSO-AM","SSO-600-SSO-DD","SSO-800-SSO-DD","SSO-800-SSO-PM"};
            // String[] revTimePrecomputedOrbitList = params.getOrbitList();



            // loop thru revTimePrecomputedIndex,   place -1 if using orbit not in revTimePrecomputedOrbitList or place index
            // if there is a -1 in revTimePrecomputedIndex, then that problem orbit is not found in the pre-computed orbit list
            // if there is an int != -1 in revTimePrecomputedIndex, then that is that problem orbit's mapping to the index of the orbit in the pre-computed orbit list
            for(int i = 0; i < params.getOrbitList().length; i++){
                String orb = params.getOrbitList()[i];
                int matchedIndex = -1;
                for(int j = 0; j < revTimePrecomputedOrbitList.length; j++){
                    if(revTimePrecomputedOrbitList[j].equalsIgnoreCase(orb)){
                        matchedIndex = j;
                        break;
                    }
                }

                // Assign -1 if unmatched. Otherwise, assign the corresponding index
                revTimePrecomputedIndex[i] = matchedIndex;
            }







//            System.out.println("--- Revolution Time " + revTimePrecomputedIndex);
//            System.out.println("--- Measurements to Instruments Keyset " + params.measurementsToInstruments.keySet());


            // revTimePrecomputedIndex: index mapping from the problem orbit list to the precomputed orbit list
            for (String param: params.measurementsToInstruments.keySet()) {

                Value v = r.eval("(update-fovs " + param + " (create$ " + MatlabFunctions.stringArraytoStringWithSpaces(params.getOrbitList()) + "))");
                // basically for each measurement, find that measurement's corresponding fov for each of the problem orbits
                // v: contains the list of measurement fovs - one for each problem orbit
                // v: contains false if no fovs were found for any of the orbits
                // v: potentially could be a list of fovs shorter than the list of orbits



                if (RU.getTypeName(v.type()).equalsIgnoreCase("LIST")) {

                    //System.out.println("------- LIST -------");


                    ValueVector thefovs = v.listValue(r.getGlobalContext());
                    String[] fovs = new String[thefovs.size()];
                    for (int i = 0; i < thefovs.size(); i++) {
                        int tmp = thefovs.get(i).intValue(r.getGlobalContext());
                        fovs[i] = String.valueOf(tmp);
                    }

                    // list of strings containing the fov values for this measurement
//                    System.out.println("The fovs: " + thefovs);
//                    System.out.println("--> ORBITS USED: " + this.orbitsUsed);
                    //System.out.println("fovs: " + fovs);



                    boolean recalculateRevisitTime = false;
                    // if any of the measurement's fovs corresponding orbits are not pre-calculated, then calc them
                    for(int i = 0; i < fovs.length; i++){
                        if(revTimePrecomputedIndex[i] == -1){
                            // If there exists a single orbit that is different from pre-calculated ones, re-calculate
                            recalculateRevisitTime = true;
                        }
                    }

                    Double therevtimesGlobal;
                    Double therevtimesUS;

                    if(recalculateRevisitTime){
                        // Do the re-calculation of the revisit times

                        int coverageGranularity = 20;

                        //Revisit times
                        CoverageAnalysis coverageAnalysis = new CoverageAnalysis(1, coverageGranularity, true, true, params.orekitResourcesPath);
                        double[] latBounds = new double[]{FastMath.toRadians(-70), FastMath.toRadians(70)};
                        double[] lonBounds = new double[]{FastMath.toRadians(-180), FastMath.toRadians(180)};

                        List<Map<TopocentricFrame, TimeIntervalArray>> fieldOfViewEvents = new ArrayList<>();

                        // For each fieldOfview-orbit combination
                        for(Orbit orb: this.orbitsUsed){
                            int fov = thefovs.get(params.getOrbitIndexes().get(orb.toString())).intValue(r.getGlobalContext());
                            // System.out.println("--> (ORBIT, FOV) (" + orb.toString() + ", " + fov + ")");

                            if(fov <= 0){
                                continue;
                            }

                            double fieldOfView = fov; // [deg]
                            double inclination = orb.getInclinationNum(); // [deg]
                            double altitude = orb.getAltitudeNum(); // [m]
                            String raanLabel = orb.getRaan();

                            int numSats = Integer.parseInt(orb.getNum_sats_per_plane());
                            int numPlanes = Integer.parseInt(orb.getNplanes());

                            Map<TopocentricFrame, TimeIntervalArray> accesses = coverageAnalysis.getAccesses(fieldOfView, inclination, altitude, numSats, numPlanes, raanLabel);
                            fieldOfViewEvents.add(accesses);
                        }

                        // Merge accesses to get the revisit time
                        Map<TopocentricFrame, TimeIntervalArray> mergedEvents = new HashMap<>(fieldOfViewEvents.get(0));

                        for(int i = 1; i < fieldOfViewEvents.size(); ++i) {
                            Map<TopocentricFrame, TimeIntervalArray> event = fieldOfViewEvents.get(i);
                            mergedEvents = EventIntervalMerger.merge(mergedEvents, event, false);
                        }

                        therevtimesGlobal = coverageAnalysis.getRevisitTime(mergedEvents, latBounds, lonBounds)/3600;
                        therevtimesUS     = therevtimesGlobal;

                    }else{
                        System.out.println("---> no new evaluations");

                        // Re-assign fovs based on the original orbit formulation, if the number of orbits is less than 5
                        if (thefovs.size() < 5) {
                            String[] new_fovs = new String[5];
                            for (int i = 0; i < 5; i++) {
                                new_fovs[i] = fovs[revTimePrecomputedIndex[i]];
                            }
                            fovs = new_fovs;
                        }
                        String key = "1" + " x " + MatlabFunctions.stringArraytoStringWith(fovs, "  ");
                        therevtimesUS = params.revtimes.get(key).get("US"); //key: 'Global' or 'US', value Double
                        therevtimesGlobal = params.revtimes.get(key).get("Global");
                    }

                    String call = "(assert (ASSIMILATION2::UPDATE-REV-TIME (parameter " +  param + ") "
                            + "(avg-revisit-time-global# " + therevtimesGlobal + ") "
                            + "(avg-revisit-time-US# " + therevtimesUS + ")"
                            + "(factHistory J" + javaAssertedFactID + ")))";

                    //System.out.println("---> final rev time for measurement" + call);
                    javaAssertedFactID++;
                    r.eval(call);
                }
            }










            r.setFocus("ASSIMILATION2");
            r.run();
//            qb.saveQuery("performance/9-MEASUREMENT-REVISIT-TIME", "REQUIREMENTS::Measurement");

            r.setFocus("ASSIMILATION");
            r.run();

            r.setFocus("FUZZY");
            r.run();
//            qb.saveQuery("performance/10-MEASUREMENT-AFTER-FUZZY", "REQUIREMENTS::Measurement");

            r.setFocus("SYNERGIES");
            r.run();

            r.setFocus("SYNERGIES-ACROSS-ORBITS");
            r.run();
//            qb.saveQuery("requirements/requirement_facts", "REQUIREMENTS::Measurement");



            // VIIRS: no requirement rules are being fired
            if ((params.getRequestMode().equalsIgnoreCase("FUZZY-CASES")) || (params.getRequestMode().equalsIgnoreCase("FUZZY-ATTRIBUTES"))) {
                r.setFocus("FUZZY-REQUIREMENTS");
            }
            else {
                r.setFocus("REQUIREMENTS");
            }
            r.run();

            // VIIRS: all subobjective satisfaction values are 0
//            qb.saveQuery("aggregationFacts", "AGGREGATION::SUBOBJECTIVE");
            if ((params.getRequestMode().equalsIgnoreCase("FUZZY-CASES")) || (params.getRequestMode().equalsIgnoreCase("FUZZY-ATTRIBUTES"))) {
                r.setFocus("FUZZY-AGGREGATION");
            }
            else {
                r.setFocus("AGGREGATION");
            }
            r.run();

            this.arch_measurements = qb.makeQuery("REQUIREMENTS::Measurement");
//            qb.saveQuery("performance/11-MEASUREMENT-FINAL", "REQUIREMENTS::Measurement");

            if ((params.getRequestMode().equalsIgnoreCase("CRISP-ATTRIBUTES")) || (params.getRequestMode().equalsIgnoreCase("FUZZY-ATTRIBUTES"))) {
                result = aggregate_performance_score_facts(params, r, qb);
            }

//            qb.saveQuery("performance/END-PERFORMANCE", "MANIFEST::Mission");

            //////////////////////////////////////////////////////////////

            this.debug = true;
            if (this.debug) {
                ArrayList<Fact> partials = qb.makeQuery("REASONING::partially-satisfied");
                ArrayList<Fact> fulls = qb.makeQuery("REASONING::fully-satisfied");
                fulls.addAll(partials);
                //System.out.println("----- DEBUG");
                //System.out.println(partials);
                //System.out.println(fulls);
                //result.setExplanations(fulls);
            }
        }


        catch (JessException e) {
            System.out.println(e.getMessage() + " " + e.getClass() + " ");
            System.out.println(e.getProgramText());
            System.out.println(e.getContext());
            System.out.println(e.getData());
            System.out.println(e.getDetail());
            System.out.println(e.getErrorCode());
            System.out.println(e.getLineNumber());
            e.printStackTrace();
            System.exit(-1);
        }
        catch (OrekitException e) {
            e.printStackTrace();
            throw new Error();
        }
        return result;
    }

    protected Result aggregate_performance_score_facts(Problem params, Rete r, QueryBuilder qb) {

        ArrayList<ArrayList<ArrayList<Double>>> subobj_scores = new ArrayList<>();
        ArrayList<ArrayList<Double>> obj_scores = new ArrayList<>();
        ArrayList<Double> panel_scores = new ArrayList<>();
        double science = 0.0;
        double cost = 0.0;
        FuzzyValue fuzzy_science = null;
        FuzzyValue fuzzy_cost = null;
        TreeMap<String, ArrayList<Fact>> explanations = new TreeMap<>();

        // Used to compose scores from SUBOBJECTIVE up to STAKEHOLDER
        TreeMap<String, Double> subobj_scores_map = new TreeMap<>();

        try {
            // General and panel scores
            ArrayList<Fact> vals = qb.makeQuery("AGGREGATION::VALUE");
            Fact val = vals.get(0);
            System.out.println("-----------------> GETTING SCIENCE VALUE");
            //System.out.println(r.getGlobalContext());
            //System.out.println(val);
            science = val.getSlotValue("satisfaction").floatValue(r.getGlobalContext());
            if (params.getRequestMode().equalsIgnoreCase("FUZZY-ATTRIBUTES") || params.getRequestMode().equalsIgnoreCase("FUZZY-CASES")) {
                fuzzy_science = (FuzzyValue)val.getSlotValue("fuzzy-value").javaObjectValue(r.getGlobalContext());
            }
            for (String str_val: MatlabFunctions.jessList2ArrayList(val.getSlotValue("sh-scores").listValue(r.getGlobalContext()), r)) {
                panel_scores.add(Double.parseDouble(str_val));
            }

            ArrayList<Fact> subobj_facts = qb.makeQuery("AGGREGATION::SUBOBJECTIVE");
            for (Fact f: subobj_facts) {
                String subobj = f.getSlotValue("id").stringValue(r.getGlobalContext());
                Double subobj_score = f.getSlotValue("satisfaction").floatValue(r.getGlobalContext());
                Double current_subobj_score = subobj_scores_map.get(subobj);
                if(current_subobj_score == null || subobj_score > current_subobj_score) {
                    subobj_scores_map.put(subobj, subobj_score);
                }
                if (!explanations.containsKey(subobj)) {
                    explanations.put(subobj, qb.makeQuery("AGGREGATION::SUBOBJECTIVE (id " + subobj + ")"));
                }
            }

            //Subobjective scores
            //System.out.println("----- EVAL SUBOBJECTIVE");
            //System.out.println(params.numPanels);
            //System.out.println(params.numObjectivesPerPanel);
            //System.out.println(params.subobjectives);
            for (int p = 0; p < params.numPanels; p++) {
                int nob = params.numObjectivesPerPanel.get(p);
                ArrayList<ArrayList<Double>> subobj_scores_p = new ArrayList<>(nob);
                for (int o = 0; o < nob; o++) {
                    ArrayList<ArrayList<String>> subobj_p = params.subobjectives.get(p);
                    ArrayList<String> subobj_o = subobj_p.get(o);
                    int nsubob = subobj_o.size();
                    ArrayList<Double> subobj_scores_o = new ArrayList<>(nsubob);
                    for (String subobj : subobj_o) {
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
        Result theresult = new Result(arch, science, cost, fuzzy_science, fuzzy_cost, subobj_scores, obj_scores,
                panel_scores, subobj_scores_map);

        // --> Programmatic Risk
//        double programmatic_risk = this.findProgrammaticRisk(qb, r);
//        System.out.println("--> PROGRAMMATIC RISK: " + programmatic_risk);
//        theresult.setProgrammaticRisk(programmatic_risk);

        // Get explanations on subobjective fulfillment
        theresult.setCapabilities(qb.makeQuery("REQUIREMENTS::Measurement"));
        theresult.setExplanations(explanations);

        //System.out.println("----- RESULT:");
        //System.out.println(subobj_scores);
        if (this.debug) {
            System.out.println("--- debug2");
            System.out.println(theresult.capabilities);
            System.out.println(theresult.explanations);
        }

        return theresult;
    }

    public double findProgrammaticRisk(QueryBuilder qb, Rete r){

        ArrayList<Fact> missions = qb.makeQuery("MANIFEST::Mission");
        ArrayList<String> all_instruments = new ArrayList<>();

        ArrayList<Double> mission_trls = new ArrayList<>();


        for(Fact mission_fact: missions){
            double sum_trl = 0;
            double min_trl = 100;
            try{
                String slot_value = mission_fact.getSlotValue("instruments").listValue(r.getGlobalContext()).toString();
                String instruments[] = slot_value.split("\\s+");
                for(String instrument: instruments){
                    all_instruments.add(instrument);
                    double trl = qb.getInstrumentTRL(instrument);
                    sum_trl += trl;
                    if(trl < min_trl){
                        min_trl = trl;
                    }
                }
                double avg_mission_trl = sum_trl / instruments.length;
                mission_trls.add(avg_mission_trl - min_trl);
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }

        // Find packaging programmatic risk
        double sum = 0;
        for(Double trl: mission_trls){
            sum += trl;
        }
        double packaging_progammatic_risk = sum / mission_trls.size();

        // Find selecting programmatic risk
        double selecting_programmatic_risk = 0;
        for(String instrument: all_instruments){
            double inst_trl = qb.getInstrumentTRL(instrument);
            if(inst_trl < 5){
                selecting_programmatic_risk ++;
            }
        }

        return selecting_programmatic_risk + packaging_progammatic_risk;
    }


    protected void evaluateCost(Problem params, Rete r, AbstractArchitecture arch, Result res, QueryBuilder qb) {

        try {
            long t0 = System.currentTimeMillis();

            r.setFocus("MANIFEST0");
            r.run();

            r.eval("(focus MANIFEST)");
            r.eval("(run)");

            designSpacecraft(r, arch, qb);
            r.eval("(focus SAT-CONFIGURATION)");
            r.eval("(run)");

            r.eval("(focus LV-SELECTION0)");
            r.eval("(run)");
            r.eval("(focus LV-SELECTION1)");
            r.eval("(run)");
            r.eval("(focus LV-SELECTION2)");
            r.eval("(run)");
            r.eval("(focus LV-SELECTION3)");
            r.eval("(run)");

            if ((params.getRequestMode().equalsIgnoreCase("FUZZY-CASES")) || (params.getRequestMode().equalsIgnoreCase("FUZZY-ATTRIBUTES"))) {
                r.eval("(focus FUZZY-COST-ESTIMATION)");
            }
            else {
                r.eval("(focus COST-ESTIMATION)");
            }
            r.eval("(run)");

            double cost = 0.0;
            FuzzyValue fzcost = new FuzzyValue("Cost", new Interval("delta",0,0),"FY04$M");
            ArrayList<Fact> missions = qb.makeQuery("MANIFEST::Mission");
            for (Fact mission: missions)  {
                cost = cost + mission.getSlotValue("lifecycle-cost#").floatValue(r.getGlobalContext());
                if (params.getRequestMode().equalsIgnoreCase("FUZZY-ATTRIBUTES") || params.getRequestMode().equalsIgnoreCase("FUZZY-CASES")) {
                    fzcost = fzcost.add((FuzzyValue)mission.getSlotValue("lifecycle-cost").javaObjectValue(r.getGlobalContext()));
                }
            }

            res.setCost(cost);
            res.setFuzzyCost(fzcost);

            if (debug) {
                res.setCostFacts(missions);
            }

        }
        catch (JessException e) {
            System.out.println(e.toString());
            System.out.println("EXC in evaluateCost: " + e.getClass() + " " + e.getMessage());
            e.printStackTrace();
        }
    }

    protected void designSpacecraft(Rete r, AbstractArchitecture arch, QueryBuilder qb) {
        try {

            r.eval("(focus PRELIM-MASS-BUDGET)");
            r.eval("(run)");

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
    }

    protected abstract void assertMissions(Problem params, Rete r, AbstractArchitecture arch) throws JessException;

    public void setDebug(boolean debug) {
        this.debug = debug;
    }
}

