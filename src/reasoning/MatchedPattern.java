package reasoning;

import java.util.List;

public class MatchedPattern {
		
	SyntacticalPattern syntacticalPattern;
	List<Triplet> triplets;
	
	public MatchedPattern(SyntacticalPattern syntacticalPattern, List<Triplet> triplets) {
		this.syntacticalPattern = syntacticalPattern;
		this.triplets = triplets;
	}
	
	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append("\nSyntactical pattern:" + syntacticalPattern.toString()+"\n");
		result.append("Matches: ("+triplets.size()+")\n");
		for (Triplet triplet : triplets) {
			result.append(triplet.toString() + "\n");
		}
		return result.toString();
	}

	public List<Triplet> getTriplets() {
		return triplets;
	}

	public SyntacticalPattern getSyntacticalPattern() {
		return syntacticalPattern;
	}

	public void setTriplet(List<Triplet> newTriplets) {
		this.triplets = newTriplets;
		
	}
}
