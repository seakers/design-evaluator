package vassar;



import com.evaluator.InsertArchitectureCostInformationMutation;
import com.evaluator.InsertArchitectureSlowMutation;
import com.evaluator.ProblemNameQuery;
import com.evaluator.RequirementRulesForSubobjectiveQuery;

// -  -  -   ____   ____                                     ______  __    _                  _
// -  -  -  |_  _| |_  _|                                  .' ___  |[  |  (_)                / |_
// -  -  -    \ \   / /,--.   .--.   .--.   ,--.   _ .--. / .'   \_| | |  __  .---.  _ .--. `| |-'
// -  -  -     \ \ / /`'_\ : ( (`\] ( (`\] `'_\ : [ `/'`\]| |        | | [  |/ /__\\[ `.-. | | |
// -  -  -      \ ' / // | |, `'.'.  `'.'. // | |, | |    \ `.___.'\ | |  | || \__., | | | | | |,
// -  -  -       \_/  \'-;__/[\__) )[\__) )\'-;__/[___]    `.____ .'[___][___]'.__.'[___||__]\__/
// -  -  -  - Gabe: take it easy m8
// -  -  -  - Antoni: you shouldn't have asked if you could, but rather if you should

import com.evaluator.type.*;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import evaluator.EvaluatorApp;
import evaluator.ResourcePaths;
import jess.Fact;
import jess.JessException;
import jess.Rete;
import jess.Value;
import jess.ValueVector;
import vassar.architecture.*;
import vassar.combinatorics.Combinatorics;
import vassar.database.DatabaseClient;
import vassar.evaluator.ADDEvaluator;
import vassar.evaluator.AbstractArchitectureEvaluator;
import vassar.evaluator.ArchitectureEvaluator;
import vassar.evaluator.NDSMEvaluator;
import vassar.jess.Requests;
import vassar.jess.Resource;
import vassar.result.Result;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class VassarClient {

    private Resource engine;

    private HashMap<ArrayList<ArrayList<String>>, Result> add_result_store;

    public static class Builder {

        private Resource engine;

        public Builder() {

        }

        public Builder setEngine(Resource engine) {
            this.engine = engine;
            return this;
        }

        public VassarClient build() {
            VassarClient build     = new VassarClient();
            build.engine           = this.engine;
            build.add_result_store = new HashMap<>();
            return build;
        }

    }

    public void setUserID(int id) {
        this.engine.dbClient.setUserID(id);
    }

    public boolean doesArchitectureExist(String input){
        return this.engine.dbClient.doesArchitectureExist(input);
    }



//
//  ______          _             _                           _     _ _            _
// |  ____|        | |           | |           /\            | |   (_) |          | |
// | |____   ____ _| |_   _  __ _| |_ ___     /  \   _ __ ___| |__  _| |_ ___  ___| |_ _   _ _ __ ___
// |  __\ \ / / _` | | | | |/ _` | __/ _ \   / /\ \ | '__/ __| '_ \| | __/ _ \/ __| __| | | | '__/ _ \
// | |___\ V / (_| | | |_| | (_| | ||  __/  / ____ \| | | (__| | | | | ||  __/ (__| |_| |_| | | |  __/
// |______\_/ \__,_|_|\__,_|\__,_|\__\___| /_/    \_\_|  \___|_| |_|_|\__\___|\___|\__|\__,_|_|  \___|
//


    public Result evaluateArchitecture(String input){

        // --> 1. Create architecture
        AbstractArchitecture arch = new Architecture(input, 1, this.engine.getProblem());

        // --> 2. Create evaluator
        AbstractArchitectureEvaluator t = new ArchitectureEvaluator(this.engine, arch, "Slow");

        // --> 3. Submit for evaluation
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        Future<Result> future = executorService.submit(t);

        // --> 4. Retrieve result
        Result result = null;
        try {
            result = future.get();
        }
        catch (Exception e) {
            System.out.println("Exception when evaluating an architecture");
            e.printStackTrace();
            System.exit(-1);
        }

        // --> 5. Return result
        return result;
    }

    public Result evaluateArchitectureNDSM(String input){

        // --> 1. Create evaluator
        String problem_name = this.engine.dbClient.getProblemName();
        NDSMEvaluator evaluator = new NDSMEvaluator.Builder(this.engine, input)
                .setProblem(problem_name)
                .build();

        // --> 2. Submit for evaluation
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        Future<Result> future = executorService.submit(evaluator);

        // --> 3. Retrieve result
        Result result = null;
        try {
            result = future.get();
        }
        catch (Exception e) {
            System.out.println("Exception when evaluating an architecture");
            e.printStackTrace();
            System.exit(-1);
        }

        // --> 4. Return result
        return result;

    }





    public Result evaluateADDArchitecture(String json_design){

        // ARCHITECTURE
        ADDArchitecture arch = new ADDArchitecture(json_design);


        if(this.add_result_store.containsKey(arch.get_array_info())){
            return this.add_result_store.get(arch.get_array_info());
        }
        else{
            System.out.println(arch.isFeasibleAssignment());

            ADDEvaluator process = new ADDEvaluator.Builder(this.engine)
                    .setArchitecture(arch)
                    .evalCost(true)
                    .evalPerformance(true)
                    .evalScheduling(true)
                    .orbitSelection(true)
                    .synergyDeclaration(true)
                    .build();

            ExecutorService executorService = Executors.newFixedThreadPool(1);

            Future<Result> future = executorService.submit(process);

            Result result = null;
            try {
                result = future.get();
            }
            catch (ExecutionException e) {
                System.out.println("Exception when evaluating an architecture");
                e.printStackTrace();
                System.exit(-1);
            }
            catch (InterruptedException e) {
                System.out.println("Execution got interrupted while evaluating an architecture");
                e.printStackTrace();
                System.exit(-1);
            }


            result.setDesignString(
                    arch.toString(result)
            );
            // this.add_result_store.put(arch.get_array_info(), result);
            return result;
        }
    }

    public Result evaluateSELECTINGArchitecture(String json_design){

        // ARCHITECTURE
        ADDArchitecture arch = new ADDArchitecture(json_design);
        arch.optimizeSelectingArchitecture(this.engine);


        if(this.add_result_store.containsKey(arch.get_array_info())){
            return this.add_result_store.get(arch.get_array_info());
        }
        else{

            System.out.println(arch.isFeasibleAssignment());

            ADDEvaluator process = new ADDEvaluator.Builder(this.engine)
                    .setArchitecture(arch)
                    .evalCost(true)
                    .evalPerformance(true)
                    .evalScheduling(true)
                    .orbitSelection(true)
                    .synergyDeclaration(true)
                    .build();

            ExecutorService executorService = Executors.newFixedThreadPool(1);

            Future<Result> future = executorService.submit(process);

            Result result = null;
            try {
                result = future.get();
            }
            catch (ExecutionException e) {
                System.out.println("Exception when evaluating an architecture");
                e.printStackTrace();
                System.exit(-1);
            }
            catch (InterruptedException e) {
                System.out.println("Execution got interrupted while evaluating an architecture");
                e.printStackTrace();
                System.exit(-1);
            }

            // System.out.println(arch.toString(""));
            // EvaluatorApp.sleep(10);

            result.setDesignString(
                    arch.toString(result)
            );
            this.add_result_store.put(arch.get_array_info(), result);
            return result;

        }
    }

    public Result evaluatePARTITIONINGArchitecture(String json_design){

        // ARCHITECTURE
        ADDArchitecture arch = new ADDArchitecture(json_design);
        arch.optimizePartitioningArchitecture(this.engine);


        if(this.add_result_store.containsKey(arch.get_array_info())){
            return this.add_result_store.get(arch.get_array_info());
        }
        else{

            System.out.println(arch.isFeasibleAssignment());

            ADDEvaluator process = new ADDEvaluator.Builder(this.engine)
                    .setArchitecture(arch)
                    .evalCost(true)
                    .evalPerformance(true)
                    .evalScheduling(true)
                    .orbitSelection(true)
                    .synergyDeclaration(true)
                    .build();

            ExecutorService executorService = Executors.newFixedThreadPool(1);

            Future<Result> future = executorService.submit(process);

            Result result = null;
            try {
                result = future.get();
            }
            catch (ExecutionException e) {
                System.out.println("Exception when evaluating an architecture");
                e.printStackTrace();
                System.exit(-1);
            }
            catch (InterruptedException e) {
                System.out.println("Execution got interrupted while evaluating an architecture");
                e.printStackTrace();
                System.exit(-1);
            }

            // System.out.println(arch.toString(""));
            // EvaluatorApp.sleep(10);

            result.setDesignString(
                    arch.toString(result)
            );
            this.add_result_store.put(arch.get_array_info(), result);
            return result;

        }
    }

    public Result evaluateTESTArchitecture(){

        // TEST ORBIT
//        SSO-700-SSO-PM
//        SSO-500-SSO-PM
        String orbit = "SSO-500-SSO-PM";

        // TEST INSTRUMENTS
        ArrayList<String> instruments = new ArrayList<>();
        instruments.add("ACE-CPR");

        SingleSat arch = new SingleSat(instruments, orbit);

        // EVALUATE
        ADDEvaluator process = new ADDEvaluator.Builder(this.engine)
                .setArchitecture(arch)
                .evalCost(true)
                .evalPerformance(true)
                .evalScheduling(true)
                .orbitSelection(false)
                .synergyDeclaration(true)
                .build();

        ExecutorService executorService = Executors.newFixedThreadPool(1);

        Future<Result> future = executorService.submit(process);

        Result result = null;
        try {
            result = future.get();
        }
        catch (ExecutionException e) {
            System.out.println("Exception when evaluating an architecture");
            e.printStackTrace();
            System.exit(-1);
        }
        catch (InterruptedException e) {
            System.out.println("Execution got interrupted while evaluating an architecture");
            e.printStackTrace();
            System.exit(-1);
        }


        return result;
    }




//     _   _ _____   _____ __  __
//    | \ | |  __ \ / ____|  \/  |
//    |  \| | |  | | (___ | \  / |
//    | . ` | |  | |\___ \| |\/| |
//    | |\  | |__| |____) | |  | |
//    |_| \_|_____/|_____/|_|  |_|

    public void computeNDSMs(){

        // 1 DIMENSIONAL
        Combinatorics.compute_NDSM_AI4SE(this.engine, this.engine.problem.instrumentList, 1);

        // 2 DIMENSIONAL
        Combinatorics.compute_NDSM_AI4SE(this.engine, this.engine.problem.instrumentList, 2);
    }

    public void computeContinuityMatrix(){


        ArrayList<String> mission1 = new ArrayList<>();
        mission1.add("BIOMASS");
        mission1.add("SMAP_RAD");
        mission1.add("SMAP_MWR");

        ArrayList<String> mission2 = new ArrayList<>();
        mission2.add("CMIS");
        mission2.add("SMAP_RAD");
        mission2.add("SMAP_MWR");

        HashMap<Integer, ArrayList<String>> missions = new HashMap<>();
        missions.put(0, mission1);
        missions.put(1, mission2);

        ArrayList<Integer> ordering = Optimization.scheduleMissions(missions, this.engine);
        System.out.println("---> MISSION ORDERING " + ordering);





        // Optimization.computeDataContinuityMatrix(this.engine);
    }


//     _____      _           _ _     _   _____
//    |  __ \    | |         (_) |   | | |  __ \
//    | |__) |___| |__  _   _ _| | __| | | |__) |___  ___  ___  _   _ _ __ ___ ___
//    |  _  // _ \ '_ \| | | | | |/ _` | |  _  // _ \/ __|/ _ \| | | | '__/ __/ _ \
//    | | \ \  __/ |_) | |_| | | | (_| | | | \ \  __/\__ \ (_) | |_| | | | (_|  __/
//    |_|  \_\___|_.__/ \__,_|_|_|\__,_| |_|  \_\___||___/\___/ \__,_|_|  \___\___|


    public void rebuildResource(int group_id, int problem_id){

        // String rootPath = "/Users/gabeapaza/repositories/seakers/design_evaluator";
        String rootPath = ""; // DOCKER

        String jessGlobalTempPath = ResourcePaths.resourcesRootDir + "/vassar/templates";
        String jessGlobalFuncPath = ResourcePaths.resourcesRootDir + "/vassar/functions";
        String jessAppPath        = ResourcePaths.resourcesRootDir + "/vassar/problems/SMAP/clp";
        String requestMode        = "CRISP-ATTRIBUTES";
        Requests newRequests = new Requests.Builder()
                                           .setGlobalTemplatePath(jessGlobalTempPath)
                                           .setGlobalFunctionPath(jessGlobalFuncPath)
                                           .setFunctionTemplates()
                                           .setRequestMode(requestMode)
                                           .setJessAppPath(jessAppPath)
                                           .build();

        Resource newResource = this.engine.rebuild(group_id, problem_id, newRequests.getRequests());
        this.engine          = newResource;
    }


//     _    _      _
//    | |  | |    | |
//    | |__| | ___| |_ __   ___ _ __ ___
//    |  __  |/ _ \ | '_ \ / _ \ '__/ __|
//    | |  | |  __/ | |_) |  __/ |  \__ \
//    |_|  |_|\___|_| .__/ \___|_|  |___/
//                  | |
//                  |_|

    public Resource getEngine(){
        return this.engine;
    }

    public DatabaseClient getDbClient(){ return this.engine.dbClient; }



}
