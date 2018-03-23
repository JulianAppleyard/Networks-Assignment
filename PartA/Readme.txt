
For these to work correctly, they must be able to obtain a full path.
When trying to run these through mira, NullPointerException were thrown in relation to obtaining a path
to the "Storage" subdirectories.


In PartA/Server/ start a Bash shell
Compile: javac ServerA.java
Run : java ServerA
Leave this shell open.

In PartA/Client/ start a new Bash shell
Compile: javac ClientA.java
run: java ClientA

Client will automatically connect to the server and will then prompt you to type a command.

Client will accept the following commands (CASE SENSITIVE):

HELP (list commands)

LIST (list files within the Server's "storage")

UPLD (upload a file from the client to the server)
-enter the FULL case-sensitive path and press enter to use the client to transfer a file to Server/Storage

DWLD (download a file from the Server/Storage to Client/Storage
-then enter the case sensitive filename on the server that you wish to transfer to the client

DELF (delete a file from the Server/Storage)
-enter the case-sensitive name of the file to delete
-after entering the filename, you will be asked to confirm deletion of the file

Note: I have had the method File.delete() fail to delete the file for some reason.
If this happens, restart the server.



QUIT (close the connection and exit the client program)