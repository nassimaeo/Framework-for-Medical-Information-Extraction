package wordnet;

import java.util.LinkedList;

import edu.princeton.cs.algs4.Queue;


public class GraphAlgorithms {
	
	private final static int GET_SAP_LENGTH = 0;
	private final static int GET_SAP_ANCESTOR = 1;
	
    // An immutable data type SAP
    private final Digraph G;

    public GraphAlgorithms(Digraph G) {
        // constructor takes a DiGraph (not necessarily a DAG)
        this.G = new Digraph(G);
    }

    /**
     * length of the shortest ancestral path between v and w;
     * -1 if no such path
     * @param v first set
     * @param w second set
     * @return shortest path length between v and w
     */
    public int length(int v, int w) {
        LinkedList<Integer> V = new LinkedList<Integer>();
        LinkedList<Integer> W = new LinkedList<Integer>();
        V.add(v);
        W.add(w);
        return this.length(V, W);
    }

    private int calculator(Iterable<Integer> v, Iterable<Integer> w, int method) {
        // method=0 --> length()
        // method=1 --> ancestor()
        
        // a common ancestor that participates in shortest ancestral path; -1 if
        // no such path
        // keep track of the visited nodes from each node seperately
        boolean[] markedV = new boolean[G.V()];
        boolean[] markedW = new boolean[G.V()];

        // keep track of the distance from each node
        int[] distToV = new int[G.V()];
        int[] distToW = new int[G.V()];
        
        
        int returnResult = Integer.MAX_VALUE;
        int bestI = -1;
        
        // Do a BFS
        Queue<Integer> queue = new Queue<Integer>();
        
        // put the the initial node in the queue
        for (int vi : v){
            if (vi <0 || vi >=G.V()) throw new IndexOutOfBoundsException();
            queue.enqueue(vi); // start at v
            markedV[vi] = true;
        }

        // put all the others initial nodes in the queues
        // to distinguish between them we use the negative sign in the queue, to avoid
        // -0 we shift negative with -1 to -0 = -1; -1 = -2 ...
        // so when we enqueue we know we side of the set we are picking from
        for (int wi : w){
            if (wi <0 || wi >=G.V()) throw new IndexOutOfBoundsException();
            queue.enqueue(-wi-1); // start at w
            markedW[wi] = true;
        }


        // repeat until no available vertex to explore
        // and prune when it is impossible to find a better distance
        while (!queue.isEmpty()) {
            //System.out.println("\nQueue:"+queue);
      
            int i = queue.dequeue();
            if (i < 0) {
                // we mark it as visited
                markedW[-i-1] = true;
                
                // for all the neighbors nodes check if they are visited
                if (markedV[-i-1]){
                    // if the left reaches the right, than compute the distance
                    int distance = distToV[-i-1] + distToW[-i-1];
                    
                    //System.out.println(distance);
                    // update the min distance if necessary
                    if ( distance < returnResult){
                        returnResult = distance;
                        bestI = -i-1;
                    }
                }

                for (Edge e : G.adj(-i-1)) {
                	int n = e.index();
                    // queue the neignbors if not visited yet and the current distance to them
                    // is less than or equal to the min distance already found
                    if (!markedW[n] && (returnResult >= distToW[-i-1])) {
                        markedW[n] = true;
                        queue.enqueue(-n-1);
                        distToW[n] = distToW[-i-1] + 1;
                    }
                }
            } else {
                // mark it as visited
                markedV[i] = true;
                // for all the neighbors nodes check if they are visited
                if (markedW[i]){
                    int distance = distToV[i] + distToW[i];
                    //System.out.println(distance);
                    // update the min distance if necessary
                    if ( distance < returnResult){
                        returnResult = distance;
                        bestI = i;
                    }
                }
                for (Edge e : G.adj(i)) {
                	int n = e.index();
                    if (!markedV[n] && (returnResult >= distToV[i])) {
                        // queue the neignbors if not visited yet and the current distance to them
                        // is less than or equal to the min distance already found
                        markedV[n] = true;
                        queue.enqueue(n);
                        distToV[n] = distToV[i] + 1;
                    }
                }                
            }
            /*
            System.out.print("MarkedV:");
            for (int k = 0; k < distToW.length; k++) {
                System.out.print(markedV[k]+" ");
            }
            System.out.print("\nMarkedW:");
            for (int k = 0; k < distToW.length; k++) {
                System.out.print(markedW[k]+" ");
            } 
            */                 
        }
        if (method==GET_SAP_LENGTH) {
            if (returnResult == Integer.MAX_VALUE)
                return -1;
            else
                return returnResult;
        } else if (method==GET_SAP_ANCESTOR) {
            return bestI;
        } else {
        	throw new IllegalArgumentException("Unkown method: " + method);
        }

    }

    public int ancestor(int v, int w) {
        // a common ancestor of v and w that participates in a shortest
        // ancestral path; -1 if no such path
        // keep track of the visited nodes from each node sepretly
        LinkedList<Integer> V = new LinkedList<Integer>();
        LinkedList<Integer> W = new LinkedList<Integer>();
        V.add(v);
        W.add(w);
        return this.ancestor(V, W);
    }

    /**
     * Compute the length of shortest ancestral path between 
     * any vertex in v and any vertex in w; -1 if no such path.
     * Keep track of the visited nodes from each node separately
     * @param v first set
     * @param w second set
     * @return shortest ancestral path length between the v and w
     */
    public int length(Iterable<Integer> v, Iterable<Integer> w) {
        return this.calculator(v, w, GET_SAP_LENGTH);
    }
	/**
	 * a common ancestor that participates in shortest ancestral path; 
	 * -1 if no such path.
	 * Keep track of the visited nodes from each node separately
	 * @param v first set
	 * @param w second set
	 * @return shortest ancestral path between the v and w
	 */
    public int ancestor(Iterable<Integer> v, Iterable<Integer> w) {
        return this.calculator(v, w, GET_SAP_ANCESTOR);
    }

    /**
     * do unit testing of this class
     * @param args
     */
    public static void main(String[] args) {

    }
}

