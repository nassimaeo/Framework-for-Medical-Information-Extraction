package nlp;

import reasoning.MetaModel;

// Obsolete (don't use it) check MainAnalyzer.java
/**
 * 
 * This class reads a post. Do the NATURAL LANGUAGE PROCESSING (NLP) to 
 * extract the sentences, the words, syntactical tree, POS. Than return
 * the Tockens that will be used to query SNOMED and WORDNET to interpret 
 * the message. A message is a textual forum post of medical content.
 * Steps:
 * - A post is a set of paragraphs.
 * - A post will be devided into sentences.
 * - Each sentence with be stemmed (keep only the root of the word) 
 * - Each sentence with analyzed for it Syntactical tree and POS.
 * 
 * 
 * @author Nassim
 *
 */
public class Interprete {

    public static void main(String[] args) throws Exception {
		String postPath = "res/posts/test.txt";
    	MainAnalyzer analyzer = new MainAnalyzer();
    	MetaModel instantiatedModel = analyzer.analyze(postPath);
    	instantiatedModel.draw();
    }

}
