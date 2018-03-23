#!/bin/bash


rmiregistry 42000 &

#run java commands from the higher directory PartB (not inside the java package partb)

#compile interfaces
javac partb/servers/serverb1/ServerB1Interface.java
javac partb/servers/serverb2/ServerB2Interface.java
javac partb/servers/serverb3/ServerB3Interface.java
javac partb/frontend/MainInterface.java

#compile servers
javac partb/servers/serverb1/ServerB1.java
javac partb/servers/serverb2/ServerB2.java
javac partb/servers/serverb3/ServerB3.java

#compile frontend and client
javac partb/frontend/FrontEnd.java
javac partb/client/ClientB.java

#start all three servers
java partb.servers.serverb1.ServerB1 &
java partb.servers.serverb2.ServerB2 &
java partb.servers.serverb3.ServerB3 &
