/**
* The client-side code of a simple FTP application.
* Author: Julian Appleyard
* Version: 0.0.1
Version notes:
* Basic outline of client's main method. eg which commands are valid.
*
* Planned structure is that each command will call a method that mirrors a method executed on the server.
* The server and client methods will be structured in way that they wait for each other's responses at the appropriate time
* so that they do not become out of sync.
**/

import java.io.*;
import java.net.*;
import java.util.Scanner;
public class FTPClient{

  private static Socket socket;

  public static void main(String [] args){
    String host = "localhost";
    int port = 42000;

    try{

      System.out.println("Connecting to " + host + " on port " + port);
      InetAddress address = InetAddress.getByName(host);

      socket = new Socket(host, port);

      System.out.println("Connected");

      OutputStream outToServer = socket.getOutputStream();
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
        // System.out.println(textToServer + " sent to server");
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
            //call upload file method
            break;

          case "LIST":
            System.out.println("Beginning procedure for listing files on the server...");
            //call list files method
            break;

          case "DWLD":
            System.out.println("Beginning procedure for downloading a file from the server...");
            //call method to download a file from the server
            break;

          case "DELF":
            System.out.println("Beginning procedure for deleting a file from the server...");
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
