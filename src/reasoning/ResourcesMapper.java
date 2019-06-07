package reasoning;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * This class deals with the mapping between the Meta-Model and the expert knowledge 
 * + the layman vocabulary. It establishes the links between the Concepts and Relationships 
 * of the MetaModel with SNOMED concepts or WordNet SynSets. 
 * 
 * (Additional resources can be added here).
 * 
 * The mapping is done manually by the knowledge engineer by creating text files
 * that establishes explicitly the links between the concepts and relationships
 * in the Meta-Model with the other resources.
 */

public class ResourcesMapper {


	/**
	 * Different kind of resources mappings
	 *   wn_: WordNet SynSets
	 *   sn_: SNOMED-CT id_codes
	 *   ow_: other words defined explicitly
	 *   nl_: can be null
	 */
	public enum RESOURCE_TYPE {wn, sn, ow, nl};

	/**
	 * For each entity of the Meta-Model, store all its mappings in a EntityLinks object
	 */
	private class EntityLinks {
		
		// each resource type has a set of codes or words associated to it
		List<String>[] mappings;
		
		@SuppressWarnings("unchecked")
		public EntityLinks() {
			int length = RESOURCE_TYPE.values().length;
			this.mappings = new LinkedList[length];
		}
		
		/**
		 * Add a code to the specified resource type
		 * @param type of the resource
		 * @param value the code or word to add
		 */
		public void add(RESOURCE_TYPE type, String value) {
			if (this.mappings[type.ordinal()] == null) {
				this.mappings[type.ordinal()] = new LinkedList<String>();
			}
			this.mappings[type.ordinal()].add(value);
		}
		
		/**
		 * get the list of codes associated with the resources type.
		 * if not null, return an empty list for nl_
		 * @param type of the resources (wn, sn, nl, ow)
		 * @return a list of codes or words
		 */
		public List<String> get(RESOURCE_TYPE type) {
			return this.mappings[type.ordinal()];
		}
		
		/**
		 * build the list of codes for each resource
		 * @return the list of codes for each resource type
		 */
		public String print() {
			StringBuilder result = new StringBuilder();
			for (RESOURCE_TYPE type : RESOURCE_TYPE.values()) {
				String codes = null;
				if (mappings[type.ordinal()] == null) codes = "empty";
				else codes = String.join(", ", this.get(type));
				result.append(type + " : " + codes + "\n");
			}
			return result.toString();
		}
	}

	// Paths to the files containing the manually established mapping
	private static String MAPPING_FILE_PATH = "res/mapping/mapping.txt";
	
	// Mapping Meta-Model Classes to WordNet, SNOMED, null or other words
	Map<String, EntityLinks> mapping;
	

	/**
	 * Loading the mapping of the Meta-Model concepts and relationships
	 * with the resources (SNOMED-CT, WordNet)
	 */
	public ResourcesMapper() throws IOException {
		// load the mapping between the Meta-Model and the resources (SNOMED and WORDNET)
		this.loadMapping();
	}

	/**
	 * Load the mapping between the meta-model entities (concepts and relationships)
	 * with the resources (WordNet and SNOMED) 
	 * @throws IOException 
	 */
	private void loadMapping() throws IOException {
		// initialize the mapping structure
		this.mapping = new HashMap<String, EntityLinks> ();
		
		FileReader conceptsMappingFile = new FileReader(new File(MAPPING_FILE_PATH));	
		BufferedReader fileReader  = new BufferedReader(conceptsMappingFile);
		String line;
		EntityLinks entityLinks = null;
		while ((line = fileReader.readLine()) != null){
			// empty line
			if (line.length() == 0) continue;
			
			// comment
			if (line.startsWith("#")) continue;
			
			// get the start code (mm_, wn_, sn_, nl_, ow_)
			String startCode = line.substring(0, 3);

			if (startCode.equals("mm_")) {
				// create a new entry for the entity
				String entityCode = line.substring(3);
				entityLinks = new EntityLinks();
				this.mapping.put(entityCode, entityLinks);
			} else {
				String resourceCode = line.substring(3);
				if (startCode.equals("wn_")) {
					entityLinks.add(RESOURCE_TYPE.wn, resourceCode);
				} else if(startCode.equals("sn_")){
					entityLinks.add(RESOURCE_TYPE.sn, resourceCode);
				} else if(startCode.equals("nl_")){
					entityLinks.add(RESOURCE_TYPE.nl, resourceCode);
				} else if(startCode.equals("ow_")){
					entityLinks.add(RESOURCE_TYPE.ow, resourceCode);
				} else {
					fileReader.close();
					throw new IOException(line);
				}
			}
		}
		fileReader.close();
		
		this.printMapping();
	}	
	
	/**
	 * Print the mapping structure of the meta-model with the other resources
	 */
	public void printMapping(){
		for (String key  : this.mapping.keySet()) {
			System.out.println(key + " :\n" + this.mapping.get(key).print());
		}
	}
	
	/**
	 * Search in the concepts and relationships mapping files for the WordNet Synset
	 * corresponding to the entity from the Meta-Model. For example, PERSON, ORGAN, etc
	 * @param w is either a SUBJECT, OBJTECT or PREDICATE (PERSON, SUFFER, etc)
	 * @return list of WordNet Synset codes to where w is associated or null if none
	 */
	public List<String> wordnetSynset(String entity) {
		if (this.mapping.containsKey(entity))
			return this.mapping.get(entity).get(RESOURCE_TYPE.wn);
		else
			throw new IllegalArgumentException();
	}
	
	/**
	 * Search in the concepts and relationships mapping files for the SNOMED-CT Code
	 * corresponding to the entity from the meta-model. For example, PERSON, ORGAN, etc
	 * @param entity is either a SUBJECT, OBJTECT or PREDICATE (PERSON, SUFFER, etc)
	 * @return list of SNOMED-CT codes to where w is associated or null if none
	 */	
	public List<String> snomedConceptID(String entity) {
		if (this.mapping.containsKey(entity))
			return this.mapping.get(entity).get(RESOURCE_TYPE.sn);
		else
			throw new IllegalArgumentException();
	}

	/**
	 * Search in the concepts and relationships mapping files to see if entity can be null.
	 * @param entity is either a SUBJECT, OBJTECT or PREDICATE (PERSON, SUFFER, etc)
	 * @return true is it can be null, false otherwise
	 */		
	public boolean canBeNull(String entity) {
		if (this.mapping.containsKey(entity))
			return (this.mapping.get(entity).get(RESOURCE_TYPE.nl) != null);
		else
			throw new IllegalArgumentException();
		
	}
	
	/**
	 * Search in the concepts and relationships mapping files for the words associated
	 * with the entity, if any.
	 * @param entity is either a SUBJECT, OBJTECT or PREDICATE (PERSON, SUFFER, etc)
	 * @return list of words or null if none
	 */			
	public List<String> otherWords(String entity) {
		if (this.mapping.containsKey(entity))
			return this.mapping.get(entity).get(RESOURCE_TYPE.ow);
		else
			throw new IllegalArgumentException();
	}
	
	/**
	 * Used for testing purposes
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		ResourcesMapper mapper = new ResourcesMapper();
		String entity1 = "SUFFER_RELATIONSHIP";
		String entity2 = "BE_RELATIONSHIP";
		List<String> result1 = mapper.wordnetSynset(entity1);
		List<String> result2 = mapper.snomedConceptID(entity2);
		
		System.out.println(entity1 + " wordnet synsets(s): " + result1);
		System.out.println(entity2 + " snomed id(s): " + result2);
		System.out.println(entity1 + " can be null: " + mapper.canBeNull(entity1));
		System.out.println(entity2 + " other word(s): " + mapper.otherWords(entity2));
	}
}
