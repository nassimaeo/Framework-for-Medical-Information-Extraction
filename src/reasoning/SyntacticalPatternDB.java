package reasoning;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import edu.stanford.nlp.trees.Tree;

/**
 * 
 * This class load all the Syntactical Patterns (Hearst Patterns) from a file.
 * It allows to add additional patterns manually or automatically. It can also
 * load patterns automatically from an Ontology such us SNOMED.
 * 
 * An example of SYntactical Pattern : PERS. has SYMP. PRONOUN. has NOUN.
 * (compute similarity between the word NOUN and the concept SYMP. to do so,
 * search for the concepts where the NOUN appear in SNOMED. Check if it linked
 * with is-a directly or indirectly to the concepts SYMP. in SNOMED) - PERS. is
 * the concept PERSON - SYMP. is the concept SYMPTOM
 * 
 * @author Nassim
 * @version 1.0
 */

public class SyntacticalPatternDB {
	// pattern format: CONCEPT_SUBJECT REL CONCEPT_OBJECT (SYNTACTICAL TREE)
	private final String PATH_TO_PATTERNS = "res/patterns.txt";
	
	final List<SyntacticalPattern> syntacticalPatterns = new LinkedList<>();

	public SyntacticalPatternDB() {
		this.loadSyntacticalPatterns();
	}

	/**
	 * Load all the syntactical structures stored in a text file.
	 * The patterns are stored in an external text file (see PATH_TO_PATTERNS)
	 * 
	 * Avoid using spaces in the patterns because you may create
	 * wrong labels for example: "NN " instead of "NN"
	 */
	private void loadSyntacticalPatterns() {
		try {
			BufferedReader syntacticalPatternFile = new BufferedReader(new FileReader(PATH_TO_PATTERNS));
			if (syntacticalPatternFile.ready()) {
				String linePattern;
				while ((linePattern = syntacticalPatternFile.readLine()) != null) {
					SyntacticalPattern newSP = new SyntacticalPattern(linePattern);
					syntacticalPatterns.add(newSP);
				}
			}
			syntacticalPatternFile.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Find all the relationships that can be extracted from the tree.
	 * 
	 * @param tree a sentence along with its parsed syntactical tree
	 * @return
	 */
	public List<MatchedPattern> getMatchingPatterns(Tree tree) {
		List<MatchedPattern> matchedPatterns = new LinkedList<MatchedPattern>();
		for (SyntacticalPattern sp : syntacticalPatterns) {
			List<Triplet> triplets = sp.match(tree);
			if (triplets.size() > 0)
				matchedPatterns.add(new MatchedPattern(sp, triplets));
		}
		return matchedPatterns;
	}

	/**
	 * Used for test.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
	}

}
