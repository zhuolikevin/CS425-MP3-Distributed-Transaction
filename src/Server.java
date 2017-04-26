import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Scanner;

public class Server extends UnicastRemoteObject implements ServerInterface {
  private String name;
  private HashMap<String, ServerObject> storage;

  Server(String name) throws RemoteException {
    this.name = name;
    this.storage = new HashMap<>();

    Registry registry;
    registry = LocateRegistry.createRegistry(9936 + (int) name.charAt(0));
    registry.rebind(this.name, this);

    System.out.println("Ready!");

    userConsole();
  }

  @Override
  public void put(String key, String value) throws RemoteException {
    this.storage.put(key, new ServerObject(value));
  }

  @Override
  public String get(String key) throws RemoteException {
    if (storage.containsKey(key)) {
      return this.storage.get(key).getValue();
    } else {
      return null;
    }
  }

  private void userConsole() {
    Scanner scan = new Scanner(System.in);
    String input = scan.nextLine();
    while(!input.equals("EXIT")) {
      String[] inputs = input.split(" ");
      switch (inputs[0]) {
        case "ME":
          try {
            System.out.println(name + "<" + InetAddress.getLocalHost().getHostAddress() + ":" + (9936 + (int) name.charAt(0)) + ">");
          } catch (UnknownHostException e) {
            e.printStackTrace();
          }
          break;
        case "STORAGE":
          boolean showLocks = inputs.length > 1;
          for (String key : storage.keySet()) {
            ServerObject valueObj = storage.get(key);
            System.out.print(key + " : " + valueObj.getValue());
            if (showLocks) {
              System.out.print(" <R-" + valueObj.getReadLock() + ", W-" + valueObj.getWriteLock() + ">");
            }
            System.out.print("\n");
          }
          System.out.println("[END] Total Objects: " + storage.keySet().size());
          break;
        default:
          System.err.println("Invalid command");
      }
      input = scan.nextLine();
    }
    scan.close();
    System.exit(0);
  }

  public static void main(String[] args) {
    if (args.length == 1) {
      String name = args[0];
      try {
        new Server(name);
      } catch (RemoteException e) {
        e.printStackTrace();
      }
    } else {
      System.err.println("Incorrect arguments!");
      System.err.println("Expected arguments: [server name]");
      System.exit(0);
    }
  }
}
