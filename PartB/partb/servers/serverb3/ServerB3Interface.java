package partb.servers.serverb3;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.io.*;
import java.net.*;

public interface ServerB3Interface extends Remote {
  void uploadFromClient() throws SocketException, IOException, RemoteException;
  byte[] listFilesOnServer() throws SocketException, IOException, RemoteException;
  void downloadToClient() throws SocketException, IOException, RemoteException;
  void deleteFileOnServer(String file_name) throws SocketException, IOException, RemoteException;

}
