package partb.servers.serverb1;


//import interface
import partb.frontend.ServerB1Interface;

//import ServerA stuff
import java.util.*;
import java.io.*;
import java.nio.file.*;
import java.net.*;
import java.nio.ByteBuffer;
import static java.lang.Math.toIntExact;

//Import java rmi stuff
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;



public class ServerB1 implements ServerB1Interface{


  //constructor
  public ServerB1(){}

  public static void main(String args[]){
    try{


      String host = "localhost";
      int port = 42000;
      //Create server object
      ServerB1 obj = new ServerB1();

      //create remote object stub from server object
      ServerB1Interface stub = (ServerB1Interface) UnicastRemoteObject.exportObject(obj, 0);

      //get Registry
      LocateRegistry.createRegistry(port);
      Registry registry = LocateRegistry.getRegistry(host, port);

      //Bind the remote object's stub in the Registry
      registry.bind("FTPB1", stub);

      //Write ready message to console
      System.err.println("FTPB1 server ready");
    } catch(Exception e){
      e.printStackTrace();
    }
  }//main

  /*
              *UPLOAD*
    This method is called when the client sends the UPLD command.
    It handles the process of uploading a file from the client's system to the server's "Storage" directory

  */

  public void uploadFromClient() throws SocketException, IOException{


  }//UPLD



/*
            *LIST*

*/


  public byte[] listFilesOnServer() throws SocketException, IOException{
      //Stream management
      System.out.println("Client requested list");
      ByteArrayOutputStream baos = new ByteArrayOutputStream();

      //Client sends operation (LIST) to list directories/files at the server
      //Server obtains listing of its directories/files
      //it is sufficient to only list the names of files/directories
      Path currentRelativePath = Paths.get("");
      String stringPath = currentRelativePath.toAbsolutePath().toString();
    //  System.out.println(stringPath); //debug
      File directory = new File(stringPath + "\\partb\\servers\\serverb1\\Storage\\");
      String[] listOfFileNames = directory.list();
      //System.out.println(listOfFileNames.length);//debug


      //loop through the list of file names to find the size of the listing
      byte[] seperatorArray = "/".getBytes("UTF-8");

      int size = 0;
      for(int i =0; i<listOfFileNames.length; i++){
        //
        int lengthOfCurrentString = listOfFileNames[i].length();
        size = size + lengthOfCurrentString;

        //convert each filename into a byte array (UTF-8 encoded)
        //and write to the ByteArrayOutputStream
        byte[] nameArray = listOfFileNames[i].getBytes("UTF-8");
        baos.write(nameArray);
        baos.write(seperatorArray);
      }

      //ByteArrayOutputStream baos will contain all filenames concatenated
      //but separated with a "/" to allow for the client to parse the filenames
      //I chose this character because it is not allowed in filenames in UNIX or Windows
      //

      byte[] listingArray = baos.toByteArray();
      baos.reset();




    //  System.out.println("debug size of directory listing: " + size);

      //convert size of listing (32 bit int) to byte array for sending to client

      ByteBuffer listBuffer = ByteBuffer.allocate(4);
      listBuffer.putInt(size);
      byte[] lengthArray = listBuffer.array();


      //Server encodes data as length + "/" + long String of filenames
      baos.write(lengthArray);
      baos.write(listingArray);


      byte [] fullArray = baos.toByteArray();

      return fullArray;


  }

  public void downloadToClient() throws SocketException, IOException{

    InputStream downloadInputStream = socket.getInputStream();
    DataInputStream dataIn = new DataInputStream(downloadInputStream);

    OutputStream os = socket.getOutputStream();
    DataOutputStream dataOut = new DataOutputStream(os);
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

  }//DWLD



/*
            *DELF*
This method handles the DELF function
The client sends the file name the server deletes file if it exists
just an outline for now
*/

  public static void deleteFileOnServer() throws SocketException, IOException {
//    try{
      OutputStream os = socket.getOutputStream();
      DataOutputStream dataOut = new DataOutputStream(os);

      InputStream delfInputStream = socket.getInputStream();
      DataInputStream dataIn = new DataInputStream(delfInputStream);
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
          //if the confirm is no, the server reutnrs to "Wait for operation from client" state
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
  }//DWLD

  public void deleteFileOnServer() throws SocketException, IOException{

  }//DELF

}//class
