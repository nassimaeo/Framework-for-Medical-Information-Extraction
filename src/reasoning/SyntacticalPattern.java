package reasoning;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import edu.stanford.nlp.trees.Tree;



public class SyntacticalPattern {
	
	/**
	 * 
	 * Each syntactical pattern is associated to a triplet (Subject Predicate
	 * Object) Each matching has % of accuracy. The formula for this value will be
	 * defined.
	 * 
	 * Example of a pattern: PERSON SUFFER SYMPTOM
	 * (S(NP(PERSON_CONCEPT_SUBJECT))(VP(VBP(SUFFER_RELATIONSHIP))(PP(SYMPTOM_CONCEPT_OBJECT))))
	 *
	 */

	class Backtrack {
		private Triplet triplet;
		private int patternPosition;
		private int treePosition;

		public Backtrack(int p, int t, Triplet triplet) {
			this.patternPosition = p;
			this.treePosition = t;
			this.triplet = triplet;
		}

		public Triplet getTriplet() {
			return this.triplet;
		}

		public void setTriplet(Triplet triplet) {
			this.triplet = triplet;
		}

		public int p() {
			return this.patternPosition;
		}

		public int t() {
			return this.treePosition;
		}
		@Override
		public String toString() {
			return "P:"+  p() +"T:"+ t() +" " +triplet.toString();
		}
	}
	
	private PatternTree rootPatternTree = null;

	private String subject;
	private String object;
	private String predicate;

	private final int SUBJECT = 0;
	private final int PREDICATE = 1;
	private final int OBJECT = 2;
	private final int PATTERN = 3;

	/**
	 * Retrieve the triplet format (SUBJECT PREDICAT OBJECT) and the TREE of the
	 * syntactical pattern. If this format is not followed throw
	 * IllegalArgumentException.
	 * 
	 * Build the syntactical pattern tree from a string of nested nodes. Throw
	 * IllegalArgumentException when the pattern in the nested list is
	 * incorrect.
	 * 
	 * @param string
	 *            SUBJECT PREDICATE OBJECT TREE(represented as nested list)
	 */
	public SyntacticalPattern(String string) {
		String[] strings = string.split(" ");
		if (strings.length != 4)
			throw new IllegalArgumentException(string);
		this.subject = strings[SUBJECT];
		this.predicate = strings[PREDICATE];
		this.object = strings[OBJECT];

		String s = strings[PATTERN];

		// lecture head position of the string pattern
		int i = 0;

		// stack all the current open elements with open brackets
		Stack<PatternTree> stack = new Stack<>();

		// add an extra ROOT node for the pattern
		rootPatternTree = new PatternTree("ROOT");
		stack.add(rootPatternTree);

		while (i < s.length()) {
			char c = s.charAt(i);
			if (c == '(') {
				StringBuilder node = new StringBuilder();
				while (++i < s.length() && s.charAt(i) != ')' && s.charAt(i) != '(') {
					node.append(s.charAt(i));
				}
				if (node.length() == 0) {
					throw new IllegalArgumentException(s);
				}
				PatternTree current = new PatternTree(node.toString());
				stack.add(current);
			} else if (c == ')') {
				PatternTree pop = stack.pop();
				if (stack.isEmpty())
					throw new IllegalArgumentException(string);
				stack.peek().addChild(pop);
				i++;
			} else {
				throw new IllegalArgumentException(string);
			}
		}

		if (stack.size() != 1 || !stack.peek().label().equals("ROOT")) {
			throw new IllegalArgumentException(string);
		}
	}

	/**
	 * Find matches of the pattern in the Tree passed as argument. The matching
	 * is performed level by level, left to right, from the root down to the
	 * leaves.
	 * 
	 * At each level, all the elements of the pattern must be present in the
	 * same order as they are present in the tree. Some additional elements
	 * might be present in the tree and can be neglected as long as all the
	 * other elements in the patterns appeared.
	 * 
	 * @param tree
	 *            syntactical tree of a sentence
	 * @return returns all matched patterns, empty list if there is no matching
	 */
	public List<Triplet> match(Tree tree) {
		List<Triplet> matchedTriplets = new LinkedList<>();
		final Stack<Tree> stack = new Stack<>();
		stack.add(tree);
		while (!stack.isEmpty()) {
			Tree subTree = stack.pop();
			if (getMatches(subTree) != null) {
				matchedTriplets.addAll(getMatches(subTree));
			}
			for (Tree t : subTree.children()) {
				stack.push(t);
			}
		}
		return matchedTriplets;
	}

	/**
	 * Get all the patterns where the current patternTree appears in the tree
	 * Used to remove the first ROOT node of the pattern and find a match.
	 * 
	 * @param tree
	 *            syntactical tree of a sentence
	 * @return the list of all the matched patterns found
	 */
	private List<Triplet> getMatches(Tree tree) {
		return recursiveMatch(tree, rootPatternTree.children().getFirst());
	}

	/**
	 * 
	 * @param tree
	 * @param pattern
	 * @return total number of matches found
	 */
	private List<Triplet> recursiveMatch(Tree tree, PatternTree pattern) {

		if (tree == null || pattern == null)
			throw new IllegalArgumentException();

		// base case
		if (pattern.isLeaf()) {
			// pattern is either a CONCEPT (Subject, Object) or RELATIONSHIP
			// (Predicate)
			return getConceptOrRelationshipMatch(tree, pattern);
		}
		

		// recursive case
		
		// check if the labels of the current nodes (pattern and tree) match
		boolean doesLabelsMatch = tree.label().toString().equals(pattern.label());
		if (!doesLabelsMatch) {
			return null;
		}
		
		Tree[] treeChildren = tree.children();
		List<PatternTree> patternChildren = pattern.children();

		Stack<Backtrack> backtrackingMatches = new Stack<>();
		Backtrack start = new Backtrack(0, 0, new Triplet(null, null, null, 1.0));
		backtrackingMatches.push(start);

		List<Triplet> tripletsFound = new LinkedList<Triplet>();

		while (!backtrackingMatches.isEmpty()) {
			
			Backtrack current = backtrackingMatches.pop();
			Triplet currentTriplet = current.getTriplet();

			if (current.p() == patternChildren.size() && current.t() <= treeChildren.length) {
				// matched pattern
				tripletsFound.add(current.getTriplet());
				continue;
			}

			int p = current.p();
			for (int t = current.t(); t < treeChildren.length; t++) {
				List<Triplet> allMatches = recursiveMatch(treeChildren[t], patternChildren.get(p));
				if (allMatches != null) {
					for (Triplet subTriplet : allMatches) {
						Backtrack newBacktrackingPosition = new Backtrack(p + 1, t + 1,
								currentTriplet.merge(subTriplet));
						backtrackingMatches.push(newBacktrackingPosition);
					}
				}
			}
			//if (backtrackingMatches.isEmpty()) System.out.println("Failed at:" + p + " " + patternChildren.get(p).label());
			//printStack(backtrackingMatches);//debut stack
		}

		if (tripletsFound.size() == 0)
			return null;
		else
			return tripletsFound;
	}

	private void printStack(Stack<Backtrack> s){
		System.out.println("\nStack:");
		for (Object object : s) {
			System.out.println(object);
		}
	}
	/**
	 * Check if the CON or REL in the leaf correspond to an entry in the tree
	 * You can use SNOMED or WordNet to enrich the interpretation.
	 * 
	 * @param tree
	 *            the remaining tree structure of the sentence in the current
	 *            match
	 * @param leaf
	 *            is either a CONCEPT or RELATIONSHIP
	 * @return
	 * 
	 */
	private List<Triplet> getConceptOrRelationshipMatch(Tree tree, PatternTree leaf) {
		
		// handle leaves nodes 
		boolean doesLabelsMatch = tree.label().toString().equals(leaf.label());
		if (doesLabelsMatch) {
			LinkedList<Triplet> triplets = new LinkedList<>();
			triplets.add(new Triplet(null, null, null, 1.0));
			return triplets;
		}
		
		String[] labels = leaf.label().split("_");
		int lastIndex = labels.length - 1;
		if (labels[lastIndex].equals("SUBJECT")) {
			return getConceptSubjectMatches(leaf.label(), getAllLeavesWords(tree));
		} else if (labels[lastIndex].equals("OBJECT")) {
			return getConceptObjectMatches(leaf.label(), getAllLeavesWords(tree));
		} else if (labels[lastIndex].equals("RELATIONSHIP")) {
			return getRelationshipMatches(leaf.label(), getAllLeavesWords(tree));
		} else {			
			//System.out.println("An error in a pattern encountered at the node:" + leaf.label());
			return null;
		}
	}

	private List<Triplet> getConceptSubjectMatches(String label, String[] allLeavesWords) {
		LinkedList<Triplet> triplets = new LinkedList<>();
		triplets.add(new Triplet(allLeavesWords, null, null, 1.0));
		return triplets;
	}

	private List<Triplet> getConceptObjectMatches(String label, String[] allLeavesWords) {
		LinkedList<Triplet> triplets = new LinkedList<>();
		triplets.add(new Triplet(null, null, allLeavesWords, 1.0));
		return triplets;
	}

	private List<Triplet> getRelationshipMatches(String label, String[] allLeavesWords) {
		LinkedList<Triplet> triplets = new LinkedList<>();
		triplets.add(new Triplet(null, allLeavesWords, null, 1.0));
		return triplets;
	}

	private String[] getAllLeavesWords(Tree tree) {

		List<Tree> allLeaves = new LinkedList<>();

		Stack<Tree> stack = new Stack<>();
		stack.add(tree);
		while (!stack.isEmpty()) {
			Tree root = stack.pop();
			if (root.isLeaf())
				allLeaves.add(root);
			for (Tree t : root.children()) {
				stack.push(t);
			}
		}
		Collections.reverse(allLeaves);
		String[] result = new String[allLeaves.size()];
		StringBuilder s = new StringBuilder();
		for (int i = 0; i < allLeaves.size(); i++) {
			Tree leaf = allLeaves.get(i);
			s.append(leaf.label());
			result[i] = leaf.label().toString();
			if (i + 1 < allLeaves.size())
				s.append(" ");
		}
		return result;
	}

	@Override
	public String toString() {
		String tripletString = getPredicate() + "(" + getSubject() + "," + getObject() + ")";
		return "\n  TREE:   \t" + rootPatternTree.toString() + "\n  TRIPLET:\t" + tripletString;
	}
	
	
	public String getSubject(){
		return this.subject;
	}
	
	public String getObject(){
		return this.object;
	}
	
	public String getPredicate(){
		return this.predicate;
	}

}
