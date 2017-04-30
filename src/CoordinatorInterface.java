import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.HashSet;

import org.jgraph.graph.DefaultEdge;
import org.jgrapht.graph.DefaultDirectedGraph;

public interface CoordinatorInterface extends Remote {
	
	HashMap<String, Long> getTransactionTimeMap() throws RemoteException;
	void putIntoTransactionTimeMap(String transactionId, long TimeStamp) throws RemoteException;
	void removeFromTransactionTimeMap (String transactionId) throws RemoteException;
	
  HashSet<String> getAbortingTransactionSet() throws RemoteException;
	void addAbortingTransaction(String transactionId) throws RemoteException;
	void removeFromAbortingTransactionSet(String transactionId) throws RemoteException;

	DefaultDirectedGraph<String, DefaultEdge> getGraph() throws RemoteException;
	void addEdge(String transactionId, HashSet<String> lockOwners) throws RemoteException;
	void removeFromGraph(String transactionId) throws RemoteException;
	boolean containsVertex(String transactionId) throws RemoteException;
}
