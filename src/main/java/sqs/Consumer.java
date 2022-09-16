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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class Consumer {

    private enum State {
        READY, RUNNING
    }
    private boolean                                    debug;
    private boolean                                    running;
    private VassarClient                               client;
    private SqsClient                                  sqsClient;
    private EcsClient                                  ecsClient;
    private String evalRequestUrl = System.getenv("EVAL_REQUEST_URL");
    private String evalResponseUrl = System.getenv("EVAL_RESPONSE_URL");
    private String privateRequestUrl = System.getenv("PRIVATE_REQUEST_URL");
    private String privateResponseUrl = System.getenv("PRIVATE_RESPONSE_URL");
    private String                                     deadLetterQueueArn;
    private ConcurrentLinkedQueue<Map<String, String>> privateQueue;
    private State                                      currentState = State.READY;
    private String                                     uuid = UUID.randomUUID().toString();
    private long                                       lastDownsizeRequestTime = System.currentTimeMillis();
    private int                                        userId;
    private boolean                                    pendingReset = false;
    private int                                        numEvalMessages;



    // --> PING
    private ConcurrentLinkedQueue<Map<String, String>> pingConsumerQueue;
    private ConcurrentLinkedQueue<Map<String, String>> pingConsumerQueueResponse;
    private PingConsumer                               pingConsumer;
    private Thread                                     pingThread = null;


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
            build.evalRequestUrl = this.requestQueueUrl;
            build.evalResponseUrl = this.responseQueueUrl;
            build.privateQueue     = this.privateQueue;
            build.running          = true;
            build.numEvalMessages  = this.getNumEvalMessages();

            // --> PING
            build.pingConsumerQueue         = new ConcurrentLinkedQueue<>();
            build.pingConsumerQueueResponse = new ConcurrentLinkedQueue<>();
            build.pingConsumer = new PingConsumer(build.pingConsumerQueue, build.pingConsumerQueueResponse);
            build.pingThread   = null;

            build.privateRequestUrl = System.getenv("PRIVATE_REQUEST_URL");
            build.privateResponseUrl = System.getenv("PRIVATE_RESPONSE_URL");

            return build;
        }

        public int getNumEvalMessages(){
            String eval_msg_count = System.getenv("MAXEVAL");
            if(eval_msg_count == null){
                return 1;
            }
            return Integer.parseInt(eval_msg_count);
        }

    }


//     _____
//    |  __ \
//    | |__) |   _ _ __
//    |  _  / | | | '_ \
//    | | \ \ |_| | | | |
//    |_|  \_\__,_|_| |_|

    public void run() throws Exception {
        int counter = 0;

        this.startPingThread();

        while (this.running){
            System.out.println("-----> (Cons) Loop iteration: " + counter + " -- " + this.currentState);


            // --> 1. Check ping queue
            this.checkPingQueue();
            if(!this.running){break;}


            // --> 2. Handle private messages
            List<Message> privateMessages = this.checkPrivateQueue();
            List<Map<String, String>> privateMessageContents = new ArrayList<>();
            for (Message msg: privateMessages) {
                HashMap<String, String> msgContents = this.processMessage(msg, true);
                privateMessageContents.add(msgContents);
            }
            for (Map<String, String> msgContents: privateMessageContents) {
                if (msgContents.containsKey("msgType")) {
                    String msgType = msgContents.get("msgType");
                    if (msgType.equals("build")) {
                        this.msgTypeBuild(msgContents);
                    }
                    else if (msgType.equals("exit")) {
                        System.out.println("----> Exiting gracefully");
                        this.running = false;
                    }
//                    else if (msgType.equals("Instrument Selection")) {
//                        this.msgTypeSELECTING(msgContents);
//                    }
//                    else if (msgType.equals("Instrument Partitioning")) {
//                        this.msgTypePARTITIONING(msgContents);
//                    }
//                    else if (msgType.equals("TEST-EVAL")) {
//                        this.msgTypeTEST_EVAL(msgContents);
//                    }
//                    else if (msgType.equals("NDSM")) {
//                        this.msgTypeNDSM(msgContents);
//                    }
//                    else if (msgType.equals("ContinuityMatrix")) {
//                        this.msgTypeContinuityMatrix(msgContents);
//                    }
//                    else if (msgType.equals("connectionRequest")) {
//                        this.msgTypeConnectionRequest(msgContents);
//                    }
//                    else if (msgType.equals("add")) {
//                        this.msgTypeADD(msgContents);
//                    }
                }
                else {
                    System.out.println("-----> INCOMING MESSAGE DIDN'T HAVE ATTRIBUTE: msgType");
                }
            }
            if (!privateMessages.isEmpty()) {
                this.deleteMessages(privateMessages, this.privateRequestUrl);
            }


            // --> 3. Handle evaluation messages
            List<Message> evalMessages = this.checkEvalQueue();
            List<Map<String, String>> evalMessageContents = new ArrayList<>();
            for (Message msg: evalMessages) {
                HashMap<String, String> msgContents = this.processMessage(msg, true);
                evalMessageContents.add(msgContents);
            }
            for (Map<String, String> msgContents: evalMessageContents) {
                if (msgContents.containsKey("msgType")) {
                    String msgType = msgContents.get("msgType");
                    if (msgType.equals("evaluate")) {
                        this.msgTypeEvaluate(msgContents);
                    }
                }
                else {
                    System.out.println("-----> INCOMING MESSAGE DIDN'T HAVE ATTRIBUTE: msgType");
                }
            }
            if (!evalMessages.isEmpty()) {
                this.deleteMessages(evalMessages, this.evalRequestUrl);
            }


            counter++;
        }
        this.sendExitMessage();
        this.closePingThread();
    }



//     _____ _               _    _                 _ _ _
//    |  __ (_)             | |  | |               | | (_)
//    | |__) | _ __   __ _  | |__| | __ _ _ __   __| | |_ _ __   __ _
//    |  ___/ | '_ \ / _` | |  __  |/ _` | '_ \ / _` | | | '_ \ / _` |
//    | |   | | | | | (_| | | |  | | (_| | | | | (_| | | | | | | (_| |
//    |_|   |_|_| |_|\__, | |_|  |_|\__,_|_| |_|\__,_|_|_|_| |_|\__, |
//                    __/ |                                      __/ |
//                   |___/                                      |___/

    private void startPingThread(){
        System.out.println("--> RUNNING PING THREAD");
        this.pingThread = new Thread(this.pingConsumer);
        this.pingThread.start();
        this.sendReadyStatus();
    }

    private void checkPingQueue() {
        if(!this.pingConsumerQueueResponse.isEmpty()){
            Map<String, String> msgContents = this.pingConsumerQueueResponse.poll();
            System.out.println("--> THERE IS A STOP MESSAGE FROM THE PING CONTAINER ");
            System.out.println(msgContents);
            this.running = false;
        }
    }

    private void sendReadyStatus(){
        Map<String, String> status_message = new HashMap<>();
        status_message.put("STATUS", "READY");
        status_message.put("PROBLEM_ID", "-----");
        status_message.put("GROUP_ID", "-----");
        status_message.put("DATASET_ID", "-----");
        this.pingConsumerQueue.add(status_message);
    }

    private void sendRunningStatus(int groupId, int problemId){
        Map<String, String> status_message = new HashMap<>();
        status_message.put("STATUS", "RUNNING");
        status_message.put("PROBLEM_ID", String.valueOf(problemId));
        status_message.put("GROUP_ID", String.valueOf(groupId));
        status_message.put("DATASET_ID", "-----");
        this.pingConsumerQueue.add(status_message);
    }

    private void closePingThread(){
        System.out.println("--> CLOSING PING THREAD");
        Map<String, String> status_message = new HashMap<>();
        status_message.put("msgType", "stop");
        this.pingConsumerQueue.add(status_message);

        try{
            this.pingThread.join();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }





//     __  __                                  _    _                 _ _ _
//    |  \/  |                                | |  | |               | | (_)
//    | \  / | ___  ___ ___  __ _  __ _  ___  | |__| | __ _ _ __   __| | |_ _ __   __ _
//    | |\/| |/ _ \/ __/ __|/ _` |/ _` |/ _ \ |  __  |/ _` | '_ \ / _` | | | '_ \ / _` |
//    | |  | |  __/\__ \__ \ (_| | (_| |  __/ | |  | | (_| | | | | (_| | | | | | | (_| |
//    |_|  |_|\___||___/___/\__,_|\__, |\___| |_|  |_|\__,_|_| |_|\__,_|_|_|_| |_|\__, |
//                                 __/ |                                           __/ |
//                                |___/                                           |___/


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
        // System.out.println("Rejected " + rejectedCount + " messages");
        return processedMessages;
    }

    // 2.5
    private boolean isMessageAllowed(Map<String, String> msgContents) {
        String msgType = msgContents.get("msgType");
        List<String> allowedTypes = new ArrayList<>();
        switch (this.currentState) {
            case READY:
                allowedTypes = Arrays.asList("build", "exit");
                break;
            case RUNNING:
                allowedTypes = Arrays.asList("build", "exit", "evaluate");
                break;
        }
        return allowedTypes.contains(msgType);
    }
    private boolean isMessageAllowedOld(Map<String, String> msgContents) {
        String msgType = msgContents.get("msgType");
        List<String> allowedTypes = new ArrayList<>();
        switch (this.currentState) {
            case READY:
                allowedTypes = Arrays.asList("connectionRequest", "statusCheck", "build");
                break;
            case RUNNING:
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


    // 3.
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

    // 4.
    public void deleteMessages(List<Message> messages, String url){
        for (Message message : messages) {
            DeleteMessageRequest deleteMessageRequest = DeleteMessageRequest.builder()
                    .queueUrl(url)
                    .receiptHandle(message.receiptHandle())
                    .build();
            this.sqsClient.deleteMessage(deleteMessageRequest);
        }
    }



    private List<Message> checkPrivateQueue(){
        List<Message> brainMessages = new ArrayList<>();
        if(this.privateRequestUrl != null){
            brainMessages = this.getMessages(this.privateRequestUrl, 1, 1);
            brainMessages = this.handleMessages(this.privateRequestUrl, brainMessages);
        }
        return brainMessages;
    }

    private List<Message> checkEvalQueue(){
        // --> Only check eval queue if status is: RUNNING

        List<Message> userMessages = new ArrayList<>();
        if (this.evalRequestUrl != null && this.currentState == State.RUNNING) {
            userMessages = this.getMessages(this.evalRequestUrl, this.numEvalMessages, 3);
            userMessages = this.handleMessages(this.evalRequestUrl, userMessages);
        }
        return userMessages;
    }



    public void sendExitMessage(){
        final Map<String, MessageAttributeValue> messageAttributes = new HashMap<>();
        messageAttributes.put("msgType",
                MessageAttributeValue.builder()
                        .dataType("String")
                        .stringValue("exit")
                        .build()
        );
        messageAttributes.put("status",
                MessageAttributeValue.builder()
                        .dataType("String")
                        .stringValue("finished")
                        .build()
        );
        this.sqsClient.sendMessage(SendMessageRequest.builder()
                .queueUrl(this.privateResponseUrl)
                .messageBody("vassar_message")
                .messageAttributes(messageAttributes)
                .delaySeconds(0)
                .build());
    }




//     __  __                                  _______
//    |  \/  |                                |__   __|
//    | \  / | ___  ___ ___  __ _  __ _  ___     | |_   _ _ __   ___  ___
//    | |\/| |/ _ \/ __/ __|/ _` |/ _` |/ _ \    | | | | | '_ \ / _ \/ __|
//    | |  | |  __/\__ \__ \ (_| | (_| |  __/    | | |_| | |_) |  __/\__ \
//    |_|  |_|\___||___/___/\__,_|\__, |\___|    |_|\__, | .__/ \___||___/
//                                 __/ |             __/ | |
//                                |___/             |___/|_|

    // --------------------
    // --- NEW MESSAGES ---
    // --------------------
    // 1. Build
    // 2. Evaluate



    public void msgTypeBuild(Map<String, String> msg_contents){

        // --> 1. Get user_id
        String userId = System.getenv("USER_ID");
        this.userId = Integer.parseInt(userId);
        this.client.setUserID(this.userId);

        // --> 2. Get group_id, problem_id
        HashMap<String, Integer> user_info = this.client.getDbClient().getUserInfo();
        int group_id = user_info.get("group_id");
        int problem_id = user_info.get("problem_id");

        // --> 3. Build resource
        this.client.rebuildResource(group_id, problem_id);

        // --> 4. Send update message to ping thread
        this.sendRunningStatus(group_id, problem_id);

        // --> 5. Send update message to private response queue
        final Map<String, MessageAttributeValue> messageAttributes = new HashMap<>();
        messageAttributes.put("msgType",
                MessageAttributeValue.builder()
                        .dataType("String")
                        .stringValue("buildAck")
                        .build()
        );
        this.sqsClient.sendMessage(SendMessageRequest.builder()
                .queueUrl(this.privateResponseUrl)
                .messageBody("vassar_message")
                .messageAttributes(messageAttributes)
                .delaySeconds(0)
                .build());

        // --> 6. Update current state
        this.currentState = State.RUNNING;
        System.out.println("\n-------------------- BUILD REQUEST --------------------");
        System.out.println("--------> GROUP ID: " + group_id);
        System.out.println("------> PROBLEM ID: " + problem_id);
        System.out.println("-------------------------------------------------------\n");
    }

    public void msgTypeEvaluate(Map<String, String> msg_contents) throws Exception {


        // --> 1. Get input / dataset_id
        HashMap<String, Integer> user_info = this.client.getDbClient().getUserInfo();
        int    dataset_id = user_info.get("dataset_id");
        String input      = msg_contents.get("input");


        // --> 2. Determine how to index design
        boolean fast = false;  // If true: only index figures of merit into db
        if(msg_contents.containsKey("fast")){
            fast = Boolean.parseBoolean(msg_contents.get("fast"));
        }


        // --> 3. Determine if request by ga or not
        boolean ga_arch = false;  // If true: send arch directly back to ga
        if(msg_contents.containsKey("ga")){
            ga_arch = Boolean.parseBoolean(msg_contents.get("ga"));
        }


        // --> 4. Determining if re-evaluating design
        boolean re_evaluate = false;
        if(msg_contents.containsKey("redo")){
            re_evaluate = Boolean.parseBoolean(msg_contents.get("redo"));
        }
        if(!re_evaluate) {
            if (this.client.doesArchitectureExist(input)) {
                System.out.println("---> Architecture already exists!!!");
                return;
            }
        }


        // --> 5. Evaluate design
        Result result = this.client.evaluateArchitecture(input, dataset_id, ga_arch, re_evaluate, fast);
        System.out.println("\n-------------------- EVALUATE REQUEST OUTPUT --------------------");
        System.out.println("-----> INPUT: " + input);
        System.out.println("------> COST: " + result.getCost());
        System.out.println("---> SCIENCE: " + result.getScience());
        System.out.println("----------------------------------------------------------------\n");
    }



    public void msgTypeEvaluateUser(Map<String, String> msg_contents) throws Exception{

        // --> 1. Get input / dataset_id
        HashMap<String, Integer> user_info = this.client.getDbClient().getUserInfo();
        int    dataset_id = user_info.get("dataset_id");
        String input      = msg_contents.get("input");

        // --> 2. Get evaluation options
        boolean re_evaluate = false;  // If true:
        boolean fast        = false;  // If true: only index figures of merit into db
        if(msg_contents.containsKey("redo")){
            re_evaluate = Boolean.parseBoolean(msg_contents.get("redo"));
        }
        if(msg_contents.containsKey("fast")){
            fast = Boolean.parseBoolean(msg_contents.get("fast"));
        }

    }
    public void msgTypeEvaluateGa(Map<String, String> msg_contents) throws Exception{

        // --> 1. Get input / dataset_id
        HashMap<String, Integer> user_info = this.client.getDbClient().getUserInfo();
        int    dataset_id = user_info.get("dataset_id");
        String input      = msg_contents.get("input");

        // --> 2. Get evaluation options
        boolean re_evaluate = false;  // If true:
        boolean fast        = false;  // If true: only index figures of merit into db
        if(msg_contents.containsKey("redo")){
            re_evaluate = Boolean.parseBoolean(msg_contents.get("redo"));
        }
        if(msg_contents.containsKey("fast")){
            fast = Boolean.parseBoolean(msg_contents.get("fast"));
        }

    }




    // --------------------
    // --- OLD MESSAGES ---
    // --------------------


    // --> DEPRECATED
    private void msgTypeConnectionRequest(Map<String, String> msgContents) {

        // --> 1. Set user_id
        String userId = msgContents.get("user_id");
        this.userId = Integer.parseInt(userId);
        this.client.setUserID(this.userId);

        // --> 2. Send status to ping thread
        int group_id   = Integer.parseInt(msgContents.get("group_id"));
        int problem_id = Integer.parseInt(msgContents.get("problem_id"));
        this.sendRunningStatus(group_id, problem_id);


        // --> 3. Build rbs for evaluation
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

        this.sqsClient.sendMessage(SendMessageRequest.builder()
                .queueUrl(this.privateResponseUrl)
                .messageBody("vassar_message")
                .messageAttributes(messageAttributes)
                .delaySeconds(0)
                .build());

        this.currentState = State.RUNNING;
    }

    public void msgTypeBuildOld(Map<String, String> msg_contents){

        // --> 1. Set user id
        String userId = System.getenv("USER_ID");
        this.userId = Integer.parseInt(userId);
        this.client.setUserID(this.userId);

        // --> 2. Build resource
        int group_id   = Integer.parseInt(msg_contents.get("group_id"));
        int problem_id = Integer.parseInt(msg_contents.get("problem_id"));
        this.client.rebuildResource(group_id, problem_id);

        // --> 3. Let brain know of status
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
        messageAttributes.put("UUID",
                MessageAttributeValue.builder()
                        .dataType("String")
                        .stringValue(this.uuid)
                        .build()
        );
        this.sqsClient.sendMessage(SendMessageRequest.builder()
                .queueUrl(this.privateResponseUrl)
                .messageBody("vassar_message")
                .messageAttributes(messageAttributes)
                .delaySeconds(0)
                .build());

        // --> Update ping thread
        if(group_id != -1 && problem_id != -1){
            this.sendRunningStatus(group_id, problem_id);
        }

        this.currentState = State.RUNNING;

        System.out.println("\n-------------------- BUILD REQUEST --------------------");
        System.out.println("--------> GROUP ID: " + group_id);
        System.out.println("------> PROBLEM ID: " + problem_id);
        System.out.println("-------------------------------------------------------\n");
    }

    public void msgTypeEvaluateOld(Map<String, String> msg_contents) throws Exception {

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

    public void msgTypeSELECTING(Map<String, String> msg_contents) throws Exception {

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

    public void msgTypePARTITIONING(Map<String, String> msg_contents) throws Exception {

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


    public void msgTypeNDSM(Map<String, String> msg_contents){

        System.out.println("---> COMPUTING NDSM");
        EvaluatorApp.sleep(1);
        this.client.computeNDSMs();
    }

    public void msgTypeContinuityMatrix(Map<String, String> msg_contents) throws Exception {


        System.out.println("---> COMPUTING CONTINUITY MATRIX");
        EvaluatorApp.sleep(1);
        this.client.computeContinuityMatrix();
    }






//     _    _      _
//    | |  | |    | |
//    | |__| | ___| |_ __   ___ _ __ ___
//    |  __  |/ _ \ | '_ \ / _ \ '__/ __|
//    | |  | |  __/ | |_) |  __/ |  \__ \
//    |_|  |_|\___|_| .__/ \___|_|  |___/
//                  | |
//                  |_|



    // --> SQS Queues
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
        String[] requestQueueUrls = this.evalRequestUrl.split("/");
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
        if (!this.queueExists(this.evalRequestUrl)) {
            CreateQueueRequest createQueueRequest = CreateQueueRequest.builder()
                .queueName(requestQueueName)
                .attributes(queueAttrs)
                .build();
            CreateQueueResponse response = sqsClient.createQueue(createQueueRequest);
        }
        SetQueueAttributesRequest setAttrReq = SetQueueAttributesRequest.builder()
            .queueUrl(this.evalRequestUrl)
            .attributes(queueAttrs)
            .build();
        sqsClient.setQueueAttributes(setAttrReq);
        

        String[] responseQueueUrls = this.evalResponseUrl.split("/");
        String responseQueueName = responseQueueUrls[responseQueueUrls.length-1];
        if (!this.queueExists(this.evalResponseUrl)) {
            CreateQueueRequest createQueueRequest = CreateQueueRequest.builder()
                .queueName(responseQueueName)
                .attributes(queueAttrs)
                .build();
                CreateQueueResponse response = sqsClient.createQueue(createQueueRequest);
        }
        setAttrReq = SetQueueAttributesRequest.builder()
            .queueUrl(this.evalResponseUrl)
            .attributes(queueAttrs)
            .build();
        sqsClient.setQueueAttributes(setAttrReq);

        this.deadLetterQueueArn = deadQueueArn;
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



    // ---> Consumer Sleep
    public void consumerSleep(int seconds){
        try                            { TimeUnit.SECONDS.sleep(seconds); }
        catch (InterruptedException e) { e.printStackTrace(); }
    }




    // --> Send Messages
    public void sendTestMessages(){
        String arch = "0000000010000000000000000";
        String arch2= "0000000010000000100000000";

        this.sendEvalMessage(arch, 0);
        this.sendEvalMessage(arch2, 0);
        this.sendBuildMessage(1, 1, 0);
        this.sendEvalMessage(arch, 0);
        this.sendExitMessage();
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
                .queueUrl(this.evalRequestUrl)
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

    public void deletePrivMessages(){
        System.out.println("---> DELETING PRIVATE MESSAGES");
        this.privateQueue.clear();
    }







    // --------------------
    // --- ECS SERVICES ---
    // --------------------

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


}
