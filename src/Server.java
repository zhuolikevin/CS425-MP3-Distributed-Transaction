import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;

public class Server extends UnicastRemoteObject implements ServerInterface {
	
  private String name;
  private HashMap<String, ServerObject> storage;
  private static final String RES_PREFIX = "../res/";
  private CoordinatorInterface coordinator;
  
  Server(ArrayList<String> addressListCoordinator, String name) throws RemoteException {
    this.name = name;
    this.storage = new HashMap<>();

    // Register self, bind to specific port
    Registry registry;
    registry = LocateRegistry.createRegistry(9936 + (int) name.charAt(0));
    registry.rebind(this.name, this);

    System.err.println("Server Ready!");

    // Wait for coordinator ready
    BufferedReader keyboardInput = new BufferedReader(new InputStreamReader(System.in));
    boolean coordinatorReady = false;
    while (!coordinatorReady) {
    	System.err.print("Coordinator ready? (y/n)\n>> ");
    	try {
			  coordinatorReady = "y".equals(keyboardInput.readLine());
		  } catch (IOException e) {
			  e.printStackTrace();
		  }
    }

    // Connect with coordinator
    String remoteIp = addressListCoordinator.get(0).split(" ")[0];
    int remotePort = Integer.parseInt(addressListCoordinator.get(0).split(" ")[1]);
    String remoteName = Character.toString((char) (remotePort - 9936));

    try {
      Registry registryCoordinator = LocateRegistry.getRegistry(remoteIp, remotePort);
      this.coordinator = (CoordinatorInterface) registryCoordinator.lookup(remoteName);;
    } catch (Exception e) {
      e.printStackTrace();
    }
    
    userConsole();
  }
  
  @Override 
  public CoordinatorInterface getCoordinator() throws RemoteException {
	  return this.coordinator;
  }

  @Override
  public void put(String key, String value) throws RemoteException { 
    if (storage.containsKey(key)) {
      storage.get(key).setValue(value);
    } else {
      storage.put(key, new ServerObject(value));
    }
  }

  @Override
  public String get(String key) throws RemoteException {
    if (storage.containsKey(key)) {
      return storage.get(key).getValue();
    } else {
      return null;
    }
  }

  @Override
  public String tryPut(String transactionId, String key) throws RemoteException {
	  // If coordinator says this transaction should be aborted, tell the client to abort
    HashSet<String> abortingIdSet = getCoordinator().getAbortingTransactionSet();
    if (abortingIdSet.contains(transactionId)) {
      return "ABORT";
    }

    // If this key is not set yet, the client can continue
    if (!storage.containsKey(key)) {
      return "SUCCESS";
    }
    
    ServerObject targetObj = storage.get(key);

    // Current lock owners despite self
    HashSet<String> lockOwners = new HashSet<>(targetObj.readLockOwner);
    if (targetObj.writeLockOwner != null) lockOwners.add(targetObj.writeLockOwner);
    if (lockOwners.contains(transactionId)) lockOwners.remove(transactionId);
    	
    if (!targetObj.getReadLock() && !targetObj.getWriteLock()) {
      // Nobody is using the object
      targetObj.setWriteLock(true);
      targetObj.writeLockOwner = transactionId;
      return "SUCCESS";
    } else if (targetObj.getReadLock() && !targetObj.getWriteLock()) {
      // Only readLock is occupied
      if (targetObj.readLockOwner.size() == 1 && targetObj.readLockOwner.contains(transactionId)) {
        // The only user of the readLock is this transaction, we can promote
        targetObj.setWriteLock(true);
        targetObj.writeLockOwner = transactionId;
        targetObj.setReadLock(false);
        targetObj.readLockOwner.remove(transactionId);
        return "SUCCESS";
      } else {
        // Share the readLock with somebody else or occupied by others, we cannot promote
      	coordinator.addEdge(transactionId, lockOwners);
        return "FAIL";
      }
    } else if (targetObj.writeLockOwner != null && targetObj.writeLockOwner.equals(transactionId)) {
      // Write lock is set by this transaction, we can continue to use
      return "SUCCESS";
    } else {
      // All other cases we can not continue
      coordinator.addEdge(transactionId, lockOwners);
      return "FAIL";
    }
  }

  @Override
  public String tryGet(String transactionId, String key) throws RemoteException {
    // If coordinator says this transaction should be aborted, tell the client to abort
    HashSet<String> abortingIdSet = getCoordinator().getAbortingTransactionSet();
    if (abortingIdSet.contains(transactionId)) {
      return "ABORT";
    }
    // If this key is not set yet, can success but actual get will get NOT FOUND
    if (!storage.containsKey(key)) {
      return "SUCCESS";
    }
    ServerObject targetObj = storage.get(key);
    
    if (!targetObj.getWriteLock()) {
      // Nobody is using the writeLock, then we can read (No matter if anyone else is also reading)
      targetObj.setReadLock(true);
      targetObj.readLockOwner.add(transactionId);
      return "SUCCESS";
    } else {
      HashSet<String> lockOwners = new HashSet<>();
      lockOwners.add(targetObj.writeLockOwner);
      coordinator.addEdge(transactionId, lockOwners);
      // Somebody is writing, we can not read
      return "FAIL";
    }
  }

  @Override
  public void releaseLocks(String transactionId) throws RemoteException {
    for (String key : storage.keySet()) {
      ServerObject curObj = storage.get(key);

      // Reset writeLock
      if (curObj.writeLockOwner != null && curObj.writeLockOwner.equals(transactionId)) {
        curObj.writeLockOwner = null;
        curObj.setWriteLock(false);
      }
      // Reset readLock
      if (curObj.readLockOwner.contains(transactionId)) {
        curObj.readLockOwner.remove(transactionId);
        if (curObj.readLockOwner.size() == 0) {
          curObj.setReadLock(false);
        }
      }
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
            System.err.println(name + "<" + InetAddress.getLocalHost().getHostAddress() + ":" + (9936 + (int) name.charAt(0)) + ">");
          } catch (UnknownHostException e) {
            e.printStackTrace();
          }
          break;
        case "STORAGE":
          boolean showLocks = inputs.length > 1;
          for (String key : storage.keySet()) {
            ServerObject valueObj = storage.get(key);
            System.err.print(key + " : " + valueObj.getValue());
            if (showLocks) {
              System.err.print(" <R-" + valueObj.getReadLock() + ", ");
              String[] transactionIds = valueObj.readLockOwner.toArray(new String[valueObj.readLockOwner.size()]);
              System.err.print(String.join(",", transactionIds));
              System.err.print("><W-" + valueObj.getWriteLock() + ", " + valueObj.writeLockOwner + ">");
            }
            System.err.print("\n");
          }
          System.err.println("[END] Total Objects: " + storage.keySet().size());
          break;
        default:
          System.err.println("Invalid command");
      }
      input = scan.nextLine();
    }
    scan.close();
    System.exit(0);
  }

  public static void main(String[] args) throws IOException {
    if (args.length == 1) {
      String name = args[0];
      try {
          BufferedReader br = new BufferedReader(new FileReader(RES_PREFIX + "address_coordinator.txt"));
          String line = br.readLine();
          ArrayList<String> addressList_coordinator = new ArrayList<>();
          while (line != null) {
        	  addressList_coordinator.add(line);
        	  line = br.readLine();
          }
          br.close();
          new Server(addressList_coordinator, name);
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
