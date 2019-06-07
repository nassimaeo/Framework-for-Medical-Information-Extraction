package snomed;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import reasoning.Levenshtein;


/**

SNOMED-CT is a medical Ontology.

This class loads SNOMED-CT entirely into memory as a graph of adjacency list
and offers an interface to interact with it.
Where:
     - each Node represent a concept
     - each Edge represent a relationship between two concepts

Distributed in the following three textual files:
     - Concepts: contains all the concepts available
     - Relationship: contains all the relationships available
     - Description: contains the names of the relationships and concepts

    (Other files exist too, but not required for our usage)
*/

public class SNOMED {
	
    private static final float LEVENSHTEIN_THRESHOLD = 0.1f;
	private final static String CONCEPTS_FILE_PATH = "res/snomed/sct2_Concept_Snapshot_INT_20150731.txt";
    private final static String RELATIONSHIPS_FILE_PATH = "res/snomed/sct2_Relationship_Snapshot_INT_20150731.txt";
    private final static String DESCRIPTIONS_FILE_PATH = "res/snomed/sct2_Description_Snapshot-en_INT_20150731.txt";
    
    public enum SearchMethod {REG_EXP, LEVENSHTEIN};
    
    /**
     * 
     * Each concept or relationship has : - A primary name called Fully
     * Specified Name (Preferred term) - And a set of synonyms names. We store
     * these names in this class Nomenclature.
     * 
     * @author Nassim
     *
     */
    private class Nomenclature {
        private String fsn; // fully specified name of the concept
        private LinkedList<String> synonyms; // list of synonyms of the concept

        public Nomenclature() {
            this.synonyms = new LinkedList<String>();
        }

        public String getFNS() {
            return this.fsn;
        }

        public LinkedList<String> getSynonyms() {
            return this.synonyms;
        }

        public void setFNS(String s) {
            this.fsn = s;
        }

        public void addSynonym(String s) {
            this.synonyms.add(s);
        }

        public String toString() {
            StringBuilder res = new StringBuilder();
            res.append("FSN:  ");
            res.append(this.fsn + "\n");
            for (String s : synonyms) {
                res.append("SYN:  " + s + "\n");
            }
            return res.toString();
        }
    }

    /**
     * The list of the names of each concept
     * From concept ID, to its nomenclature
     */
    HashMap<Long, Nomenclature> nomenclatureOfConcepts;

    /**
     * The list of the names of each relationship
     * From concept ID, to its nomenclature
     */
    HashMap<Long, Nomenclature> nomenclatureOfRelationships;
    
    /**
     * List of codes of the relationships
     */
    HashSet<Long> relationshipsCodes;

    /**
     * 
     * A Node represents a concept in SNOMED-CT, - We keep its original id; - We
     * associate a new sequential id called index. To lookup the concept in a
     * constant time; - We build the Edge of each concepts;
     * 
     * NB: The name of the concept is saved the variable nomenclature.
     * 
     * @author Nassim
     *
     */
    private class Node {
        private int index; // staring from 0 .. N : Total number of concepts
        private long id; // the id of the concept in SNOMED
        private LinkedList<Edge> relationships; // the list of the relationships
                                                // going out from the current
                                                // node

        public Node(int index, long id) {
            this.index = index;
            this.id = id;
            this.relationships = new LinkedList<Edge>();
        }

        public int getIndex() {
            return this.index;
        }

        public long getId() {
            return id;
        }

        public LinkedList<Edge> getRelationships() {
            return this.relationships;
        }

        public void addEdge(Edge e) {
            this.relationships.add(e);
        }
    }

    /**
     * 
     * An Edge represent a relationship between two Nodes (or concepts) in
     * SNOMED - We keep the original id only (No need to index edges); - We save
     * the relationshipGroup to group edge of a source Node together as defined
     * in SNOMED - We save the typeId also. (the type of the relationship: is-a,
     * ...)
     * 
     * NB: The name of the relationship is saved the variable nomenclature. 
     * 
     * @author Nassim
     *
     */
    private class Edge {
        private long id; // the id of the relationship in SNOMED
        private long relationshipGroup; // grouping relationships
        private long typeId; // is-a: 116680003, occurence 246454002 ..., ...
        private Node next; // the concept to where the relationship is pointing

        public Edge(long id, Node next, long typeId, long relationshipGroup) {
            this.id = id;
            this.relationshipGroup = relationshipGroup;
            this.typeId = typeId;
            this.next = next;
        }

        public long getId() {
            return this.id;
        }

        public long getRelationshipGroup() {
            return this.relationshipGroup;
        }

        public long getTypeId() {
            return this.typeId;
        }

        public boolean isA() {
            return this.typeId == 116680003;
        }
    }

    /**
     * This is the adjacency list of the graph SNOMED-CT Staring from [0 .. N-1]
     * : where N = Total number of concepts
     */
    private Node[] concepts;

    /**
     * Used to get the index number of a concept id to improve the lookup of
     * concepts indexes
     */
    private HashMap<Long, Integer> reversedIndex;

    /**
     * Total number of relationships
     */
    int R;

	private static Scanner in;

    /**
     * 
     * The constructor loads SNOMED into memory from three textual files of
     * SNOMED-CT.
     * 
     * @param concepts file containing_the_concepts
     * @param relationships file_containing_the_relationships
     * @param descriptions file_contains_the_descriptions
     * @throws IOException
     */
    public SNOMED(String conceptsFile, String relationshipsFile, String descriptionsFile) throws IOException {
        /*
         * ====================
         * Loading the concepts 
         * ====================
         * id (conceptId) *
         * effectiveTime
         * active
         * moduleId
         * definitionStatusId
         * ====================
         * 
         * Retrieve only the conceptId from this table.
         * All the other fields are not relevant for us.
         * 
         */

        // Load the concepts table from the file
        ConceptsTable c = new ConceptsTable(conceptsFile);

        // Initialize the reversedIndex of concepts id -> index
        reversedIndex = new HashMap<Long, Integer>(); 

        // total number of active concepts in SNOMED-CT
        int N = c.size();
        
        // Initializing the list of concepts
        this.concepts = new Node[N];
        
        // Iterate throw the rows of concepts
        Iterator<Concept> cptIterator = c.iterator();
        
        // Build the sequential index
        int i = 0;
        while (cptIterator.hasNext()) {
            // get the next concept
            Concept cpt = cptIterator.next();
            
            // build the node: index -> id
            Node n = new Node(i, cpt.getConceptId());
            
            // build the reversedIndex
            reversedIndex.put(cpt.getConceptId(), i);
            
            // assign the node (id, index, relationships) to its assigned index
            this.concepts[i] = n;
            i++;
        }
        c = null; // free the memory

        /*
         * =========================
         * Loading the relationships
         * =========================
         * id (relationshipId) *
         * effectiveTime active
         * moduleId
         * sourceId
         * destinationId
         * relationshipGroup
         * typeId
         * characteristicTypeId 
         * moduleId 
         * =========================
         */

        // keep track of the list of relationships available in SNOMED.
        relationshipsCodes = new HashSet<Long>();

        // count the number of relationships in the graph
        R = 0;

        // Load the relationships table from the file
        RelationshipsTable r = new RelationshipsTable(relationshipsFile);

        Iterator<Relationship> relIterator = r.iterator();
        while (relIterator.hasNext()) {
            Relationship rel = relIterator.next();
            
            // get the index of the starting node (concept) of the relationship
            int indexFrom = reversedIndex.get(rel.getSourceId());
            
            // get the index of the finish node (concept) of the relationship
            int indexTo = reversedIndex.get(rel.getDestinationId());
            
            // build the edge (reltionId, conceptIdTo, relationTypeId, relationCharacteTypeId)
            Edge edge = new Edge(rel.getId(), concepts[indexTo], rel.getTypeId(), rel.getCharacteristicTypeId());
            
            // add the edge to the list of edges of the conceptIdFrom
            concepts[indexFrom].addEdge(edge);
            
            // keep the list of all the available relationships
            relationshipsCodes.add(rel.getTypeId());
            
            // count the number of relationships
            R++; 
        }

        /*
         * ========================
         * Loading the descriptions
         * ======================== 
         * id (descriptionId) 
         * effectiveTime 
         * active
         * moduleId 
         * conceptId 
         * languageCode 
         * typeId 
         * term 
         * caseSignificanceId
         * ========================
         */

        // Initialize the list of names for concepts and relationships
        nomenclatureOfConcepts = new HashMap<Long, Nomenclature>();
        nomenclatureOfRelationships = new HashMap<Long, Nomenclature>();

        // Load the descriptions table from the file
        DescriptionsTable d = new DescriptionsTable(descriptionsFile);

        Iterator<Description> dstIterator = d.iterator();
        int counter = 0;
        while (dstIterator.hasNext()) {
        	//TODO remove this (used for testing performances with different Ontology sizes
        	//if (counter++ == 300000) break;
            Description dst = dstIterator.next();
            Long conceptId = dst.getConceptId();

            // Check if it is a concept or relationship nomenclature
            if(relationshipsCodes.contains(conceptId)){
                // It's a relationship nomenclature
                // initialize the nomenclature if the concept is seen for the 1st time
                if (!this.nomenclatureOfRelationships.containsKey(conceptId)) {
                    Nomenclature nmc = new Nomenclature();
                    this.nomenclatureOfRelationships.put(conceptId, nmc);
                }
                // add the name to the list of nomenclatures (concept or relationship)
                if (dst.isFSN())
                    // add it as Fully Specified Name
                    this.nomenclatureOfRelationships.get(conceptId).setFNS(dst.getTerm());
                else
                    // add it as a Synonym
                    this.nomenclatureOfRelationships.get(conceptId).addSynonym(dst.getTerm());
            }
            else
            {
                // It's a concept nomenclature
                // Make sure the concept is still active
                // (because some descriptions are active even if the concept is not)
                if (reversedIndex.containsKey(conceptId)) {
                    // initialize the nomenclature if the concept is seen for the 1st time
                    if (!this.nomenclatureOfConcepts.containsKey(conceptId)) {
                        Nomenclature nmc = new Nomenclature();
                        this.nomenclatureOfConcepts.put(conceptId, nmc);
                    }
                    // add the name to the list of nomenclatures (concept or relationship)
                    if (dst.isFSN())
                        // add it as Fully Specified Name
                        this.nomenclatureOfConcepts.get(conceptId).setFNS(dst.getTerm());
                    else
                        // add it as a Synonym
                        this.nomenclatureOfConcepts.get(conceptId).addSynonym(dst.getTerm());
                }
            }
            
        }
        d = null; // free the memory.

    }

    /**
     * 
     * Return the closest common concept in the IS-A hierarchy of SNOMED linking
     * conceptA and conceptB together.
     * 
     * @param conceptA
     * @param conceptB
     * @return closestAncestralConcept
     */
    public Node closestAncestralConcept(Node conceptA, Node conceptB) {
        return null;
    }

    /**
     * 
     * Return the shortest ancestral path in the IS-A hierarchy of SNOMED
     * linking conceptA and conceptB together.
     * 
     * @param conceptA
     * @param conceptB
     * @return lsitOfConceptsInTheShortestAncestralPath
     */
    public Node[] shortestAncestralPath(Node conceptA, Node conceptB) {
        return null;
    }

    /**
     * 
     * Returns the shortest path between conceptA and conceptB using all the
     * relationships offered by SNOMED (IS-A, partOf, etc)
     * 
     * @param conceptA
     * @param conceptB
     * @return listOfConceptsInThePath
     */
    public Node[] getShortestPath(Node conceptA, Node conceptB) {
        return null;
    }

    /**
     * 
     * Returns the list of concepts where the searchWord appears
     * 
     * @param searchWord
     * @return conceptsWhereSearchWordAppears
     */
    public Node[] getConcepts(String searchWord) {
        return null;
    }

    /**
     * 
     * Returns the list of relationships where the word searchWord Appears The
     * set of relationships are known in SNOMED, like partOf, location, ... This
     * function is not very useful in this case.
     * 
     * @param searchWord
     * @return relationshipsWhereSearchWordAppears
     */
    public Edge[] getRelationships(String searchWord) {
        return null;
    }

    /**
     * Returns the total number of Relationships
     */
    private int R() {
        return R;
    }

    /**
     * Returns the total number of Concepts
     */
    private int C() {
        return this.concepts.length;
    }

    /**
     * Print the whole list of relationships available in SNOMED
     */
    public void printRelationships() {
        /*
        System.out.println("Total number of relationships:" + this.R());
        System.out.println("Number of relationship types: " + this.relationshipsCodes.size());
        for (long lc : this.relationshipsCodes) {
            System.out.println("Code: " + lc + "\n" + this.nomenclature.get(lc));
        }
        */
        System.out.println("Total number of relationships:" + this.R());
        System.out.println("Number of relationship types: " + this.nomenclatureOfRelationships.size());
        for (Long lc : this.nomenclatureOfRelationships.keySet()) {
            System.out.println("Code: " + lc + "\n" + this.nomenclatureOfRelationships.get(lc));
        }
    }

    /**
     * Find all the nodes that are crossed on the way up to the root
     * including multi-parents nodes. The path is not organized.
     * @param snomedIdFromWord
     * @return the list of codes from the snomedIdStartNode to the root
     */
	public HashSet<String> getNodesToRoot(Long snomedIdStartNode) {
		
		HashSet<String> result = new HashSet<String>();
		HashSet<Long> visited = new HashSet<>();
		Stack<Long> stack = new Stack<>();
		stack.push(snomedIdStartNode);
		
        while (!stack.isEmpty()) {
        	Long current = stack.pop();
        	result.add(current.toString());
        	visited.add(current);
            for (Edge e : this.concepts[this.reversedIndex.get(current)].getRelationships()) {
                if (e.isA()) {
                    Long next = e.next.getId();
                    if (!visited.contains(next))
                    	stack.push(next);
                }
            }
        }
		return result;
	}
	
	
    /**
     * (Helper function) Print the path from the concept id to the root. Prints
     * only the first found path. (Many path may exists because a concept can
     * have many parents)
     */
    private void getPathToRoot(int index) {
        System.out.println("Concept ID: " + this.concepts[index].getId());
        System.out.println(this.nomenclatureOfConcepts.get(this.concepts[index].getId()));
        System.out.println("Relationships:");
        boolean root = false;
        while (!root) {
            root = true;
            for (Edge e : this.concepts[index].getRelationships()) {
                if (e.isA()) {
                    System.out.println("Relationship:");
                    System.out.println(this.nomenclatureOfRelationships.get(e.getTypeId()));
                    System.out.println("Concept:");
                    System.out.println(this.nomenclatureOfConcepts.get(e.next.getId()));
                    index = this.reversedIndex.get(e.next.getId());
                    root = false;
                    // TODO find only one path ! change the break to get all the
                    // paths
                    break;
                }
            }
        }
    }

    /**
     * Print the path from the concept index (1..N) to the root. N = total
     * number of concepts. Prints only the first found path. (Many path may
     * exists because a concept can have many parents)
     */
    public void getPathToRootIndex() {
        System.out.println("(Path to Root) Introduce an Integer between [0 - " + this.C() + "]: ");
        in = new Scanner(System.in);
        int input = in.nextInt();
        this.getPathToRoot(input);
    }

    /**
     * Print the path from the concept id to the root. Prints only the first
     * found path. (Many path may exists because a concept can have many
     * parents)
     */
    public void getPathToRootId() {
        System.out.println("(Path to Root) Introduce the concept Id or something else to abort: ");
        in = new Scanner(System.in);
        long input = in.nextLong();
        if (this.reversedIndex.containsKey(input)) {
            int index = this.reversedIndex.get(input);
            this.getPathToRoot(index);
        } else {
            System.out.println("Unkown concept: " + input);
        }
    }


	
	
    /**
     * Search for a word in the list of concepts names
     */
    public void searchWords() {
        System.out.println("Search for the word : ");
        in = new Scanner(System.in);
        String query = in.next();
        if (query.length() > 0) {
            System.out.println("Seaching for:" + query);
            for (Long n : this.nomenclatureOfConcepts.keySet()) {
                if (this.searchWithRegExp(this.nomenclatureOfConcepts.get(n), query)) {
                    if (!this.reversedIndex.containsKey(n))
                        System.out.print("[Unlinked Term]");
                    if (this.relationshipsCodes.contains(n))
                        // remove this line later (we are searching only the concepts)
                        System.out.println("Rel:" + n + "\t" + this.nomenclatureOfRelationships.get(n).getFNS());
                    else
                        System.out.println("Cpt:" + n + "\t" + this.nomenclatureOfConcepts.get(n).getFNS());
                }
            }
        }
    }

    /**
     * Search for concepts where a list of words appears in the
     * concepts names
     */
    public void searchAlistOfwords() {
        // initialize the list of words
        List<String> words = new LinkedList<String>();
        in = new Scanner(System.in);
        
        // reading the list of words
        while (true) {
            System.out.println("List of words (stop to stop) : ");
            String word = in.next();
            if (word.equals("stop"))
                break;
            
            words.add(word);
        }
        
        HashMap<String,Object> results = this.getMatchesForListOfWords(words, SearchMethod.REG_EXP);
        HashSet<String> result = (HashSet<String>) results.get("SNOMED_IDs");
    	
        System.out.println(result.size() + " Match(s)");
        for (String string : result) {
            System.out.println(" -> " + string + "\t" + this.nomenclatureOfConcepts.get(Long.parseLong(string)).getFNS());
        }
        
    }
    
    /**
     * Search all the concepts (and relationships after adaptation) 
     * where all the words appear using a cumulative search,
     * i.e. all words must appear in the concept's nomenclature
     * 
     * @param words
     */
    public HashMap<String, Object> getMatchesForListOfWords(List<String> words, SearchMethod searchMethod) {
    	
    	/*
    	System.out.print(" ---- > ToSEARCH ");
    	for (String string : words) {
			System.out.print(string  + " ");
		}
    	System.out.println(" ");
    	*/
    	
        // Intersect the sets returned for each word queried
        HashSet<String> intersection = new HashSet<String>();

        // Keep only the best value of Levenshtein, in case of equality take the longer
        double bestLevenshteinValueFound = 1;
        double levenshteinValueFound = 1;
        
        // Don't intersect for the first word
        boolean firstWord = true;
        for (String word : words) {
            // Build the temporary result of the query
            HashSet<String> newIntersection = new HashSet<String>();
            System.out.println("Seaching for: " + word);
            
            // Search only in the concepts name (not in the relationships)
            for (Long n : this.nomenclatureOfConcepts.keySet()) {
            	
            	// compare word to nomenclature using (Reg_Exp, or Levenshtein)
            	boolean searchResult = false;
				switch (searchMethod) {
					case REG_EXP:
						searchResult = this.searchWithRegExp(this.nomenclatureOfConcepts.get(n), word);
					break;
					case LEVENSHTEIN:
						levenshteinValueFound = this.searchWithLevenshtein(this.nomenclatureOfConcepts.get(n), word);
				        if (levenshteinValueFound < LEVENSHTEIN_THRESHOLD) {
				        	searchResult = true;
				        } else {
				        	searchResult = false;				        	
				        }
					break;
				}
            	
            	
                if (searchResult) {
                    if (this.relationshipsCodes.contains(n)) {
                        //remove this test later! (only concepts)
                        //String name = "Rel:" + n + "\t" +this.nomenclatureOfRelationships.get(n).getFNS();
                        //relationship
                        //if (firstWord || intersection.contains(name) ) newIntersection.add(name);                                
                    } else {
                        //String name = "Cpt:" + n + "\t" +this.nomenclatureOfConcepts.get(n).getFNS();
                    	String snomed_id = n.toString();
                        // concept
                        if (firstWord || intersection.contains(snomed_id) ) {
                        	
            				switch (searchMethod) {
        					case REG_EXP: newIntersection.add(snomed_id); break;
        					case LEVENSHTEIN:
        						// add only the best match
        						/*
        						if (bestLevenshteinValueFound > levenshteinValueFound) {
        							bestLevenshteinValueFound = levenshteinValueFound;
        							newIntersection.clear();
        							newIntersection.add(snomed_id);
        						}
        						*/
        						//add all found matches
        						newIntersection.add(snomed_id);
        						break;
            				}
            				
                        	
                        }
                    }
                }
            }
            firstWord = false;
            intersection = newIntersection;
        }
        
    	HashMap<String, Object> result = new HashMap<>();
    	
    	result.put("SNOMED_IDs", intersection);
    	result.put("LEVENSHTEIN_VAL", new Double(bestLevenshteinValueFound));
    	
        return result;
    }

    /**
     * Print the list of relationships and concepts coming out from conceptId.
     */
    public void getAllOutneighborsId() {
        System.out.println("(Out Neighbors) Introduce the concept Id or something else to abort: ");
        in = new Scanner(System.in);
        long input = in.nextLong();
        if (this.relationshipsCodes.contains(input)) {
            System.out.println("It's a relationship: ");
            System.out.println(this.nomenclatureOfRelationships.get(input));
        } else {
            if (this.reversedIndex.containsKey(input)) {
                int index = this.reversedIndex.get(input);
                this.getAllOutneighbors(index);
            } else {
                System.out.println("Unkown concept: " + input);
            }
        }
    }

    /**
     * Print the list of relationships and concepts coming out of the concept
     * index.
     */
    public void getAllOutneighborsIndex() {
        System.out.println("(Out Neighbors) Introduce an Integer between [0 - " + (this.C() - 1)
                + "] or something else to abort: ");
        in = new Scanner(System.in);
        int input = in.nextInt();
        if (input >= 0 && input <= this.C() - 1) {
            this.getAllOutneighbors(input);
        } else {
            System.out.println("Abort. Input: " + input);
        }
    }

    /**
     * (Helper function) Print the list of relationships and concepts coming out
     * of a concept index.
     */
    private void getAllOutneighbors(int index) {
        System.out.println("Concept ID: " + this.concepts[index].getId());
        System.out.println(this.nomenclatureOfConcepts.get(this.concepts[index].getId()));
        System.out.println("Relationships:");
        for (Edge e : this.concepts[index].getRelationships()) {
            System.out.println("Relationship:");
            System.out.println(this.nomenclatureOfRelationships.get(e.getTypeId()));
            System.out.println("Concept:");
            System.out.println(this.nomenclatureOfConcepts.get(e.next.getId()));
        }
    }

    /**
     * (Helper function)
     * 
     * Search for a word in a nomonclature of a concept or relationship
     * 
     * @param nomenclature
     * @param query
     * @return
     */
    private boolean searchWithRegExp(Nomenclature nomenclature, String word) {
        String fsn = nomenclature.getFNS();
        
    	// Search using regular expressions
    	
        String q = ".*" + word + ".*";
        int flags = 0;
        flags += Pattern.CASE_INSENSITIVE;
        Pattern p = Pattern.compile(q,flags);
        Matcher m = p.matcher(fsn);

        if (m.matches())
            return true;
        for (String input : nomenclature.getSynonyms()) {
            m = p.matcher(input);
            if (m.matches())
                return true;
        }
        return false;
    }
    
    private double searchWithLevenshtein(Nomenclature nomenclature, String word) {    	
        // Search using Levenshtein distance
    	String fsn = nomenclature.getFNS();
    	
    	//TODO remove this later (used only with incomplete trimmed ontologies  test different sizes)
    	//if (fsn == null) return 0;
    	
        String s1 = fsn.toLowerCase();
        String s2 = word.toLowerCase();
        double bestVal = 1;
        double tmpVal = Levenshtein.distanceNormalized(s1.toLowerCase(), s2.toLowerCase());
        if (tmpVal < bestVal)
        	bestVal = tmpVal;
        
        for (String input : nomenclature.getSynonyms()) {
        	tmpVal = Levenshtein.distanceNormalized(input.toLowerCase(), s2.toLowerCase());
        	if (tmpVal < bestVal)
        		bestVal = tmpVal;        			
        }
        
        return bestVal;
    }

    public static SNOMED loadSnomed() throws IOException {
    	System.out.println("Heap size: "+java.lang.Runtime.getRuntime().maxMemory()+" Bytes");
    	
        // Load the Snapshot version of SNOMED-CT    	      
        SNOMED snomed = new SNOMED(CONCEPTS_FILE_PATH, RELATIONSHIPS_FILE_PATH, DESCRIPTIONS_FILE_PATH);
        
        System.out.printf("Total number of concepts = %d \n", snomed.C());
        System.out.printf("Total number of relationships = %d \n", snomed.R());
        return snomed;
    }
    

    
    
    
    
    /**
     * Search for concepts where a list of words appears in the
     * concepts names
     */
    public void searchSnomedConceptsInsideAlistOfwords() {
        // initialize the list of words from where to extract information
        List<String> wordsList = new LinkedList<String>();
        in = new Scanner(System.in);
        
        // reading the list of words and formatting them
        while (true) {
            System.out.println("List of words ( (End of Report) to stop) : ");
            String line = in.nextLine();
            if (line.equals("(End of Report)")) {
                break;
            }
            else {
                line.replaceAll("-", " ");
                line.replaceAll(",", " ");
                line.replaceAll(".", " ");
                String[] listOfWords = line.split(" ");
                for (String string : listOfWords) {
                	wordsList.add(string);	
				}
            }
            
        }
        
        // put the list of words into a String[]
        String[] words = new String[wordsList.size()];
        words = wordsList.toArray(words);
        
        // search the possible concepts cited in the words.
        
        //List<String> result = this.getAllFoundConceptsSweep(words);
        List<String> result = this.getAllFoundConceptsNoSweep(words);
        
        // print the found matches
        System.out.println(result.size() + " Match(s)");
        for (String string : result) {
            System.out.println(" -> " + string + "\t" + this.nomenclatureOfConcepts.get(Long.parseLong(string)).getFNS());
        }
        
    }
    
    
    
    /**
     * We sweep the words from left to right 
     * We increase the number of words to query each time until no result is returned.
     * We use START word and END word. Start <= END.
     * If we had a list of concepts found, than we return the result.
     * 
     * @param words a list of words contained in a medical document
     * @return a list of all possible concepts found in the list of words
     */
    public List<String> getAllFoundConceptsSweep(String[] words){
    	
    	// sort the list of concepts found by SIMILARITY in case of equality take the longer chaine
    	
    	// initialize the list containing the founds concepts
    	List<String> snomedConceptsFound = new LinkedList<>();
    	
    	// keep track of the best Levenshtein value so far
    	double bestLevenshteinValue = 1;
    	
    	// start the first words, and build the longest chain of words which returns a result
    	// if not result increase the start the repeat the process until the start words is last word
    	for (int startWordPos = 0; startWordPos < words.length; startWordPos++) {
    		
    		// save the previous found result of the longest chain so far
			HashSet<String> previousChaineCollectedCpt = new HashSet<String>();
			
			// the chain of words
			String chaine = "";
			
			// each time add a new word to the chain and query for results
    		for (int endWordPos = startWordPos; endWordPos < words.length; endWordPos++) {
    			chaine = chaine + words[endWordPos] + " ";
    			System.out.println("chaine: " + chaine);
    			
    			// put the chain in a list(one element)
    			List<String> chaineToQuery = new LinkedList<String>();
    			chaineToQuery.add(chaine.trim());
    			HashMap<String,Object> results = getMatchesForListOfWords(chaineToQuery, SearchMethod.LEVENSHTEIN);
    			HashSet<String> tmpRes = (HashSet<String>) results.get("SNOMED_IDs");
    			double levenshteinValue = (double) results.get("LEVENSHTEIN_VAL");
    	    	
    			if (bestLevenshteinValue < levenshteinValue) {
    				continue;
    			} else {
    				bestLevenshteinValue = levenshteinValue;
    			}
    			
    			// save the results
    			if ( tmpRes.size() != 0 ) {
    				previousChaineCollectedCpt.clear();
    				previousChaineCollectedCpt.addAll(tmpRes);
    			}
    			
    			// if we don't get any result or we reach the end of the chain
				if ( tmpRes.size() == 0 || endWordPos == (words.length-1) ) {
					snomedConceptsFound.addAll(previousChaineCollectedCpt);
					System.out.println("[" + chaine.trim() + "] : " + previousChaineCollectedCpt.size() + " (concepts found)");
					for (String string : previousChaineCollectedCpt) System.out.println("SNOMED_ID: " + string);
					break;
				}
				
				//go to the next chain
				startWordPos++;
				
    		}
    	}
		return snomedConceptsFound;
    }
    

    /**
     * Merge all the words into one separated by space.
     * If we had a list of concepts found, than we return the result.
     * 
     * @param words a list of words contained in a medical document
     * @return a list of all possible concepts found in the list of words
     */
    public List<String> getAllFoundConceptsNoSweep(String[] words){
    	
    	// initialize the list containing the founds concepts
    	List<String> snomedConceptsFound = new LinkedList<>();
    	
    	// keep track of the best Levenshtein value so far
    	String chaine = "";

    	for (int i = 0; i < words.length; i++) {
    		chaine = chaine + words[i] + " ";
    	}
    	
		List<String> chaineToQuery = new LinkedList<String>();
		chaineToQuery.add(chaine.trim());
		HashMap<String,Object> results = getMatchesForListOfWords(chaineToQuery, SearchMethod.LEVENSHTEIN);
		HashSet<String> tmpRes = (HashSet<String>) results.get("SNOMED_IDs");
		double levenshteinValue = (double) results.get("LEVENSHTEIN_VAL");

		// if we don't get any result or we reach the end of the chain
		if ( tmpRes.size() != 0 ) {
			snomedConceptsFound.addAll(tmpRes);
			System.out.println("[" + chaine.trim() + "] : " + tmpRes.size() + " (concepts found)");
			for (String string : tmpRes) System.out.println("SNOMED_ID: " + string);
		}
				

		return snomedConceptsFound;
    }    
    
    
    // Used for tests and debugging
    public static void main(String[] args) throws IOException {


    	SNOMED snomed = SNOMED.loadSnomed();
    	

        while (true) {
            try {
                System.out.println("Choose from the list below:"
                		+ "\n0:stop"
                		+ "\n1:Get all the out-neighbors of a concept index."
                        + "\n2:Get all the out-neighbors of a concept id."
                        + "\n3:Search for the concepts where the word appears"
                        + "\n4:Get the path to the root of a concept index"
                        + "\n5:Get the path to the root of a concept id"
                        + "\n6:Print the list of relationships and their names" 
                        + "\n7:Search for a list of words: "
                        + "\n8:Search concepts in a list of lines '(End of Report)' to end");

                in = new Scanner(System.in);
            	int choice = in.nextInt();

            	if (choice == 0)
            		break;
            	
                if (choice == 1)
                    // Get all the out-neighbors of a concept index
                    snomed.getAllOutneighborsIndex();

                if (choice == 2)
                    // Get all the out-neighbors of a concept id
                    snomed.getAllOutneighborsId();

                if (choice == 3)
                    // Search for the concepts where the word appears
                    snomed.searchWords();

                if (choice == 4)
                    // Get the path to the root of a concept.
                    snomed.getPathToRootIndex();

                if (choice == 5)
                    // Get the path to the root of a concept.
                    snomed.getPathToRootId();

                if (choice == 6)
                    // Print the list of relationships and their names
                    snomed.printRelationships();

                if (choice == 7)
                    // Print the list of relationships and their names
                    snomed.searchAlistOfwords();

                if (choice == 8)
                    // Print the list of concepts found in a chaine of characters and their names
                	snomed.searchSnomedConceptsInsideAlistOfwords();
                
            } catch (Exception e) {
                e.printStackTrace();
                break;
            }
        }
    }

}