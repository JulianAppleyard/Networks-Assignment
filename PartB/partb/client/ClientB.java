package partb.client;


import partb.frontend.MainInterface;


//import ClientA dependencies
import java.io.*;
import java.nio.file.*;
import java.nio.ByteBuffer;
import java.net.*;
import java.util.Scanner;
import java.util.concurrent.*;
import java.lang.*;

//import rmi dependencies
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;



public class ClientB{

  public ClientB(String host, int port) throws UnknownHostException, IOException{

  }//constructor



  public static MainInterface stub;
  public static String host;
  //for socket connections
  public static int frontPort;


/*
            *MAIN METHOD*

*/






  public static void main(String [] args){
    host = "localhost";
    int port = 42000;
    frontPort = 39000;
    boolean isTerminated = false;

    try{
      //Get registry
      Registry registry = LocateRegistry.getRegistry(host, port);

      //Lookup the remote object from registry
      //and create a stub for it
      //ServerB1Interface stub = (ServerB1Interface) registry.lookup("FTPB1");
      stub = (MainInterface) registry.lookup("MAIN");
      System.out.println("Connected to server");
      while(true){

        //Use Scanner to record user input
        Scanner keyboard = new Scanner(System.in);


        System.out.println("Enter a command");
        String textToServer = keyboard.nextLine();

        switch(textToServer){
          case "HELP":
            System.out.println("Valid commands are:");
            System.out.println("UPLD : to upload a file to the server");
            System.out.println("LIST : to list the directories/files at the server");
            System.out.println("DWLD : to download a file from the server");
            System.out.println("DELF : to delete a file from the server");
            System.out.println("QUIT : to terminate the connection and exit the client\n");
            break;

          case "UPLD":



            System.out.println("Beginning procedure for uploading a file to the server...");

            System.out.println("Enter path of file to be uploaded");
            String path = keyboard.nextLine();
            //call upload file method
            uploadToServer(path);
            break;

          case "LIST":


            System.out.println("Beginning procedure for listing files on the server...");
            String[] stringListing = stub.getMasterListing();

            //call list files method
            listFilesOnServer(stringListing);
            break;

          case "DWLD":


            System.out.println("Beginning procedure for downloading a file from the server...");
            System.out.println("Enter the name of the file on the server that you want to download");
            String filename = keyboard.nextLine();

            //call method to download a file from the server
            stub.downloadToClient();
            downloadFromServer(host, port, filename);
            break;

          case "DELF":

            System.out.println("Beginning procedure for deleting a file from the server...");
            System.out.println("Enter the name of the file on the server that you want to delete");

            String file_name = keyboard.nextLine();

            String[] fileList = stub.getMasterListing();

            deleteFileOnServer(file_name, fileList);
            //call delete files method
            break;

          case "QUIT":
            //socket.close();
            isTerminated = true;
            break;

          default:
            System.out.println("Invalid command. Type 'HELP' to see valid commands.");
        }
        if(isTerminated){
          break;
        }
      }//inner while
    }catch(IOException e){
      isTerminated = true;
      System.out.println("Error connecting to server. Ensure server is running and try again");
      e.printStackTrace();
    }catch(Exception e){
      e.printStackTrace();
    }

  }//MAIN


/**
            *UPLD*
**/
  public static void uploadToServer(String filePath) throws InterruptedException, SocketException, IOException{

    Path p = Paths.get(filePath);
    String file_name = p.getFileName().toString();

    File fileToBeUploaded = p.toFile();
    long fileSize = fileToBeUploaded.length();
    int intFileSize = (int) (long) fileSize;




    //the file must exist on the clientside to proceed
    boolean exists = fileToBeUploaded.exists();


    //the file must be not a directory
    if(fileToBeUploaded.isDirectory()){
      exists = false;
    }

    if(!exists){
      System.out.println("File does not exist");
      //if it doesn't exist, exit to main menu without calling frontend

    }else{
      //otherwise, continue with the process
      //tell the frontend to get ready
      stub.startUpload();



      String[] masterListing = stub.getMasterListing();

      //this regex should find the last period (.) in the filename. Should be the one right before the extension
      String regex = "\\.(?=[^\\.]*$)";

      String[] splitArray = file_name.split(regex);

      //test to see if the filename exists in the listing already
      //if it does, increment j by one and insert j within parentheses before the file extension
      //eg SmallFile.txt becomes SmallFile(1).txt
      int j = 0;

      for(int i=0; i <masterListing.length; i++){
        if(file_name.equals(masterListing[i])){
          j++;
          file_name = splitArray[0] + "("+ j +")"+"."+splitArray[1];
          i=-1;
        }
      }








      Scanner input = new Scanner(System.in);
      boolean reliabilityFlag = false;

      //see if the user wants a high-reliability upload
      while(true){
        System.out.println("Make this a high-reliability upload? Type 'yes' or 'no' to specify.");


        String response = input.nextLine();


        if(response.equalsIgnoreCase("yes")){
          reliabilityFlag = true;
          break;
        }else if(response.equalsIgnoreCase("no")){
          reliabilityFlag = false;
          break;
        }else{
          System.out.println("Invalid response. Type 'yes' to specify high-reliability upload or 'no' to opt-out");
          //only break the loop if they use a valid response
        }
      }//while

      //connect to frontend
      Socket socket = new Socket(host, frontPort);
      OutputStream oStream = socket.getOutputStream();
      DataOutputStream dataOut = new DataOutputStream(oStream);

      //send reliability first
      ByteBuffer reliableBuffer = ByteBuffer.allocate(4);
      if(reliabilityFlag){
        reliableBuffer.putInt(1);
      }else{
        reliableBuffer.putInt(-1);
      }
      byte[] reliableArray = reliableBuffer.array();

      dataOut.write(reliableArray, 0, reliableArray.length);


      //start timing
      double startTime = System.currentTimeMillis();



      //next array of data is the length of filename and the filename
      int fileNameLength = file_name.length();
      short fileNameShortLength = (short) fileNameLength;


      ByteBuffer nameLengthBuffer = ByteBuffer.allocate(2);
      nameLengthBuffer.putShort(fileNameShortLength);
      byte[] lengthArray = nameLengthBuffer.array();

      byte[] nameArray = file_name.getBytes("UTF-8");

      //concatenate the two arrays
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      baos.write(lengthArray);
      baos.write(nameArray);
      byte[] outArray = baos.toByteArray();

      //send off the concatenated byte array
      TimeUnit.SECONDS.sleep(1);

      dataOut.write(outArray, 0, outArray.length);
      TimeUnit.SECONDS.sleep(1);


      //ack?

     //next array of data is the size of the file in 32 bit int



     ByteBuffer sizeBuff = ByteBuffer.allocate(4);
     sizeBuff.putInt(intFileSize);
     byte[] sizeArray = sizeBuff.array();

     dataOut.write(sizeArray, 0, sizeArray.length);

      /*
        Start the upload of the file itself
      */
      FileInputStream fileStream = new FileInputStream(fileToBeUploaded);


      //the below for loop will iterate once for every kB
      //it reads and sends the file in bytes 1024 bytes at a time
      // one extra in case filesize is less than 1024 (in which case int division will give 0)
      //the extra one also covers remaining bytes left over because of integer division
      System.out.println("Uploading...");
      int numOfIterations = (intFileSize/1024)+1;


      for(int i=0; i<numOfIterations; i++){
          int remaining = fileStream.available();
          if(remaining >= 1024){
            byte[] fileBytes = new byte[1024];
            fileStream.read(fileBytes, 0, 1024);
            dataOut.write(fileBytes, 0, 1024);
          }
          //if there is less than 1024 bytes left, make the bytearray that size instead of 1024
          else{
            byte[] fileBytes = new byte[remaining];
            fileStream.read(fileBytes, 0, remaining);
            dataOut.write(fileBytes, 0, remaining);
          }

        }

      double uploadTime = System.currentTimeMillis() - startTime;
      double uploadTimeSeconds = uploadTime /1000.00;
      System.out.println(fileSize + " bytes uploaded in " + uploadTimeSeconds + " seconds");


    }//exists else

  //exit to main menu

  }//UPLD


  /*
              *LIST*
    The Front-End outputs an array of strings which represent unique files in the distributed System
    The client therefore needs only to output each String to the user

  */
  public static void listFilesOnServer(String[] stringListing){
      try{
        String[] directoryListing = stringListing;


        int listingLength = 0;

        if(stringListing.length==1 && stringListing[0].equals("")){
          //if there are no files, the String array will contain one entry containing the empty string
          // in this case the length number of files should be displayed as 0 not 1.
          listingLength = 0;

        }else{

          listingLength = directoryListing.length;
        }

        System.out.println("\n");
        System.out.println("There are " +listingLength + " files in the server's storage:");


        //print each result
        for(int i = 0; i<directoryListing.length; i++){
          System.out.println(directoryListing[i]);
        }

        //A space at the bottom of this list is desirable for readability
        System.out.println("\n");

      }catch(Exception e){
        e.printStackTrace();
      }
    }//LIST




/*
            *DWLD*
*/
  public static void downloadFromServer(String host, int port, String file_name){


  }//DWLD





  /*
              *DELF
    Takes as input the list of strings generated by stub.getMasterListing() and the filename

  */

  public static void deleteFileOnServer(String filename, String[] stringListing) throws SocketException, IOException{
    Scanner recordInput = new Scanner(System.in);
    boolean fileExists = false;


    //check if file is in the system
    for(int i=0; i< stringListing.length; i++){
      if(filename.equals(stringListing[i])){
        fileExists=true;
        break;//found it, dont need to keep looping
      }
    }


    //if the file is found to exist, confirm with user that they want to proceed with deletion
    if(fileExists){

      while(true){
        System.out.println("Are you sure you want to delete " + filename + " from system storage?");
        System.out.println("Enter 'yes' to delete or 'no' to abort deletion process");
        String input = recordInput.nextLine();

        if(input.equalsIgnoreCase("yes")){

          //user confirms, tell front-end to delete file and then break the loop to exit to main menu
          stub.deleteFileOnServer(filename);
          System.out.println("File deleted");
          break;

        }else if(input.equalsIgnoreCase("no")){

          //user aborts, break loop to exit to main menu without deleting file
          System.out.println("Delete abandoned by the user!");
          break;

        }else{
          //user enters neither yes nor no, restart loop
          System.out.println("Invalid response. Enter 'yes' to delete the file or 'no' to abort deletion process");
        }
      }//while

    //if the file doesn't exist, inform the user and exit to main menu
    }else{
      System.out.println("File does not exist.");
    }
  }//DELF


}//class
