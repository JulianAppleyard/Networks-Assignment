/**
* The client-side code of a simple FTP application.
* Author: Julian Appleyard
* Version: 0.9
Version notes:
* All functions operating.
* Comments could use cleaning up.
* Remove debug statements
* doublecheck requirements
*
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

public class ClientA{

  private static Socket socket;
  private static OutputStream outToServer;


  public ClientA(String host, int port) throws UnknownHostException, IOException{

    System.out.println("Connecting to " + host + " on port " + port + "..."); //debug
    socket = new Socket(host, port);

    System.out.println("Connected"); //debug
  }


/*
              *MAIN METHOD*
  This main method first calls the construcutor for a new ClientA object. The constructor takes the host and port number
  and immediately atttempts to connect to the server.
  If connection fails, the constructor throws an exception and the client exits on the assumption that the server is not running.
  Upon connecting, the client enters into a loop to take operations from the user through the command line.

  Valid operations (case sensitive) are:
    HELP: lists valid commands to the user (client only)
    (the rest of these are sent to the server and call methods on the server as well)

    UPLD: calls the method which handles uploading of files to the server's storage
    LIST: calls the method which displays a list of files in the server's storage subdirectory
    DWLD: calls the method which handles downloading of files from the server's storage to the client's storage
    DELF: calls the method which handles deleting of a file from the server's storage
    QUIT: handles the termination of the connection to the server and the closing of the client

*/

  public static void main(String [] args){
    String host = "localhost";
    int port = 42000;

    boolean isTerminated = false;

    try{

      ClientA client = new ClientA(host, port);


      //Stream handling
      outToServer = socket.getOutputStream();
      OutputStreamWriter osw = new OutputStreamWriter(outToServer);

      //BufferedWriter is used to write string to the server
      BufferedWriter bw = new BufferedWriter(osw);


      while(true){

        //Use Scanner to record user input

        Scanner keyboard = new Scanner(System.in);


        System.out.println("Enter a command");
        String textToServer = keyboard.nextLine();

        //user input is case sensitive
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
            bw.write(textToServer +"\n");
            bw.flush();


            System.out.println("Beginning procedure for uploading a file to the server...");

            System.out.println("Enter path of file to be uploaded");
            String path = keyboard.nextLine();
            //call upload file method
            uploadToServer(path);
            break;

          case "LIST":
            bw.write(textToServer +"\n");
            bw.flush();

            System.out.println("Beginning procedure for listing files on the server...");
            //call list files method
            listFilesOnServer();
            break;

          case "DWLD":
            bw.write(textToServer +"\n");
            bw.flush();

            System.out.println("Beginning procedure for downloading a file from the server...");
            System.out.println("Enter the name of the file on the server that you want to download");
            String filename = keyboard.nextLine();

            //call method to download a file from the server
            downloadFromServer(filename);
            break;

          case "DELF":
            bw.write(textToServer +"\n");
            bw.flush();


            System.out.println("Beginning procedure for deleting a file from the server...");
            System.out.println("Enter the name of the file on the server that you want to delete");

            String file_name = keyboard.nextLine();

            deleteFileOnServer(file_name);
            //call delete files method
            break;

          case "QUIT":
            bw.write(textToServer +"\n");
            bw.flush();

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
      isTerminated = true;
      //e.printStackTrace();
      System.out.println("Error connecting to server. Ensure server is running and try again");

    }

  }//main






/*
            *UPLD*
Method for uploading a File to the Server

*/
  public static void uploadToServer(String filePath){

    try{
      Path p = Paths.get(filePath);

      File fileToBeUploaded = p.toFile();
      //the file must exist on the clientside
      boolean exists = fileToBeUploaded.exists();
      // the file must also not be a directory
      if(fileToBeUploaded.isDirectory()){
        exists = false;
      }
      int confirm = 0;

      if(!exists){
        //if the file does not exist or is a directory, tell the server to abort the upload process
        confirm = -1;
        DataOutputStream existOut = new DataOutputStream(outToServer);
        ByteBuffer existBuffer = ByteBuffer.allocate(4);
        existBuffer.putInt(confirm);
        byte[] existArray = existBuffer.array();
        existOut.write(existArray, 0, existArray.length);

        System.out.println("File does not exist");

      }else{
        confirm =1;
        String file_name = p.getFileName().toString(); //throws FileNotFoundException
      //  System.out.println("DEBUG FILENAME: " +file_name);
        int fileNameLength = file_name.length();
        short fileNameShortLength = (short) fileNameLength;
        //There is a max length that this will work for do I need to account for this?

        //System.out.println("File namelength is: " + fileNameLength); //debug
        //System.out.println("Length in Short is: " + fileNameShortLength); //debug
        // Outline wants the length of the file name to be sent in short int
        //short shortNameLength = fileNameLength;
        //send name and length of name (in short)


        //the file exists on the client, so tell the server to proceed with the upload process
        DataOutputStream dataOut = new DataOutputStream(outToServer);
        ByteBuffer existBuffer = ByteBuffer.allocate(4);
        existBuffer.putInt(confirm);
        byte[] existOut = existBuffer.array();
        dataOut.write(existOut, 0, existOut.length);



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

      //  System.out.println("Sending byteArray...");//DEbug

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
        int intFileSize = (int) (long) longFileSize;

      //  System.out.println("Int filesize: "+ intFileSize);//debug
        ByteBuffer sizeBuff = ByteBuffer.allocate(4);
        sizeBuff.putInt(intFileSize);
        byte[] sizeArray = sizeBuff.array();

        dataOut.write(sizeArray, 0, sizeArray.length);

        //send file
        //convert file into byte array
        FileInputStream fileStream = new FileInputStream(fileToBeUploaded);

        int numOfIterations = (intFileSize/1024)+1;
        //System.out.println("Number of iterations: " + numOfIterations);

        double startTime = System.currentTimeMillis();
        //FileInputStream reads files as bytes and DataOutputStream writes bytes to server

        // the below for loop will iterate once for every kB
        // one extra in case filesize is less than 1024 (in which case int division will give 0)
        // the extra one also covers remaining bytes left over because of integer division
        System.out.println("Transferring...");
        for(int i=0; i<numOfIterations; i++){
          int remaining = fileStream.available();
          //System.out.println(remaining + " bytes remaining");//debug
          if(remaining >= 1024){
            byte[] fileBytes = new byte[1024];
            fileStream.read(fileBytes, 0, 1024);
            dataOut.write(fileBytes, 0, 1024);
            //System.out.println("Iteration completed: " + i);//debug
          }
          //if there is less than 1024 bytes left, make the bytearray that size instead of 1024
          else{
            byte[] fileBytes = new byte[remaining];
            fileStream.read(fileBytes, 0, remaining);
            dataOut.write(fileBytes, 0, remaining);
          }

        }

        double transferTime = System.currentTimeMillis() - startTime;
        double seconds = transferTime /1000.00;
        System.out.println(intFileSize + " bytes transfered in " + seconds + " seconds");
      }


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

  public static void downloadFromServer(String file_name){
    try{
      InputStream is = socket.getInputStream();
      DataInputStream dataIn = new DataInputStream(is);
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
        File newFile = new File(stringPath + "\\Storage\\" + file_name);

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
  }


/*
            *LIST*


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

      //System.out.println("debug size of directory listing: " + size); //debug

      //Client receives the size and goes into a loop to read directory listing
      //
      //System.out.println("debug waiting for directory listing itself...");//debug
      byte[] listingArray = new byte[size*4]; //individual characters are encoded with 1 to 4 bytes in UTF-8
      dataIn.read(listingArray);

      //decode byte array to string using UTF-8
      String names = new String(listingArray, "UTF-8");
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

    //  System.out.println("Sending byteArray...");//debug

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


}
