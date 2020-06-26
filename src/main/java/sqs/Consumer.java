package sqs;

import software.amazon.awssdk.services.sqs.model.*;
import vassar.VassarClient;
import software.amazon.awssdk.services.sqs.SqsClient;
import vassar.result.Result;

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

    public static class Builder {

        private boolean        debug;
        private VassarClient   client;
        private SqsClient      sqsClient;
        private String         queueUrl;

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

        public Builder debug(boolean debug) {
            this.debug = debug;
            return this;
        }

        public Consumer build(){
            Consumer build  = new Consumer();
            build.sqsClient = this.sqsClient;
            build.debug     = this.debug;
            build.client    = this.client;
            build.queueUrl  = this.queueUrl;
            build.running   = true;
            return build;
        }

    }



    // ----- MESSAGE TYPES -----
    // 1. Evaluate architecture message
    // 2. Reload rete object from database



    public void run() {
        int counter = 0;

        // this.sendTestMessages();

        while(this.running){
            System.out.println("-----> Loop iteration: " + counter);
            this.consumerSleep(1);

            List<Message> messages = this.getMessages(1, 5);

            for (Message msg: messages){
                HashMap<String, String> msg_contents = this.processMessage(msg);
                System.out.println(msg_contents);

                if(msg_contents.containsKey("msgType")){
                    String msgType = msg_contents.get("msgType");
                    if(msgType.equals("evaluate")){
                        this.mgsTypeEvaluate(msg_contents);
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
            this.deleteMessages(messages);
            counter++;
        }


    }



    // ---> MESSAGE TYPES
    public void mgsTypeEvaluate(HashMap<String, String> msg_contents){

        String input  = msg_contents.get("input");

        if(this.client.doesArchitectureExist(input)){
            System.out.println("---> Architecture already exists!!!");
            System.out.println("---> INPUT: " + input);
            this.consumerSleep(3);
            return;
        }

        Result result = this.client.evaluateArchitecture(input);

        System.out.println("\n-------------------- EVALUATE REQUEST OUTPUT --------------------");
        System.out.println("-----> INPUT: " + input);
        System.out.println("------> COST: " + result.getCost());
        System.out.println("---> SCIENCE: " + result.getScience());
        System.out.println("----------------------------------------------------------------\n");
        this.consumerSleep(3);
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


    // ---> MESSAGE FLOW
    // 1.
    public List<Message> getMessages(int maxMessages, int waitTimeSeconds){
        ReceiveMessageRequest receiveMessageRequest = ReceiveMessageRequest.builder()
                .queueUrl(this.queueUrl)
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
    public void deleteMessages(List<Message> messages){
        for (Message message : messages) {
            DeleteMessageRequest deleteMessageRequest = DeleteMessageRequest.builder()
                    .queueUrl(queueUrl)
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


    private void sendMessage(Map<String, MessageAttributeValue> messageAttributes, int delay){
        this.sqsClient.sendMessage(SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody("")
                .messageAttributes(messageAttributes)
                .delaySeconds(delay)
                .build());
    }


}
