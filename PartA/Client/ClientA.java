/**
* The client-side code of a simple FTP application.
* Author: Julian Appleyard
* Version: 0.5
Version notes:
* CONN LIST DELF QUIT are all working as expected (so far)
* UPLD behaves improperly when using a file larger than a text file
*
* Planned structure is that each command will call a method that mirrors a method executed on the server.
* The server and client methods will be structured in way that they wait for each other's responses at the appropriate time
* so that they do not become out of sync.
**/

import java.io.*;
import java.nio.file.*;
import java.nio.ByteBuffer;
import java.net.*;
import java.util.Scanner;
import java.lang.*;
import static java.lang.Math.toIntExact;

public class ClientA{

  private static Socket socket;
  private static OutputStream outToServer;


  public ClientA(String host, int port){

    try{
      System.out.println("Connecting to " + host + " on port " + port + "..."); //debug
      socket = new Socket(host, port);

      System.out.println("Connected"); //debug
    } catch(Exception e){
      e.printStackTrace();
    }
  }

/* Method for uploading a File to the Server

*/
  public static void uploadToServer(String filePath){

    try{
      Path p = Paths.get(filePath);

      File fileToBeUploaded = p.toFile();
      boolean exists = fileToBeUploaded.exists();
      if(!exists){
        DataOutputStream boolOut = new DataOutputStream(outToServer);


        System.out.println("File does not exist");
      }else{
        String file_name = p.getFileName().toString(); //throws FileNotFoundException
        System.out.println("DEBUG FILENAME: " +file_name);
        int fileNameLength = file_name.length();
        short fileNameShortLength = (short) fileNameLength;
        //There is a max length that this will work for do I need to account for this?

        //System.out.println("File namelength is: " + fileNameLength); //debug
        //System.out.println("Length in Short is: " + fileNameShortLength); //debug
        // Outline wants the length of the file name to be sent in short int
        //short shortNameLength = fileNameLength;
        //send name and length of name (in short)



        DataOutputStream dataOut = new DataOutputStream(outToServer);


        //need to send length and name in one go
        // convert to byte array with first two values being the length in short
        ByteBuffer bb = ByteBuffer.allocate(2);
        bb.putShort(fileNameShortLength);
        byte[] lengthArray = bb.array();
        //encode the file name in second byte array using UTF-8
        byte[] nameArray = file_name.getBytes("UTF-8");
        //System.out.println(lengthArray.length);//debug
        //System.out.println(nameArray.length);//debug

        //concatenate the two arrays
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(lengthArray);
        baos.write(nameArray);
        byte[] outArray = baos.toByteArray();

        System.out.println("Sending byteArray...");//DEbug

        //send the concatenated byte array to the server
        dataOut.write(outArray, 0, outArray.length);



        //Now wait for ack from server that it received the file name and name length
        InputStream iStream = socket.getInputStream();
        InputStreamReader isr = new InputStreamReader(iStream);
        BufferedReader br = new BufferedReader(isr);
        String response = br.readLine();

        //System.out.println("ack was: " + response);//debug
        //System.out.println("ack received from server");//debug

        //Client sends the size of the file, which maay be a 32 bit value sent in bytes
        long longFileSize = fileToBeUploaded.length();
        int intFileSize = toIntExact(longFileSize); //Will throw an ArithmeticException in case of overflow
        System.out.println("Int filesize: "+ intFileSize);
        ByteBuffer sizeBuff = ByteBuffer.allocate(4);
        sizeBuff.putInt(intFileSize);
        byte[] sizeArray = sizeBuff.array();

        dataOut.write(sizeArray, 0, sizeArray.length);

        //send file
        //convert file into byte array
        FileInputStream fileStream = new FileInputStream(fileToBeUploaded);

        int numOfIterations = (intFileSize/1024)+1;
        System.out.println("Number of iterations: " + numOfIterations);


        //FileInputStream reads files as bytes and DataOutputStream writes bytes to server

        // the below for loop will iterate once for every kB
        // one extra in case filesize is less than 1024 (in which case int division will give 0)
        // the extra one also covers remaining bytes left over because of integer division
        for(int i=0; i<numOfIterations; i++){
          int remaining = fileStream.available();
          System.out.println(remaining + " bytes remaining");
          if(remaining >= 1024){
            byte[] fileBytes = new byte[1024];
            fileStream.read(fileBytes, 0, 1024);
            dataOut.write(fileBytes, 0, 1024);
            System.out.println("Iteration completed: " + i);//debug
          }
          //if there is less than 1024 bytes left, make the bytearray that size instead of 1024
          else{
            byte[] fileBytes = new byte[remaining];
            fileStream.read(fileBytes, 0, remaining);
            dataOut.write(fileBytes, 0, remaining);
          }
        }
      }
      //String progress = br.readLine();
      //System.out.println(progress);

    }catch(NullPointerException e){
      e.printStackTrace();
    }
    catch(FileNotFoundException e){
      System.out.println("File not found.");
    }
    catch(IOException e){
      e.printStackTrace();
    }
    catch(Exception e){
      e.printStackTrace();
    }
  }

//  */
  /*
  public static void downloadFromServer(){
    //send length of file name in short int followed by file file_name

    //
  }
  */

  public static void listFilesOnServer(){
    try{
      InputStream iStream = socket.getInputStream();
      DataInputStream dataIn = new DataInputStream(iStream);

      //Server sends the size of directory listing to client as 32bit int

      byte[] sizeArray = new byte[4];
      dataIn.read(sizeArray);
      ByteBuffer sizeBuff = ByteBuffer.wrap(sizeArray);
      int size = sizeBuff.getInt(0);

      System.out.println("DEBUG size of directory listing: " + size); //debug

      //Client receives the size and goes into a loop to read directory listing
      //
      System.out.println("DEBUG waiting for directory listing itself...");//debug
      byte[] listingArray = new byte[size*4]; //individual characters are encoded with 1 to 4 bytes in UTF-8
      dataIn.read(listingArray);

      //decode byte array to string using UTF-8
      String names = new String(listingArray, "UTF-8");
      names = names.trim();

      //find separate filenames around the "/" character, which is in no filenames as it is not allowed by UNIX and Windows systems
      String[] directoryListing = names.split("/");

      System.out.println("\n");
      System.out.println("There are " +directoryListing.length + " files in the server's storage:");
      for(int i = 0; i<directoryListing.length; i++){
        System.out.println(directoryListing[i]);
      }
      System.out.println("\n");



    }catch(IOException e){
      e.printStackTrace();
    }
  }

/*
  This method handles the DELF Function
  The client sends the filename and
*/

  public static void deleteFileOnServer(String file_name){
    try{
      InputStream is = socket.getInputStream();
      DataInputStream dataIn = new DataInputStream(is);
      DataOutputStream dataOut = new DataOutputStream(outToServer);
      int nameLengthInt = file_name.length();
      short nameLengthShort = (short) nameLengthInt;

      //"Client sends the length of the file name (short) followed by the file name"
      ByteBuffer bb = ByteBuffer.allocate(2);
      bb.putShort(nameLengthShort);

      //Convert the two values into byte arrays
      byte[] lengthArray = bb.array();
      byte[] nameArray = file_name.getBytes("UTF-8");

      //concatenate the two byte arrays
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      baos.write(lengthArray);
      baos.write(nameArray);
      byte[] outArray = baos.toByteArray();

      System.out.println("Sending byteArray...");//debug

      //send the concatenated byte array to the server
      dataOut.write(outArray, 0, outArray.length);

      //wait for integer confirmation code
      byte[] confirmBytes = new byte[4];
      dataIn.read(confirmBytes);
      ByteBuffer inBuffer = ByteBuffer.wrap(confirmBytes);
      int confirm = inBuffer.getInt();

      String command = "";
      byte[] commandBytes = new byte[0];
      if(confirm == 1){
        System.out.println("Are you sure you want to delete " + file_name + " from the server's storage?");
        System.out.println("Enter 'yes' to delete or 'no' to abort deletion process");

        while(true){
          Scanner keyboard = new Scanner(System.in);
          command = keyboard.nextLine().trim();

          if(command.equalsIgnoreCase("yes")){
            System.out.println("Confirming deletion with server...");//debug

            commandBytes = command.getBytes();
            dataOut.write(commandBytes, 0, commandBytes.length);
            //wait for ack of deletion

            byte[] ackBytes = new byte[50];
            dataIn.read(ackBytes);
            String ack = new String(ackBytes, "UTF-8");
            System.out.println("Server responded with: " + ack);

            break;
          }
          else if(command.equalsIgnoreCase("no")){

            System.out.println("Delete abandoned by the user!");

            commandBytes = command.getBytes();
            dataOut.write(commandBytes, 0, commandBytes.length);
            break;
          }
          else{
            System.out.println("Invalid response. Enter 'yes' to delete the file or 'no' to abort deletion process");
          }
          //The client thne sends the user's confirm ("yes" or "no") back to the server

        }

      }else{
        System.out.println("The file does not exist on server");
        //returns to "prompt user for operation" state
      }


    }catch(IOException e){
      e.printStackTrace();
    }
  }

  public static void main(String [] args){
    String host = "localhost";
    int port = 42000;

    try{

      ClientA client = new ClientA(host, port);

      outToServer = socket.getOutputStream();

      OutputStreamWriter osw = new OutputStreamWriter(outToServer);
      BufferedWriter bw = new BufferedWriter(osw);

      Boolean isTerminated = false;

      while(true){

        //Use Scanner to record user input

        Scanner keyboard = new Scanner(System.in);
        System.out.println("Enter message to send to " + host);
        String textToServer = keyboard.nextLine();


        bw.write(textToServer +"\n");
        bw.flush();
        //System.out.println(textToServer + " sent to server");
        switch(textToServer){
          case "HELP":
            System.out.println("Valid commands are:");
            System.out.println("UPLD : to upload a file to the server");
            System.out.println("LIST : to list the directories/files at the server");
            System.out.println("DWLD : to download a file from the server");
            System.out.println("DELF : to delete a file from the server \n");
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
            //call list files method
            listFilesOnServer();
            break;

          case "DWLD":
            System.out.println("Beginning procedure for downloading a file from the server...");
            //call method to download a file from the server
            break;

          case "DELF":
            System.out.println("Beginning procedure for deleting a file from the server...");
            System.out.println("Enter the name of the file on the server that you want to delete");

            String filename = keyboard.nextLine();

            deleteFileOnServer(filename);
            //call delete files method
            break;

          case "QUIT":
            socket.close();
            isTerminated = true;
            break;

          default:
            System.out.println("Invalid command. Type 'HELP' to see valid commands.");
        }
        if(isTerminated){
          break;
        }
      }//innner while
    }//try
    catch(IOException e){
      e.printStackTrace();
    }
  }

}
