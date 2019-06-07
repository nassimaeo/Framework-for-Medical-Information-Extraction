package reasoning;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import snomed.SNOMED;
import wordnet.WordNet;

/**
 * 
 * A Meta Model that describes a patient state. It was designed after the
 * analysis of several medical posts on forums written mostly by lay-persons
 * seeking help and advice online. The instantiation of a MetaModel gives us a
 * workable model. A model that represent the initial description of the
 * patient. [see the image in the paper]
 * 
 * @author Nassim
 * @version 1.0
 */

public class MetaModel {

	// Determine the accuracy threshold of the triplets of the model to retain
	// It ranges between [0.0, 1.0] or [0%, 100%]
	private static double THRESHOLD = 0.75;
	
	// will perform the evaluation of the extracted triplet 
	private ReasoningEngine reasoningEngine;
	
	// Triplets constituting the instantiated model
	private List<MatchedPattern> matchedPatternsRetained;
	
	// Establish the links between the meta-model and all other resources to use
	// in our case the resources are : snomed and wordnet
	private ResourcesMapper resourcesMapper;
	
	// List of SNOMED_ID founds
	private List<String> snomedIds;

	// file name where the post where constructed from
	private String postFilePath;
	
	/**
	 * Build the instantiated model (interpretation of the message) using SNOMED and WORDNET as resources.
	 * You can visualize the interpreted message with draw().
	 * @param wordnet
	 * @param snomed
	 * @param postFilePath 
	 * @throws IOException 
	 */
	public MetaModel(WordNet wordnet, SNOMED snomed, ResourcesMapper resourcesMapper, String postFilePath) throws IOException {
		//save the file name treated
		this.postFilePath = postFilePath;
		
		//initialize the list of triplets that will constitute the interpretation model
		this.matchedPatternsRetained = new LinkedList<MatchedPattern>();
			
		//load the mapping of the resources with the Meta-Model
		this.resourcesMapper = resourcesMapper;

		//initialize the list of SNOMED_id which will be extracted
		this.snomedIds = new LinkedList<>();
		
		//initialize the reasoning engine
		reasoningEngine = new ReasoningEngine(wordnet, snomed, resourcesMapper);
	}


	/**
	 * Add to the list of snomed_id the concepts that we referred to in the post.
	 * @param id Snomed_ID
	 */
	public void addSnomedID(String id) {
		this.snomedIds.add(id);
	}
	
	public void addSnomedIDs(List<String> snomedIdsFound) {
		for (String string : snomedIdsFound) {
			addSnomedID(string);
		}
	}
	
	/**
	 * Add all the patterns in the list of matched patterns to the meta-model.
	 * Keep only the relevant triplets and discard the rest.
	 * This function modify the Triplets of the matched syntactical patterns.
	 * The Meta-Model will guide the construction of the interpretation by deciding
	 * which triplet in the matched patterns to keep, and which one to throw away.
	 * (We rely on the accuracy threshold to decide. It will be defined later)
	 * We use Induction, Deduction and Abduction mechanisms to improve the interpretation.
	 * 
	 * @param matchedPatterns patterns along with all their instantiated triplets to evaluate
	 */
	public void addMatchedPattens(List<MatchedPattern> matchedPatterns) {
		if (matchedPatterns == null) throw new NullPointerException();
		if (matchedPatterns.size() == 0) return;

		for (MatchedPattern matchedPattern : matchedPatterns) {
			// get the syntactical pattern
			SyntacticalPattern syntacticalPattern = matchedPattern.getSyntacticalPattern();
			
			// evaluate each extracted triplet with regard to their syntactical pattern
			List<Triplet> newTriplets = new LinkedList<>();
			for (Triplet triplet : matchedPattern.getTriplets()) {
				
				// perform the reasoning algorithm on the extracted triplets
				double accuracy = reasoningEngine.evaluate(syntacticalPattern, triplet);
				
				// keep the triplets that exceeded the threshold as part of the interpretation model
				if (accuracy >= THRESHOLD){
					System.out.println(triplet + " accur: " + accuracy);
					newTriplets.add(triplet);
					this.addSnomedIDs(reasoningEngine.foundSnomedIDs);
				}
			}
			
			// ignore if no triplet was relevant to the relationship
			if (newTriplets.size() > 0) {
				matchedPattern.setTriplet(newTriplets);
				this.matchedPatternsRetained.add(matchedPattern);
			}
		}
	}
	


	/**
	 * Draw the Meta-Model structure along with the instantiated triplets extracted.
	 */
	public void draw() {
		new GraphModel(this.matchedPatternsRetained);
	}
	
	
	/**
	 * Print the list of CUIs that were extracted from the text to populate the Meta-Model.
	 */
	public void printCUIs() {
		//TODO
		/*
		System.out.println("List of ("+snomedIds.size()+") CUIs for the File:" + postFilePath);
		for (String cui : snomedIds) {
			System.out.println(cui);
		}
		*/
	}
	
	/**
	 * Print the list of SNOMED_IDS that were extracted from the text to populate the Meta-Model.
	 */
	public void printSnomedIDs() {
		System.out.println("List of ("+snomedIds.size()+") CUIs for the File:" + postFilePath);
		for (String id : snomedIds) {
			System.out.println(id);
		}
	}
	
	/**
	 * return the list of all the snomedct_id extracted form the text
	 * 
	 * @return
	 */
	public List<String> getSnomedIds() {
		return snomedIds;
	}


	/**
	 * Used for testing purposes only
	 * @param args
	 * @throws IOException 
	 */
    public static void main(String[] args) throws IOException {
        WordNet wn = WordNet.loadWordNet();
        SNOMED snomed = SNOMED.loadSnomed();
        ResourcesMapper resourcesMapper = new ResourcesMapper();
        MetaModel model = new MetaModel(wn, snomed,resourcesMapper, "no_file_name");
        List<MatchedPattern> matchedPatterns = new LinkedList<>();
        model.addMatchedPattens(matchedPatterns);
        model.draw();
    }



}
