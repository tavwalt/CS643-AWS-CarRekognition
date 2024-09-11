package org.example;

// Name: Tavaris
// CS643 Programming Assignment 1
// Code for EC2-A
// Java package Maven artifacts for the SDK 2.x


import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.DetectLabelsRequest;
import software.amazon.awssdk.services.rekognition.model.DetectLabelsResponse;
import software.amazon.awssdk.services.rekognition.model.Image;
import software.amazon.awssdk.services.rekognition.model.Label;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;
import java.util.List;


public class CarRecogMaven {

    public static void main(String[] args) {


        String bucketName = "cs643-njit-project1";
        String queueName = "tavCar.fifo"; // -1 is the last on to get processed in the FIFO queue
        String queueGroup = "group1";


        S3Client s3bucket = S3Client.builder()
                .region(Region.US_EAST_1)
                .build();
        RekognitionClient rekClient = RekognitionClient.builder()
                .region(Region.US_EAST_1)
                .build();
        SqsClient sqsClient = SqsClient.builder()
                .region(Region.US_EAST_1)
                .build();


        processBucketImages(s3bucket, rekClient, sqsClient, bucketName, queueName, queueGroup);
        }


    public static void processBucketImages(S3Client s3bucket, RekognitionClient rekClient, SqsClient sqsClient, String bucketName,
                                           String queueName, String queueGroup) {

        // Variable for my queueURL-- Retrieve the queueUrl
        String queueUrl = "";
        try {
            ListQueuesRequest QueReq = ListQueuesRequest.builder()
                    .queueNamePrefix(queueName)
                    .build();
            //Capture response of Queue creation(sqs)
            ListQueuesResponse QueResponse = sqsClient.listQueues(QueReq);



            if (QueResponse.queueUrls().size() == 0)
            {//NB: This is the URL I am requesting via a GET_request "https://sqs.us-east-1.amazonaws.com/789905007233/tavCar.fifo"
                GetQueueUrlRequest getURLQue = GetQueueUrlRequest.builder()
                        .queueName(queueName)
                        .build();

                //creating a queue for SQS, to be used in rekogination
                queueUrl = sqsClient.getQueueUrl(getURLQue).queueUrl();
            }


            else {
                //If there are values in the Queue get it; items from 0 to=Integer.MaxValue
                queueUrl = QueResponse.queueUrls().get(0);
            }
        }

        catch (QueueNameExistsException e) {
            throw e;
        }


        // Process the 10 images in the S3 bucket
        try {
            //Request items in s3-bucket
            ListObjectsV2Request listObjectsReqManual = ListObjectsV2Request.builder().bucket(bucketName).maxKeys(10)
                    .build();

            //capture response
            ListObjectsV2Response listObjResponse = s3bucket.listObjectsV2(listObjectsReqManual);

            //For the contents in the ListResponse...do below
            for (S3Object obj : listObjResponse.contents() )
            {
                System.out.println("Gathered image in cs643-njit-project1: " + obj.key());

                //Take Image from 3Bucket using AWS Rekognition tool
             Image img = Image.builder().s3Object(software.amazon.awssdk.services.rekognition.model.S3Object
                                .builder().bucket(bucketName).name(obj.key()).build()).build();


             //AWS Rekognition
                DetectLabelsRequest request = DetectLabelsRequest.builder().image(img).minConfidence((float) 80)
                        .build();
                DetectLabelsResponse result = rekClient.detectLabels(request);

                //Listing all label response
                List<Label> labels = result.labels();
                for (Label label : labels) {
                    if (label.name().equals("Car")) {

                        //Printed Lables so you can see them with confidence
                        System.out.println(label);

                        //send **obj.key .eg. "1.jpg"** message to the Queue(sqs)
                        sqsClient.sendMessage(SendMessageRequest.builder().messageGroupId(queueGroup).queueUrl(queueUrl)
                          .messageBody(obj.key()).build());
                        break;
                    }
                }

            }

            // Signal the end of image processing by sending "-1" to the queue
            sqsClient.sendMessage(SendMessageRequest.builder().queueUrl(queueUrl).messageGroupId(queueGroup).messageBody("-1").build());

        } catch (Exception e) {
            System.err.println(e.getLocalizedMessage());
            System.exit(1);
        }
    }
}
