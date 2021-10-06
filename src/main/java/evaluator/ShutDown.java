package evaluator;

import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteQueueRequest;
import software.amazon.awssdk.services.sqs.model.DeleteQueueResponse;


// The purpose of this class is to delete the instance's private queue


public class ShutDown extends Thread{

    private String private_queue_url;
    private final SqsClient sqs;

    public ShutDown(String private_queue_url){
        this.private_queue_url = private_queue_url;
        this.sqs = SqsClient.builder()
                            .region(Region.US_EAST_2)
                            .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                            .build();
    }

    @Override
    public void run(){
        System.out.println("--> DELETING PRIVATE QUEUE:" + this.private_queue_url);

        DeleteQueueRequest  request  = DeleteQueueRequest.builder().queueUrl(this.private_queue_url).build();
        DeleteQueueResponse response = this.sqs.deleteQueue(request);
        this.sqs.close();
    }

}
