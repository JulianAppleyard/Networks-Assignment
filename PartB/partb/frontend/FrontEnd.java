package partb.frontend;


//import own interface and server interfaces
import partb.frontend.MainInterface;
import partb.servers.serverb1.*;
import partb.servers.serverb2.*;
import partb.servers.serverb3.*;


import java.util.*;
import java.io.*;
import java.nio.file.*;
import java.net.*;
import java.nio.ByteBuffer;
import static java.lang.Math.toIntExact;

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

    public static void main(String args[]){
      String host = "localhost";
      int mainPort = 42000;

      try{


        //Setup RMI for the front-end itself
        FrontEnd obj = new FrontEnd();
        MainInterface mainStub = (MainInterface) UnicastRemoteObject.exportObject(obj, 0);
        //get Registry

        //DO THIS OUTSIDE THE PROGRAM? script?
      //  LocateRegistry.createRegistry(mainPort);
        Registry registry = LocateRegistry.getRegistry(host, mainPort);

        registry.bind("MAIN", mainStub);

        //Setup RMI for ServerB1
        stubB1 = (ServerB1Interface) registry.lookup("FTPB1");
        //Setup RMI for ServerB2
        stubB2 = (ServerB2Interface) registry.lookup("FTPB2");
        //Setup RMI for ServerB3
        stubB3 = (ServerB3Interface) registry.lookup("FTPB3");


        System.out.println("FrontEnd running");
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




    public void uploadFromClient() throws SocketException, IOException{

    }





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
      byte[] byteArrayS3 = stubB1.listFilesOnServer();

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

      String[] threeDecoded = new String[3];
      threeDecoded[0] = decodedS1;
      threeDecoded[1] = decodedS2;
      threeDecoded[2] = decodedS3;

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

    String[] threeDecoded = new String[3];
    threeDecoded[0] = decodedS1;
    threeDecoded[1] = decodedS2;
    threeDecoded[2] = decodedS3;

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
