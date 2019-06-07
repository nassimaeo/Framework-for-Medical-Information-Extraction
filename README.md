# A Framework for information extraction applied in healthcare

## What is this code?
This code is a Framework for extracting medical information from textual documents using an ontology driven approach combined with lexico-syntactical extraction patterns. For more details read the paper [1]

## Can I use this code?
Yes, this code is published under the GPLv3 licence. If you use any part of this code please cite the paper [1].

## What do you need?

### Snomed-CT: 

We can not distribute SNOMED with our code. You need to create an account on NLM in order to download it. The domain ontology(SNOMED-CT) is used to identify the medical concepts. 

```
public class SNOMED { 
	... 
	private final static String CONCEPTS_FILE_PATH = "res/snomed/sct2_Concept_Snapshot_INT_20150731.txt"; 
	private final static String RELATIONSHIPS_FILE_PATH = "res/snomed/sct2_Relationship_Snapshot_INT_20150731.txt";
	private final static String DESCRIPTIONS_FILE_PATH = "res/snomed/sct2_Description_Snapshot-en_INT_20150731.txt"; 
	... 
}

public class UMLS {
	... 	
	// UMLS File containing the mapping between SNOMED-CT_id and UMLS_CUI
	public static final String CUI_SNOMED_ID_FILE = "res/eval/MRCONSO.RRF";
	... 	
}	
```

### Wordnet

It is used to look up similar words

```
public class WordNet { 
	... 
	private final static String WORDNET_NOUNS_FILE_PATH = "res/wordnet/data.noun";
	private final static String WORDNET_ADJECTIVES_FILE_PATH = "res/wordnet/data.adj";
	private final static String WORDNET_ADVERBS_FILE_PATH = "res/wordnet/data.adv"; 
	private final static String WORDNET_VERBS_FILE_PATH = "res/wordnet/data.verb"; 
	... 
}
```

### princeton-library

It is used by Wordnet for the graph data structure manipulations.

```
/medical_nlp/princeton_library/algs4.jar
```

### jgraphx-master

It is used to represent the resulted graphical interpretation models.

```
/medical_nlp/jgraphx-master/lib/jgraphx.jar
```

### stanford-parser-full-2015-12-09

It is used to generate the syntactical trees of the sentences.

```
/medical_nlp/stanford-parser-full-2015-12-09/slf4j-api.jar
/medical_nlp/stanford-parser-full-2015-12-09/slf4j-simple.jar
/medical_nlp/stanford-parser-full-2015-12-09/stanford-parser-3.6.0-javadoc.jar
/medical_nlp/stanford-parser-full-2015-12-09/stanford-parser-3.6.0-models.jar
/medical_nlp/stanford-parser-full-2015-12-09/stanford-parser-3.6.0-sources.jar
/medical_nlp/stanford-parser-full-2015-12-09/stanford-parser.jar
```

## How to run this code?</h3>

* To run WordNet alone: WordNet.java
* To run Snomed-CT alone: SNOMED.java
* To run the complete pipeline: MainAnalyzer.java<br /> 

## How to use the Framework?</h3>

* Define your meta-model: a theoretical model defining the prerequisites
* Define your extraction patterns in res/patterns.txt
* Set your alignment in res/mapping/mapping.txt
* Put the text you want to process in res/posts/test.txt
* See the result as a graph


### [1] Citation
https://doi.org/10.1108/IJWIS-03-2018-0017