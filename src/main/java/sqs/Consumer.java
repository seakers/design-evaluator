package sqs;

import evaluator.EvaluatorApp;
import software.amazon.awssdk.services.sqs.model.*;
import vassar.VassarClient;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeName;
import software.amazon.awssdk.services.ecs.EcsClient;
import software.amazon.awssdk.services.ecs.model.DescribeServicesRequest;
import software.amazon.awssdk.services.ecs.model.DescribeServicesResponse;
import software.amazon.awssdk.services.ecs.model.Service;
import software.amazon.awssdk.services.ecs.model.StopTaskRequest;
import software.amazon.awssdk.services.ecs.model.StopTaskResponse;
import software.amazon.awssdk.services.ecs.model.UpdateServiceRequest;
import software.amazon.awssdk.services.ecs.model.UpdateServiceResponse;
import vassar.result.Result;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class Consumer implements Runnable {

    private enum State {
        WAITING_FOR_USER, READY
    }
    private boolean                                    debug;
    private boolean                                    running;
    private VassarClient                               client;
    private SqsClient                                  sqsClient;
    private EcsClient                                  ecsClient;
    private String                                     requestQueueUrl;
    private String                                     responseQueueUrl;
    private String                                     deadLetterQueueArn;
    private ConcurrentLinkedQueue<Map<String, String>> privateQueue;
    private State                                      currentState = State.WAITING_FOR_USER;
    private String                                     uuid = UUID.randomUUID().toString();
    private String                                     userRequestQueueUrl = null;
    private String                                     userResponseQueueUrl = null;
    private long                                       lastPingTime = System.currentTimeMillis();
    private long                                       lastDownsizeRequestTime = System.currentTimeMillis();
    private int                                        userId;
    private boolean                                    pendingReset = false;


    public static class Builder {

        private boolean                               debug;
        private VassarClient                          client;
        private SqsClient                             sqsClient;
        private EcsClient                             ecsClient;
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

        public Builder setECSClient(EcsClient ecsClient) {
            this.ecsClient = ecsClient;
            return this;
        }

        public Builder debug(boolean debug) {
            this.debug = debug;
            return this;
        }

        public Consumer build(){
            Consumer build         = new Consumer();
            build.sqsClient        = this.sqsClient;
            build.ecsClient        = this.ecsClient;
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

        // Ensure queues exist
        this.createConnectionQueues();

        while (this.running) {
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
            List<Message> userMessages = new ArrayList<>();

            if (this.currentState == State.WAITING_FOR_USER) {
                connectionMessages = this.getMessages(this.requestQueueUrl, 1, 1);
                connectionMessages = this.handleMessages(this.requestQueueUrl, connectionMessages);
                messages.addAll(connectionMessages);
            }

            // CHECK USER QUEUE
            if (this.currentState == State.READY) {
                if (this.userRequestQueueUrl != null) {
                    userMessages = this.getMessages(this.userRequestQueueUrl, 5, 1);
                    userMessages = this.handleMessages(this.userRequestQueueUrl, userMessages);
                    messages.addAll(userMessages);
                }
            }

            // PROCESS ALL MESSAGES
            for (Message msg: messages) {
                HashMap<String, String> msgContents = this.processMessage(msg, true);
                messagesContents.add(msgContents);
            }

            for (Map<String, String> msgContents: messagesContents) {

                if (msgContents.containsKey("msgType")) {
                    String msgType = msgContents.get("msgType");
                    if (msgType.equals("connectionRequest")) {
                        this.msgTypeConnectionRequest(msgContents);
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
                    else if (msgType.equals("reset")) {
                        this.msgTypeReset(msgContents);
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
            if (this.pendingReset) {
                this.currentState = State.WAITING_FOR_USER;
                this.userRequestQueueUrl = null;
                this.userResponseQueueUrl = null;
                this.pendingReset = false;
            }
            counter++;
        }
    }

    private boolean queueExists(String queueUrl) {
        ListQueuesResponse listResponse = this.sqsClient.listQueues();
        for (String url: listResponse.queueUrls()) {
            if (queueUrl.equals(url)) {
                return true;
            }
        }
        return false;
    }

    private boolean queueExistsByName(String queueName) {
        ListQueuesResponse listResponse = this.sqsClient.listQueues();
        for (String url: listResponse.queueUrls()) {
            String[] nameSplit = url.split("/");
            String name = nameSplit[nameSplit.length-1];
            if (queueName.equals(name)) {
                return true;
            }
        }
        return false;
    }

    private String getQueueArn(String queueUrl) {
        ArrayList<QueueAttributeName> attrList = new ArrayList<>();
        attrList.add(QueueAttributeName.QUEUE_ARN);
        GetQueueAttributesRequest attrRequest = GetQueueAttributesRequest.builder()
            .queueUrl(queueUrl)
            .attributeNames(attrList)
            .build();
        GetQueueAttributesResponse attrResponse = sqsClient.getQueueAttributes(attrRequest);
        String queueArn = attrResponse.attributes().get(QueueAttributeName.QUEUE_ARN);
        return queueArn;
    }

    private String getQueueUrl(String queueName) {
        GetQueueUrlRequest request = GetQueueUrlRequest.builder()
            .queueName(queueName)
            .build();
        GetQueueUrlResponse response = this.sqsClient.getQueueUrl(request);
        return response.queueUrl();
    }

    private void createConnectionQueues() {
        String[] requestQueueUrls = this.requestQueueUrl.split("/");
        String requestQueueName = requestQueueUrls[requestQueueUrls.length-1];

        String deadQueueArn = "";
        if (!this.queueExistsByName("dead-letter")) {
            CreateQueueRequest deadQueueRequest = CreateQueueRequest.builder()
                .queueName("dead-letter")
                .build();
            CreateQueueResponse response = sqsClient.createQueue(deadQueueRequest);
            String deadQueueUrl = response.queueUrl();
            deadQueueArn = this.getQueueArn(deadQueueUrl);
        }
        else {
            String deadQueueUrl = this.getQueueUrl("dead-letter");
            deadQueueArn = this.getQueueArn(deadQueueUrl);
        }

        Map<QueueAttributeName, String> queueAttrs = new HashMap<>();
        queueAttrs.put(QueueAttributeName.MESSAGE_RETENTION_PERIOD, Integer.toString(5*60));
        queueAttrs.put(QueueAttributeName.REDRIVE_POLICY, "{\"maxReceiveCount\":\"3\", \"deadLetterTargetArn\":\"" + deadQueueArn + "\"}");
        if (!this.queueExists(this.requestQueueUrl)) {
            CreateQueueRequest createQueueRequest = CreateQueueRequest.builder()
                .queueName(requestQueueName)
                .attributes(queueAttrs)
                .build();
            CreateQueueResponse response = sqsClient.createQueue(createQueueRequest);
        }
        SetQueueAttributesRequest setAttrReq = SetQueueAttributesRequest.builder()
            .queueUrl(this.requestQueueUrl)
            .attributes(queueAttrs)
            .build();
        sqsClient.setQueueAttributes(setAttrReq);
        

        String[] responseQueueUrls = this.responseQueueUrl.split("/");
        String responseQueueName = responseQueueUrls[responseQueueUrls.length-1];
        if (!this.queueExists(this.responseQueueUrl)) {
            CreateQueueRequest createQueueRequest = CreateQueueRequest.builder()
                .queueName(responseQueueName)
                .attributes(queueAttrs)
                .build();
                CreateQueueResponse response = sqsClient.createQueue(createQueueRequest);
        }
        setAttrReq = SetQueueAttributesRequest.builder()
            .queueUrl(this.responseQueueUrl)
            .attributes(queueAttrs)
            .build();
        sqsClient.setQueueAttributes(setAttrReq);

        this.deadLetterQueueArn = deadQueueArn;
    }

    private void checkTimers() {
        switch (this.currentState) {
            case WAITING_FOR_USER:
                if (System.currentTimeMillis() - this.lastPingTime > 60*60*1000) {
                    this.downsizeAwsService();
                }
                break;
            case READY:
                if (System.currentTimeMillis() - this.lastPingTime > 30*60*1000) {
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
        int rejectedCount = 0;
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
                rejectedCount += 1;
            }
        }
        System.out.println("Rejected " + rejectedCount + " messages");
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
            case READY:
                allowedTypes = Arrays.asList("build", "ping", "statusCheck", "evaluate", "add", "Instrument Selection", "Instrument Partitioning", "TEST-EVAL", "NDSM", "ContinuityMatrix", "reset", "exit");
                break;
        }
        // Check for both allowedTypes and UUID match
        boolean isAllowed = false;
        if (allowedTypes.contains(msgType)) {
            isAllowed = true;
            if (msgContents.containsKey("UUID")) {
                String msgUUID = msgContents.get("UUID");
                if (!msgUUID.equals(this.uuid)) {
                    isAllowed = false;
                }
            }
        }
        return isAllowed;
    }


    // ---> MESSAGE TYPES
    private void msgTypeConnectionRequest(Map<String, String> msgContents) {
        String userId = msgContents.get("user_id");
        this.userId = Integer.parseInt(userId);
        this.client.setUserID(this.userId);

        int group_id   = Integer.parseInt(msgContents.get("group_id"));
        int problem_id = Integer.parseInt(msgContents.get("problem_id"));
        int run_id     = Integer.parseInt(msgContents.getOrDefault("run_id", "-1"));

        // Create queues for private communication
        QueueUrls queueUrls = createUserQueues(userId, run_id);

        // Build evaluation container
        this.client.rebuildResource(group_id, problem_id);
        System.out.println("\n-------------------- BUILD REQUEST --------------------");
        System.out.println("--------> GROUP ID: " + group_id);
        System.out.println("------> PROBLEM ID: " + problem_id);
        System.out.println("---------> USER ID: " + this.userId);
        System.out.println("-------------------------------------------------------\n");

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
                .messageBody("vassar_message")
                .messageAttributes(messageAttributes)
                .delaySeconds(0)
                .build());


        this.currentState = State.READY;
        this.userRequestQueueUrl = queueUrls.requestUrl;
        this.userResponseQueueUrl = queueUrls.responseUrl;
        this.lastPingTime = System.currentTimeMillis();
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
        messageAttributes.put("UUID",
                MessageAttributeValue.builder()
                        .dataType("String")
                        .stringValue(this.uuid)
                        .build()
        );

        this.sqsClient.sendMessage(SendMessageRequest.builder()
                .queueUrl(this.userResponseQueueUrl)
                .messageBody("vassar_message")
                .messageAttributes(messageAttributes)
                .delaySeconds(0)
                .build());
        this.lastPingTime = System.currentTimeMillis();
    }

    private void msgTypePing(Map<String, String> msgContents) {
        this.lastPingTime = System.currentTimeMillis();
        // Send ping ack back
        final Map<String, MessageAttributeValue> messageAttributes = new HashMap<>();
        messageAttributes.put("msgType",
                              MessageAttributeValue.builder()
                                .dataType("String")
                                .stringValue("pingAck")
                                .build()
        );
        messageAttributes.put("UUID",
                              MessageAttributeValue.builder()
                                .dataType("String")
                                .stringValue(this.uuid)
                                .build()
        );
        this.sqsClient.sendMessage(SendMessageRequest.builder()
                                    .queueUrl(this.userResponseQueueUrl)
                                    .messageBody("vassar_message")
                                    .messageAttributes(messageAttributes)
                                    .delaySeconds(0)
                                    .build());
    }

    private void msgTypeReset(Map<String, String> msgContents) {
        this.pendingReset = true;
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

        // Ensure we are on the right problem for the data, rebuild if not
        if (!this.client.checkDatasetInfo(datasetId)) {
            int groupId = this.client.getGroupID();
            int newProblemId = this.client.getDatasetInfo(datasetId).dataset().problem_id();
            this.client.rebuildResource(groupId, newProblemId);
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

        // Send message announcing it's ready to eval architectures - does this message still need to be sent?
        final Map<String, MessageAttributeValue> messageAttributes = new HashMap<>();
        messageAttributes.put("msgType",
                MessageAttributeValue.builder()
                        .dataType("String")
                        .stringValue("buildDone")
                        .build()
        );
        messageAttributes.put("type",
                MessageAttributeValue.builder()
                        .dataType("String")
                        .stringValue("evaluator")
                        .build()
        );
        messageAttributes.put("UUID",
                MessageAttributeValue.builder()
                        .dataType("String")
                        .stringValue(this.uuid)
                        .build()
        );
        this.sqsClient.sendMessage(SendMessageRequest.builder()
                .queueUrl(this.userResponseQueueUrl)
                .messageBody("vassar_message")
                .messageAttributes(messageAttributes)
                .delaySeconds(0)
                .build());

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

        List<Message> messages = new ArrayList<>(this.sqsClient.receiveMessage(receiveMessageRequest).messages());

        Collections.sort(messages, (Message a, Message b) -> {
            Long timestamp_a = Long.parseLong(a.attributes().get(MessageSystemAttributeName.SENT_TIMESTAMP));
            Long timestamp_b = Long.parseLong(b.attributes().get(MessageSystemAttributeName.SENT_TIMESTAMP));
            Long diff = timestamp_a - timestamp_b;
            if (diff > 0) {
                return 1;
            }
            else if (diff < 0) {
                return -1;
            }
            else {
                return 0;
            }
        });
        
        return messages;
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
                .messageBody("vassar_message")
                .messageAttributes(messageAttributes)
                .delaySeconds(delay)
                .build());
    }

    private void sendDirectMessage(String url, Map<String, MessageAttributeValue> messageAttributes, int delay){
        this.sqsClient.sendMessage(SendMessageRequest.builder()
                .queueUrl(url)
                .messageBody("vassar_message")
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

    private QueueUrls createUserQueues(String userId, Integer run_id) {
        String requestQueueName = "user-queue-request-" + userId;
        String responseQueueName = "user-queue-response-" + userId;
        if (run_id != -1) {
            requestQueueName += "-" + run_id;
            responseQueueName += "-" + run_id;
        }
        Map<String,String> tags = new HashMap<>();
        tags.put("USER_ID", userId);
        Map<QueueAttributeName, String> queueAttrs = new HashMap<>();
        queueAttrs.put(QueueAttributeName.MESSAGE_RETENTION_PERIOD, Integer.toString(5*60));
        queueAttrs.put(QueueAttributeName.REDRIVE_POLICY, "{\"maxReceiveCount\":\"3\", \"deadLetterTargetArn\":\"" + this.deadLetterQueueArn + "\"}");
        
        String newUserRequestQueueUrl = "";
        if (!this.queueExistsByName(requestQueueName)) {
            CreateQueueRequest createQueueRequest = CreateQueueRequest.builder()
                .queueName(requestQueueName)
                .attributes(queueAttrs)
                .tags(tags)
                .build();
            CreateQueueResponse response = this.sqsClient.createQueue(createQueueRequest);
            newUserRequestQueueUrl = response.queueUrl();
        }
        else {
            newUserRequestQueueUrl = this.getQueueUrl(requestQueueName);
            SetQueueAttributesRequest setAttrReq = SetQueueAttributesRequest.builder()
                .queueUrl(newUserRequestQueueUrl)
                .attributes(queueAttrs)
                .build();
            this.sqsClient.setQueueAttributes(setAttrReq);
        }
        
        String newUserResponseQueueUrl = "";
        if (!this.queueExistsByName(responseQueueName)) {
            CreateQueueRequest createQueueRequest = CreateQueueRequest.builder()
                .queueName(responseQueueName)
                .attributes(queueAttrs)
                .tags(tags)
                .build();
            CreateQueueResponse response = this.sqsClient.createQueue(createQueueRequest);
            newUserResponseQueueUrl = response.queueUrl();
        }
        else {
            newUserResponseQueueUrl = this.getQueueUrl(responseQueueName);
            SetQueueAttributesRequest setAttrReq = SetQueueAttributesRequest.builder()
                .queueUrl(newUserResponseQueueUrl)
                .attributes(queueAttrs)
                .build();
            this.sqsClient.setQueueAttributes(setAttrReq);
        }

        QueueUrls returnVal = new QueueUrls();
        returnVal.requestUrl = newUserRequestQueueUrl;
        returnVal.responseUrl = newUserResponseQueueUrl;

        return returnVal;
    }

    private void downsizeAwsService() {
        // Only do this if in AWS
        long timeSinceLastRequest = System.currentTimeMillis() - this.lastDownsizeRequestTime;
        if (System.getenv("DEPLOYMENT_TYPE").equals("AWS") && timeSinceLastRequest > 5*60*1000) {
            this.lastDownsizeRequestTime = System.currentTimeMillis();
            // Check service for number of tasks
            String clusterArn = System.getenv("CLUSTER_ARN");
            String serviceArn = System.getenv("SERVICE_ARN");
            DescribeServicesRequest request = DescribeServicesRequest.builder()
                                                                     .cluster(clusterArn)
                                                                     .services(serviceArn)
                                                                     .build();
            DescribeServicesResponse response = this.ecsClient.describeServices(request);
            if (response.hasServices()) {
                Service service = response.services().get(0);
                Integer desiredCount = service.desiredCount();
                // Downscale tasks if more than 5
                if (desiredCount > 5) {
                    UpdateServiceRequest updateRequest = UpdateServiceRequest.builder()
                                                                             .cluster(clusterArn)
                                                                             .desiredCount(desiredCount-1)
                                                                             .service(serviceArn)
                                                                             .build();
                    UpdateServiceResponse updateResponse = this.ecsClient.updateService(updateRequest);

                    // Close myself as the extra task
                    String taskArn = getTaskArn();
                    StopTaskRequest stopRequest = StopTaskRequest.builder()
                                                                 .cluster(clusterArn)
                                                                 .task(taskArn)
                                                                 .build();
                    StopTaskResponse stopResponse = this.ecsClient.stopTask(stopRequest);
                }
            }
        }
    }

    private String getTaskArn() {
        String taskArn = "";
        try {
            String baseUrl = System.getenv("ECS_CONTAINER_METADATA_URI_V4");
            URL url = new URL(baseUrl + "/task");

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.connect();

            //Getting the response code
            int responsecode = conn.getResponseCode();

            if (responsecode != 200) {
                throw new RuntimeException("HttpResponseCode: " + responsecode);
            }
            else {

                String inline = "";
                Scanner scanner = new Scanner(url.openStream());

                //Write all the JSON data into a string using a scanner
                while (scanner.hasNext()) {
                    inline += scanner.nextLine();
                }

                //Close the scanner
                scanner.close();

                //Using the JSON simple library parse the string into a json object
                JSONParser parse = new JSONParser();
                JSONObject responseObj = (JSONObject) parse.parse(inline);

                //Get the required object from the above created object
                taskArn = (String)responseObj.get("TaskARN");
            }

            conn.disconnect();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return taskArn;
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
