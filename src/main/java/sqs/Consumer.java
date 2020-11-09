package sqs;

import evaluator.EvaluatorApp;
import software.amazon.awssdk.services.sqs.model.*;
import vassar.VassarClient;
import software.amazon.awssdk.services.sqs.SqsClient;
import vassar.result.Result;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class Consumer implements Runnable{

    private boolean      debug;
    private boolean      running;
    private VassarClient client;
    private SqsClient    sqsClient;
    private String       queueUrl;
    private String       privateQueueUrl;

    public static class Builder {

        private boolean        debug;
        private VassarClient   client;
        private SqsClient      sqsClient;
        private String         queueUrl;
        private String         privateQueueUrl;

        public Builder(SqsClient sqsClient){
            this.sqsClient = sqsClient;
        }

        public Builder setVassarClient(VassarClient client) {
            this.client = client;
            return this;
        }

        public Builder setQueueUrl(String queueUrl) {
            this.queueUrl = queueUrl;
            return this;
        }

        public Builder setPrivateQueueUrl(String privateQueueUrl) {
            this.privateQueueUrl = privateQueueUrl;
            return this;
        }

        public Builder debug(boolean debug) {
            this.debug = debug;
            return this;
        }

        public Consumer build(){
            Consumer build        = new Consumer();
            build.sqsClient       = this.sqsClient;
            build.debug           = this.debug;
            build.client          = this.client;
            build.queueUrl        = this.queueUrl;
            build.privateQueueUrl = this.privateQueueUrl;
            build.running         = true;
            return build;
        }

    }



    // ----- MESSAGE TYPES -----
    // 1. Evaluate architecture message
    // 2. Reload rete object from database



    public void run() {
        int counter = 0;

        // this.sendTestMessages();
        this.deletePrivMessages();

        while(this.running){
            System.out.println("-----> Loop iteration: " + counter);
            // this.consumerSleep(1);
            List<Message> messages = new ArrayList<>();
            boolean privateMsg = true;

            // CHECK PRIVATE QUEUE FIRST - IF PRIVATE QUEUE EMPTY, CHECK EVAL QUEUE
            // messages = this.getMessages(this.privateQueueUrl, 1, 1);
            if(messages.isEmpty()){
                messages   = this.getMessages(this.queueUrl, 3, 5);
                privateMsg = false;
            }

            for (Message msg: messages){
                HashMap<String, String> msg_contents = this.processMessage(msg);
                System.out.println(msg_contents);

                if(msg_contents.containsKey("msgType")){
                    String msgType = msg_contents.get("msgType");
                    if(msgType.equals("evaluate")){
                        this.msgTypeEvaluate(msg_contents);
                    }
                    else if(msgType.equals("add")){
                        this.msgTypeADD(msg_contents);
                    }
                    else if(msgType.equals("Instrument Selection")){
                        this.msgTypeSELECTING(msg_contents);
                    }
                    else if(msgType.equals("Instrument Partitioning")){
                        this.msgTypePARTITIONING(msg_contents);
                    }
                    else if(msgType.equals("TEST-EVAL")){
                        this.msgTypeTEST_EVAL(msg_contents);
                    }
                    else if(msgType.equals("NDSM")){
                        this.msgTypeNDSM(msg_contents);
                    }
                    else if(msgType.equals("ContinuityMatrix")){
                        this.msgTypeContinuityMatrix(msg_contents);
                    }
                    else if(msgType.equals("build")){
                        this.msgTypeBuild(msg_contents);
                    }
                    else if(msgType.equals("exit")){
                        System.out.println("----> Exiting gracefully");
                        this.running = false;
                    }
                }
                else{
                    System.out.println("-----> INCOMING MESSAGE DIDN'T HAVE ATTRIBUTE: msgType");
                    // this.consumerSleep(10);
                }
            }

            if(privateMsg){
                this.deleteMessages(messages, this.privateQueueUrl);
            }
            else{
                this.deleteMessages(messages, this.queueUrl);
            }

            counter++;
        }


    }



    // ---> MESSAGE TYPES
    public void msgTypeEvaluate(HashMap<String, String> msg_contents){

        String  input       = msg_contents.get("input");
        boolean ga_arch     = false;
        boolean re_evaluate = false;
        boolean fast        = false;


        if(msg_contents.containsKey("ga")){
            ga_arch = Boolean.parseBoolean(msg_contents.get("ga"));
        }

        if(msg_contents.containsKey("redo")){
            re_evaluate = Boolean.parseBoolean(msg_contents.get("redo"));
        }

        if(msg_contents.containsKey("fast")){
            fast = Boolean.parseBoolean(msg_contents.get("fast"));
        }

        if(!re_evaluate) {
            if (this.client.doesArchitectureExist(input)) {
                System.out.println("---> Architecture already exists!!!");
                System.out.println("---> INPUT: " + input);
                this.consumerSleep(1);
                return;
            }
        }

        Result result = this.client.evaluateArchitecture(input, ga_arch, re_evaluate, fast);

        System.out.println("\n-------------------- EVALUATE REQUEST OUTPUT --------------------");
        System.out.println("-----> INPUT: " + input);
        System.out.println("------> COST: " + result.getCost());
        System.out.println("---> SCIENCE: " + result.getScience());
        System.out.println("----------------------------------------------------------------\n");
        // this.consumerSleep(3);
    }

    public void msgTypeADD(HashMap<String, String> msg_contents){

        String input  = msg_contents.get("input");
        String rQueue = msg_contents.get("rQueue");
        String UUID   = msg_contents.get("UUID");

        System.out.println("\n-------------------- EVALUATE ADD REQUEST INPUT --------------------");
        System.out.println("---------> INPUT: " + input);
        System.out.println("--> RETURN QUEUE: " + rQueue);
        System.out.println("----------> UUID: " + UUID);
        System.out.println("-------------------------------------------------------------------\n");
//        EvaluatorApp.sleep(5);

        Result result = this.client.evaluateADDArchitecture(input);

        this.sendResultMessage(rQueue, UUID, result);

        System.out.println("\n-------------------- EVALUATE ADD REQUEST OUTPUT --------------------");
        System.out.println("----> DESIGN STRING: " + result.getDesignString());
        System.out.println("------------> INPUT: " + input);
        System.out.println("-------------> COST: " + result.getCost());
        System.out.println("----------> SCIENCE: " + result.getScience());
        System.out.println("--> DATA CONTINUITY: " + result.getDataContinuityScore());
        System.out.println("--------------------------------------------------------------------\n");
        // this.consumerSleep(20);
    }


    public void msgTypeSELECTING(HashMap<String, String> msg_contents){

        String input  = msg_contents.get("input");
        String rQueue = msg_contents.get("rQueue");
        String UUID   = msg_contents.get("UUID");

        System.out.println("\n-------------------- EVALUATE SELECTING REQUEST INPUT --------------------");
        System.out.println("---------> INPUT: " + input);
        System.out.println("--> RETURN QUEUE: " + rQueue);
        System.out.println("----------> UUID: " + UUID);
        System.out.println("-------------------------------------------------------------------\n");
        // EvaluatorApp.sleep(5);

        Result result = this.client.evaluateSELECTINGArchitecture(input);

        this.sendResultMessage(rQueue, UUID, result);

        System.out.println("\n-------------------- EVALUATE SELECTING REQUEST OUTPUT --------------------");
        System.out.println("------------> INPUT: " + input);
        System.out.println("-------------> COST: " + result.getCost());
        System.out.println("----------> SCIENCE: " + result.getScience());
        System.out.println("--> DATA CONTINUITY: " + result.getDataContinuityScore());
        System.out.println("----> DESIGN STRING: " + result.getDesignString());
        System.out.println("--------------------------------------------------------------------\n");
        System.out.println("--> SELECTING <--");
        // EvaluatorApp.sleep(5);
    }

    public void msgTypePARTITIONING(HashMap<String, String> msg_contents){

        String input  = msg_contents.get("input");
        String rQueue = msg_contents.get("rQueue");
        String UUID   = msg_contents.get("UUID");

        System.out.println("\n-------------------- EVALUATE PARTITIONING REQUEST INPUT --------------------");
        System.out.println("---------> INPUT: " + input);
        System.out.println("--> RETURN QUEUE: " + rQueue);
        System.out.println("----------> UUID: " + UUID);
        System.out.println("-------------------------------------------------------------------\n");

        Result result = this.client.evaluatePARTITIONINGArchitecture(input);

        this.sendResultMessage(rQueue, UUID, result);

        System.out.println("\n-------------------- EVALUATE PARTITIONING REQUEST OUTPUT --------------------");
        System.out.println("------------> INPUT: " + input);
        System.out.println("-------------> COST: " + result.getCost());
        System.out.println("----------> SCIENCE: " + result.getScience());
        System.out.println("--> DATA CONTINUITY: " + result.getDataContinuityScore());
//        System.out.println("----> DESIGN STRING: " + result.getDesignString());
        System.out.println("--------------------------------------------------------------------\n");
        // EvaluatorApp.sleep(5);
    }

    public void msgTypeTEST_EVAL(HashMap<String, String> msg_contents){

        System.out.println("\n-------------------- TEST EVALUATION --------------------");

        Result result = this.client.evaluateTESTArchitecture();

        System.out.println("\n-------------------- EVALUATE ADD REQUEST OUTPUT --------------------");
        System.out.println("-------------> COST: " + result.getCost());
        System.out.println("----------> SCIENCE: " + result.getScience());
        System.out.println("--> DATA CONTINUITY: " + result.getDataContinuityScore());
        System.out.println("----> DESIGN STRING: " + result.getDesignString());
        System.out.println("--------------------------------------------------------------------\n");
        // EvaluatorApp.sleep(5);
    }


    public void msgTypeBuild(HashMap<String, String> msg_contents){
        int group_id   = Integer.parseInt(msg_contents.get("group_id"));
        int problem_id = Integer.parseInt(msg_contents.get("problem_id"));

        this.client.rebuildResource(group_id, problem_id);

        System.out.println("\n-------------------- BUILD REQUEST --------------------");
        System.out.println("--------> GROUP ID: " + group_id);
        System.out.println("------> PROBLEM ID: " + problem_id);
        System.out.println("-------------------------------------------------------\n");
        // this.consumerSleep(5);
    }

    public void msgTypeNDSM(HashMap<String, String> msg_contents){

        System.out.println("---> COMPUTING NDSM");
        EvaluatorApp.sleep(1);
        this.client.computeNDSMs();
    }

    public void msgTypeContinuityMatrix(HashMap<String, String> msg_contents){


        System.out.println("---> COMPUTING CONTINUITY MATRIX");
        EvaluatorApp.sleep(1);
        this.client.computeContinuityMatrix();
    }


    // ---> MESSAGE FLOW
    // 1.
    public List<Message> getMessages(String url, int maxMessages, int waitTimeSeconds){
        ReceiveMessageRequest receiveMessageRequest = ReceiveMessageRequest.builder()
                .queueUrl(url)
                .waitTimeSeconds(waitTimeSeconds)
                .maxNumberOfMessages(maxMessages)
                .attributeNames(QueueAttributeName.ALL)
                .messageAttributeNames("All")
                .build();
        return this.sqsClient.receiveMessage(receiveMessageRequest).messages();
    }

    // 2.
    public HashMap<String, String> processMessage(Message msg){
        HashMap<String, String> contents = new HashMap<>();
        contents.put("body", msg.body());
        System.out.println("\n--------------- SQS MESSAGE ---------------");
        System.out.println("--------> BODY: " + msg.body());
        for(String key: msg.messageAttributes().keySet()){
            contents.put(key, msg.messageAttributes().get(key).stringValue());
            System.out.println("---> ATTRIBUTE: " + key + " - " + msg.messageAttributes().get(key).stringValue());
        }
        System.out.println("-------------------------------------------\n");
        // this.consumerSleep(5);
        return contents;
    }

    // 3.
    public void deleteMessages(List<Message> messages, String url){
        for (Message message : messages) {
            DeleteMessageRequest deleteMessageRequest = DeleteMessageRequest.builder()
                    .queueUrl(url)
                    .receiptHandle(message.receiptHandle())
                    .build();
            this.sqsClient.deleteMessage(deleteMessageRequest);
        }
    }

    // ---> THREAD SLEEP
    public void consumerSleep(int seconds){
        try                            { TimeUnit.SECONDS.sleep(seconds); }
        catch (InterruptedException e) { e.printStackTrace(); }
    }


    // ---> DEBUG MESSAGES
    public void sendTestMessages(){
        String arch = "0000000010000000000000000";
        String arch2= "0000000010000000100000000";

        this.sendEvalMessage(arch, 0);
        this.sendEvalMessage(arch2, 0);
        this.sendBuildMessage(1, 1, 0);
        this.sendEvalMessage(arch, 0);
        this.sendExitMessage(0);
    }

    public void sendEvalMessage(String input, int delay){

        final Map<String, MessageAttributeValue> messageAttributes = new HashMap<>();
        messageAttributes.put("msgType",
                MessageAttributeValue.builder()
                        .dataType("String")
                        .stringValue("evaluate")
                        .build()
        );
        messageAttributes.put("input",
                MessageAttributeValue.builder()
                        .dataType("String")
                        .stringValue(input)
                        .build()
        );
        this.sendMessage(messageAttributes, delay);

    }

    public void sendBuildMessage(int group_id, int problem_id, int delay){
        final Map<String, MessageAttributeValue> messageAttributes = new HashMap<>();
        messageAttributes.put("msgType",
                MessageAttributeValue.builder()
                        .dataType("String")
                        .stringValue("build")
                        .build()
        );
        messageAttributes.put("group_id",
                MessageAttributeValue.builder()
                        .dataType("String")
                        .stringValue(String.valueOf(group_id))
                        .build()
        );
        messageAttributes.put("problem_id",
                MessageAttributeValue.builder()
                        .dataType("String")
                        .stringValue(String.valueOf(problem_id))
                        .build()
        );
        this.sendMessage(messageAttributes, delay);
    }

    public void sendExitMessage(int delay){
        final Map<String, MessageAttributeValue> messageAttributes = new HashMap<>();
        messageAttributes.put("msgType",
                MessageAttributeValue.builder()
                        .dataType("String")
                        .stringValue("exit")
                        .build()
        );

        this.sendMessage(messageAttributes, delay);
    }

    public void sendResultMessage(String url, String UUID, Result result){
        final Map<String, MessageAttributeValue> messageAttributes = new HashMap<>();
        messageAttributes.put("UUID",
                MessageAttributeValue.builder()
                        .dataType("String")
                        .stringValue(UUID)
                        .build()
        );
        messageAttributes.put("science",
                MessageAttributeValue.builder()
                        .dataType("String")
                        .stringValue(Double.toString(result.getScience()))
                        .build()
        );
        messageAttributes.put("cost",
                MessageAttributeValue.builder()
                        .dataType("String")
                        .stringValue(Double.toString(result.getCost()))
                        .build()
        );
        messageAttributes.put("design",
                MessageAttributeValue.builder()
                        .dataType("String")
                        .stringValue(result.getDesignString())
                        .build()
        );
        messageAttributes.put("data_continuity",
                MessageAttributeValue.builder()
                        .dataType("String")
                        .stringValue(Double.toString(result.getDataContinuityScore()))
                        .build()
        );
        this.sendDirectMessage(url, messageAttributes, 0);
    }

    private void sendMessage(Map<String, MessageAttributeValue> messageAttributes, int delay){
        this.sqsClient.sendMessage(SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody("")
                .messageAttributes(messageAttributes)
                .delaySeconds(delay)
                .build());
    }

    private void sendDirectMessage(String url, Map<String, MessageAttributeValue> messageAttributes, int delay){
        this.sqsClient.sendMessage(SendMessageRequest.builder()
                .queueUrl(url)
                .messageBody("")
                .messageAttributes(messageAttributes)
                .delaySeconds(delay)
                .build());
    }


    // --> DELETE PRIVATE MESSAGES
    public void deletePrivMessages(){
        System.out.println("---> DELETING PRIVATE MESSAGES");
        List<Message> messages = this.getMessages(this.privateQueueUrl, 10, 1);
        this.deleteMessages(messages, this.privateQueueUrl);
    }


    public static boolean purgeQueue(SqsClient sqsClient, String queue_url){
        System.out.println("---> PURGE QUEUE: " + queue_url);
        // App.sleep(2);

        PurgeQueueRequest purgeQueueRequest = PurgeQueueRequest.builder()
                .queueUrl(queue_url)
                .build();
        sqsClient.purgeQueue(purgeQueueRequest);
        return true;
    }

}
