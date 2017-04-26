import java.io.BufferedReader;
import java.io.FileReader;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class Client {
  private String transactionId;
  private boolean transactionFlag;
  private HashMap<String, ServerInterface> serverInterfaceHashMap;
  private HashMap<String, HashMap<String, String>> tentativeStorage;

  private static final String RES_PREFIX = "../res/";

  Client(ArrayList<String> addressList, String transactionId) {
    this.transactionId = transactionId;
    this.serverInterfaceHashMap = new HashMap<>();
    this.transactionFlag = false;
    this.tentativeStorage = new HashMap<>();

    // Set up connections with servers
    for (int i = 0; i < addressList.size(); i++) {
      String remoteIp = addressList.get(i).split(" ")[0];

      int remotePort = Integer.parseInt(addressList.get(i).split(" ")[1]);
      String remoteName = Character.toString((char) (remotePort - 9936));

      try {
        Registry registry = LocateRegistry.getRegistry(remoteIp, remotePort);
        ServerInterface remoteServer = (ServerInterface) registry.lookup(remoteName);

        this.serverInterfaceHashMap.put(remoteName, remoteServer);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    System.out.println("Ready!");

    // User input
    userConsole();
  }

  private void commitTransaction() {
    // TODO
  }

  private void abortTransaction() {
    // TODO
  }

  private void userConsole() {
    Scanner scan = new Scanner(System.in);
    String input = scan.nextLine();
    while(!input.equals("EXIT")) {
      String[] inputs = input.split(" ");
      try {
        switch (inputs[0]) {
          case "SERVERS":
            for (String serverName : serverInterfaceHashMap.keySet()) {
              System.out.println(serverName);
            }
            System.out.println("[END] Total Servers: " + serverInterfaceHashMap.keySet().size());
            break;
          case "BEGIN":
            transactionFlag = true;
            System.out.println("OK");
            break;
          case "SET":
            if (!transactionFlag) {
              System.err.println("Please BEGIN before SET");
            } else if (inputs.length != 3) {
              System.err.println("Invalid command");
            } else {
              String serverKey = inputs[1];
              String value = inputs[2];
              String serverName = serverKey.split("\\.")[0];
              String key = serverKey.split("\\.")[1];
              if (!serverInterfaceHashMap.containsKey(serverName)) {
                System.err.println("Server [" + serverName + "] doesn't exist");
              } else {
                ServerInterface targetServer = serverInterfaceHashMap.get(serverName);
                // Save tentative result locally
                HashMap<String, String> fakeServerStorage = tentativeStorage.get(serverName);
                if (fakeServerStorage != null) {
                  fakeServerStorage.put(key, value);
                } else {
                  fakeServerStorage = new HashMap<>();
                  fakeServerStorage.put(key, value);
                  tentativeStorage.put(serverName, fakeServerStorage);
                }
                // Make write attempts until "Success"
                String successFlag = targetServer.tentativePut(transactionId, key);
                while (successFlag.equals("Fail")) {
                  try {
                    Thread.sleep(500);
                  } catch (InterruptedException e) {
                    e.printStackTrace();
                  }
                }
                if (successFlag.equals("Abort")) {
                  abortTransaction();
                } else {
                  System.out.println("OK");
                }
              }
            }
            break;
          case "GET":
            if (!transactionFlag) {
              System.err.println("Please BEGIN before GET");
            } else if (inputs.length != 2) {
              System.err.println("Invalid command");
            } else {
              String serverKey = inputs[1];
              String serverName = serverKey.split("\\.")[0];
              String key = serverKey.split("\\.")[1];
              if (!serverInterfaceHashMap.containsKey(serverName)) {
                System.err.println("Server [" + serverName + "] doesn't exist");
              } else {
                ServerInterface targetServer = serverInterfaceHashMap.get(serverName);
                String value = targetServer.get(key);
                if (value == null) {
                  System.out.println("NOT FOUND");
                } else {
                  System.out.println(serverName + "." + key + " = " + value);
                }
              }
            }
            break;
          case "COMMIT":
            transactionFlag = false;
            commitTransaction();
            break;
          case "ABORT":
            transactionFlag = false;
            abortTransaction();
            break;
          default:
            System.err.println("Invalid command");
        }
      } catch (RemoteException e) {
        e.printStackTrace();
      }
      input = scan.nextLine();
    }
    scan.close();
    System.exit(0);
  }

  public static void main(String[] args) {
    if (args.length == 1) {
      String transactionId = args[0];
      try {
        // Read address book from file to an ArrayList
        BufferedReader br = new BufferedReader(new FileReader(RES_PREFIX + "address_server_local.txt"));
        String line = br.readLine();
        ArrayList<String> addressList = new ArrayList<>();
        while (line != null) {
          addressList.add(line);
          line = br.readLine();
        }
        br.close();

        // Initialize the client
        new Client(addressList, transactionId);
      } catch (Exception e) {
        e.printStackTrace();
        System.exit(0);
      }
    } else {
      System.err.println("Incorrect arguments!");
      System.err.println("Expected arguments: [server name]");
      System.exit(0);
    }
  }
}
