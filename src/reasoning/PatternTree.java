package reasoning;

import java.util.LinkedList;

/**
 * We use an tree data structure to represent the syntactical patterns. 
 * A leaf might link to a (CONCEPT(SUBJECT, OBJECT) or RELATIONSHIP(PREDICAT)): 
 * 1- Expert Concept (SNOMED CONCEPT) Symptom, Disease, Condition, etc 
 * 2- Expert Relationship (SNOMED RELATIONSHIP) is-a, due to, etc 
 * 3- A regular Concept 
 * 4- A regular Relationship
 * 
 * Use the words in the leafs to define the right concept or relationship. Use
 * WordNet to search for Synonyms and improve the coverage for the words.
 * 
 * @author Nassim
 *
 */
enum Type { SUBJECT, PREDICAT, OBJECT }

public class PatternTree {

	private String label;
	private LinkedList<PatternTree> children;
	private Type type;

	public PatternTree(String label) {
		if (label == null)
			throw new NullPointerException();
		this.label = label;
		this.children = new LinkedList<>();
	}

	public String label() {
		return label;
	}

	public LinkedList<PatternTree> children() {
		return children;
	}

	public void addChildren(LinkedList<PatternTree> children) {
		if (children == null)
			throw new NullPointerException();
		this.children.addAll(children);
	}

	public void addChild(PatternTree child) {
		if (child == null)
			throw new NullPointerException();
		this.children.add(child);
	}

	public boolean isLeaf() {
		return this.children.size() == 0;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public Type expected() throws Exception {
		if (!isLeaf())
			throw new Exception();
		return type;
	}
	
	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append("(");
		result.append(this.label);
		for (PatternTree patternTree : children) {
			result.append(patternTree.toString());
		}
		result.append(")");
		return result.toString();
	}
}