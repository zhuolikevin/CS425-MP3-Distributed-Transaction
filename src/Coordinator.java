import java.io.BufferedReader;
import java.io.FileReader;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

import org.jgraph.graph.DefaultEdge;
import org.jgrapht.graph.DefaultDirectedGraph;

public class Coordinator extends UnicastRemoteObject implements CoordinatorInterface {
	private static final long serialVersionUID = 1L;
  private static final String RES_PREFIX = "../res/";

	private String name;
	private List<ServerInterface> serverInterfaceList;
	private HashMap<String, Long> transactionTimeMap;
	private HashSet<String> abortingTransactions;
	private DefaultDirectedGraph<String, DefaultEdge> graph;

  Coordinator(ArrayList<String> addressList, String name) throws RemoteException, UnknownHostException {
	  this.name = name;
	  this.serverInterfaceList = new ArrayList<>();
	  this.transactionTimeMap = new HashMap<>();
	  this.abortingTransactions = new HashSet<>();
	  this.graph = new DefaultDirectedGraph<>(DefaultEdge.class);

    // The name of the coordinator can be set to F
	  Registry registry;
	  registry = LocateRegistry.createRegistry(9936 + (int) name.charAt(0));
	  registry.rebind(this.name, this);

	  System.err.println("Coordinator Ready!");
	  
	  // Set up connections with servers
    for (int i = 0; i < addressList.size(); i++) {
      String remoteIp = addressList.get(i).split(" ")[0];

      int remotePort = Integer.parseInt(addressList.get(i).split(" ")[1]);
      String remoteName = Character.toString((char) (remotePort - 9936));

      try {
        Registry registry_server = LocateRegistry.getRegistry(remoteIp, remotePort);
        ServerInterface remoteServer = (ServerInterface) registry_server.lookup(remoteName);

        this.serverInterfaceList.add(remoteServer);
        
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    // Set up deadlock detector
    Timer detectorTimer = new Timer(true);
    DeadlockDetector detector = new DeadlockDetector(this);
    detectorTimer.schedule(detector, 0, 500);

    userConsole();
  }

  @Override 
  public void addEdge(String transactionId, HashSet<String> lockOwners) throws RemoteException {
	  graph.addVertex(transactionId);
	  for (String lockOwnerId : lockOwners) {
      graph.addVertex(lockOwnerId);
      graph.addEdge(transactionId, lockOwnerId);
	  }
  }

  @Override
  public DefaultDirectedGraph<String, DefaultEdge> getGraph() throws RemoteException {
    return graph;
  }
  
  @Override
  public void putIntoTransactionTimeMap (String transactionId, long TimeStamp) throws RemoteException {
	  transactionTimeMap.put(transactionId, TimeStamp);
  }
  
  @Override
  public void removeFromTransactionTimeMap (String transactionId) throws RemoteException {
	  transactionTimeMap.remove(transactionId);
  }

  @Override
  public HashMap<String, Long> getTransactionTimeMap() throws RemoteException {
	  return transactionTimeMap;
  }
  
  @Override
  public HashSet<String> getAbortingTransactionSet() throws RemoteException {
	  return abortingTransactions;
  }
  
  @Override 
  public void removeFromGraph(String transactionId) throws RemoteException {
	  graph.removeVertex(transactionId);
  }
  
  @Override
  public void removeFromAbortingTransactionSet(String transactionId) throws RemoteException {
	  abortingTransactions.remove(transactionId);
  }
  
  @Override
  public boolean containsVertex(String transactionId) throws RemoteException {
	  return graph.containsVertex(transactionId);
  }

  @Override
  public void addAbortingTransaction(String transactionId) throws RemoteException {
    abortingTransactions.add(transactionId);
  }

  private void userConsole() throws UnknownHostException {
	    Scanner scan = new Scanner(System.in);
	    String input = scan.nextLine();
	    while(!input.equals("EXIT")) {
	      String[] inputs = input.split(" ");
	      switch (inputs[0]) {
	        case "GRAPH":
            String[] vertexes = graph.vertexSet().toArray(new String[graph.vertexSet().size()]);
            System.err.println(String.join(",", vertexes));
            for (DefaultEdge edge : graph.edgeSet()) {
              System.err.println(graph.getEdgeSource(edge) + "->" + graph.getEdgeTarget(edge));
            }
	          break;
	        case "IDTIME":
	          for (String key : transactionTimeMap.keySet()) {
	            long timeStamp = transactionTimeMap.get(key);
	            System.err.println(key + " : " + timeStamp);
	          }
	          System.err.println("[END] Total Transactions: " + transactionTimeMap.keySet().size());
	          break;
	        case "IDABORT":
	        	for (String id : abortingTransactions) {
	        		System.err.println(id);
	        	}
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
        try{
        BufferedReader br = new BufferedReader(new FileReader(RES_PREFIX + "address_server_local.txt"));
        String line = br.readLine();
        ArrayList<String> addressList = new ArrayList<>();
        while (line != null) {
          addressList.add(line);
          line = br.readLine();
        }
        br.close();

        // Initialize the coordinator
        new Coordinator(addressList, name);}
        catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
          }
      } else {
        System.err.println("Incorrect arguments!");
        System.err.println("Expected arguments: [coordinator name]");
	        System.exit(0);
	      }
  }
}
