import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ServerInterface extends Remote {
  /**
   * Put a key-value pair in this server
   * @param key
   * @param value
   * @throws RemoteException
   */
  void put(String key, String value) throws RemoteException;

  /**
   * Get a value at this server by key
   * @param key
   * @return value associated with the key
   * @throws RemoteException
   */
  String get(String key) throws RemoteException;

  /**
   * A client's action attempt to write to an object.
   * Do not actually put, but check/initial write lock
   * @param transactionId Transaction/Client ID for the attempt
   * @param key Key of the write attempt
   * @return "Success", "Fail" or "Abort"
   * @throws RemoteException
   */
  String tentativePut(String transactionId, String key) throws RemoteException;
}
