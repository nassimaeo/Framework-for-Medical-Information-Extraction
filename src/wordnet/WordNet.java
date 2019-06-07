package wordnet;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;




/**
 * 
 * 
 */
public class WordNet {
	private static final String NEWLINE = System.getProperty("line.separator");
	
	private final static String WORDNET_NOUNS_FILE_PATH = "res/wordnet/data.noun";
	private final static String WORDNET_ADJECTIVES_FILE_PATH = "res/wordnet/data.adj";
	private final static String WORDNET_ADVERBS_FILE_PATH = "res/wordnet/data.adv";
	private final static String WORDNET_VERBS_FILE_PATH = "res/wordnet/data.verb";
	
	// This value can be extracted from WordNet. 
	// Run it with a large value of DIGRAPH_SIZE.
	// Then print the size of the Indexer and update DIGRAPH_SIZE.
	private static final int DIGRAPH_SIZE = 117597;
	

	
    // translate synset_offset in WordNet files to indexes for better references in the graph
	private Indexer indexer;

	//<SynsetID> contain the words <Word1, Word2, ...,WordN>
    private HashMap<Integer, List<String>> synsets;
    
    // <WORDS> is present in all the SynSets <SynsetID1, SynsetID2, ... , SynsetIDN>
    private HashMap<String, LinkedList<Integer>> words;
    	
    // links all the Nouns, Adjectives, Adverbs and Verbs of WordNet
    private Digraph wordnetDigraph;

    // SAP object (Shortest Ancestral Path)
    private GraphAlgorithms algorithms;
    
    // used to read WordNet files
    private Scanner in;
    
    /**
     * The constructor takes the name of the two input files.
     * Loads the Entire WordNet including Nouns, Adjectives, Adverbs and Verbs.
     * (This uses only the nouns of WordNet)
  
     * @param nouns path to the file containing Synsets nouns (and their relationships) of WordNet
     * @param adjectives path to the file containing Synsets adjectives (and their relationships) of WordNet
     * @param adverbes path to the file containing Synsets adverbes (and their relationships) of WordNet
     * @param verbs path to the file containing Synsets verbs (and their relationships) of WordNet
     * @throws IOException 
     */
    public WordNet(String nouns, String adjectives, String adverbes, String verbs) throws IOException {
    	wordnetDigraph = new Digraph(DIGRAPH_SIZE);
    	this.indexer = new Indexer();
    	this.synsets = new HashMap<>();
    	this.words = new HashMap<>();

    	loadFile(nouns, synsets, words, POS.NOUN);
    	loadFile(verbs, synsets, words, POS.VERB);
    	loadFile(adverbes, synsets, words, POS.ADV);
    	loadFile(adjectives, synsets, words, POS.ADJ);
       

        // set object used to compute the length(), distance(), etc in the Diagraph
        this.algorithms = new GraphAlgorithms(wordnetDigraph);

        
    	System.out.println("Total number of different synsets: " + this.indexer.size());
    	System.out.println("Total number of words : " + this.words.size());
    }
    

    /**
     * Read a WordNet file data.TYPE (TYPE: verb, noun, adj or adv) and
     * load all the SynSets in them plus the links chosen in WORDNET_RELATIONS.
     * See (wndb.5) for the data structure.
     * All the link are regarded the same. They are are not labeled.
     * @param filePath path to the WordNet data.TYPE file to load
     * @param wordsPerSynset kept only if you want to save word in separate HashMaps
     * @param allWords kept only if you want to save word in separate HashMaps
     * @param posEnum type of file to load
     * @throws IOException
     */
    private void loadFile(String filePath, HashMap<Integer, List<String>> wordsPerSynset, HashMap<String, LinkedList<Integer>> allWords, POS posEnum) throws IOException {
    	System.out.println("[WordNet] "+filePath+" ...");
        BufferedReader fileReader = new BufferedReader(new FileReader(filePath));
        String line;
        for(int i = 0; i < 29; i++) fileReader.readLine(); //skip the license lines
        while ((line = fileReader.readLine()) != null) {
        	in = new Scanner(line);
        	int synset_offset = in.nextInt();
        	in.nextInt(); //lex_filenum ignore
        	in.next(); //IGNORE
        	indexer.addIndex(synset_offset, posEnum);
        	int sourceIndex = indexer.getIndex(synset_offset, posEnum);
        	int w_cnt = Integer.parseInt(in.next(), 16);
        	LinkedList<String> words = new LinkedList<>();
        	for (int i = 0; i<w_cnt; i++){
        		String word = in.next();
        		words.add(word);
        		in.next(); //lex_id ignore
        	}
        	wordsPerSynset.put(sourceIndex, words);
        	for (String string : words) {
        		LinkedList<Integer> refs = allWords.get(string);
        		if (refs == null){
        			refs = new LinkedList<>();
        			refs.add(sourceIndex);
        			allWords.put(string, refs);
        		} else {
        			refs.add(sourceIndex);
        		}
			}
        	int p_cnt = in.nextInt();
        	for (int i = 0; i < p_cnt; i++){
        		String pointer_symbol = in.next();
        		int synset_offset_target = in.nextInt();
        		char pos = in.next().charAt(0);
        		in.next(); //source/target ignore
        		for (WORDNET_RELATIONS relationType : WORDNET_RELATIONS.values()) {
					if (relationType.getType().equals(pointer_symbol)){
						int targetIndex = 0;
						switch (pos) {
						case 'n': targetIndex = indexer.getIndex(synset_offset_target, POS.NOUN); break;
						case 'v': targetIndex = indexer.getIndex(synset_offset_target, POS.VERB); break;
						case 'r': targetIndex = indexer.getIndex(synset_offset_target, POS.ADV); break;
						case 'a': targetIndex = indexer.getIndex(synset_offset_target, POS.ADJ); break;
						case 's': targetIndex = indexer.getIndex(synset_offset_target, POS.ADJ); break;
						default: System.out.println("Unknown char: " + pos); break;
						};
						this.wordnetDigraph.addEdge(sourceIndex, targetIndex, pointer_symbol);
	        			// A character is ambiguous in WordNet (avoid duplicate insertions)
	        			break;
	        		}						
				}
			}
        	//ignore frames for verbs, and gloss for all
        }
        fileReader.close();
        System.out.println("[WordNet] "+filePath+" DONE ");
    }



    /**
     * return all the words available in all the Synsets of WordNet
     * @return an iterator of words list 
     */
    public Iterable<String> words() {
        return this.words.keySet();
    }

    /**
     * Check if a word is present in WordNet.
     * This function is case sensitive.
     * is the word present in WordNet?
     * @param word
     * @return
     */
    public boolean isWord(String word) {
        if (word == null)
            throw new NullPointerException();
        
        return this.words.containsKey(word) ;
    }

    /**
     * distance between nounA and nounB
     * distance(A,B) = distance is the minimum length of
     * any ancestral path between any synset v of A and any synset w of B
     * @param nounA
     * @param nounB
     * @return
     */
    public int distance(String wordA, String wordB) {

        if (wordA == null || wordB == null)
            throw new NullPointerException();
        if (!this.isWord(wordA) || !this.isWord(wordB))
            throw new IllegalArgumentException();
        // initialize the visited nodes
        return this.algorithms.length(this.words.get(wordA), this.words.get(wordB));
    }

    public String sap(String wordA, String wordB) {
        // a synset (second field of synsets.txt) that is the common ancestor of
        // nounA and nounB
        // in a shortest ancestral path (defined below)
        if (wordA == null || wordB == null)
            throw new NullPointerException();
        if (!this.words.containsKey(wordA) || !this.words.containsKey(wordB))
            throw new IllegalArgumentException();

        int commonAncestor = this.algorithms.ancestor(this.words.get(wordA), this.words.get(wordB));
        return this.getSynsetIDName(commonAncestor);
    }

    
    /**
     * Perform a search in the keys (Nouns, Verbs, Adverbs. and Adjectives) of WordNet.
     * If there is a complete CASE_INSENSITIVE matches just return them.
     * If it doesn't exist, return all other possible derivatives
     * @param wordToSearch to search
     * @return list of keys to WordNet Synsets
     */
    public List<String> searchKeys(String wordToSearch) {
    	//String query = ".*(^|_)" + wordToSearch + "($|_).*";
        //String query = ".*(^|_)" + wordToSearch + ".*"; // solve dwell VS dwelling!
        String query = ".*" + wordToSearch + ".*"; // more general pattern
        int flags = 0 + Pattern.CASE_INSENSITIVE;
        Pattern pattern = Pattern.compile(query,flags);
            	
    	List<String> completeMatches = new LinkedList<>();
    	List<String> derivativeMatches = new LinkedList<>();
    	
    	for (String word : this.words()) {
    		Matcher matcher = pattern.matcher(word);
    		if (matcher.matches()){
    			//complete match, return just the matched elements
    			if (wordToSearch.length() == word.length()) {
    				completeMatches.add(word);
    			} else {
    				derivativeMatches.add(word);
    			}
    		}
		}
    	if (completeMatches.size() > 0) return completeMatches;
    	else return derivativeMatches;
    	
    }
    
    /**
     * Get all the synsets id where: word is available
     * @param word the searched word
     * @return list of synset, or empty list if none.
     */
    public List<Integer> getSynsets(String word) {
    	if (word == null) throw new NullPointerException();
    	List<Integer> result = this.words.get(word);
    	if (result == null) {
    		return new LinkedList<>();
    		//throw new IllegalArgumentException(word + " word not found");
    	}else{
    		return result;
    	}
    }

    /**
     * Get all the parents (only parents not ancestors) SynSetID of the SynSet v
     * @param v synsetID
     * @return list of parents synsetIDs
     */
    public Iterable<Edge> getParents(int v) {
    	return this.wordnetDigraph.adj(v);
    }
    
    /**
     * Return all the possible paths to the root.
     * A SynsetID may have many parents, thus many paths to root.
     * 
     * @param v SynsetID 
     * @return all the paths found or empty list if no path found
     */
    public List<LinkedList<Integer>> getPathsToRoot(int v){
    	// the list of a list of paths to the root
    	List<LinkedList<Integer>> result = new LinkedList<LinkedList<Integer>>();

		// For each Parent, get all the possible paths to the root
		// and merge the ancestral paths found with the current vertex
    	for (Edge parentID : this.getParents(v)) {
    		for (LinkedList<Integer> list : this.getPathsToRoot(parentID.index())) {
        		LinkedList<Integer> newPath = new LinkedList<>();
        		newPath.add(v);
        		newPath.addAll(list);
        		result.add(newPath);
			}
		}
    	
		// if it is the root vertex add add the root vertex as the path
    	if (result.size() == 0) {
			LinkedList<Integer> path = new LinkedList<>();
			path.add(v);
			result.add(path);
    	}
		
    	return result;
    }
    
    
    /**
     * Print the name of the SynsetID v
     * @return string representing the nouns in the SynSetID v
     */
    public String getSynsetIDName(int v) {
    	List<String> vWords = this.synsets.get(v);
    	if (vWords == null) 
    		throw new IllegalArgumentException("Index (" + v + ") not found");
    	return String.join(" ", vWords);
    }    
    
    
    public void printGetPathsToRoot(int v) {
    	List<LinkedList<Integer>> result = getPathsToRoot(v);
    	for (LinkedList<Integer> linkedList : result) {
    		System.out.println("Path:");
			for (Integer integer : linkedList) {
				System.out.println(" "+integer + " " + this.getSynsetIDName(integer));
			}
			System.out.println(" [End path]");
		}
    }
    
    public String getStringParents(int v) {
    	StringBuilder result = new StringBuilder();
    	Iterable<Edge> iter = this.getParents(v);
    	for (Edge child : iter) {
			result.append(child + " " + getSynsetIDName(child.index()) + NEWLINE);
		}
    	return result.toString();
    }


    
	public String getStringAllWords() {
		StringBuilder result = new StringBuilder();
		result.append("All Words: (Words) -> [list of synsetID]" + NEWLINE);
		for (String w : this.words()) {
			result.append(w + " -> [ ");
			for (Integer inde : this.words.get(w)){
				result.append(inde + " ");
			}
			result.append("]"+NEWLINE);
		}
		return result.toString();
	}
	
	public String getStringAllSynsets() {
		StringBuilder result = new StringBuilder();
		result.append("All synset: (SynsetID) -> [Words]" + NEWLINE);
		for (Integer i : this.synsets.keySet()) {
			result.append(i + " -> [ ");
			for (String w : this.synsets.get(i)){
				result.append(w + " ");
			}
			result.append("]"+NEWLINE);
		}
		return result.toString();
	}

	public String getStringDigraph() {
		return this.wordnetDigraph.toString();		
	}
	
    /**
     * Loads WordNet from the file data.TYPE
     * where TYPE: {noun, verb, adj, adv}
     * 
     * @return an instance of WordNet
     * @throws IOException
     */
    public static WordNet loadWordNet() throws IOException {
    	System.out.println("Heap size: "+java.lang.Runtime.getRuntime().maxMemory()+" Bytes");
    	return new WordNet(WORDNET_NOUNS_FILE_PATH, WORDNET_ADJECTIVES_FILE_PATH, WORDNET_ADVERBS_FILE_PATH, WORDNET_VERBS_FILE_PATH);
    }

    
    /**
     * do unit testing
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {

    	WordNet wordnet = WordNet.loadWordNet();
    	
    	//wordnet.printAllSynsets();
    	//wordnet.printAllWords();
    	//wordnet.getStringDigraph();
    	
    	while(true){
    		Scanner in = new Scanner(System.in);
    		System.out.println("Input a Word to search: ");
    		String word = in.next();
	    	for (String key : wordnet.searchKeys(word)) {
	    		StringBuilder listSynsetsID = new StringBuilder();
	    		for (Integer id :  wordnet.getSynsets(key)) {
					listSynsetsID.append(id + " ");
					System.out.println("  " + id + " " + wordnet.getSynsetIDName(id));
				}
	    		System.out.println(key + " " +listSynsetsID.toString());
			}
    	}

    	
    	/*
        WordNet wn = WordNet.loadWordNet();

        wn.printGetPathsToRoot(58);
        wn.printGetPathsToRoot(39607);

        wn.printParents(68986);
        wn.printParents(71648);
        
        
        String nounA = "affliction";
        String nounB = "self-pity";
       
        for (String keyA : wn.searchKeys(nounA)) {
            for (String keyB : wn.searchKeys(nounB)) {
            	int distance = wn.distance(keyA, keyB);
            	System.out.print("Distance: [" + distance + "] ");
            	String format = "Common Node Of: [%s(%s)] and [%s(%s)] = [%s]";
            	String print = String.format(format,nounA,keyA,nounB,keyB,wn.sap(keyA, keyB));
                System.out.println(print);
    		}
		}			
    	 */

    }
}
