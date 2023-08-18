package vassar.jess;

import jess.*;
import vassar.GlobalScope;
import vassar.database.DatabaseClient;
import vassar.database.template.TemplateRequest;
import vassar.evaluator.ModelParser;
import vassar.jess.func.RawSafety;
import vassar.problem.Problem;
import vassar.evaluator.spacecraft.LaunchVehicle;
import vassar.matlab.MatlabFunctions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class Resource {

    // Build Member Variables
    private ArrayList<Userfunction> buildFuncs;
    private ArrayList<TemplateRequest> buildRequests;


    // Evaluate Member Variables
    private Rete           engine;
    private String         appPath;
    public  DatabaseClient dbClient;
    public  Problem        problem;
    private String         requestMode;
    private QueryBuilder   queryBuilder;
    private MatlabFunctions mFuncs;

    public Problem.Builder            problemBuilder;

    public static class Builder {

        // Build Member Variables
        private ArrayList<Userfunction> buildFuncs;

        // Evaluate Member Variables
        private Rete                       engine;
        private String                     appPath;
        private String                     requestMode;
        private DatabaseClient             dbClient;
        private ArrayList<TemplateRequest> requests;
        private Problem.Builder            problemBuilder;
        private QueryBuilder               queryBuilder;
        private MatlabFunctions            mFuncs;

        public Builder(DatabaseClient dbClient){
            this.engine         = new Rete();
            this.problemBuilder = new Problem.Builder();
            this.dbClient       = dbClient;
            this.queryBuilder   = new QueryBuilder.Builder(this.engine).build();
            this.mFuncs         = new MatlabFunctions();

            this.engine.addUserfunction(this.mFuncs);
        }

        public Builder addUserFunctionBatch(ArrayList<Userfunction> funcs) {
            this.buildFuncs = funcs;
            funcs.forEach( func -> this.engine.addUserfunction(func));
            return this;
        }

        public Builder setRequests(ArrayList<TemplateRequest> requests) {
            this.requests = requests;
            return this;
        }

        public Builder setRequestMode(String requestMode) {
            this.requestMode = requestMode;
            this.problemBuilder.setRequestMode(requestMode);
            return this;
        }

        public Resource build(){
            Resource build       = new Resource();
            build.buildRequests  = this.requests;
            build.buildFuncs     = this.buildFuncs;
            build.dbClient       = this.dbClient;
            build.engine         = this.engine;
            build.appPath        = this.appPath;
            build.requestMode    = this.requestMode;
            build.queryBuilder   = this.queryBuilder;
            build.problemBuilder = this.problemBuilder;
            build.mFuncs         = this.mFuncs;
            return build;
        }
    }

    public Rete getEngine() {
        return this.engine;
    }
    public Problem getProblem() {
        return this.problem;
    }
    public QueryBuilder getQueryBuilder() { return this.queryBuilder; }



    public Resource rebuild(int group_id, int problem_id, ArrayList<TemplateRequest> newRequests){

        if(group_id != -1){
            this.dbClient.setGroupID(group_id);
        }

        if(problem_id != -1){
            this.dbClient.setProblemID(problem_id);
        }

        this.dbClient.resubscribe();
        this.dbClient.resetCache();

        GlobalScope.init();

        Resource newResource = new Resource.Builder(this.dbClient).addUserFunctionBatch(this.buildFuncs).setRequests(newRequests).setRequestMode(this.requestMode).build();
        newResource.init();
        return   newResource;
    }

    public void init() {

        // EVALUATE TEMPLATE REQUESTS
        long startTime = System.nanoTime();
        this.buildRequests.forEach(
                request -> this.dbClient.processTemplateRequest(request, this.problemBuilder).evaluate(this.engine)
        );
        long endTime = System.nanoTime();
        System.out.println("Took "+(endTime - startTime) + " ns");

        // EVALUATE RULES
        this.evaluateRules();

        // RESET
        try                { this.engine.reset(); }
        catch(Exception e) { System.out.println("Failed final jess reset"); }

        // BUILD LAUNCH VEHICLES
        this.buildLaunchVehicles(
                queryBuilder.makeQuery("DATABASE::Launch-vehicle")
        );

        // RESET
        try                { this.engine.reset(); }
        catch(Exception e) { System.out.println("Failed final jess reset"); }

        // PRECOMPUTE QUERIES
        this.loadPrecomputedQueries();

        // BUILD PROBLEM
        this.problem = this.problemBuilder.build(this.dbClient);

        // PARSE RULES
//        try{
//            ModelParser mParser = new ModelParser(this.engine);
//            mParser.saveVocabulary();
//        }
//        catch (Exception ex){
//            ex.printStackTrace();
//        }
    }

    private void evaluateRules() {

        int cnt = 0;
        Iterator<HasLHS> ruleIter = RawSafety.castType(this.engine.listDefrules());
        Iterator<HasLHS> ruleIterCheck = RawSafety.castType(this.engine.listDefrules());
        while (ruleIter.hasNext()) {
            HasLHS ruleCheck = ruleIterCheck.next();
            if (ruleCheck instanceof Defquery) {
                ruleIter.next();
                ruleIter.remove();
            }
            else if (ruleCheck instanceof Defrule) {
                cnt++;
                Defrule currentRule = (Defrule)ruleIter.next();
                String rule = "(?*rulesMap* put " + currentRule.getName() + " " + cnt + ")";
                System.out.println(rule);
                try {
                    this.engine.eval(rule);
                }
                catch (Exception e) {
                    System.out.println("Exception while post-processing final rule set " + e);
                }
            }
        }
    }

    private void buildLaunchVehicles(ArrayList<Fact> facts){
        try {
            this.engine.reset();
            for (Fact lv: facts) {
                String id = lv.getSlotValue("id").stringValue(this.engine.getGlobalContext());
                double cost = lv.getSlotValue("cost").floatValue(this.engine.getGlobalContext());
                double diam = lv.getSlotValue("diameter").floatValue(this.engine.getGlobalContext());
                double height = lv.getSlotValue("height").floatValue(this.engine.getGlobalContext());
                HashMap<String, ValueVector> payload_coeffs = new HashMap<>();
                ValueVector payload_LEO_polar = lv.getSlotValue("payload-LEO-polar").listValue(this.engine.getGlobalContext());
                ValueVector payload_SSO = lv.getSlotValue("payload-SSO").listValue(this.engine.getGlobalContext());
                ValueVector payload_LEO_equat = lv.getSlotValue("payload-LEO-equat").listValue(this.engine.getGlobalContext());
                ValueVector payload_MEO = lv.getSlotValue("payload-MEO").listValue(this.engine.getGlobalContext());
                ValueVector payload_GEO = lv.getSlotValue("payload-GEO").listValue(this.engine.getGlobalContext());
                ValueVector payload_HEO = lv.getSlotValue("payload-HEO").listValue(this.engine.getGlobalContext());
                payload_coeffs.put("LEO-polar", payload_LEO_polar); // LEO-near-polar
                payload_coeffs.put("LEO-near-polar", payload_LEO_polar);
                payload_coeffs.put("SSO-SSO", payload_SSO);
                payload_coeffs.put("LEO-equat", payload_LEO_equat);
                payload_coeffs.put("MEO-polar", payload_MEO);
                payload_coeffs.put("GEO-equat", payload_GEO);
                payload_coeffs.put("HEO-polar", payload_HEO);
                LaunchVehicle lvh = new LaunchVehicle(id, payload_coeffs, diam, height, cost);
                this.mFuncs.addLaunchVehicletoDB(id, lvh);
            }
        }
        catch (Exception e) {
            System.out.println("Error building launch vehicles: " + e);
            e.printStackTrace();
        }
    }

    private void loadPrecomputedQueries(){
        HashMap<String,Fact> db_instruments = new HashMap<>();
        Problem params = this.problemBuilder.build(this.dbClient);
        System.out.println("----> NUM INSTRUMENTS " + params.getNumInstr());
        for (int i = 0; i < params.getNumInstr(); i++) {
            String instr = params.getInstrumentList()[i];
            System.out.println(instr);
            ArrayList<Fact> facts = queryBuilder.makeQuery("DATABASE::Instrument (Name " + instr + ")");
            Fact f = facts.get(0);
            db_instruments.put(instr, f);
        }
        queryBuilder.addPrecomputedQuery("DATABASE::Instrument", db_instruments);
    }

    public void resetEngine(){
        try{
            this.engine.reset();
        } catch (JessException e) {
            e.printStackTrace();
        }
    }



}
