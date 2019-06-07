package wordnet;

import java.util.HashMap;


/**
 * This class convert the offsets of WrdNet files into indexes (0..N).
 * This is used by the graph to index its elements with better performances.
 */

public class Indexer {

	// new index values
	private int counter = 0;
	
	// <Offset> to <Index>
	HashMap<POS, HashMap<Integer, Integer>> indexMapper;
	
	/**
	 * Construct a Mapper of irregular keys to an incremental type of indexes.
	 */
	public Indexer() {
		indexMapper = new HashMap<>();
		for (POS pos : POS.values()) {
			indexMapper.put(pos, new HashMap<>());
		}
	}

	/**
	 * Check whether an index is a Noun, Verb, Adjective or Adverb
	 * @param index to check
	 * @param pos part of speech expected
	 * @return true or false
	 */
	public boolean is(int index, POS pos){
		return this.indexMapper.get(pos).containsKey(index);
	}
	
	
	/**
	 * If it is already inserted. do nothing
	 * else insert it and increment the counter
	 * @param offset
	 */
	public void addIndex(int offset, POS pos) {
		HashMap<Integer, Integer> mapper = this.indexMapper.get(pos);
		Integer result = mapper.get(offset);
		if (result == null) {
			mapper.put(offset, counter);
			counter++;
		}
	}
	/**
	 * If the index is not inserted yet. Insert it and return its index.
	 * else just return its index.
	 * @param offset
	 * @return index [0..N]
	 */
	public int getIndex(int offset, POS pos) {
		HashMap<Integer, Integer> mapper = this.indexMapper.get(pos);
		Integer result = mapper.get(offset);
		if (result == null) {
			this.addIndex(offset, pos);
			return this.getIndex(offset, pos);
		} else {
			return mapper.get(offset);
		}
	}
	/**
	 * Number of elements in the indexer
	 * @return size
	 */
	public int size() {
		int totalSize = 0;
		for (POS pos : POS.values()) {
			HashMap<Integer, Integer> mapper = this.indexMapper.get(pos);
			totalSize+= mapper.size();
		}
		return totalSize;
	}
	
	/**
	 * Print the content of the indexer
	 */
	public void print() {
		for (POS pos : POS.values()) {
			HashMap<Integer, Integer> mapper = this.indexMapper.get(pos);
			for (Integer key : mapper.keySet()) {
				System.out.println(pos + " " + key + " " + mapper.get(key));
			}			
		}
	}
}