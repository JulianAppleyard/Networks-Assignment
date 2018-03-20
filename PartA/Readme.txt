
I was not able to run my program on university machines first.

I first run the server program by running the command
java ServerA
in a bash (in the "Server" directory)
Then run the client program:
java ClientA
in a bash command line (in the "Client" directory)

The client will automatically connect to the server and will then prompt you to send a message to the server.
The client will accept the following commands (CASE SENSATIVE)

HELP (list valid commands)

LIST (list all files within the Server's storage directory

UPLD (upload a file from the client to the server)
-after typing UPLD, you will be prompted to enter the path to the file
-enter the full path and press enter to use the client to transfer a file to the "Storage" subdirectory in the server

DWLD (download a file from the server's "Storage" directory to the client's "Storage" subdirectory

DELF (delete a file from the server's storage)

QUIT (close the connection and exit the client program)