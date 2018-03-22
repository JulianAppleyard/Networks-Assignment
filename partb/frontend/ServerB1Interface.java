package partb.frontend;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.io.*;
import java.net.*;

public interface ServerB1Interface extends Remote {
  void uploadFromClient() throws SocketException, IOException, RemoteException;
  byte[] listFilesOnServer() throws SocketException, IOException, RemoteException;
  void downloadToClient() throws SocketException, IOException, RemoteException;
  void deleteFileOnServer() throws SocketException, IOException, RemoteException;

}
