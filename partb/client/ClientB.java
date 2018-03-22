package partb.client;


import partb.frontend.ServerB1Interface;
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


  public static void main(String [] args){
    String host = "localhost";
    int port = 42000;

    boolean isTerminated = false;
    try{
      //Get registry
      Registry registry = LocateRegistry.getRegistry(host, port);

      //Lookup the remote object from registry
      //and create a stub for it
      ServerB1Interface stub = (ServerB1Interface) registry.lookup("FTPB1");

      while(true){

        //Use Scanner to record user input
        Scanner keyboard = new Scanner(System.in);


        System.out.println("Enter message to send to " + host);
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
            byte[] byteArray = stub.listFilesOnServer();

            //call list files method
            listFilesOnServer(byteArray);
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

            stub.deleteFileOnServer();
            deleteFileOnServer(file_name);
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


  public static void deleteFileOnServer(String filename){

  }//DELF

  public static void uploadToServer(String path){

  }//UPLD

  public static void downloadFromServer(String host, int port, String file_name){

    try{


      Socket socket = new Socket(host, port);
      InputStream is = socket.getInputStream();
      DataInputStream dataIn = new DataInputStream(is);


      OutputStream outToServer = socket.getOutputStream();
      DataOutputStream dataOut = new DataOutputStream(outToServer);
      int nameLengthInt = file_name.length();
      short nameLengthShort = (short) nameLengthInt;

      //send length of file name in short int followed by file file_name
      ByteBuffer bb = ByteBuffer.allocate(2);
      bb.putShort(nameLengthShort);

      //Convert the two values into byte arrays
      byte[] lengthArray =  bb.array();
      byte[] nameArray = file_name.getBytes("UTF-8");

      //concatenate the two byte arrays
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      baos.write(lengthArray);
      baos.write(nameArray);
      byte[] outArray = baos.toByteArray();

      //send the concatenated byte array to the server
      dataOut.write(outArray, 0, outArray.length);

      //wait for integer confirmation code and then decode it
      byte[] confirmBytes = new byte[4];  //int takes up 1-4 bytes
      dataIn.read(confirmBytes);
      ByteBuffer inBuffer = ByteBuffer.wrap(confirmBytes);
      int confirm = inBuffer.getInt();

      //Integer confirm code will be the filesize as 32-bit int if the file exists
      //if it doesnt exist, server will respond with -1 (32-bit int)
      if(confirm != -1){

        int fileSize = confirm;

        Path currentRelativePath = Paths.get("");
        String stringPath = currentRelativePath.toAbsolutePath().toString();
        File newFile = new File(stringPath + "partb\\client\\Storage\\" + file_name);

        //this regex should find the last period (.) in the filename. This will be the one right before the extension
        //
        String regex = "\\.(?=[^\\.]*$)";

        /*
        If the filename already exists on the client, rename the file to include (1) before the file extension
        eg: SmallFile.txt becomes SmallFile(1).txt
        The following while loop will continue to loop until it finds a filename which doesn't exist in the client's storage
        */
        int j=1;
        while(true){
          if(newFile.exists()){
            //The regex splits around the last period (.) found in the file name
            String[] stringArray = file_name.split(regex);
            newFile = new File(stringPath + "\\Storage\\" + stringArray[0] + "(" + j + ")" + "." + stringArray[1]);
          }else{
            //once it finds a name that makes newFile.exist() return false, it breaks out of the loop
            break;
          }
          j++;
        }
        newFile.createNewFile();


        //Server now sends the file to the client
        FileOutputStream fileOutputWriter = new FileOutputStream(newFile);
        //Client reads "file size" bytes from server
        double startTime = System.currentTimeMillis();
        System.out.println("Transferring...");

        //the client saves the file to disk as "file name"
        /*
        The below for loop will iterate once for every kB
        with one extra in case filesize is less than 1024 bytes (in which case int division will give 0)
        The extra one also covers remaining bytes left over because of integer division
        It reads the file 1024 bytes at a time and writes that same amount immediately
        */
        int numOfIterations = (fileSize/1024)+1;

        for(int i =0; i<numOfIterations; i++){
          //on the last iteration, the remaining bytes will be less than 10244, so create a byte array with that size instead of 1024
          if(i== numOfIterations-1){
            int remaining = fileSize%1024;
            byte[] fileBytes = new byte[remaining];
            dataIn.read(fileBytes, 0, remaining);
            fileOutputWriter.write(fileBytes, 0, remaining);
          }else{
            byte[] fileBytes = new byte[1024];
            dataIn.read(fileBytes, 0, 1024);
            fileOutputWriter.write(fileBytes, 0, 1024);
          }
        }
        //calculate transfer
        double transferTime = System.currentTimeMillis() - startTime;
        double seconds = transferTime /1000.00;
        System.out.println(fileSize + " bytes transferred in " + seconds + " seconds");

        //Inform user that the transfer was successful and return to "prompt user for operation state"

        System.out.println("File transfer successful");

      }
      else{
        System.out.println("File does not exist on server.");
        //returns to "promp user for operation" state
      }

  }catch(IOException e){
    e.printStackTrace();
  }
  }//DWLD


/*
            *LIST*
  The server outputs a byte array with the first four (4) bytes represent the size of the directory listing (ie number String characters)
  The directory listing is the names of all the files in the servers' storage
*/
  public static void listFilesOnServer(byte[] byteArray){
    try{

      //Server sends the size of directory listing to client as 32bit int

      ByteBuffer listBuffer = ByteBuffer.wrap(byteArray);
      int size = listBuffer.getInt(); //first four bytes are the size of the listing in integer

      byte[] nameArray = new byte[listBuffer.remaining()];
      listBuffer.get(nameArray);


      //decode byte array to string using UTF-8
      String names = new String(nameArray, "UTF-8");
      names = names.trim();

      String[] directoryListing;
      if(names.length()==0){
        directoryListing = new String[0];
      }else{
        //find separate filenames around the "/" character, which is in no filenames as it is not allowed by UNIX and Windows systems
        directoryListing = names.split("/");
      }
      System.out.println("\n");
      System.out.println("There are " +directoryListing.length + " files in the server's storage:");
      for(int i = 0; i<directoryListing.length; i++){
        System.out.println(directoryListing[i]);
      }
      System.out.println("\n");



    }catch(IOException e){
      e.printStackTrace();
    }
  }//LIST

}//class
