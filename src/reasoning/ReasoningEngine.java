package reasoning;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import snomed.SNOMED;
import snomed.SNOMED.SearchMethod;
import wordnet.WordNet;

/**
 * Perform the evaluation of the extracted triplets with regard to their 
 * syntactical pattern structure.
 * A triplet is composed of a PREDICATE(SUBJECT,OBJECT) each one of them 
 * is a list of words.
 * A syntactical pattern has a subject, object and predicate each one of 
 * them is aligned with the resources (WordNet or SNOMED-CT).
 * The reasoning algorithm will evaluate if a words of the triplet is 
 * coherent with its corresponding resources concepts using either:
 * Deduction, Induction or Abduction.
 * 
 * @author Nassim
 *
 */
public class ReasoningEngine {
	
	WordNet wordnet;
	SNOMED snomed;
	ResourcesMapper resourcesMapper;

	// This will contain the SNOMED-CT ID extracted from the text for each evaluated triplet
	// reset this to an empty list each time we call evaluate
	// then it will be filled with the found SNOMED concepts when the new triplet is evaluated.
	public List<String> foundSnomedIDs;
	
	/**
	 * Initialize the reasoning engine to evaluate the triplet with regard to their
	 * syntactical patterns using the resources mapper that establishes links between
	 * the Meta-Model concepts or relationships and WordNet_Synsets or SNOMED_concepts
	 * @param wordnet
	 * @param snomed
	 * @param resourcesMapper
	 */
	public ReasoningEngine(WordNet wordnet, SNOMED snomed, ResourcesMapper resourcesMapper){
		this.wordnet = wordnet;
		this.snomed = snomed;
		this.resourcesMapper = resourcesMapper;
	}
	
	
	/**
	 * Evaluate the accuracy of the triplet with regard to the syntactical pattern
	 * 
	 * @param syntacticalPattern
	 * @param triplet
	 * @return the accuracy of the triplet (0.0 wrong, 1.0 highly accurate)
	 */
	public double evaluate(SyntacticalPattern syntacticalPattern, Triplet triplet){
		
		// re-initialize the list of the found snomed id to an empty list
		this.foundSnomedIDs = new LinkedList<>();
		
		// get the meta-model structure of the pattern
		String object = syntacticalPattern.getObject();
		String subject = syntacticalPattern.getSubject();
		String predicate = syntacticalPattern.getPredicate();
		
		// get the triplet's terms for their corresponding structure
		String[] subjectTerms = triplet.getSubject();
		String[] predicateTerms = triplet.getPredicate();
		String[] objectTerms = triplet.getObject();
		
		// print the triplet to evaluate for debugging purposes
		String subT="", objT="", preT="";
		if (objectTerms != null) objT = String.join(" ", objectTerms);
		if (subjectTerms != null) subT = String.join(" ", subjectTerms);
		if (predicateTerms != null) preT = String.join(" ", predicateTerms);
		
		
		// run the evaluation algorithm for each component
		double accuracySub = evaluateEntity(subject, triplet.getSubject());
		double accuracyPrd = evaluateEntity(predicate, triplet.getPredicate());
		double accuracyObj = evaluateEntity(object, triplet.getObject());
		
		//print debugging data
		String result = String.format("Evaluating: S[%s:%s(%f)], P[%s:%s(%f)], O[%s:%s(%f)]", subject,subT,accuracySub, predicate,preT,accuracyPrd, object,objT,accuracyObj);
		System.out.println(result);
		
		/*
		 1- Direct match with the resources? null, WordNet, words?
		 2- Deduction: with {SNOMED-CT, WordNet}
		 3- Induction: with {WordNet, Hypernyms, Synonyms, See-Also, or Distances}
		 4- Abduction: with {null, or direct words}
		 */
		return getAccuracy(accuracySub, accuracyPrd, accuracyObj);
	}
	

	/**
	 * 
	 * Calculate the overall accuracy of a triplet using to the accuracy of each element of triplet apart.
	 * (Subject concept, Predicate relationship, Object concept)
	 * 
	 * @param accuracySub accuracy of subject concept
	 * @param accuracyPrd accuracy of predicate relationship
	 * @param accuracyObj accuracy of object concept
	 * @return accuracy of the triplet
	 */
	private double getAccuracy(double accuracySub, double accuracyPrd, double accuracyObj) {
		// You can fine tune the accuracy method of each triplet P(S,O) here.
		//return (accuracySub+accuracyPrd+accuracyObj)/3.0 ;
		//return (accuracySub+accuracyObj)/2.0 ;
		return accuracySub * accuracyPrd * accuracyObj;
	}
	

	/**
	 *  Concept and relationship Evaluation (Subject, Predicate or Object).
	 *  
	 *  Use the words to evaluate either Subject, Predicate or Object as follow:
	 *  1- Can is be null? 
	 *  2- Is there a direct hit in the list of manually defined words?
	 *  3- Is there is a match with WordNet?
	 *  4- Is there a match with SNOMED-CT?
	 *  
	 * return 
	 *   
	 * @param entity type Sub, Pre or Obj
	 * @param words word to evaluate
	 * @return 0.0 if no result or 1.0 if there is a match
	 */
	private double evaluateEntity(String entity, String[] words) {
		// 1- Can it be null?
		if (resourcesMapper.canBeNull(entity) && words == null) return 1.0;
				
		// 2- Is it manually defined?
		List<String> otherWords = resourcesMapper.otherWords(entity);
		boolean allMatch = true;
		if (otherWords != null) {
			for (String word : words) {
				String wordLowerCase = word.toLowerCase();
				boolean match = false;
				for (String otherWord : otherWords) {
					if (wordLowerCase.equals(otherWord.toLowerCase())) {
						match = true;
						break;
					}
				}
				if (match == false) {
					allMatch = false;
					break;
				}
			}
			if (allMatch) return 1.0;
		}
		
		// Ignore one letter words
		// TODO change this if you want to ignore other word lengths
		if (words.length==1 && words[0].length()==1) return 0;
		
		// 3- WordNet alignment
		List<String> wordnetSynsets = resourcesMapper.wordnetSynset(entity);
		allMatch = true;
		if (wordnetSynsets != null){
			for (String word : words) {
				boolean match = false;
				for (String synset : wordnetSynsets) {
					Integer synsetId = Integer.parseInt(synset);
					List<Integer> synsetsOfWord = wordnet.getSynsets(word);
					if (synsetsOfWord.contains(synsetId)) {
						match = true;
						break;
					}
				}
				if (match == false) {
					allMatch = false;
					break;
				}
			}
			if (allMatch) return 1.0;
		}
		
		// 4- SNOMED-CT alignment
		List<String> snomedIDs = resourcesMapper.snomedConceptID(entity);
		if (snomedIDs != null) {
			
			// New search method using incremental search combined with Levenshtein distance
			List<String> snomedIdsFound = snomed.getAllFoundConceptsNoSweep(words);
			List<String> snomedIdsFoundEvaluated = new LinkedList<>();
			// We used this third list to keep the list of medical concepts that do fall 
			// only in the Disease or Symptom categories.
			// TODO Modify this code below to chose the concepts to include
			//List<String> snomedIdsFoundDiseasesAndSymptomsEvaluated = new LinkedList<>();
			
			for (String snomedIdFromWord : snomedIdsFound) {				
				Long id = Long.parseLong(snomedIdFromWord);
				HashSet<String> pathToRoot = snomed.getNodesToRoot(id);
				for (String snomedId : snomedIDs) {
					if (pathToRoot.contains(snomedId)) {
						// print the found SNOMED_ID
						//System.out.println(" ---> SNOMED_ID" + snomedIdFromWord);
						// save the found SNOMED_ID
						snomedIdsFoundEvaluated.add(snomedIdFromWord);
						// TODO save the Symptoms and Diseases of GoldBunchMark
						/*
						if (snomedId.equals("404684003") || snomedId.equals("52988006")) {
							snomedIdsFoundDiseasesAndSymptomsEvaluated.add(snomedIdFromWord);
						}
						*/
					}
				}
				
				// TODO for now we can break for the gold corpus
				/*
				if (snomedIdsFoundDiseasesAndSymptomsEvaluated.size() > 0) {
					break;
				}
				*/
			}			
			if (snomedIdsFoundEvaluated.size() > 0) {
				// TODO take all snomedCT concepts founds				
				for (String string : snomedIdsFoundEvaluated) {
					this.foundSnomedIDs.add(string);	
				}
				
				// TODO take only diseases and symptoms
				/*
				for (String string : snomedIdsFoundDiseasesAndSymptomsEvaluated) {
					this.foundSnomedIDs.add(string);	
				}
				*/
				return 1.0;
			}
			
			// Old method of research using Regular Expression
			/*
			LinkedList<String> listWords = new LinkedList<String>();
			for (String word : words) listWords.add(word);
			
			// Search the FNS and SYN of the SNOMED-CT concepts with the list of words (to rewrite the function)
			HashSet<String> snomedCodesFromWords = snomed.getMatchesForListOfWords(listWords, SNOMED.SearchMethod.REG_EXP); 
			
			// save the SNOMED-CT it that were extracted from the text file
			List<String> snomedIdsFound = new LinkedList<>();
			
			for (String snomedIdFromWord : snomedCodesFromWords) {				
				Long id = Long.parseLong(snomedIdFromWord);
				HashSet<String> pathToRoot = snomed.getNodesToRoot(id);
				for (String snomedId : snomedIDs) {
					if (pathToRoot.contains(snomedId)) {
						// print the found SNOMED_ID
						System.out.println(" ---> SNOMED_ID" + snomedIdFromWord);
						// save the found SNOMED_ID					
						snomedIdsFound.add(snomedIdFromWord);
					}
				}
			}
			if (snomedIdsFound.size() > 0) return 1.0;
			*/
		}

		return 0.0;
	}
	

}
