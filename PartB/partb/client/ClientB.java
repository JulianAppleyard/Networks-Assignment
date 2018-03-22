package partb.client;


import partb.frontend.MainInterface;


//import ClientA dependencies
import java.io.*;
import java.nio.file.*;
import java.nio.ByteBuffer;
import java.net.*;
import java.util.Scanner;
import java.lang.*;
import static java.lang.Math.toIntExact;

//import rmi dependencies
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class ClientB{

  public ClientB(String host, int port) throws UnknownHostException, IOException{

  }//constructor


/*
            *MAIN METHOD*

*/

  public static MainInterface stub;
  public static void main(String [] args){
    String host = "localhost";
    int port = 42000;

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
            stub.uploadFromClient();
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
      //e.printStackTrace();
    }catch(Exception e){
      e.printStackTrace();
    }

  }//MAIN


/*
            *UPLD*
*/
  public static void uploadToServer(String path){

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
    .

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
