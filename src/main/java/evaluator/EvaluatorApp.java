/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package evaluator;

import jess.Userfunction;
import okhttp3.OkHttpClient;
import seakers.orekit.util.OrekitConfig;
import vassar.GlobalScope;
import vassar.VassarClient;
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
import software.amazon.awssdk.services.sqs.SqsClientBuilder;
import software.amazon.awssdk.regions.Region;

import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EvaluatorApp {

    public static void main(String[] args) {

//  _____ _   _ _____ _______
// |_   _| \ | |_   _|__   __|
//   | | |  \| | | |    | |
//   | | | . ` | | |    | |
//  _| |_| |\  |_| |_   | |
// |_____|_| \_|_____|  |_|
//
        Logger.getLogger(OkHttpClient.class.getName()).setLevel(Level.FINE);
        Logger.getLogger(OkHttpClient.class.getName()).addHandler(new ConsoleHandler());
        String coverageDatabase = ResourcePaths.resourcesRootDir + "/orekit/CoverageDatabase";
        String orekitInit       = ResourcePaths.resourcesRootDir + "/orekit";

        System.setProperty("orekit.coveragedatabase", coverageDatabase);
        OrekitConfig.init(1, orekitInit);

        GlobalScope.measurementsToSubobjectives = new HashMap<>();
        GlobalScope.subobjectivesToMeasurements = new HashMap<>();

        String outputFilePath     = ResourcePaths.rootDirectory + "/debug/dbOutput.json";
        String outputPath         = ResourcePaths.rootDirectory + "/debug";

        String requestQueueUrl    = System.getenv("VASSAR_REQUEST_URL");
        String responseQueueUrl   = System.getenv("VASSAR_RESPONSE_URL");
        String apolloUrl          = System.getenv("APOLLO_URL");
        String apolloWsUrl        = System.getenv("APOLLO_URL_WS");

        boolean debug = true;

        ArrayList<Userfunction> userFuncs = new ArrayList<>() {{
            add( new SameOrBetter() );
            add( new Improve() );
            add( new Worsen() );
        }};

        // -----> JESS REQUESTS
        String jessGlobalTempPath = ResourcePaths.resourcesRootDir + "/vassar/templates";
        String jessGlobalFuncPath = ResourcePaths.resourcesRootDir + "/vassar/functions";
        String jessAppPath        = ResourcePaths.resourcesRootDir + "/vassar/problems/SMAP/clp";
        String requestMode        = System.getenv("REQUEST_MODE");

        Requests requests = new Requests.Builder()
                                        .setGlobalTemplatePath(jessGlobalTempPath)
                                        .setGlobalFunctionPath(jessGlobalFuncPath)
                                        .setFunctionTemplates()
                                        .setRequestMode(requestMode)
                                        .setJessAppPath(jessAppPath)
                                        .build();


        // -----> When scaling, private queue names will be random
        ConcurrentLinkedQueue<Map<String, String>> queue = new ConcurrentLinkedQueue<>();

        System.out.println("\n------------------ VASSAR INIT ------------------");
        System.out.println("----------> APOLLO URL: " + apolloUrl);
        System.out.println("-----> INPUT QUEUE URL: " + requestQueueUrl);
        System.out.println("-----> OUTPUT QUEUE URL: " + responseQueueUrl);
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
        SqsClientBuilder sqsClientBuilder = SqsClient.builder()
                                                           .region(Region.US_EAST_2);
        if (System.getenv("AWS_STACK_ENDPOINT") != null) {
            sqsClientBuilder.endpointOverride(URI.create(System.getenv("AWS_STACK_ENDPOINT")));
        }
        final SqsClient sqsClient = sqsClientBuilder.build();


        QueryAPI queryAPI = new QueryAPI.Builder(apolloUrl, apolloWsUrl)
                                        .privateQueue(queue)
                                        .sqsClient(sqsClient)
                                        .build();

        // Ensure debug folder exists

        DebugAPI debugAPI = new DebugAPI.Builder(outputFilePath)
                                        .setOutputPath(outputPath)
                                        .newFile()
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
                                         .setRequestQueueUrl(requestQueueUrl)
                                         .setResponseQueueUrl(responseQueueUrl)
                                         .setPrivateQueue(queue)
                                         .debug(debug)
                                         .build();

        // RUN CONSUMER
        evaluator.run();

        sqsClient.close();
    }

    // ---> SLEEP
    public static void sleep(int seconds){
        try                            { TimeUnit.SECONDS.sleep(seconds); }
        catch (InterruptedException e) { e.printStackTrace(); }
    }
}
