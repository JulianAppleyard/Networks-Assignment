package partb.servers.serverb2;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.io.*;
import java.net.*;

public interface ServerB2Interface extends Remote {
  void startUploadSocket() throws RemoteException;
  byte[] listFilesOnServer() throws SocketException, IOException, RemoteException;
  void downloadToClient() throws SocketException, IOException, RemoteException;
  void deleteFileOnServer(String file_name) throws SocketException, IOException, RemoteException;

}
