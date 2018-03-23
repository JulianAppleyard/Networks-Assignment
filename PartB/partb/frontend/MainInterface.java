package partb.frontend;


import java.rmi.Remote;
import java.rmi.RemoteException;
import java.io.*;
import java.net.*;

public interface MainInterface extends Remote {
  void startUpload() throws RemoteException;
  String[] getMasterListing() throws SocketException, IOException, RemoteException;
  void downloadToClient() throws SocketException, IOException, RemoteException;
  void deleteFileOnServer(String file_name) throws SocketException, IOException, RemoteException;

}
