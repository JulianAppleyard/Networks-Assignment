package partb.frontend;


//import own interface and server interfaces
import partb.frontend.MainInterface;
import partb.servers.serverb1.*;
import partb.servers.serverb2.*;
import partb.servers.serverb3.*;


import java.util.*;
import java.util.concurrent.*;

import java.io.*;
import java.nio.file.*;
import java.net.*;
import java.nio.ByteBuffer;

//Import java rmi stuff
import java.rmi.*;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;



public class FrontEnd implements MainInterface{

    public FrontEnd(){}

    public static ServerB1Interface stubB1;
    public static ServerB2Interface stubB2;
    public static ServerB3Interface stubB3;
    public static String host;

    //for socket connections
    public static boolean uploadCheck;
    public static boolean downloadCheck;
    public static int clientPort;//the port to create sockets to connect to client on
    public static int portServerB1;//the ports to connect to each server on sockets
    public static int portServerB2;
    public static int portServerB3;

    public static void main(String args[]){
      host = "localhost";
      int mainPort = 42000;
      int clientPort = 39000;
      portServerB1 = 42001;
      portServerB2 = 42002;
      portServerB3 = 42003;

      try{


        //Setup RMI for the front-end itself
        FrontEnd obj = new FrontEnd();
        MainInterface mainStub = (MainInterface) UnicastRemoteObject.exportObject(obj, 0);
        //get Registry


        Registry registry = LocateRegistry.getRegistry(host, mainPort);

        registry.bind("MAIN", mainStub);

        //Setup RMI for ServerB1
        stubB1 = (ServerB1Interface) registry.lookup("FTPB1");
        //Setup RMI for ServerB2
        stubB2 = (ServerB2Interface) registry.lookup("FTPB2");
        //Setup RMI for ServerB3
        stubB3 = (ServerB3Interface) registry.lookup("FTPB3");


        System.out.println("FrontEnd running");
        uploadCheck = false;
        downloadCheck = false;

        while(true){
          TimeUnit.SECONDS.sleep(1);
          //System.out.println("checking");//debug

          if(uploadCheck || downloadCheck){

      //      System.out.println("Creating socket connection to client...");//debug
            ServerSocket serverSocket = new ServerSocket(clientPort);


            if(uploadCheck){
              uploadFromClient(serverSocket);

              uploadCheck = false;
              serverSocket.close();
            }

            if(downloadCheck){

            downloadCheck = false;
            }

          }



        }








      }catch(AlreadyBoundException e){

        e.printStackTrace();
        System.out.println("FrontEnd is already running");
      }catch(NotBoundException e){
        //e.printStackTrace();
        System.out.println("System not running!");
      }catch(Exception e){
        e.printStackTrace();
      }
    }//MAIN


/**
            *UPLD*

* For each upload the front end will allocate the file for upload to the file server
with the smallest number of files stored.
*The client can also specify a "high reliability" option in a file upload request to force
the frontend to upload the file to all servers


**/
  public void startUpload(){
    System.out.println("Starting Upload process...");
    uploadCheck = true;
  }


  public static void uploadFromClient(ServerSocket serverSocket) throws InterruptedException, SocketException, IOException{


    /*
      Find the server with the lowest number of files on it
    */
    //get a list from each
    //first obtain list of files from each server
    byte[] byteArrayS1 = stubB1.listFilesOnServer();
    byte[] byteArrayS2 = stubB2.listFilesOnServer();
    byte[] byteArrayS3 = stubB3.listFilesOnServer();

    //each byte array contains the byte representation of one long string of each file name
    //followed by a "/" concatenated together

    //First decode the byte into the concatenated string
    String decodedS1 = new String(byteArrayS1, "UTF-8");
    String decodedS2 = new String(byteArrayS2, "UTF-8");
    String decodedS3 = new String(byteArrayS3, "UTF-8");

    //remove potential whitespace
    decodedS1 = decodedS1.trim();
    decodedS2 = decodedS2.trim();
    decodedS3 = decodedS3.trim();


    String[] listServer1 = decodedS1.split("/");
    String[] listServer2 = decodedS2.split("/");
    String[] listServer3 = decodedS3.split("/");


    if(listServer1.length == 1 && listServer1[0].equals("")){
      listServer1 = new String[0];
    }
    if(listServer2.length == 1 && listServer2[0].equals("")){
      listServer2 = new String[0];
    }
    if(listServer3.length == 1 && listServer3[0].equals("")){
      listServer3 = new String[0];
    }





    int smallestLength = listServer1.length; //by default ServerB1 will be selected
    int smallestServer = 1;

    if(listServer2.length < smallestLength){
      smallestLength = listServer2.length;
      smallestServer = 2;
    }
    if(listServer3.length < smallestLength){
      smallestLength = listServer3.length;
      smallestServer = 3;
    }



    //wait for a connection and then accept and create streams
    Socket clientSocket = serverSocket.accept();
    System.out.println("Socket Connection to client established");//debug
    InputStream uploadInputStream = clientSocket.getInputStream();
    DataInputStream dataIn = new DataInputStream(uploadInputStream);
    boolean isReliable = false;

    byte[] reliableArray = new byte[4];

    //receive encoded reliability int
    dataIn.read(reliableArray);

    //decode reliability value
    ByteBuffer reliableBuffer = ByteBuffer.wrap(reliableArray);
    int reliableCode = reliableBuffer.getInt();

    if(reliableCode == 1){
      isReliable = true;
    }else{
      isReliable = false;
      System.out.println("is not reliable");//debug
    }

    //decide which server(s) to send to
    boolean toServerB1 = false;
    boolean toServerB2 = false;
    boolean toServerB3 = false;

    if(smallestServer == 1 || isReliable){
      toServerB1 = true;
      //inform server 1 to prepare socket
      stubB1.startUploadSocket();
    }
    if(smallestServer == 2 || isReliable){
      toServerB2 = true;
      //inform server 2 to prepare socket
      stubB2.startUploadSocket();
    }
    if(smallestServer == 3 || isReliable){
      toServerB3 = true;
      //inform server 3 to prepare socket
      stubB3.startUploadSocket();
    }




    //first the client sends the length of the file name and the filename itself
    byte[] lengthName = new byte[1024]; //assume that the name of the file wont take up a kB

    dataIn.read(lengthName);
    //the front end does not need to deocode this


    Socket socketB1;
    Socket socketB2;
    Socket socketB3;
    OutputStream outB1;
    OutputStream outB2;
    OutputStream outB3;
    DataOutputStream dataToB1;
    DataOutputStream dataToB2;
    DataOutputStream dataToB3;
    if(toServerB1){
      //create connection
      System.out.println("Creating connection to ServerB1");
      socketB1 = new Socket(host, portServerB1);
      //create Streams
      outB1 = socketB1.getOutputStream();
      dataToB1 = new DataOutputStream(outB1);

      //send data

      dataToB1.write(lengthName, 0, lengthName.length);
    }else{
      socketB1 = null;
      outB1 = null;
      dataToB1 = null;

    }
    if(toServerB2){
      //create connection
      System.out.println("Creating connection to ServerB2");
      socketB2 = new Socket(host, portServerB2);
      //create Streams
      outB2 = socketB2.getOutputStream();
      dataToB2 = new DataOutputStream(outB2);

      //send data

      dataToB2.write(lengthName, 0, lengthName.length);
    }else{
      socketB2 = null;
      outB2 = null;
      dataToB2 = null;

    }
    if(toServerB3){
      //create connection
      System.out.println("Creating connection to ServerB3");
      socketB3 = new Socket(host, portServerB3);
      //create Streams
      outB3 = socketB3.getOutputStream();
      dataToB3 = new DataOutputStream(outB3);

      //send data

      dataToB3.write(lengthName, 0, lengthName.length);
    }else{
      socketB3 = null;
      outB3 = null;
      dataToB3 = null;

    }
    //the next set of data is the file filesize

    byte[] sizeArray = new byte[5];
    dataIn.read(sizeArray);

    ByteBuffer sizeBuff = ByteBuffer.wrap(sizeArray);
    int fileSize = sizeBuff.getInt(0);

    if(toServerB1){
    //  System.out.println("Sending filesize to B1");
      dataToB1.write(sizeArray);
    }
    if(toServerB2){
      //System.out.println("Sending filesize to B2");
      dataToB2.write(sizeArray);
    }
    if(toServerB3){
      //System.out.println("Sending filesize to B3");
      dataToB3.write(sizeArray);
    }



    /*
    The below for loop will iterate once for every kB
    There is one extra in case filesize is less than 1024 (in which case int division will give 0)
    The extra one also covers remaining bytes left over because of integer division
    */
    int numOfIterations = (fileSize/1024)+1;
    //System.out.println(fileSize);//debug
    for(int i=0; i<numOfIterations; i++){
      if(i == numOfIterations-1){//if it is the last iteration
        //the last byte array should have the size of the following modulus
        int remaining = fileSize%1024;
        byte[] fileBytes = new byte[remaining];
        dataIn.read(fileBytes, 0, remaining);
        TimeUnit.MILLISECONDS.sleep(500);
        if(toServerB1){
          //System.out.println("Sending final pieces to B1");
          dataToB1.write(fileBytes, 0, remaining);
          //send data

          socketB1.close();

        }
        if(toServerB2){
          dataToB2.write(fileBytes, 0, remaining);
          //send data

          socketB2.close();

        }
        if(toServerB3){
          dataToB3.write(fileBytes, 0, remaining);
          //send data

          socketB3.close();
        }
      }else{
        byte[] fileBytes = new byte[1024];
        dataIn.read(fileBytes, 0, 1024);

        if(toServerB1){
          dataToB1.write(fileBytes, 0, 1024);
          //send data



        }
        if(toServerB2){
          dataToB2.write(fileBytes, 0, 1024);
          //send data

        }
        if(toServerB3){
          dataToB3.write(fileBytes, 0, 1024);
          //send data

        }
      }//else
    }//for

  }//UPLD





/*
            *LIST*
    The front-end here needs to obtain lists of files in all three servers' storage.
    The front-end should remove duplicates.
    How does this interact with renaming of uploaded duplicates?
    -a new file being uploaded should be renamed if any files already exist with that name


*/

  public String[] getMasterListing() throws SocketException, IOException{

    try{
      //Stream management
      System.out.println("Client requested list");
      ByteArrayOutputStream baos = new ByteArrayOutputStream();

      byte[] byteArrayS1 = stubB1.listFilesOnServer();
      byte[] byteArrayS2 = stubB2.listFilesOnServer();
      byte[] byteArrayS3 = stubB3.listFilesOnServer();

      //each byte array contains the byte representation of one long string of each file name
      //followed by a "/" concatenated together

      //First decode the byte into the concatenated string
      String decodedS1 = new String(byteArrayS1, "UTF-8");
      String decodedS2 = new String(byteArrayS2, "UTF-8");
      String decodedS3 = new String(byteArrayS3, "UTF-8");

      //remove potential whitespace
      decodedS1 = decodedS1.trim();
      decodedS2 = decodedS2.trim();
      decodedS3 = decodedS3.trim();


      String[] listServer1 = decodedS1.split("/");
      String[] listServer2 = decodedS2.split("/");
      String[] listServer3 = decodedS3.split("/");

      //then deconcatenate each string to obtain a String array of filenames for each server


      //Assemble a master listing by removing duplicates and combining the listingss



      ArrayList<String> masterListing = new ArrayList<String>(0);


      //First add all filenames from the first server to the masterListing
      for(int i=0; i<listServer1.length; i++){
        masterListing.add(listServer1[i]);
      }

      masterListing.trimToSize();


      //Then check filenames from the second server against the masterListing before adding them
      for(int i=0; i<listServer2.length; i++){
        boolean isAlreadyThere =false; //we assume a given filename isn't already there until proven otherwise
        for(int j=0; j<masterListing.size(); j++){
          if(masterListing.get(j).equals(listServer2[i])){
            isAlreadyThere = true;
            break;//we found a match, we dont need to keep checking
          }
        }//inner for
        if(isAlreadyThere){
          //do nothing: the file already exists in the masterListing
        }else{
          masterListing.add(listServer2[i]);
        }
      }//outer for

      masterListing.trimToSize();



      //Then check filenmes from the third server against the masterListing before adding them
      for(int i=0; i<listServer3.length; i++){
        boolean isAlreadyThere= false; //we assume a given filename isn't alreayd there until proven otherwise
        for(int j=0; j<masterListing.size(); j++){
          if(masterListing.get(j).equals(listServer3[i])){
            isAlreadyThere = true;
            break;//we found a match, we dont need to keep checking
          }
        }//inner for
        if(isAlreadyThere){
          //do nothing: the file already exists in the masterListing
        }else{
          masterListing.add(listServer3[i]);
        }
      }//outer for

      String[] masterArray = new String[0];
      //Having now obtained a master listing of all unique files on the server, we must convert to bytes

      masterListing.trimToSize();

      //first convert to array of string
      masterArray = masterListing.toArray(masterArray); //toArray() will allocate a new array if it doesnt fit

      return masterArray;



    }catch(Exception e){
      e.printStackTrace();
      return null;
    }
  }//LIST

  public void downloadToClient() throws SocketException, IOException{

  }//DWLD

/**
            *DELF*
*  In order to delete a file from the system, the front-end needs to check every server storage for the file.
*  Then delete the file from EVERY server on which it exists
*
* This method is quite strict in that it assumes that:
1. There are no duplicates within a given server. If a filename gets uploaded twice it will be renamed by the upload method.
The checking for duplicates upon upload should be done acroess all servers.
2. That the filename is givne in a case sensitive manner
3. That the file exists on at least one server

**/
  public void deleteFileOnServer(String file_name)throws SocketException, IOException{


    //Assume the file is not on any server
    boolean isOn1 = false;
    boolean isOn2 = false;
    boolean isOn3 = false;

    /*
    check all three servers for the existence of the file_name given
    */

    //first obtain list of files from each server
    byte[] byteArrayS1 = stubB1.listFilesOnServer();
    byte[] byteArrayS2 = stubB2.listFilesOnServer();
    byte[] byteArrayS3 = stubB3.listFilesOnServer();

    //each byte array contains the byte representation of one long string of each file name
    //followed by a "/" concatenated together

    //First decode the byte into the concatenated string
    String decodedS1 = new String(byteArrayS1, "UTF-8");
    String decodedS2 = new String(byteArrayS2, "UTF-8");
    String decodedS3 = new String(byteArrayS3, "UTF-8");

    //remove potential whitespace
    decodedS1 = decodedS1.trim();
    decodedS2 = decodedS2.trim();
    decodedS3 = decodedS3.trim();

    String[] listServer1 = decodedS1.split("/");
    String[] listServer2 = decodedS2.split("/");
    String[] listServer3 = decodedS3.split("/");

    //iterate through first server listing to see if file exists there
    for(int i=0; i<listServer1.length; i++){
      if(file_name.equals(listServer1[i])){
        isOn1 = true;
        break;//we have found it on this server, no need to continue looping
      }
    }

    //iterate through second server listing to see if file exists there
    for(int i=0; i<listServer2.length; i++){
      if(file_name.equals(listServer2[i])){
        isOn2 = true;
        break;//we have found it on this server, no need to continue looping
      }
    }

    //iterate through third server listing to see if file exists there
    for(int i=0; i<listServer3.length; i++){
      if(file_name.equals(listServer3[i])){
        isOn3 = true;
        break;//we have found it on this server, no need to continue looping
      }
    }



    if(isOn1){
      System.out.println("Deleting from ServerB1");//debug
      stubB1.deleteFileOnServer(file_name);
    }
    if(isOn2){
      System.out.println("Deleting from ServerB2");//debug
      stubB2.deleteFileOnServer(file_name);
    }
    if(isOn3){
      System.out.println("Deleting from ServerB3");//debug
      stubB3.deleteFileOnServer(file_name);
    }

  }//DELF



}//class
