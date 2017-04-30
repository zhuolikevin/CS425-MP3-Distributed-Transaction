import java.io.BufferedReader;
import java.io.FileReader;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.jgraph.graph.DefaultEdge;
import org.jgrapht.*;
import org.jgrapht.alg.CycleDetector;
import org.jgrapht.graph.DefaultDirectedGraph;

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
	private DirectedGraph<String, DefaultEdge> graph;

  Coordinator(ArrayList<String> addressList, String name) throws RemoteException {
	  
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

  }

  @Override 
  public void addEdgeDetectCycle(String t1, HashSet<String> t2) throws RemoteException {
	  if (!graph.containsVertex(t1))
		  graph.addVertex(t1);
	  for (String id : t2) {
		  if (!graph.containsVertex(id))
			  graph.addVertex(id);
		  if (!graph.containsEdge(t1, id))
			  graph.addEdge(t1, id);
	  }
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
		  graph.removeVertex(latestTransaction);
		  cycleDetector = new CycleDetector<String, DefaultEdge>(graph);
		  haveCycle = cycleDetector.detectCycles();
	  }
	  return;
  }
  
  @Override
  public HashMap<String, Long> getIdtimeMap() throws RemoteException {
	  return this.id_time;
  }
  
  @Override
  public HashSet<String> getIdtoAbort() throws RemoteException {
	  return this.id_abort;
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
