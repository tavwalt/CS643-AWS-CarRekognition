CS643
Programming Assignment 1
Name: Tavaris
Date: 06/30/2024

****AWS Steps****
Setup the cloud environment and run AWS-ProgAssign1Maven-CarRecog2.jar and AWS-ProgAssign1Maven-CarTEXT2.jar 
in the cloud on 2 separate EC2 instances, follow the below steps.

First, you will need to login to your AWS Console through the AWS Educate Starter Account link.


--EC2 Setup [Perform for EC2_A and EC2_B]
Click on "Services" -> "EC2"
Click on "Instances" in the drop down "Instances" Section 
Click on "Launch Instances"

	Add a "Name"
		OR (click "Add additional tags")
	Insert "EC2_A" under "Key" and "Value"
	
Select the "Amazon Linux, Amazon Linux 2023 AMI 2023.5.20240624.0 x86_64 HVM kernel-6.1     (free tier eligible)"  
Select the "t2.micro type (Free tier eligible)" 

Click "Create new key pair" (If you do not have one)
	On the dialog that pops up, select "Create a new key pair" in the drop down
	Name it "EC2-A-KeyPair" 
	Keep "RSA" and ".pem" selected
	Hit "Download key pair" to get your private key
	
Under "Network Settings"
	Choose your "security group"
	All SSH and set your ip(0.0.0.0/0 - will all connection from anywhere)
	
Under "Configure Storage"
	leave default 1x 8GiB (General purpose SSD gp3)
	
Go to "Summary" details
	Ensure "Number of instances" is "1"

Click "Launch instance" after reviewing your information
	
	You will probably see a status of "Pending" for the Instance State of each EC2. 
	While waiting for these to switch to "Running," open a terminal and move the .pem 
	file you downloaded to a home directory where you will build your code using a IDE on
	your local device.
	

--Run the following command to set the correct permissions for the .pem file:
	$ chmod 400 EC2-A-KeyPair.pem
	
	
--IAM - attach a IAM(Policy). We used the "LabRole" for the class which has the roles below:
	AmazonRekognitionFullAccess, AmazonS3FullAccess, AmazonSQSFullAccess
		→ Go to your EC2 instances:
			Click "Actions"
			Go to "Security"
			Click "Modify IAM role"
			Choose "LabInstanceProfile"
			Then click " Update IAM role"



--Connect to your EC2 instances (after they have started running), 
Run the following command in your terminal (replacing <YOUR_EC2_INSTANCE_PUBLIC_DNS> with 
the "Public IPv4 DNS" attribute of either EC2 instance):
	$ ssh -i "EC2-A-KeyPair" ec2-user@<YOUR_EC2_INSTANCE_PUBLIC_IPV4_ADDRESS>:/home/ec2-user/

	
--Credentials Setup [Access and Secret Keys]
	Was not need because our class LabRole already had IAM services for S3, SQS and Rekognition
	

--Java Installation on your two EC2 instances 
Run the commands to install Java on your local machine, run the following commands:
	//To check what version you are running
		java -version 

	//Use to download .gz file from “https://www.oracle.com/java/technologies/javase/jdk19-archive-downloads.html”
		wget https://download.oracle.com/java/19/archive/jdk-19.0.2_linux-x64_bin.tar.gz

	//Extract the .gz file 
	tar zxvf jdk-19.0.2_linux-x64_bin.tar.gz

	//Move file to shared folder so all users have access on machine
	sudo mv jdk-19.0.2 /usr/share/

	//Open profile configuration file
		sudo vim /etc/profile

		//*Add to top of config profile*
				export JAVA_HOME=/usr/share/jdk-19.0.2
				export PATH=$JAVA_HOME/bin:$PATH
				export CLASSPATH=.:$JAVA_HOME/lib/dt.jar:$JAVA_HOME/lib/tools.jar
			 		→ ( exit and save with [ :wq ] )

																		*************************************************
																		***Extract Files and Run the application Codes***
																		*************************************************

--Extract files from "Tavaris-CS643Assign1.zip"
Tavaris-CS643Assign1/output.txt

--Java code, Jar file and xml for EC2-A located:
EC2-A-CarRecog/CarRecogMaven.java
EC2-A-CarRecog/pomCarRecogMaven.xml
EC2-A-CarRecog/AWS-ProgAssign1Maven-CarRecog2.jar
EC2-A-CarRecog/1.EC2-A imgLabelConfidence.txt

--Java code, Jar file and xml for EC2-B located:
EC2-B-CarTEXT/CarTextMaven.java
EC2-B-CarTEXT/pomCarTextMaven.xml
EC2-B-CarTEXT/AWS-ProgAssign1Maven-CarTEXT2.jar
EC2-B-CarTEXT/2.snapshot EC2-B-Consumer Poll.png


--SSH into EC2-B and run the following command:
	$ java -jar AWS-ProgAssign1Maven-CarTEXT2.jar
	**This will begin running the code for TEXT recognition. However, since it depends on items in the AWS(SQS) queue 
	to process, it will wait until we begin running the "car recognition code". 
	 
	 
--SSH into EC2-A and run the following command:
	$ java -jar AWS-ProgAssign1Maven-CarRecog2.jar
	
Now, both programs will be running simultaneously. The program on EC2-A is processing all images in the AWS-S3 bucket (cs643-njit-project1) and sending the indexes(KeyVal) of images that are  
recognized (AWS-Rekognition) as "cars" to EC2-B through AWS-SQS. EC2-B then consumes(Poll) the data from AWS-SQS and uses AWS-Rekognition tool to extract "Text" found in the respective "car" images in AWS-S3.
Finally, once both programs have finished running, you will find a file named "output.txt" on my EC2-B instance in the /home/ec2-user/ directory. 
This output file will display the indexes of images that contained both cars and text, along with the associated text from each image.
