package sqs;

import evaluator.EvaluatorApp;
import software.amazon.awssdk.services.sqs.model.*;
import vassar.VassarClient;
import software.amazon.awssdk.services.sqs.SqsClient;
import vassar.result.Result;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

public class Consumer implements Runnable {

    private enum State {
        WAITING_FOR_USER, WAITING_FOR_ACK, UNINITIALIZED, READY
    }
    private boolean                                    debug;
    private boolean                                    running;
    private VassarClient                               client;
    private SqsClient                                  sqsClient;
    private String                                     requestQueueUrl;
    private String                                     responseQueueUrl;
    private ConcurrentLinkedQueue<Map<String, String>> privateQueue;
    private State                                      currentState = State.WAITING_FOR_USER;
    private String                                     uuid = UUID.randomUUID().toString();
    private String                                     userRequestQueueUrl = null;
    private String                                     userResponseQueueUrl = null;
    private long                                       lastPingTime = System.currentTimeMillis();


    public static class Builder {

        private boolean                               debug;
        private VassarClient                          client;
        private SqsClient                             sqsClient;
        private String                                requestQueueUrl;
        private String                                responseQueueUrl;
        private ConcurrentLinkedQueue<Map<String, String>> privateQueue;

        public Builder(SqsClient sqsClient){
            this.sqsClient = sqsClient;
        }

        public Builder setVassarClient(VassarClient client) {
            this.client = client;
            return this;
        }

        public Builder setRequestQueueUrl(String queueUrl) {
            this.requestQueueUrl = queueUrl;
            return this;
        }

        public Builder setResponseQueueUrl(String queueUrl) {
            this.responseQueueUrl = queueUrl;
            return this;
        }

        public Builder setPrivateQueue(ConcurrentLinkedQueue<Map<String, String>> privateQueue) {
            this.privateQueue = privateQueue;
            return this;
        }

        public Builder debug(boolean debug) {
            this.debug = debug;
            return this;
        }

        public Consumer build(){
            Consumer build         = new Consumer();
            build.sqsClient        = this.sqsClient;
            build.debug            = this.debug;
            build.client           = this.client;
            build.requestQueueUrl  = this.requestQueueUrl;
            build.responseQueueUrl = this.responseQueueUrl;
            build.privateQueue     = this.privateQueue;
            build.running          = true;
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
            System.out.println("Current State: " + this.currentState);
            
            List<Map<String, String>> messagesContents = new ArrayList<>();

            // Check if timers are expired for different states
            this.checkTimers();

            // CHECK PRIVATE QUEUE FIRST - IF PRIVATE QUEUE EMPTY, CHECK EVAL QUEUE
            if (!this.privateQueue.isEmpty()) {
                ArrayList<Map<String,String>> returnMessages = new ArrayList<>();
                while (!this.privateQueue.isEmpty()) {
                    Map<String, String> msgContents = this.privateQueue.poll();
                    if (isMessageAllowed(msgContents)) {
                        messagesContents.add(msgContents);
                    }
                    else {
                        returnMessages.add(msgContents);
                    }
                }
                this.privateQueue.addAll(returnMessages);
            }
            
            // CHECK CONNECTION QUEUE
            List<Message> messages = new ArrayList<>();
            List<Message> connectionMessages = new ArrayList<>();
            connectionMessages = this.getMessages(this.requestQueueUrl, 3, 5);
            connectionMessages = this.handleMessages(this.requestQueueUrl, connectionMessages);
            messages.addAll(connectionMessages);

            // CHECK USER QUEUE
            List<Message> userMessages = new ArrayList<>();
            if (this.userRequestQueueUrl != null) {
                userMessages = this.getMessages(this.userRequestQueueUrl, 3, 5);
                userMessages = this.handleMessages(this.userRequestQueueUrl, userMessages);
                messages.addAll(userMessages);
            }

            // PROCESS ALL MESSAGES
            for (Message msg: messages) {
                HashMap<String, String> msgContents = this.processMessage(msg, true);
                messagesContents.add(msgContents);
            }

            for (Map<String, String> msgContents: messagesContents){

                if(msgContents.containsKey("msgType")) {
                    String msgType = msgContents.get("msgType");
                    if (msgType.equals("connectionRequest")) {
                        this.msgTypeConnectionRequest(msgContents);
                    }
                    else if (msgType.equals("connectionAck")) {
                        this.msgTypeConnectionAck(msgContents);
                    }
                    else if (msgType.equals("statusCheck")) {
                        this.msgTypeStatusCheck(msgContents);
                    }
                    else if (msgType.equals("evaluate")) {
                        this.msgTypeEvaluate(msgContents);
                    }
                    else if (msgType.equals("add")) {
                        this.msgTypeADD(msgContents);
                    }
                    else if (msgType.equals("Instrument Selection")) {
                        this.msgTypeSELECTING(msgContents);
                    }
                    else if (msgType.equals("Instrument Partitioning")) {
                        this.msgTypePARTITIONING(msgContents);
                    }
                    else if (msgType.equals("TEST-EVAL")) {
                        this.msgTypeTEST_EVAL(msgContents);
                    }
                    else if (msgType.equals("NDSM")) {
                        this.msgTypeNDSM(msgContents);
                    }
                    else if (msgType.equals("ContinuityMatrix")) {
                        this.msgTypeContinuityMatrix(msgContents);
                    }
                    else if (msgType.equals("build")) {
                        this.msgTypeBuild(msgContents);
                    }
                    else if (msgType.equals("ping")) {
                        this.msgTypePing(msgContents);
                    }
                    else if (msgType.equals("exit")) {
                        System.out.println("----> Exiting gracefully");
                        this.running = false;
                    }
                }
                else {
                    System.out.println("-----> INCOMING MESSAGE DIDN'T HAVE ATTRIBUTE: msgType");
                    // this.consumerSleep(10);
                }
            }

            if (!connectionMessages.isEmpty()) {
                this.deleteMessages(connectionMessages, this.requestQueueUrl);
            }
            if (!userMessages.isEmpty()) {
                this.deleteMessages(userMessages, this.userRequestQueueUrl);
            }

            counter++;
        }
    }

    private void checkTimers() {
        switch (this.currentState) {
            case WAITING_FOR_ACK:
                if (System.currentTimeMillis() - this.lastPingTime > 1*60*1000) {
                    this.currentState = State.WAITING_FOR_USER;
                    this.userRequestQueueUrl = null;
                    this.userResponseQueueUrl = null;
                }
                break;
            case UNINITIALIZED:
                if (System.currentTimeMillis() - this.lastPingTime > 5*60*1000) {
                    this.currentState = State.WAITING_FOR_USER;
                    this.userRequestQueueUrl = null;
                    this.userResponseQueueUrl = null;
                }
                break;
            case READY:
                if (System.currentTimeMillis() - this.lastPingTime > 5*60*1000) {
                    this.currentState = State.WAITING_FOR_USER;
                    this.userRequestQueueUrl = null;
                    this.userResponseQueueUrl = null;
                }
                break;
            default:
                break;
        }
    }

    private List<Message> handleMessages(String queueUrl, List<Message> messages) {
        List<Message> processedMessages = new ArrayList<>();
        for (Message msg: messages) {
            HashMap<String, String> msgContents = this.processMessage(msg, false);
            if (isMessageAllowed(msgContents)) {
                processedMessages.add(msg);
            }
            else {
                // Reject the message and send back to queue
                ChangeMessageVisibilityRequest changeMessageVisibilityRequest = ChangeMessageVisibilityRequest.builder()
                    .queueUrl(queueUrl)
                    .receiptHandle(msg.receiptHandle())
                    .visibilityTimeout(1)
                    .build();
                this.sqsClient.changeMessageVisibility(changeMessageVisibilityRequest);
            }
        }
        return processedMessages;
    }

    // Filter valid messages based on the current state of the system
    private boolean isMessageAllowed(Map<String, String> msgContents) {
        String msgType = msgContents.get("msgType");
        List<String> allowedTypes = new ArrayList<>();
        switch (this.currentState) {
            case WAITING_FOR_USER:
                allowedTypes = Arrays.asList("connectionRequest", "statusCheck");
                break;
            case WAITING_FOR_ACK:
                allowedTypes = Arrays.asList("connectionAck", "statusCheck");
                break;
            case UNINITIALIZED:
                allowedTypes = Arrays.asList("build", "ping", "statusCheck");
                break;
            case READY:
                allowedTypes = Arrays.asList("build", "ping", "statusCheck", "evaluate", "add", "Instrument Selection", "Instrument Partitioning", "TEST-EVAL", "NDSM", "ContinuityMatrix", "exit");
                break;
        }
        return allowedTypes.contains(msgType);
    }


    // ---> MESSAGE TYPES
    private void msgTypeConnectionRequest(Map<String, String> msgContents) {
        String userId = msgContents.get("user_id");

        // Create queues for private communication
        QueueUrls queueUrls = createUserQueues(userId);

        final Map<String, MessageAttributeValue> messageAttributes = new HashMap<>();
        messageAttributes.put("msgType",
                MessageAttributeValue.builder()
                        .dataType("String")
                        .stringValue("isAvailable")
                        .build()
        );
        messageAttributes.put("UUID",
                MessageAttributeValue.builder()
                        .dataType("String")
                        .stringValue(this.uuid)
                        .build()
        );
        messageAttributes.put("type",
                MessageAttributeValue.builder()
                        .dataType("String")
                        .stringValue("evaluator")
                        .build()
        );
        messageAttributes.put("user_id",
                MessageAttributeValue.builder()
                        .dataType("String")
                        .stringValue(userId)
                        .build()
        );
        messageAttributes.put("request_queue_url",
                MessageAttributeValue.builder()
                        .dataType("String")
                        .stringValue(queueUrls.requestUrl)
                        .build()
        );
        messageAttributes.put("response_queue_url",
                MessageAttributeValue.builder()
                        .dataType("String")
                        .stringValue(queueUrls.responseUrl)
                        .build()
        );

        this.sqsClient.sendMessage(SendMessageRequest.builder()
                .queueUrl(this.responseQueueUrl)
                .messageBody("")
                .messageAttributes(messageAttributes)
                .delaySeconds(0)
                .build());
        this.currentState = State.WAITING_FOR_ACK;
        this.userRequestQueueUrl = queueUrls.requestUrl;
        this.userResponseQueueUrl = queueUrls.responseUrl;
        this.lastPingTime = System.currentTimeMillis();
    }

    private void msgTypeConnectionAck(Map<String, String> msgContents) {
        String receivedUUID = msgContents.get("UUID");

        if(receivedUUID.equals(this.uuid)) {
            this.currentState = State.UNINITIALIZED;
        }
        else {
            System.out.println("UUID does not match!");
        }
    }

    private void msgTypeStatusCheck(Map<String, String> msgContents) {
        final Map<String, MessageAttributeValue> messageAttributes = new HashMap<>();
        messageAttributes.put("msgType",
                MessageAttributeValue.builder()
                        .dataType("String")
                        .stringValue("currentStatus")
                        .build()
        );
        String currentStatus = "";
        switch (this.currentState) {
            case WAITING_FOR_USER:
                currentStatus = "waiting_for_user";
                break;
            case WAITING_FOR_ACK:
                currentStatus = "waiting_for_ack";
                break;
            case UNINITIALIZED:
                currentStatus = "uninitialized";
                break;
            case READY:
                currentStatus = "ready";
                break;
        }
        messageAttributes.put("current_status",
                MessageAttributeValue.builder()
                        .dataType("String")
                        .stringValue(currentStatus)
                        .build()
        );

        this.sqsClient.sendMessage(SendMessageRequest.builder()
                .queueUrl(this.userResponseQueueUrl)
                .messageBody("")
                .messageAttributes(messageAttributes)
                .delaySeconds(0)
                .build());
        this.lastPingTime = System.currentTimeMillis();
    }

    private void msgTypePing(Map<String, String> msgContents) {
        this.lastPingTime = System.currentTimeMillis();
    }
    
    public void msgTypeEvaluate(Map<String, String> msg_contents){

        String  input       = msg_contents.get("input");
        Integer datasetId   = Integer.parseInt(msg_contents.get("dataset_id"));
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

        Result result = this.client.evaluateArchitecture(input, datasetId, ga_arch, re_evaluate, fast);

        System.out.println("\n-------------------- EVALUATE REQUEST OUTPUT --------------------");
        System.out.println("-----> INPUT: " + input);
        System.out.println("------> COST: " + result.getCost());
        System.out.println("---> SCIENCE: " + result.getScience());
        System.out.println("----------------------------------------------------------------\n");
        // this.consumerSleep(3);
    }

    public void msgTypeADD(Map<String, String> msg_contents){

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


    public void msgTypeSELECTING(Map<String, String> msg_contents){

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

    public void msgTypePARTITIONING(Map<String, String> msg_contents){

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

    public void msgTypeTEST_EVAL(Map<String, String> msg_contents){

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


    public void msgTypeBuild(Map<String, String> msg_contents){
        int group_id   = Integer.parseInt(msg_contents.get("group_id"));
        int problem_id = Integer.parseInt(msg_contents.get("problem_id"));

        this.client.rebuildResource(group_id, problem_id);
        if (this.currentState == State.UNINITIALIZED) {
            this.currentState = State.READY;
            
            // Send message announcing it's ready to eval architectures
            final Map<String, MessageAttributeValue> messageAttributes = new HashMap<>();
            messageAttributes.put("msgType",
                    MessageAttributeValue.builder()
                            .dataType("String")
                            .stringValue("isReady")
                            .build()
            );
            messageAttributes.put("type",
                    MessageAttributeValue.builder()
                            .dataType("String")
                            .stringValue("evaluator")
                            .build()
            );
            this.sqsClient.sendMessage(SendMessageRequest.builder()
                    .queueUrl(this.userResponseQueueUrl)
                    .messageBody("")
                    .messageAttributes(messageAttributes)
                    .delaySeconds(0)
                    .build());
        }

        System.out.println("\n-------------------- BUILD REQUEST --------------------");
        System.out.println("--------> GROUP ID: " + group_id);
        System.out.println("------> PROBLEM ID: " + problem_id);
        System.out.println("-------------------------------------------------------\n");
        // this.consumerSleep(5);
    }

    public void msgTypeNDSM(Map<String, String> msg_contents){

        System.out.println("---> COMPUTING NDSM");
        EvaluatorApp.sleep(1);
        this.client.computeNDSMs();
    }

    public void msgTypeContinuityMatrix(Map<String, String> msg_contents){


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
    public HashMap<String, String> processMessage(Message msg, boolean printInfo){
        HashMap<String, String> contents = new HashMap<>();
        contents.put("body", msg.body());
        for(String key: msg.messageAttributes().keySet()){
            contents.put(key, msg.messageAttributes().get(key).stringValue());
        }
        if (printInfo) {
            System.out.println("\n--------------- SQS MESSAGE ---------------");
            System.out.println("--------> BODY: " + msg.body());
            for(String key: msg.messageAttributes().keySet()){
                System.out.println("---> ATTRIBUTE: " + key + " - " + msg.messageAttributes().get(key).stringValue());
            }
            System.out.println("-------------------------------------------\n");
        }
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
                .queueUrl(this.requestQueueUrl)
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
        this.privateQueue.clear();
    }

    private class QueueUrls {
        public String requestUrl;
        public String responseUrl;
    }

    private QueueUrls createUserQueues(String userId) {
        String requestQueueName = "user-queue-request-" + userId;
        String responseQueueName = "user-queue-response-" + userId;
        Map<String,String> tags = new HashMap<>();
        tags.put("USER_ID", userId);
        CreateQueueRequest createQueueRequest = CreateQueueRequest.builder()
            .queueName(requestQueueName)
            .tags(tags)
            .build();
        CreateQueueResponse response = sqsClient.createQueue(createQueueRequest);
        String requestQueueUrl = response.queueUrl();

        createQueueRequest = CreateQueueRequest.builder()
            .queueName(responseQueueName)
            .tags(tags)
            .build();
        response = sqsClient.createQueue(createQueueRequest);
        String responseQueueUrl = response.queueUrl();

        QueueUrls returnVal = new QueueUrls();
        returnVal.requestUrl = requestQueueUrl;
        returnVal.responseUrl = responseQueueUrl;

        return returnVal;
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
