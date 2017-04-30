import java.io.BufferedReader;
import java.io.FileReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import org.jgraph.graph.DefaultEdge;
import org.jgrapht.*;
import org.jgrapht.alg.CycleDetector;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.EdgeReversedGraph;

public class Coordinator extends UnicastRemoteObject implements CoordinatorInterface{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String name;
	private List<ServerInterface> serverInterfaceList;
	private static final String RES_PREFIX = "../res/";
    private HashMap<String, Long> id_time;
	private HashSet<String> id_abort;
	private DefaultDirectedGraph<String, DefaultEdge> graph;

  Coordinator(ArrayList<String> addressList, String name) throws RemoteException, UnknownHostException {
	  
	  this.name = name;
	  this.serverInterfaceList = new ArrayList<ServerInterface>();
	  this.id_time = new HashMap<>();
	  this.id_abort = new HashSet<>();
	  this.graph = new DefaultDirectedGraph<String, DefaultEdge>(DefaultEdge.class);
// The name of the coordinator can be set to F
	  Registry registry;
	  registry = LocateRegistry.createRegistry(9936 + (int) name.charAt(0));
	  registry.rebind(this.name, this);

	  System.out.println("Coordinator Ready!");
	  
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
    
    userConsole();

  }

  @Override 
  public void addEdgeDetectCycle(String t1, HashSet<String> t2) throws RemoteException {
	  if (!graph.containsVertex(t1))
	  {
		  graph.addVertex(t1);
		  System.out.println("Successfully add vertex " + t1);
		  }
	  for (String id : t2) {
		  if (!graph.containsVertex(id))
			  {
			  graph.addVertex(id);
			  System.out.println("Successfully add vertex " + id);
			  }
		  if (!graph.containsEdge(t1, id))
		  {
			  DefaultEdge edgeAdded;
			  edgeAdded = graph.addEdge(t1, id);
			  System.out.println((String)edgeAdded.getSource() + "->" + (String)edgeAdded.getTarget());
			  System.out.println("Successfully add edge from " + t1 + " to " + id);
		  }
	  }
	  
//	  Set<String> vertex = graph.vertexSet();
//	  for (String id : vertex) {
//		  System.out.println(id);
//	  }
	  
	  DirectedGraph<String, DefaultEdge> revGraph = new EdgeReversedGraph<>(graph);
	  DirectedGraph<String, DefaultEdge> graphCopy = new EdgeReversedGraph<>(revGraph);
      
	  CycleDetector<String, DefaultEdge> cycleDetector = new CycleDetector<String, DefaultEdge>(graph);
	  boolean haveCycle = cycleDetector.detectCycles();
	  while (haveCycle) {
		  HashSet<String> cycleId = (HashSet<String>) cycleDetector.findCycles();
		  long maxTimeStamp = 0;
		  String latestTransaction = null;
		  for (String id : cycleId) {
			  if (id_time.get(id) > maxTimeStamp)
				 maxTimeStamp = id_time.get(id); 
			     latestTransaction = id;
		  }
		  id_abort.add(latestTransaction);
//		  graph.removeVertex(latestTransaction);
		  graphCopy.removeVertex(latestTransaction);
		  cycleDetector = new CycleDetector<String, DefaultEdge>(graphCopy);
		  haveCycle = cycleDetector.detectCycles();
	  }
	  return;
  }
  
  @Override
  public void putIntoIdtimeMap (String transactionId, long TimeStamp) throws RemoteException {
	  this.id_time.put(transactionId, TimeStamp);
  }
  
  @Override
  public Long getFromIdtimeMap (String transactionId) throws RemoteException {
	  return this.id_time.get(transactionId);
  }
  
  @Override
  public void removeFromIdtimeMap (String transactionId) throws RemoteException {
	  this.id_time.remove(transactionId);
  }

  @Override
  public HashMap<String, Long> getIdtimeMap() throws RemoteException {
	  return this.id_time;
  }
  
  @Override
  public HashSet<String> getIdtoAbort() throws RemoteException {
	  return this.id_abort;
  }
  
  @Override
  public DirectedGraph<String, DefaultEdge> getGraph() throws RemoteException {
	  return this.graph;
  }
  
  @Override 
  public void removeFromGraph(String transactionId) throws RemoteException {
	  this.graph.removeVertex(transactionId);
  }
  
  @Override
  public void removeFromIdtoAbort(String transactionId) throws RemoteException {
	  this.id_abort.remove(transactionId);
  }
  
  private void userConsole() throws UnknownHostException {
	    Scanner scan = new Scanner(System.in);
	    String input = scan.nextLine();
	    while(!input.equals("EXIT")) {
	      String[] inputs = input.split(" ");
	      switch (inputs[0]) {
	        case "GRAPH":
	          Set<DefaultEdge> allEdges = graph.edgeSet();
			for (DefaultEdge edge : allEdges) {
				System.out.println(edge.getSource() + "->" + edge.getTarget());
			}
	          break;
	        case "IDTIME":
	          for (String key : id_time.keySet()) {
	            long TimeStamp = id_time.get(key);
	            System.out.println(key + " : " + TimeStamp);
	          }
	          System.out.println("[END] Total Transactions: " + id_time.keySet().size());
	          break;
	        case "IDABORT":
	        	for (String id : id_abort) {
	        		System.out.println(id);
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
