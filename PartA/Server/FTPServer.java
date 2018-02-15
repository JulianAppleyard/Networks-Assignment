/**
* The server-side code of a simple FTP application. The file transfer itself
* should take place using TCP with the client.
* Author: Julian Appleyard
* Version: 0.0.1
* see FTPclient for version notes
**/

import java.util.*;
import java.io.*;
import java.net.*;

public class FTPServer {
  private static Socket socket;

/*
This method handles the UPLD function.
The client sends the operation "UPLD" to upload a local file to the server.
PSUEDO-CODE stage
*/
/*
  public static void uploadFromClient(short fileLength){
    //Client sends the length of the file name which will be sent (short int)

    // Followed by the file_name (character string)
    String file_name =

    //send acknowledgement that the server is ready to receive

    //Client sends the size of the file, which maay be a 32 bit value sent in bytes
  }

*/
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

        InputStream is = socket.getInputStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);

        OutputStream os = socket.getOutputStream();
        OutputStreamWriter osw = new OutputStreamWriter(os);
        BufferedWriter bw = new BufferedWriter(osw);
        bw.write("Send valid command. Type HELP to list commands");
        bw.flush();
        String fromClient;

        while(true){
          fromClient = br.readLine();
          System.out.println("Client sent: " + fromClient);
          // Read command from client

          switch(fromClient){

            case "UPLD":
              //calling UPLOAD method
              //uploadFromClient(short fileLength);

              break;

            case: "LIST":
              //calling LIST method
              break;

            case "DWLD":
              //calling DOWNLOAD method

              break;

            case: "DELF":
              //calling DELETE FILE method

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
