/**
* The server-side code of a simple FTP application.
* Author: Julian Appleyard
* Version: 0.0
**/

import java.io.*;
import java.net.*;

class Server{

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
}
