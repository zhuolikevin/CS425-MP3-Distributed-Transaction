import java.io.BufferedReader;
import java.io.FileReader;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class Client {
  private HashMap<String, ServerInterface> serverInterfaceHashMap;

  private static final String RES_PREFIX = "../res/";

  Client(ArrayList<String> addressList) {
    this.serverInterfaceHashMap = new HashMap<>();

    // Set up connections with servers
    for (int i = 0; i < addressList.size(); i++) {
//      String remoteIp = addressList.get(i).split(" ")[0];

      String remoteIp = "localhost";

      int remotePort = Integer.parseInt(addressList.get(i).split(" ")[1]);
      String remoteName = Character.toString((char) (remotePort - 9936));

      System.out.println(remoteName + "<" + remoteIp + ":" + remotePort + ">");
      try {
        Registry registry = LocateRegistry.getRegistry(remoteIp, remotePort);
        ServerInterface remoteServer = (ServerInterface) registry.lookup(remoteName);

        System.out.println(remoteName + "<" + remoteIp + ":" + remotePort + ">");

        this.serverInterfaceHashMap.put(remoteName, remoteServer);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    // User input
    userConsole();
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
            break;
          case "SET":
            if (inputs.length != 3) {
              System.err.println("Invalid command");
            } else {
              String serverKey = inputs[1];
              String value = inputs[2];
              String serverName = serverKey.split(".")[0];
              String key = serverKey.split(".")[1];
              if (!serverInterfaceHashMap.containsKey(key)) {
                System.err.println("Server [" + serverName + "] doesn't exist");
              } else {
                ServerInterface targetServer = serverInterfaceHashMap.get(serverName);
                targetServer.put(key, value);
                System.out.println("OK");
              }
            }
            break;
          case "GET":
            if (inputs.length != 2) {
              System.err.println("Invalid command");
            } else {
              String serverKey = inputs[1];
              String serverName = serverKey.split(".")[0];
              String key = serverKey.split(".")[1];
              if (!serverInterfaceHashMap.containsKey(key)) {
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
      new Client(addressList);
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(0);
    }
  }
}
