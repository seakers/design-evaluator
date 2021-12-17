package vassar.evaluator;

import evaluator.EvaluatorApp;
import jess.Fact;
import jess.JessException;
import jess.Rete;
import org.paukov.combinatorics3.IGenerator;
import vassar.architecture.AbstractArchitecture;
import vassar.combinatorics.Combinatorics;
import vassar.combinatorics.NDSM2;
import vassar.evaluator.spacecraft.Orbit;
import vassar.jess.QueryBuilder;
import vassar.jess.Resource;
import vassar.jess.utils.Interval;
import vassar.result.FuzzyValue;
import vassar.result.Result;

import java.util.*;
import java.util.concurrent.Callable;

import org.paukov.combinatorics3.Generator;

public class NDSMEvaluator implements Callable<Result> {

    public HashMap<String, ArrayList<String>> design;
    public HashMap<String, HashMap<String,NDSM2>> _1DSM;
    public HashMap<String, HashMap<String,NDSM2>> _2DSM;
    public HashMap<String, Double> panel_weights_map;
    public Resource engine;
    public String design_string;
    public String[] orbits;
    public String[] instruments;


    public static class Builder{

        public HashMap<String, HashMap<String,NDSM2>> _1DSM;
        public HashMap<String, HashMap<String,NDSM2>> _2DSM;
        public Resource engine;
        public String design_string;
        public String[] orbits;

        public Builder(Resource engine, String design_string){
            this.engine = engine;
            this.design_string = design_string;
            this.orbits = engine.getProblem().orbitList;
        }

        public Builder setProblem(String problem_name){
            String _1DSM_file = "";
            String _2DSM_file = "";

            if(Objects.equals(problem_name, "ClimateCentric")){
                _1DSM_file = "/app/output/DSM-Climate_1-1-2021-11-10-17-41-01.dat";
                _2DSM_file = "/app/output/DSM-Climate_1-2-2021-11-10-17-56-41.dat";
            }
            else if(Objects.equals(problem_name, "ClimateCentric_1")){
                _1DSM_file = "/app/output/DSM-Climate_1-1-2021-11-10-17-41-01.dat";
                _2DSM_file = "/app/output/DSM-Climate_1-2-2021-11-10-17-56-41.dat";
            }
            else if(Objects.equals(problem_name, "ClimateCentric_2")){
                _1DSM_file = "/app/output/DSM-Climate_2-1-2021-11-10-18-14-15.dat";
                _2DSM_file = "/app/output/DSM-Climate_2-2-2021-11-10-18-30-07.dat";
            }
            else{
                System.out.println("--> INVALID NDSM PROBLEM !!!");
                return this;
            }

            this.set_1DSM(_1DSM_file);
            this.set_2DSM(_2DSM_file);
            return this;
        }

        public void set_1DSM(String path){
            this._1DSM = new HashMap<>();
            HashMap<String, NDSM2> ndsm_map = Combinatorics.getNDSM2s(path);
            int dim = 1;

            // Iterate over orbits, unpack NDSM map
            for(String orbit: this.orbits){
                String s1_key = "S1DSM" + dim + "@" + orbit; // Oceanic
                String s2_key = "S2DSM" + dim + "@" + orbit; // Atmosphere
                String s3_key = "S3DSM" + dim + "@" + orbit; // Terrestrial
                String d_key =  "DDSM" + dim + "@" + orbit;  // Data-Continuity
                String c_key =  "CDSM" + dim + "@" + orbit;  // Cost

                HashMap<String, NDSM2> temp = new HashMap<>();
                temp.put("Oceanic", ndsm_map.get(s1_key));
                temp.put("Atmosphere", ndsm_map.get(s2_key));
                temp.put("Terrestrial", ndsm_map.get(s3_key));
                temp.put("data-continuity", ndsm_map.get(d_key));
                temp.put("cost", ndsm_map.get(c_key));

                this._1DSM.put(orbit, temp);
            }
        }

        public void set_2DSM(String path){
            this._2DSM = new HashMap<>();
            HashMap<String, NDSM2> ndsm_map = Combinatorics.getNDSM2s(path);
            int dim = 2;

            // Iterate over orbits, unpack NDSM map
            for(String orbit: this.orbits){
                String s1_key = "S1DSM" + dim + "@" + orbit; // Oceanic
                String s2_key = "S2DSM" + dim + "@" + orbit; // Atmosphere
                String s3_key = "S3DSM" + dim + "@" + orbit; // Terrestrial
                String d_key =  "DDSM" + dim + "@" + orbit;  // Data-Continuity
                String c_key =  "CDSM" + dim + "@" + orbit;  // Cost

                HashMap<String, NDSM2> temp = new HashMap<>();
                temp.put("Oceanic", ndsm_map.get(s1_key));
                temp.put("Atmosphere", ndsm_map.get(s2_key));
                temp.put("Terrestrial", ndsm_map.get(s3_key));
                temp.put("data-continuity", ndsm_map.get(d_key));
                temp.put("cost", ndsm_map.get(c_key));

                this._2DSM.put(orbit, temp);
            }
        }

        public NDSMEvaluator build(){
            NDSMEvaluator build = new NDSMEvaluator();
            build.design_string = design_string;
            build.design = new HashMap<>();
            build.orbits = this.engine.getProblem().orbitList;
            build.instruments = this.engine.getProblem().instrumentList;

            // Iterate over orbits - build design
            int counter = 0;
            for(int x = 0; x < build.orbits.length; x++){
                String orbit = build.orbits[x];
                for(int y = 0; y < build.instruments.length; y++){
                   String instrument = build.instruments[y];
                   if(build.design_string.charAt(counter) == '1'){
                       if(!build.design.containsKey(orbit)){
                           ArrayList<String> orb_insts = new ArrayList<>();
                           build.design.put(orbit, orb_insts);
                       }
                       build.design.get(orbit).add(instrument);
                   }
                    counter++;
                }
            }

            build.panel_weights_map = this.engine.getProblem().panelWeightMap;
            build._1DSM = this._1DSM;
            build._2DSM = this._2DSM;
            build.engine = this.engine;
            return build;
        }
    }



    public Result call(){

        // --> Result
        Result result = new Result();

        long startTime = System.nanoTime();
        double cost = this.evaluate_cost(result);
        double oceanic_score = this.evaluate_score("Oceanic");
        double atmospheric_score = this.evaluate_score("Atmosphere");
        double terrestrial_score = this.evaluate_score("Terrestrial");
        double data_continuity_score = this.evaluate_score("data-continuity");
        double fairness = this.evaluate_fairness(oceanic_score, atmospheric_score, terrestrial_score);
        double programmatic_risk = this.evaluate_programmatic_risk();
        long endTime = System.nanoTime();
        System.out.println("Took "+(endTime - startTime) / 1000000000.0 + " ns");

        System.out.println("--> OCEANIC " + oceanic_score);
        System.out.println("--> ATMOSPHERE " + atmospheric_score);
        System.out.println("--> TERRESTRIAL " + terrestrial_score);
        System.out.println("--> DATA CONTINUITY " + data_continuity_score);
        System.out.println("--> FAIRNESS " + fairness);
        System.out.println("--> PROGRAMMATIC RISK " + programmatic_risk);
        System.out.println("--> COST " + cost);


        HashMap<String, Double> panel_scores = new HashMap<>();
        panel_scores.put("Terrestrial", terrestrial_score);
        panel_scores.put("Atmosphere", atmospheric_score);
        panel_scores.put("Oceanic", oceanic_score);
        ArrayList<Double> panel_scores_list = new ArrayList<>();
        for(String panel: this.engine.getProblem().panelNames){
            panel_scores_list.add(panel_scores.get(panel));
        }
        result.panelScores = panel_scores_list;
        result.fairnessScore = fairness;
        result.dataContinuity = data_continuity_score;
        result.cost = cost;
        result.programmaticRisk = programmatic_risk;




        return result;
    }


//     _____          _
//    / ____|        | |
//   | |     ___  ___| |_
//   | |    / _ \/ __| __|
//   | |___| (_) \__ \ |_
//    \_____\___/|___/\__|

    public void load_design_rbs(boolean reset){
        // Reset engine
        if(reset){
            this.engine.resetEngine();
        }


        // 1. Load architecture into Jess
        String mission_name = "";
        int counter = 0;
        for(String orbit: this.design.keySet()){
            Orbit orb = new Orbit(orbit, 1, 1);

            // MISSION DEFINITION
            mission_name = "mission" + Integer.toString(counter);
            mission_name = orbit;
            String mission = "(assert (MANIFEST::Mission ";
            String payload = "";
            for(String instrument: this.design.get(orbit)){
                payload += (" " + instrument);
            }

            // ASSERTIONS: synergies now asserted after orbit is selected (in ADDEvaluator)
            String mission_fact_str = "(MANIFEST::Mission (Name "+mission_name+") (instruments " + payload + ") (lifetime 5) (launch-date 2015) (select-orbit no) "+ orb.toJessSlots() +" (factHistory F0))";
            String synergy_fact_str = "(SYNERGIES::cross-registered-instruments (instruments " + payload + ") (platform " + orbit + ") (degree-of-cross-registration spacecraft) (factHistory F0))";

            // ASSERT
            try {

                Fact mission_fact = this.engine.getEngine().assertString(mission_fact_str);
                Fact synergy_fact = this.engine.getEngine().assertString(synergy_fact_str);

            } catch (JessException e) {
                e.printStackTrace();
            }

            counter++;
        }
    }

    public void load_comparators(){
        // Add Comparators
        try{
            this.engine.getEngine().eval("(bind ?*science-multiplier* 1.0)");
            this.engine.getEngine().eval("(defadvice before (create$ >= <= < >) (foreach ?xxx $?argv (if (eq ?xxx nil) then (return FALSE))))");
            this.engine.getEngine().eval("(defadvice before (create$ sqrt + * **) (foreach ?xxx $?argv (if (eq ?xxx nil) then (bind ?xxx 0))))");
        }
        catch (JessException e){
            e.printStackTrace();
        }
    }

    public double evaluate_cost(Result res){
        Rete r = this.engine.getEngine();
        double cost = 0.0;
        this.load_comparators();

        // 1. Reset engine globals
        this.resetEngineGlobals(true);

        // 2. Initialize engine
        this.load_design_rbs(true);


        // 3. Run Jess
        try{
            r.setFocus("MANIFEST0");
            r.run();

            r.eval("(focus MANIFEST)");
            r.eval("(run)");

            r.setFocus("CAPABILITIES");
            r.run();

            this.designSpacecraft(r, this.engine.getQueryBuilder());

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

            r.eval("(focus COST-ESTIMATION)");
            r.eval("(run)");

            ArrayList<Fact> missions = this.engine.getQueryBuilder().makeQuery("MANIFEST::Mission");
            for (Fact mission: missions)  {
                cost = cost + mission.getSlotValue("lifecycle-cost#").floatValue(r.getGlobalContext());
            }
            res.setCostFacts(missions);
        }
        catch (JessException e){
            e.printStackTrace();
        }

        return cost;
    }

    public void resetEngineGlobals(boolean globals){
        try{
            if(globals){
                this.engine.getEngine().eval("(set-reset-globals TRUE)");
            }
            this.engine.getEngine().eval("(reset)");
        }
        catch (JessException e){

            e.printStackTrace();
        }
    }

    protected void designSpacecraft(Rete r, QueryBuilder qb) {
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




//     _____           __
//    |  __ \         / _|
//    | |__) |__ _ __| |_ ___  _ __ _ __ ___   __ _ _ __   ___ ___
//    |  ___/ _ \ '__|  _/ _ \| '__| '_ ` _ \ / _` | '_ \ / __/ _ \
//    | |  |  __/ |  | || (_) | |  | | | | | | (_| | | | | (_|  __/
//    |_|   \___|_|  |_| \___/|_|  |_| |_| |_|\__,_|_| |_|\___\___|


    // Oceanic, Atmospheric, Terrestrial, Data Continuity
    public double evaluate_score(String key){
        double score = 0.0;

        // 1. Iterate over orbits
        for(String orbit: this.design.keySet()){
            double sat_score = 0.0;

            // NDSMs
            NDSM2 temp_1DSM = this._1DSM.get(orbit).get(key);
            NDSM2 temp_2DSM = this._2DSM.get(orbit).get(key);


            // Instruments
            String[] instruments = this.design.get(orbit).toArray(new String[0]);

            // -- INDIVIDUAL SCORE
            for(String instrument: instruments){
                String[] inst_arr = new String[]{instrument};

                // Iterate over dsm map keys
                for(String[] inst_key: temp_1DSM.map.keySet()){
                    if(inst_key[0].equals(instrument)){
                        sat_score += temp_1DSM.map.get(inst_key);
                    }
                }
            }

            // -- SYNERGY SCORE
            IGenerator gen = Generator.combination(instruments).simple(2);
            Iterator it = gen.iterator();
            double synergy_score = 0;
            while(it.hasNext()){
                String[] instrument_combination = ((ArrayList<String>)it.next()).toArray(new String[0]);

                double total_score = 0;
                for(String[] inst_key: temp_2DSM.map.keySet()){
                    ArrayList<String> inst_key_list = new ArrayList(Arrays.asList(inst_key));
                    if(inst_key_list.contains(instrument_combination[0]) && inst_key_list.contains(instrument_combination[1])){
                        total_score += temp_2DSM.map.get(inst_key);
                        break;
                    }
                }

                double sub_score = 0;
                for(String instrument: instrument_combination){
                    for(String[] inst_key: temp_1DSM.map.keySet()){
                        ArrayList<String> inst_key_list = new ArrayList(Arrays.asList(inst_key));
                        if(inst_key_list.contains(instrument)){
                            sub_score += temp_1DSM.map.get(inst_key);
                        }
                    }
                }
                synergy_score = synergy_score + (total_score - sub_score);
            }
            sat_score += synergy_score;
            sat_score *= this.get_science_multiplier(orbit);
            score += sat_score;
        }
        if(this.panel_weights_map.containsKey(key)){
            score = score * this.panel_weights_map.get(key);
        }
        return score;
    }

    // Programmatic Risk
    public double evaluate_programmatic_risk(){
        QueryBuilder q_builder = this.engine.getQueryBuilder();
        ArrayList<Double> mission_trls = new ArrayList<>();
        ArrayList<String> all_instruments = new ArrayList<>();
        for(String orbit: this.design.keySet()){
            double sat_trl_sum = 0;
            double min_inst_trl = 100;
            ArrayList<String> instruments = this.design.get(orbit);
            for(String instrument: instruments){
                if(!all_instruments.contains(instrument)){
                    all_instruments.add(instrument);
                }
                double inst_trl = q_builder.getInstrumentTRL(instrument);
                sat_trl_sum += inst_trl;
                if(inst_trl < min_inst_trl){
                    min_inst_trl = inst_trl;
                }
            }
            double avg_mission_trl = sat_trl_sum / instruments.size();
            mission_trls.add(avg_mission_trl - min_inst_trl);
        }

        // Find packaging programmatic risk
        double sum = 0;
        for(Double trl: mission_trls){
            sum += trl;
        }
        double packaging_progammatic_risk = sum / mission_trls.size();


        double selecting_programmatic_risk = 0;
        for(String instrument: all_instruments){
            double inst_trl = q_builder.getInstrumentTRL(instrument);
            if(inst_trl < 5){
                selecting_programmatic_risk++;
            }
        }

        return (packaging_progammatic_risk + selecting_programmatic_risk);
    }

    // Fairness
    public double evaluate_fairness(double oceanic, double atmospheric, double data_continuity){
        ArrayList<Double> list = new ArrayList<>();
        list.add(oceanic);
        list.add(atmospheric);
        list.add(data_continuity);
        Collections.sort(list);
        return (list.get(1) - list.get(0));
    }

    public double get_science_multiplier(String orbit){
        double science_multiplier = 1;

        ArrayList<Fact> facts = this.engine.getQueryBuilder().getMissionCanMeasure(orbit);
        try{
            for(Fact fact: facts){
                double data_rate_dc = fact.getSlotValue("data-rate-duty-cycle#").floatValue(this.engine.getEngine().getGlobalContext());
                if(science_multiplier > data_rate_dc){
                    science_multiplier = data_rate_dc;
                }
            }
        }
        catch (JessException e){
            e.printStackTrace();
        }
        return science_multiplier;
    }




}




