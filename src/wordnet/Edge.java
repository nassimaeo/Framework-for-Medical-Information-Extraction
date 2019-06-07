package wordnet;


/**
 * Represent a labeled link between to words.
 * @author Nassim
 *
 */
public class Edge {
	
	private String label;
	private int index;
	
	public Edge(String label, int index) {
		this.label = label;
		this.index = index;
	}
	
	public String label() {
		return label;
	}
	
	public int index() {
		return index;
	}
	
	
	@Override
	public String toString() {
		return "("+this.label+"," + index + ")";
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Edge) {
			Edge b = (Edge) obj;
			return this.label().equals(b.label()) && this.index == b.index;
		}
		return false;
	}
}