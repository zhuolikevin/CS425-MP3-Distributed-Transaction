import org.jgraph.graph.DefaultEdge;
import org.jgrapht.alg.CycleDetector;
import org.jgrapht.graph.DefaultDirectedGraph;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TimerTask;

public class DeadlockDetector extends TimerTask {
  private Coordinator thisCoordinator;

  DeadlockDetector(Coordinator thisCoordinator) {
    this.thisCoordinator = thisCoordinator;
  }
  @Override
  public void run() {
    try {
      DefaultDirectedGraph<String, DefaultEdge> curGraph = thisCoordinator.getGraph();

      CycleDetector<String, DefaultEdge> cycleDetector = new CycleDetector<>(curGraph);
      if (cycleDetector.detectCycles()) {
        HashSet<String> vertices = (HashSet<String>) cycleDetector.findCycles();
        HashMap<String, Long> transactionTimeMap = thisCoordinator.getTransactionTimeMap();

        long maxTimeStamp = 0;
        String latestTransaction = null;
        for (String transactionId : vertices) {
          if (transactionTimeMap.get(transactionId) > maxTimeStamp) {
            maxTimeStamp = transactionTimeMap.get(transactionId);
            latestTransaction = transactionId;
          }
        }
        thisCoordinator.removeFromGraph(latestTransaction);
        thisCoordinator.addAbortingTransaction(latestTransaction);
      }
    } catch (RemoteException e) {
      e.printStackTrace();
    }
  }
}