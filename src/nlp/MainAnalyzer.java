package nlp;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.trees.Tree;
import evaluation.Eval;
import evaluation.UMLS;
import reasoning.MatchedPattern;
import reasoning.MetaModel;
import reasoning.ResourcesMapper;
import reasoning.SyntacticalPatternDB;
import snomed.SNOMED;
import wordnet.WordNet;

/**
 * This class loads all the necessary resources to interpret messages:
 * WordNet, SNOMED, Stanford Syntactical Parser, HearstPatterns.
 * 
 * Then it reads a post to build its model according to the Meta-Model [See the paper].
 * 
 * This class reads a post. Do the NATURAL LANGUAGE PROCESSING (NLP) to 
 * extract the sentences, and builds its syntactical tree (along with Part Of Speech).
 * Then, it sends the result to be HearstPattern Matched to find all the triplets
 * that can be extracted.
 * 
 * Once the triplets are extracted, it sends the result to the Meta-Model which performs
 * the reasoning steps to interpret the triplets and keep the correct ones and discard 
 * the wrong ones. The Meta-Model relies on WordNet and SNOMED, plus the reasoning mechanisms
 * (Induction, Abduction, deduction) to interpret the message.
 * 
 * A message is a textual forum post of medical content.
 * Steps:
 * - A post is a set of paragraphs.
 * - A post will be divided into sentences.
 * - Each sentence is analyzed to generate its Syntactical tree and POS.
 * - Triplets will be extracted from sentences using the defined syntactical patterns.
 * - Each Triplet will be evaluated in oder to keep the accurate relationships.
 * 
 * 
 * @author Nassim
 * @version 1.0
 */


public class MainAnalyzer {
   
	// models for Stanford Parser
	private LexicalizedParser lp;
	
	// SNOMED (Ontology)
	private SNOMED snomed;
	
	// WordNet (Thesaurus)
	private WordNet wordnet;

	// Establish the links between the meta-model and all other resources to use
	// in our case the resources are : snomed and wordnet
	private ResourcesMapper resourcesMapper;
	
	// load the syntactical patterns
	private SyntacticalPatternDB syntacticalPatternsDB;
	
	// UMLS SNOMED_IDs->CUI
	private UMLS umls;
	
    public MainAnalyzer() throws Exception {
    	
		// load the syntactical patterns to extract triplets
		this.syntacticalPatternsDB = new SyntacticalPatternDB();
    	
        // load the mapping of the ontologies and Meta-Model
        this.resourcesMapper = new ResourcesMapper();
        
		// loading the parser
		this.lp =  LexicalizedParser.loadModel();
	
		//loading the resources
        this.wordnet = WordNet.loadWordNet();
        this.snomed = SNOMED.loadSnomed();
        
        // load UMLS for the conversion of the SnomedIDs to UMLS_CUIs for evaluation of the SHARE_CLEF corpus
        this.umls = new UMLS();
        
        //@TODO
        // this task was taken to the Meta-Model
		// loading words to Concepts matching
		// loading words to Relationships matching
    }
    
    /**
     * 
     * Convert SNOMED_ID into its corresponding CUI
     * 
     * @param snomedId
     * @return CUI
     */
    public String getCUIofSnomedID(String snomedId) {
    	return umls.getCUIofSnomedId(snomedId);
    }
    
    /**
     * Analyze a health seeker description and build its interpretation model.
     * @param postFilePath a file containing the patient descriptions
     * @return an instance (model) of the Meta-Model
     * @throws IOException
     */
    public MetaModel analyze(String postFilePath) throws IOException {
   	
		// read the paragraph from the file postFilePath
		Reader reader = new FileReader(postFilePath);
		
		// prepare the document to be parsed
		DocumentPreprocessor docProc = new DocumentPreprocessor(reader);

		// split the paragraph into sentences
		List<String> sentenceList = new ArrayList<String>();
		for (List<HasWord> sentence : docProc) {
		   String sentenceString = Sentence.listToString(sentence);
		   sentenceList.add(sentenceString.toString());
		}
		

		
		// build the interpretation model
		MetaModel instantiatedModel = new MetaModel(wordnet, snomed, resourcesMapper, postFilePath);
		
		// analyze each sentence and push it to the Meta-Model
		for (String sentence : sentenceList) {
			// build the syntactical tree of the paragraph
			Tree tree = lp.parse(sentence);
			
			// print the sentence and its syntactical tree
			System.out.println("Sentence : " + sentence);
			System.out.println("Parser   : " + tree.toString());
			
			// retrieve all the matching triplets 
			List<MatchedPattern> matchedPatterns =  syntacticalPatternsDB.getMatchingPatterns(tree);
			instantiatedModel.addMatchedPattens(matchedPatterns);

		}
		return instantiatedModel;
	
    }

    /**
     * This function is used for the evaluation purposes of the framework.
     * The generated CUIs can be used to compute the Recall, Precision, Relaxed Accuracy and Strict Accuracy.
     * @param postPath file containing the text to process
     * @param snomedIds list of the medical concepts found
     * @throws IOException
     */
    public void writeExtractedCUIintoAFile(String postPath, List<String> snomedIds) throws IOException {
        // File reader
    	BufferedWriter bufferedWriter = null;               
    	bufferedWriter = new BufferedWriter(new FileWriter(postPath+Eval.RESULT__FILE_EXT));

    	for (int i=0; i < snomedIds.size(); i++) {
    		String snomedId = snomedIds.get(i);
			bufferedWriter.write( this.getCUIofSnomedID(snomedId));
			if ( (i+1) < snomedIds.size() ) bufferedWriter.write(" ");
		}
    	
    	bufferedWriter.close();
    }
    
    /**
     * Used for testing
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
    	MainAnalyzer analyzer = new MainAnalyzer();
    	
    	// calculate the execution time
    	long startTime = System.currentTimeMillis();
    	
    	
    	// use this for testing purposes
		String postPath = "res/posts/test.txt";
    	MetaModel instantiatedModel = analyzer.analyze(postPath);
    	instantiatedModel.draw();
    	
    	
    	/*
    	// extract the concepts found
    	List<String> snomedIds = instantiatedModel.getSnomedIds();
    	for (String string : snomedIds) {
    		System.out.println(string +" <-> "+ analyzer.umls.getCUIofSnomedId(string));
		}
    	//analyzer.writeExtractedCUIintoAFile(postPath, snomedIds);
    	*/
    	
		/*
    	// using this for evaluation purposes   	
		File f = new File(Eval.TRAIN_FOLDER_PATH);
		String[] fileNames = f.list();
		// Reading the file names in the side the folder
		for (String fileName : fileNames) {
			if (fileName.equals("desktop.ini")) continue;
			String filePath = Eval.TRAIN_FOLDER_PATH + "/"+fileName;
			System.out.println("analysing: " + filePath);
	    	MetaModel instantiatedModelGoldCorpus = analyzer.analyze(filePath);
	    	//instantiatedModelGoldCorpus.draw();
	    	List<String> snomedIds = instantiatedModelGoldCorpus.getSnomedIds();
	    	for (String string : snomedIds) {
	    		System.out.println(string +" <-> "+ analyzer.umls.getCUIofSnomedId(string));
			}
    		analyzer.writeExtractedCUIintoAFile(Eval.RESULT_FOLDER_PATH + "/" + fileName, snomedIds);
		}
		// launch the evaluation script
    	Eval.eval();
		*/
		
    	// calculate the execution time
    	long endTime = System.currentTimeMillis();
    	long duration = (endTime - startTime);  //divide by 1000000 to get milliseconds.
    	System.out.println("Total execution time: " + duration + " milliseconds");
    	System.out.println(millisToShortDHMS(duration));
   }

    /**
     * Convert a duration in millisecond to a human readable format
     * @param duration
     * @return
     */
    public static String millisToShortDHMS(long duration) {
    	// source: https://stackoverflow.com/questions/180158/how-do-i-time-a-methods-execution-in-java
        String res = "";    // java.util.concurrent.TimeUnit;
        long days       = TimeUnit.MILLISECONDS.toDays(duration);
        long hours      = TimeUnit.MILLISECONDS.toHours(duration) -
                          TimeUnit.DAYS.toHours(TimeUnit.MILLISECONDS.toDays(duration));
        long minutes    = TimeUnit.MILLISECONDS.toMinutes(duration) -
                          TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(duration));
        long seconds    = TimeUnit.MILLISECONDS.toSeconds(duration) -
                          TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration));
        long millis     = TimeUnit.MILLISECONDS.toMillis(duration) - 
                          TimeUnit.SECONDS.toMillis(TimeUnit.MILLISECONDS.toSeconds(duration));

        if (days == 0)      res = String.format("%02d:%02d:%02d.%04d", hours, minutes, seconds, millis);
        else                res = String.format("%dd %02d:%02d:%02d.%04d", days, hours, minutes, seconds, millis);
        return res;
    }
}
