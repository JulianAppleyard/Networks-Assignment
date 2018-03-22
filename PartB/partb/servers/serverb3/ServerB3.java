package partb.servers.serverb3;


//import interface
import partb.servers.serverb3.ServerB3Interface;

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



public class ServerB3 implements ServerB3Interface{


  //constructor
  public ServerB3(){}

  public static void main(String args[]){
    try{


      String host = "localhost";
      int port = 42000;
      //Create server object
      ServerB3 obj = new ServerB3();

      //create remote object stub from server object
      ServerB3Interface stub = (ServerB3Interface) UnicastRemoteObject.exportObject(obj, 0);

      //get Registry
      Registry registry = LocateRegistry.getRegistry(host, port);

      //Bind the remote object's stub in the Registry
      registry.bind("FTPB3", stub);

      //Write ready message to console
      System.err.println("FTPB3 server ready");
    } catch(Exception e){
      e.printStackTrace();
      System.out.println("FTB3 startup failed");
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
      System.out.println("producing list of files on ServerB3");//debug

      System.out.println("Client requested list");
      ByteArrayOutputStream baos = new ByteArrayOutputStream();

      //Client sends operation (LIST) to list directories/files at the server
      //Server obtains listing of its directories/files
      //it is sufficient to only list the names of files/directories

      Path currentRelativePath = Paths.get("");
      String stringPath = currentRelativePath.toAbsolutePath().toString();

      File directory = new File(stringPath + "\\partb\\servers\\serverb3\\Storage\\");
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
    stringPath = stringPath + "\\partb\\servers\\serverb3\\Storage\\" + file_name;

    File fileObject = new File(stringPath);

    Boolean isSuccees = fileObject.delete();
  }//DELF

}//class
