package org.example;

// Name: Tavaris
// CS643 Programming Assignment 1
// Code for EC2-B
// Java package Maven artifacts for the SDK 2.x


//Region Service = US_EAST_1
    import software.amazon.awssdk.regions.Region;
//Client for Rekognition service
    import software.amazon.awssdk.services.rekognition.RekognitionClient;
//Using Image,DetectTextRequest,DetectTextResponse,S3Object,TextDetection,TextTypes Objects
    import software.amazon.awssdk.services.rekognition.model.*;

//Client for SQS service
    import software.amazon.awssdk.services.sqs.SqsClient;
//Using DeleteMessageRequest, GetQueueUrlRequest,ListQueuesRequest,ListQueuesResponse,Message,QueueNameExistsException,ReceiveMessageRequest
    import software.amazon.awssdk.services.sqs.model.*;
//Using List,Map,HashMap,Iterator
    import java.util.*;
//Using FileWriter,IOException
    import java.io.*;

public class CarTextMaven {

    public static void main(String[] args) {

        String bucketName = "cs643-njit-project1";
        String queueName = "tavCar.fifo"; // -1 is the last on to get processed in the FIFO queue

        RekognitionClient rekClient = RekognitionClient.builder()
                .region(Region.US_EAST_1)
                .build();
        SqsClient sqs = SqsClient.builder()
                .region(Region.US_EAST_1)
                .build();

        processCarImages(rekClient, sqs, bucketName, queueName);
    }


    public static void processCarImages( RekognitionClient rekClient, SqsClient sqs, String bucketName,
                                        String queueName) {

        // Pull SQS until the queue is created by CarRecogMaven
        while (true) {
            ListQueuesRequest ReqQList = ListQueuesRequest.builder()
                    .queueNamePrefix(queueName)
                    .build();
            ListQueuesResponse ResQList = sqs.listQueues(ReqQList);

                System.out.println("Queue is empty " +ResQList.equals(ResQList));

            //Get number of elements in a list
            if (ResQList.queueUrls().size() > 0)
            { break; }
        }


        // Retrieve the queueURL
        String queueUrl = "";
        try {
            //NB: This is the URL I am requesting via a GET_request "https://sqs.us-east-1.amazonaws.com/789905007233/tavCar.fifo"
            GetQueueUrlRequest getReqQ = GetQueueUrlRequest.builder()
                    .queueName(queueName)
                    .build();
            queueUrl = sqs.getQueueUrl(getReqQ)
                    .queueUrl();
        } catch (QueueNameExistsException e) {
            throw e;}

        // Process cars from Queue images
        try {
            boolean endOfQ = false;
            HashMap<String, String> outputs = new HashMap<String, String>();

            while (!endOfQ) {
                // Retrieve next image index
                ReceiveMessageRequest MsgReqReceived = ReceiveMessageRequest.builder().queueUrl(queueUrl)
                        .maxNumberOfMessages(1).build();
                List<Message> LSmessages = sqs.receiveMessage(MsgReqReceived).messages();


                if (LSmessages.size() > 0) {

                    //Returns the element at the specified position in this list ".get(0)".
                    Message GETmessage = LSmessages.get(0);
                    String labelKey = GETmessage.body();

                    if (labelKey.equals("-1")) {
                        //When instance A terminates its image processing, it adds index -1 to the queue
                        // to signal to instance B that no more indexes will come.
                        endOfQ = true;
                        System.out.println("End of processing car image text from cs643-njit-project1 bucket: " + labelKey);
                    }
                        else {
                            System.out.println("Processing car image with text from cs643-njit-project1 bucket: " + labelKey);

                            //Take Image from 3Bucket using AWS Rekognition tool
                            Image img = Image.builder().s3Object(S3Object.builder().bucket(bucketName).name(labelKey).build())
                                    .build();
                            DetectTextRequest request = DetectTextRequest.builder()
                                    .image(img)
                                    .build();
                            DetectTextResponse result = rekClient.detectText(request);

                            //CarText (textDetections) array, will be used with my "for-each" loop below
                            List<TextDetection> textDetections = result.textDetections();

                            if (textDetections.size() != 0) {
                                String text = "";

                                //Output elements in the cars array, using a "for-each" loop
                                for (TextDetection textDetected : textDetections) {

                                    //If "textDetected" has WORDS
                                    if (textDetected.type().equals(TextTypes.WORD)){
                                        text = text.concat(" " + textDetected.detectedText());

                                    outputs.put(labelKey, text); }
                                }
                            }
                        }

                    // Delete the message in the queue now that it's been handled
                    DeleteMessageRequest deleteMessageRequest = DeleteMessageRequest.builder().queueUrl(queueUrl)
                            .receiptHandle(GETmessage.receiptHandle())
                            .build();
                    sqs.deleteMessage(deleteMessageRequest);
                }
            }

            try {
                FileWriter writerFile = new FileWriter("output.txt");

                Iterator<Map.Entry<String, String>> it = outputs.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry<String, String> pair = it.next();
                    writerFile.write(pair.getKey() + ":" + pair.getValue() + "\n");
                    it.remove();
                }

                writerFile.close();
                System.out.println("Results written to file output.txt");
            } catch (IOException e) {
                System.out.println("An error occurred writing to file.");
                e.printStackTrace();
            }
        } catch (Exception e) {
            System.err.println(e.getLocalizedMessage());
            System.exit(1);
        }
    }
}