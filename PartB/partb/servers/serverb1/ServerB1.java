package partb.servers.serverb1;


//import interface
import partb.servers.serverb1.ServerB1Interface;

//import ServerA stuff
import java.util.*;
import java.util.concurrent.*;
import java.io.*;
import java.nio.file.*;
import java.net.*;
import java.nio.ByteBuffer;

//Import java rmi stuff
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;



public class ServerB1 implements ServerB1Interface{


  //constructor
  public ServerB1(){}

  public static boolean uploadCheck;
  public static int frontPort;


  public static void main(String args[]){
    try{


      String host = "localhost";
      int port = 42000;
      int frontPort = 42001;
      //Create server object
      ServerB1 obj = new ServerB1();

      //create remote object stub from server object
      ServerB1Interface stub = (ServerB1Interface) UnicastRemoteObject.exportObject(obj, 0);

      //get Registry
      Registry registry = LocateRegistry.getRegistry(host, port);

      //Bind the remote object's stub in the Registry
      registry.bind("FTPB1", stub);

      //Write ready message to console
      System.err.println("FTPB1 server ready");

      while(true){
        TimeUnit.SECONDS.sleep(1);
        if(uploadCheck){
          ServerSocket serverSocket = new ServerSocket(frontPort);
          if(uploadCheck){
            uploadFromClient(serverSocket);
            uploadCheck = false;
            serverSocket.close();
          }

        }



      }//while




    } catch(Exception e){
      e.printStackTrace();
      System.out.println("FTB1 startup failed");

    }
  }//main



  /*
              *UPLOAD*
    This method is called when the client sends the UPLD command.
    It handles the process of uploading a file from the client's system to the server's "Storage" directory

  */
  public void startUploadSocket(){
    uploadCheck = true;

  }


  public static void uploadFromClient(ServerSocket serverSocket) {
    try{
      System.out.println("Uploading to ServerB1");
      Socket frontSocket = serverSocket.accept();
      InputStream frontInputStream = frontSocket.getInputStream();
      DataInputStream frontDataIn = new DataInputStream(frontInputStream);

      //first the frontend sends the length of the file name and the filename itself
      byte[] lengthName = new byte[1024];

      frontDataIn.read(lengthName);

      ByteBuffer buffer = ByteBuffer.wrap(lengthName);
      //decode length
      short lengthShort = buffer.getShort(0); //takes first two bytes

      //the rest of the string is the filename
      byte[] temp = new byte[buffer.remaining()];
      buffer.get(temp);

      String file_name = new String(temp, "UTF-8");
      file_name = file_name.trim();

      //send acknowledgement?


      //next set of data is the file filesize

      byte[] sizeArray = new byte[5];
      frontDataIn.read(sizeArray);
      ByteBuffer sizeBuff = ByteBuffer.wrap(sizeArray);
      int fileSize = sizeBuff.getInt(0);


      Path currentRelativePath = Paths.get("");
      String stringPath = currentRelativePath.toAbsolutePath().toString();
      File newFile = new File(stringPath + "\\partb\\servers\\serverb1\\Storage\\" + file_name);



      newFile.createNewFile();

      FileOutputStream fileOutputWriter = new FileOutputStream(newFile);

      //next set of data is the file itself

      /*
      The below for loop will iterate once for every kB
      There is one extra in case filesize is less than 1024 (in which case int division will give 0)
      The extra one also covers remaining bytes left over because of integer division
      */
      //System.out.println(fileSize);//debug
      int numOfIterations = (fileSize/1024)+1;

      for(int i=0; i<numOfIterations; i++){
        if(i == numOfIterations-1){
          int remaining = fileSize%1024;
          byte[] fileBytes = new byte[remaining];
          frontDataIn.readFully(fileBytes, 0, remaining);
          //System.out.println("B1 receieved last pieces");
          fileOutputWriter.write(fileBytes, 0, remaining);
        }else{
          byte[] fileBytes = new byte[1024];
          frontDataIn.read(fileBytes, 0, 1024);
          fileOutputWriter.write(fileBytes, 0, 1024);
        }
      }
      System.out.println("File written to ServerB1's storage");
      uploadCheck = false;
      frontSocket.close();
    }catch(Exception e){
      e.printStackTrace();
    }
  }//UPLD



/*
            *LIST*

*/


  public byte[] listFilesOnServer() throws SocketException, IOException{
      //Stream management
      System.out.println("Listing files on ServerB1");

      ByteArrayOutputStream baos = new ByteArrayOutputStream();

      //Client sends operation (LIST) to list directories/files at the server
      //Server obtains listing of its directories/files
      //it is sufficient to only list the names of files/directories

      Path currentRelativePath = Paths.get("");
      String stringPath = currentRelativePath.toAbsolutePath().toString();

      File directory = new File(stringPath + "\\partb\\servers\\serverb1\\Storage\\");
      String[] listOfFileNames = directory.list();



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

      //Server encodes data as length + "/" + long String of filenames
      //  return Array
      return listingArray;

  }


/**
            *DWLD*

**/





  public void downloadToClient() throws SocketException, IOException{


  }//DWLD



/**
            *DELF*

*   This method handles the DELF function for this server.
*   The frontend checks if the file exists on this server before requesting to delete it from here.
*   Therefore, this method need only take the filename and proceed with the deletion process.

**/

  public void deleteFileOnServer(String file_name) throws SocketException, IOException {

    //construct a path to where the file is
    Path currentRelativePath = Paths.get("");
    String stringPath = currentRelativePath.toAbsolutePath().toString();
    stringPath = stringPath + "\\partb\\servers\\serverb1\\Storage\\" + file_name;

    File fileObject = new File(stringPath);

    Boolean isSuccess = fileObject.delete();
    while(!isSuccess){
      isSuccess = fileObject.delete();
    }

  }//DELF

}//class
