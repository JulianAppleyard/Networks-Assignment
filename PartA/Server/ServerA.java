/**
* The server-side code of a simple FTP application. The file transfer itself
* should take place using TCP with the client.
* Author: Julian Appleyard
* Version: 0.9
* see ClientA for version notes


**/

import java.util.*;
import java.io.*;
import java.nio.file.*;
import java.net.*;
import java.nio.ByteBuffer;
import static java.lang.Math.toIntExact;

public class ServerA {
  private static Socket socket;
  private static InputStream is;
/*
This method handles the UPLD function.
The client sends the operation "UPLD" to upload a local file to the server.
current this will overwrite files if they have the same name
*/

  public static void uploadFromClient(){
    try{
      //client checks if the file exists on the clientside

        DataInputStream dataIn = new DataInputStream(is);
        boolean exists = false;
        byte[] confirmArray = new byte[4];
        dataIn.read(confirmArray);
        ByteBuffer confirmBuffer = ByteBuffer.wrap(confirmArray);
        int confirm = confirmBuffer.getInt();

        if(confirm==1){
          exists=true;
        }

        if(!exists){
          System.out.println("Client file does not exist. Returning to main menu...");
        }else{

          byte[] inArray = new byte[50];
          //byte array has an arbitrary limited length
          //is this bad practice?
          dataIn.read(inArray);

          //System.out.println("Recieving file name...");//debug

          //Client sends the length of the file name which will be sent (short int) and the file_name itself
          // These will be sent encoded in a byte Array
          // the first two values are the length of the name in type short
          // the remaining will be the file_name itself encoded as UTF-8

          ByteBuffer buffer = ByteBuffer.wrap(inArray);

          short lengthShort = buffer.getShort(0);
          //System.out.println("DEBUG length: "+ lengthShort); //debug

          //System.out.println(buffer.toString());//debug
          byte[] temp = new byte[buffer.remaining()];
          buffer.get(temp);

          String file_name = new String(temp,"UTF-8");
          file_name = file_name.trim();
        //  System.out.println("dEBUG string: "+ file_name);//debug

          //send acknowledgement that the server is ready to receive
          OutputStream oStream = socket.getOutputStream();
          OutputStreamWriter osr = new OutputStreamWriter(oStream);
          BufferedWriter bw = new BufferedWriter(osr);

          String message = "Send file size" + "\n";
          //System.out.println("Sending ack");//debug
          bw.write(message, 0, message.length());
          bw.flush();
        //  System.out.println("Ack sent"); //debug

          //Server receives and decodes file filesize

          byte[] sizeArray = new byte[5];
          dataIn.read(sizeArray);
          ByteBuffer sizeBuff = ByteBuffer.wrap(sizeArray);
          int fileSize = sizeBuff.getInt(0); //what do I do with this?


          //ok so if the file is too big it will fuck everything up
          System.out.println("filesize: " + fileSize);//debug

          Path currentRelativePath = Paths.get("");
          String stringPath = currentRelativePath.toAbsolutePath().toString();
          File newFile = new File(stringPath + "\\Storage\\" + file_name);


          //this regex should find the last period (.) in the filename. Should be the one right before the extension
          String regex = "\\.(?=[^\\.]*$)";

          int j=1;
          while(true){
            //If the filename already exists on the server, rename the file to include (1)
            // eg SmallFile.txt becomes SmallFile(1).txt
            //This will keep looping until it finds a filename which does not exist in the server's storage
            //
            if(newFile.exists()){
              //The regex splits around the last period (.) found in the file name
              String[] nameArray = file_name.split(regex);
              System.out.println(stringPath + "\\Storage\\" + nameArray[0]+"("+j+")"+"."+nameArray[1]);//debug
              newFile = new File(stringPath + "\\Storage\\" + nameArray[0]+"("+j+")"+"."+nameArray[1]);
            }
            else{
              break;
            }
            j++;
          }
          System.out.println(newFile.getPath());//debug
          newFile.createNewFile();

          FileOutputStream fileOutputWriter = new FileOutputStream(newFile);

          int numOfIterations = (fileSize/1024)+1;
        //  System.out.println("Number of iterations: " + numOfIterations);//debug


          /*
          The below for loop will iterate once for every kB
          There is one extra in case filesize is less than 1024 (in which case int division will give 0)
          The extra one also covers remaining bytes left over because of integer division
          */

          for(int i=0; i<numOfIterations; i++){
            if(i == numOfIterations-1){
              int remaining = fileSize%1024;
              byte[] fileBytes = new byte[remaining];
              dataIn.read(fileBytes, 0, remaining);
              fileOutputWriter.write(fileBytes, 0, remaining);
            }else{
              byte[] fileBytes = new byte[1024];
              dataIn.read(fileBytes, 0, 1024);
              fileOutputWriter.write(fileBytes, 0, 1024);
              //System.out.println("Iteration completed: " + i); //debug
            }
          }

          System.out.println("File written");
          bw.flush();
        }
    }catch(IOException e){
      e.printStackTrace();
    }
  }
/*
Method for listing files on the server
Going to only list files in the "Storage" directory
What exceptions are thrown?
*/

  public static void listFilesOnServer(){
    try{

      //Stream management
      OutputStream os = socket.getOutputStream();
      DataOutputStream listDataOut = new DataOutputStream(os);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();

      //Client sends operation (LIST) to list directories/files at the server
      //Server obtains listing of its directories/files
      //it is sufficient to only list the names of files/directories
      Path currentRelativePath = Paths.get("");
      String stringPath = currentRelativePath.toAbsolutePath().toString();
      File directory = new File(stringPath + "\\Storage\\");
      String[] listOfFileNames = directory.list();


      //loop through the list of file names to find the size of the listing

      int size = 0;
      for(int i =0; i<listOfFileNames.length; i++){
        //
        int lengthOfCurrentString = listOfFileNames[i].length();
        size = size + lengthOfCurrentString;

        //convert each filename into a byte array (UTF-8 encoded)
        //and write to the ByteArrayOutputStream
        byte[] nameArray = listOfFileNames[i].getBytes("UTF-8");
        baos.write(nameArray);
        byte[] seperatorArray = "/".getBytes("UTF-8");
        baos.write(seperatorArray);
      }

      //ByteArrayOutputStream baos will contain all filenames concatenated
      //but separated with a "/" to allow for the client to parse the filenames
      //I chose this character because it is not allowed in filenames in UNIX or Windows
      //

      byte[] listingArray = baos.toByteArray();


    //  System.out.println("debug size of directory listing: " + size);

      //convert size of listing (32 bit int) to byte array for sending to client

      ByteBuffer listBuffer = ByteBuffer.allocate(4);
      listBuffer.putInt(size);
      byte[] lengthArray = listBuffer.array();

      //System.out.println("Sending lengthArray...");//debug
      listDataOut.write(lengthArray, 0, lengthArray.length);

      //System.out.println("Sending listingArray...");//debug
      listDataOut.write(listingArray, 0, listingArray.length);

    }catch(Exception e){
      e.printStackTrace();
    }
  }

/*
This method handles the DWNLD function.
The client requests a file and the server sends the file to the client
just an outline for now
*/

  public static void downloadToClient(){
    try{
      OutputStream os = socket.getOutputStream();
      DataOutputStream dataOut = new DataOutputStream(os);
      DataInputStream dataIn = new DataInputStream(is);
      byte[] inArray = new byte[50];

      //Client sends the length of the file name (short int) and the filename (String)
      dataIn.read(inArray);

      ByteBuffer inBuffer = ByteBuffer.wrap(inArray);

      /*
        Server decodes file name size and file name
      */

      //the first two bytes are the file name length in short
      //
      short lengthShort = inBuffer.getShort(0);

      //The remaining bytes are the file name String
      //
      byte[] nameBytes = new byte[inBuffer.remaining()];
      inBuffer.get(nameBytes);

      String file_name = new String (nameBytes, "UTF-8");
      file_name = file_name.trim();

      //Arrange the path to the file
      //the file is in the \Storage\ directory
      //
      Path currentRelativePath = Paths.get("");
      String stringPath = currentRelativePath.toAbsolutePath().toString();
      stringPath = stringPath + "\\Storage\\" + file_name;

      File f = new File(stringPath);

      //check if file exists at that location
      Boolean fileExists = f.exists();
      //if the file is a directory, treat it as if it doesn't exist (abort the download process)
      if(f.isDirectory()){
        fileExists = false;
      }

      if(fileExists){
        //if the file does exist, server returns the size of the file to the client as a 32-bit int
        long fileSizeLong = f.length();
        int fileSizeInt = toIntExact(fileSizeLong);

        ByteBuffer outBuffer = ByteBuffer.allocate(4);
        outBuffer.putInt(fileSizeInt);
        byte[] outArray = outBuffer.array();
        dataOut.write(outArray, 0, outArray.length);

        // now the server sends the file to the client
        FileInputStream fileStream = new FileInputStream(f);


        //FileInputStream reads files as bytes and DataOutputStream writes bytes to client stream

        /*
        The below for loop will iterate once for every kB
        with one extra in case filesize is less than 1024 bytes (in which case int division will give 0)
        The extra one also covers remaining bytes left over because of integer division.
        */

        int numOfIterations = (fileSizeInt/1024)+1;
        System.out.println("Transferring...");//debug

        for(int i=0; i<numOfIterations; i++){
          int remaining = fileStream.available();

          if(remaining >= 1024){
            byte[] fileBytes = new byte[1024];
            fileStream.read(fileBytes, 0, 1024);
            dataOut.write(fileBytes, 0, 1024);
          }
          //if there is less than 1024 bytes left, make the byte array that size instead of 1024
          else{
            byte[] fileBytes = new byte[remaining];
            fileStream.read(fileBytes, 0, remaining);
            dataOut.write(fileBytes, 0, remaining);
          }
        }

      }else{
        // if the file does not exist, the server returns 32 bit int value -1
        int confirm = -1;
        ByteBuffer outBuffer = ByteBuffer.allocate(4);
        outBuffer.putInt(confirm);
        byte[] outArray = outBuffer.array();
        dataOut.write(outArray, 0, outArray.length);
      }
    }catch(IOException e){

    }
  }



/*
This method handles the DELF function
The client sends the file name the server deletes file if it exists
just an outline for now
*/

  public static void deleteFileOnServer(){
    try{
      OutputStream os = socket.getOutputStream();
      DataOutputStream dataOut = new DataOutputStream(os);
      DataInputStream dataIn = new DataInputStream(is);
      byte[] inArray = new byte[50];

      //Client sends the length of the file name (short int) and the filename (String)
      dataIn.read(inArray);
      System.out.println("Receiving file name...");//debug

      ByteBuffer inBuffer = ByteBuffer.wrap(inArray);
      /*
        Server decodes file name size and file name
      */

      //The first two bytes are the file name length in short
      //
      short lengthShort = inBuffer.getShort(0);
      //System.out.println("Debug length: " + lengthShort);

      //The remaining bytes are the file name String
      //
      byte[] nameBytes = new byte[inBuffer.remaining()];
      inBuffer.get(nameBytes);

      String file_name = new String(nameBytes, "UTF-8");
      file_name = file_name.trim();
      //System.out.println("Debug file to be deleted: " + file_name);

      Path currentRelativePath = Paths.get("");
      String stringPath = currentRelativePath.toAbsolutePath().toString();
      stringPath = stringPath + "\\Storage\\" + file_name;

      File f = new File(stringPath);
    //  System.out.println(stringPath);
    //  System.out.println(f.exists());//debug

      Boolean fileExists = f.exists();

      //Server checks if the file to be deleted exists or not
      if(fileExists){
        //if the file does exist, the server sends a positive confirm (integer value 1)
        int confirm = 1;
        ByteBuffer outBuffer = ByteBuffer.allocate(4);
        outBuffer.putInt(confirm);
        byte[] outArray = outBuffer.array();
        dataOut.write(outArray, 0, outArray.length);

        byte[] commandBytes = new byte[50];
        dataIn.read(commandBytes);

        String command = new String(commandBytes, "UTF-8");
        command = command.trim();

        if(command.equalsIgnoreCase("yes")){
          //if the confirm is yes the server deletes the requested file and returns an ack to the client
          //to indicate the success or failure of file deletion
          //
          Boolean isSuccess = f.delete();
          String ack = "";
          if(isSuccess){
            ack = "File deleted successfully";
          }
          else{
            ack = "Deletion failed";
          }
          //send ack to client
          byte[] ackBytes = ack.getBytes("UTF-8");
          dataOut.write(ackBytes, 0, ackBytes.length);

        }
        else if(command.equalsIgnoreCase("no")){
          //if the confirm is no, the server reutnrs to "Wait for operatio from client" state
        }

      }
      else{
        // If the file does not exist server sends a negative confirm (integer value -1)
        int confirm = -1;
        ByteBuffer outBuffer = ByteBuffer.allocate(4);
        outBuffer.putInt(confirm);
        byte[] outArray = outBuffer.array();
        dataOut.write(outArray, 0, outArray.length);
        System.out.println("File does not exist. Returning to main menu...");
      }
      //method exits to main menu
    }catch(IOException e){
      e.printStackTrace();
    }
  }

  public static void main (String args[]){
    try{

      int port = 42000;
      ServerSocket serverSocket = new ServerSocket(port);
      System.out.println("Server started and listening to port: " + port);

      while(true){
        Boolean isTerminated = false;
        //ServerSocket.accept() outputs a socket object
        socket = serverSocket.accept();
        System.out.println("Client Connnected");

        //Create an InputStream for the Socket

        is = socket.getInputStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);

        OutputStream os = socket.getOutputStream();
        OutputStreamWriter osw = new OutputStreamWriter(os);
        BufferedWriter bw = new BufferedWriter(osw);

        String fromClient;

        while(true){
          fromClient = br.readLine();
          System.out.println("Client sent: " + fromClient);
          // Read command from client

          switch(fromClient){

            case "UPLD":
              //calling UPLOAD method
              System.out.println("Beginning procedure for uploading a file to the server...");
              uploadFromClient();

              break;

            case "LIST":
              //calling LIST method
              listFilesOnServer();
              break;

            case "DWLD":
              //calling DOWNLOAD method
              downloadToClient();
              break;

            case "DELF":
              //calling DELETE FILE method
              deleteFileOnServer();
              break;

            case "QUIT":
              //maybe put this in its own method?
              System.out.println("Client terminated connection");
              socket.close();
              isTerminated = true;
              break;

            default: //"bad command"
              System.out.println("Bad command");
              break;
          }//switch
          if(isTerminated){
            break;
          }
        }//while inner
      }//while outer
    }//try
      catch(IOException e){
        e.printStackTrace();
        }
  }//main
}
  /*
  Main
  Server opens port and goes into "wait for connection" state.
  -Client connects


  CONN
  The client sends a request to the server to connect to it. The server accepts the connection
  and waits for requests from the client.


  UPLD

  LIST

  DWLD

  DELF



  QUIT
1. Client sends operation (QUIT) to exit.
2. Client closes socket, and exits.
3. Server closes socket and goes back to "wait for operation from client" state.
4. Client informs user that session has been closed.

  */
