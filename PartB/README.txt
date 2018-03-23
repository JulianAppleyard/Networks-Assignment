I did not get a chance to test this system on the University computers. However, when trying to 
run PartA files through mira, NullPointerExceptions were thrown in relation to obtaining a path
to the "Storage" subdirectories.
These files use the same methods to obtain paths so will have the same issue.
I think for these to work correctly they must be able to obtain a full path.

The startupscript.sh includes statement to compile all *.java files for PartB, create an rmi registry at the correct port
and also includes commands to start all three servers at once.

The way I usually ran this program:
Open three bash command lines in /PartB/

In the first: sh startupscript.sh
(The script may take a few seconds to run, so wait for the FTPBX ready messages before proceeding)
In the second: java partb.frontend.FrontEnd
In the third: java partb.client.ClientB

Enter one of the following (case-sensitive) commands in the client:

HELP: display list of valid commands

UPLD: upload a file to the system
-when prompted, enter the FULL path to the file you wish to upload

LIST: list the files on the system

DWLD: not attempted

DELF: delete a file from the system
-enter the case-sensitive name of the file you want to delete

QUIT: terminates the connection to the system and closes the client

