package vassar;



// -  -  -   ____   ____                                     ______  __    _                  _
// -  -  -  |_  _| |_  _|                                  .' ___  |[  |  (_)                / |_
// -  -  -    \ \   / /,--.   .--.   .--.   ,--.   _ .--. / .'   \_| | |  __  .---.  _ .--. `| |-'
// -  -  -     \ \ / /`'_\ : ( (`\] ( (`\] `'_\ : [ `/'`\]| |        | | [  |/ /__\\[ `.-. | | |
// -  -  -      \ ' / // | |, `'.'.  `'.'. // | |, | |    \ `.___.'\ | |  | || \__., | | | | | |,
// -  -  -       \_/  \'-;__/[\__) )[\__) )\'-;__/[___]    `.____ .'[___][___]'.__.'[___||__]\__/
// -  -  -   - Gabe: take it easy m8

import jess.Rete;
import vassar.architecture.AbstractArchitecture;
import vassar.architecture.Architecture;
import vassar.evaluator.AbstractArchitectureEvaluator;
import vassar.evaluator.ArchitectureEvaluator;
import vassar.jess.Requests;
import vassar.jess.Resource;
import vassar.result.Result;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class VassarClient {

    private Resource engine;

    public static class Builder {

        private Resource engine;

        public Builder() {

        }

        public Builder setEngine(Resource engine) {
            this.engine = engine;
            return this;
        }

        public VassarClient build() {
            VassarClient build = new VassarClient();
            build.engine = this.engine;
            return build;
        }

    }




    public void indexArchitecture(Result result, String bitString, boolean ga, boolean redo){

        double cost    = result.getCost();
        double science = result.getScience();

        int archID = this.engine.dbClient.indexArchitecture(bitString, science, cost, ga, redo);

        // TODO: write update queries for archiecture re-evaluations
        this.indexArchitectureScoreExplanations(result, archID);
    }

    private void indexArchitectureScoreExplanations(Result result, int archID){

        System.out.println("--- indexing architecture score explanations");

        for (int i = 0; i < this.engine.problem.panelNames.size(); ++i) {

            // getArchitectureScoreExplanation
            this.engine.dbClient.indexArchitectureScoreExplanation(
                    this.engine.problem.panelNames.get(i),
                    result.getPanelScores().get(i),
                    this.engine.problem.panelWeights.get(i),
                    archID
            );

            for (int j = 0; j < this.engine.problem.objNames.get(i).size(); ++j) {

                // getPanelScoreExplanation
                this.engine.dbClient.indexPanelScoreExplanation(
                        this.engine.problem.objNames.get(i).get(j),  // objective name
                        result.getObjectiveScores().get(i).get(j),   // objective satisfaction
                        this.engine.problem.objWeights.get(i).get(j), // objective weight
                        archID
                );


                for (int k = 0; k < this.engine.problem.subobjectives.get(i).get(j).size(); ++k) {

                    // getObjectiveScoreExplanation
                    this.engine.dbClient.indexObjectiveScoreExplanation(
                            this.engine.problem.subobjectives.get(i).get(j).get(k), // subobjective name
                            result.getSubobjectiveScores().get(i).get(j).get(k),    // subobjective satisfaction
                            this.engine.problem.subobjWeights.get(i).get(j).get(k), // subobjective weight
                            archID
                    );

                }
            }
        }
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

    public Result evaluateArchitecture(String bitString, boolean ga, boolean redo){

        AbstractArchitecture arch = new Architecture(bitString, 1, this.engine.getProblem());

        System.out.println(arch.isFeasibleAssignment());

        AbstractArchitectureEvaluator t = new ArchitectureEvaluator(this.engine, arch, "Slow");

        ExecutorService executorService = Executors.newFixedThreadPool(1);

        Future<Result> future = executorService.submit(t);

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

        this.indexArchitecture(result, bitString, ga, redo);

        return result;
    }




//  _____      _           _ _     _   _____
// |  __ \    | |         (_) |   | | |  __ \
// | |__) |___| |__  _   _ _| | __| | | |__) |___  ___  ___  _   _ _ __ ___ ___
// |  _  // _ \ '_ \| | | | | |/ _` | |  _  // _ \/ __|/ _ \| | | | '__/ __/ _ \
// | | \ \  __/ |_) | |_| | | | (_| | | | \ \  __/\__ \ (_) | |_| | | | (_|  __/
// |_|  \_\___|_.__/ \__,_|_|_|\__,_| |_|  \_\___||___/\___/ \__,_|_|  \___\___|


    public void rebuildResource(int group_id, int problem_id){

        // String rootPath = "/Users/gabeapaza/repositories/seakers/design_evaluator";
        String rootPath = ""; // DOCKER

        String jessGlobalTempPath = rootPath + "/app/src/main/java/vassar/database/template/defs";
        String jessGlobalFuncPath = rootPath + "/app/src/main/java/vassar/jess/utils/clp";
        String jessAppPath        = rootPath + "/app/problems/smap/clp";
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


}
