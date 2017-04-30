import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.HashSet;

public interface CoordinatorInterface {
	
	HashMap<String, Long> getIdtimeMap() throws RemoteException;
	
    HashSet<String> getIdtoAbort() throws RemoteException;
    
    void addEdgeDetectCycle(String t1, HashSet<String> t2) throws RemoteException;

}
