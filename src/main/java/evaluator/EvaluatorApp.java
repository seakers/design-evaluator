/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package evaluator;

import jess.Userfunction;
import seakers.orekit.util.OrekitConfig;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;
import vassar.GlobalScope;
import vassar.VassarClient;
import vassar.architecture.Optimization;
import vassar.combinatorics.Combinatorics;
import vassar.combinatorics.Nto1pair;
import vassar.database.DatabaseClient;
import vassar.database.service.DebugAPI;
import vassar.database.service.QueryAPI;
import vassar.jess.Resource;
import vassar.jess.Requests;
import vassar.jess.func.Improve;
import vassar.jess.func.SameOrBetter;
import vassar.jess.func.Worsen;
import sqs.Consumer;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;


import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
public class EvaluatorApp {

    public static void main(String[] args) {


//        Combinatorics.readNDSM_File("/app/output/DSM-2-2020-10-10-17-11-54.dat", 30);
//        Combinatorics.readNDSM_File("/app/output/DSM-3-2020-10-10-17-16-40.dat", 30);
//        Combinatorics.combineNDSM_File("DSM-2-2020-10-10-17-11-54", "SDSM");
//        Combinatorics.combineNDSM_File("DSM-3-2020-10-10-17-16-40", "SDSM");
//        Combinatorics.combineNDSM_File("DSM-5-2020-10-10-17-17-33", "SDSM");

//        ArrayList<String> insts = new ArrayList<>();
//        insts.add("SMAP_RAD");
//        insts.add("SMAP_MWR");
//        insts.add("VIIRS");
//        insts.add("CMIS");
//        insts.add("BIOMASS");
//
//        HashMap<Integer, ArrayList<String>> partition = Optimization.partitionInstruments(insts);
//        System.out.println("-----> FINAL PARTITION");
//        for(Integer key: partition.keySet()){
//            System.out.println("--> " + key + " " + partition.get(key));
//        }
//        System.exit(0);

//  _____ _   _ _____ _______
// |_   _| \ | |_   _|__   __|
//   | | |  \| | | |    | |
//   | | | . ` | | |    | |
//  _| |_| |\  |_| |_   | |
// |_____|_| \_|_____|  |_|
//

        String coverage_database = Files.root_directory + "/src/main/java/vassar/evaluator/coverage/orekit/CoverageDatabase";
        String orekit_init       = Files.root_directory + "/src/main/java/vassar/evaluator/coverage/orekit";

        System.setProperty("orekit.coveragedatabase", coverage_database);
        OrekitConfig.init(1, orekit_init);

        GlobalScope.measurementsToSubobjectives = new HashMap<>();
        GlobalScope.subobjectivesToMeasurements = new HashMap<>();

        // String rootPath = "/Users/gabeapaza/repositories/seakers/design_evaluator";
        String rootPath = ""; // DOCKER


        String outputFilePath     = Files.root_directory + "/debug/dbOutput.json";
        String outputPath         = Files.root_directory + "/debug";
//        String apollo_url         = System.getenv("APOLLO_URL");
//        String localstackEndpoint = System.getenv("AWS_STACK_ENDPOINT");
//        String queue_url          = System.getenv("EVAL_QUEUE_URL");
//        String private_queue_name = System.getenv("PRIVATE_QUEUE_NAME");
//        String apollo_ws_url      = System.getenv("APOLLO_URL_WS");
        String apollo_url         = Files.apollo_url;
        String localstackEndpoint = Files.localstackEndpoint;
        String queue_url          = Files.queue_url;
        String private_queue_name = Files.private_queue_name;
        String apollo_ws_url      = Files.apollo_ws_url;
        boolean debug             = true;

        int group_id;   //    = Integer.parseInt(System.getenv("GROUP_ID"));
        int problem_id; // = Integer.parseInt(System.getenv("PROBLEM_ID"));
        problem_id = 1; // HARDCODE
        group_id = 1;   // HARDCODE



        ArrayList<Userfunction> userFuncs = new ArrayList<>() {{
            add( new SameOrBetter() );
            add( new Improve() );
            add( new Worsen() );
        }};


        // -----> JESS REQUESTS
        String jessGlobalTempPath = Files.root_directory + "/src/main/java/vassar/database/template/defs";
        String jessGlobalFuncPath = Files.root_directory + "/src/main/java/vassar/jess/utils/clp";
        String jessAppPath        = Files.root_directory + "/problems/smap/clp";
        String requestMode        = System.getenv("REQUEST_MODE");
        requestMode = "CRISP-CASES"; // HARDCODE


        Requests requests = new Requests.Builder()
                                        .setGlobalTemplatePath(jessGlobalTempPath)
                                        .setGlobalFunctionPath(jessGlobalFuncPath)
                                        .setFunctionTemplates()
                                        .setRequestMode(requestMode)
                                        .setJessAppPath(jessAppPath)
                                        .build();


        // -----> When scaling, private queue names will be random
        if(private_queue_name.equals("RANDOM")){
            private_queue_name = "vassar_private_" + EvaluatorApp.getSaltString(8);
        }

        System.out.println("\n------------------ VASSAR INIT ------------------");
        System.out.println("----------> APOLLO URL: " + apollo_url);
        System.out.println("----> AWS ENDPOINT URL: " + localstackEndpoint);
        System.out.println("-----> INPUT QUEUE URL: " + queue_url);
        System.out.println("--> PRIVATE QUEUE NAME: " + private_queue_name);
        System.out.println("---------------> GROUP: " + group_id);
        System.out.println("-------------> PROBLEM: " + problem_id);
        System.out.println("--------> REQUEST MODE: " + requestMode);
        System.out.println("-------------------------------------------------------\n");





//  _           _ _     _
// | |         (_) |   | |
// | |__  _   _ _| | __| |
// | '_ \| | | | | |/ _` |
// | |_) | |_| | | | (_| |
// |_.__/ \__,_|_|_|\__,_|
//


        // --> SQS
        final SqsClient sqsClient = SqsClient.builder()
                                       .region(Region.US_EAST_2)
                                       .endpointOverride(URI.create(localstackEndpoint))
                                       .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                                       .build();

        // PRIVATE QUEUE
        String private_queue_url = EvaluatorApp.createPrivateQueue(sqsClient, private_queue_name, problem_id);
        Consumer.purgeQueue(sqsClient, private_queue_url);




        QueryAPI queryAPI = new QueryAPI.Builder(apollo_url, apollo_ws_url)
                                        .groupID(group_id)
                                        .problemID(problem_id)
                                        .privateQueue(private_queue_url)
                                        .sqsClient(sqsClient)
                                        .build();

        DebugAPI debugAPI = new DebugAPI.Builder(outputFilePath)
                                        .newFile()
                                        .setOutputPath(outputPath)
                                        .build();

        DatabaseClient dbClient = new DatabaseClient.Builder()
                                        .debug(debug)
                                        .queryClient(queryAPI)
                                        .debugClient(debugAPI)
                                        .subscribe()
                                        .build();

        Resource engine = new Resource.Builder(dbClient)
                                        .addUserFunctionBatch(userFuncs)      // - Improve(), SameOrBetter(), Worsen()
                                        .setRequests(requests.getRequests())  // - eval: template requests (+ functions)
                                        .setRequestMode(requestMode)
                                        .build();

        VassarClient vClient = new VassarClient.Builder()
                                        .setEngine(engine)
                                        .build();



        Consumer evaluator = new Consumer.Builder(sqsClient)
                                         .setVassarClient(vClient)
                                         .setQueueUrl(queue_url)
                                         .setPrivateQueueUrl(private_queue_url)
                                         .debug(debug)
                                         .build();



        // RUN CONSUMER
        Thread cThread = new Thread(evaluator);
        cThread.start();
    }

    public static String createPrivateQueue(SqsClient sqsClient, String private_queue_name, int problem_id){
        Map<String, String> queueParams = new HashMap<>();
        queueParams.put("problem_id", String.valueOf(problem_id));
        queueParams.put("type", "vassar_eval_private");
        CreateQueueRequest createQueueRequest = CreateQueueRequest.builder()
                .queueName(private_queue_name)
                .tags(queueParams)
                .build();
        sqsClient.createQueue(createQueueRequest);

        GetQueueUrlResponse getQueueUrlResponse = sqsClient.getQueueUrl(GetQueueUrlRequest.builder().queueName(private_queue_name).build());
        String private_url = getQueueUrlResponse.queueUrl();
        return private_url;
    }

    public static String getSaltString(int length) {
        String SALTCHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
        StringBuilder salt = new StringBuilder();
        Random rnd = new Random();
        while (salt.length() < length) { // length of the random string.
            int index = (int) (rnd.nextFloat() * SALTCHARS.length());
            salt.append(SALTCHARS.charAt(index));
        }
        String saltStr = salt.toString();
        return saltStr;
    }

    // ---> SLEEP
    public static void sleep(int seconds){
        try                            { TimeUnit.SECONDS.sleep(seconds); }
        catch (InterruptedException e) { e.printStackTrace(); }
    }
}
