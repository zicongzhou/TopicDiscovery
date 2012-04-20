/*
 * This program is the reporting program overloaded to serve two purposes. 
 * First, it handles Gibbs samples as they arrive as required by its interface. 
 * Second, it provides a topic and document report that can be used to assign 
 * topics to documents later. 
 * 
 * Zicong Zhou, March 2011
 * 
 */


package edu.zicong.topics;

import java.util.List;

import com.aliasi.cluster.LatentDirichletAllocation.GibbsSample;
import com.aliasi.corpus.ObjectHandler;
import com.aliasi.symbol.SymbolTable;
import com.aliasi.util.ObjectToCounterMap;
import com.aliasi.util.ObjectToDoubleMap;

public class LatentDirichletAllocater implements ObjectHandler<GibbsSample> {
 /* Symbol table is used to associate the integer identifiers with which LDA works with 
  * the tokens from which they arose. 
  */
	private final SymbolTable mSymbolTable;

	public LatentDirichletAllocater(SymbolTable symbolTable) {
	    mSymbolTable = symbolTable;
	}
	
	public void handle(GibbsSample sample) {
		// do nothing in handle, may add some lines for displaying purpose. 
	}

	
	public double reportEntropy(GibbsSample sample){
		/* We define the cross-entropy rate as reported here is the negative log 
		 * (base 2) probability of the entire corpus of documents divided by the 
		 * number of tokens in the entire corpus. 
		 */
		double xEntropyRate= -sample.corpusLog2Probability() / sample.numTokens();
		System.out.println("numTopics="+sample.numTopics()+"  Entropgy Rate="+ xEntropyRate);
		return xEntropyRate;
	}

	public void reportParameters(GibbsSample sample) {
		/* Just prints general corpus and estimator parameters from the sample.
		 * parameters includes samples, number of documents, number of tokens, 
		 * number of words and number of topics in the Gibbs Sampling.
		 */
		
	    System.out.println("sample=" + sample.epoch());
	    System.out.println("numDocuments=" + sample.numDocuments());
	    System.out.println("numTokens=" + sample.numTokens());
	    System.out.println("numWords=" + sample.numWords());
	    System.out.println("numTopics=" + sample.numTopics());
	}

	// report topics by number of words
	public String[][] reportTopics(GibbsSample sample, int maxWords) {
		/* topicArray is used to store the keywords for each topic,
		 * basically two dimension array determined by number of topics and 
		 * number of words(maxWords) to describe the topic.   
		 */
		String[][] topicArray=new String[sample.numTopics()][maxWords];
	    for (int topic = 0; topic < sample.numTopics(); ++topic) {
	        ObjectToCounterMap<Integer> counter = new ObjectToCounterMap<Integer>();
	        for (int word = 0; word < sample.numWords(); ++word)
	   //   It then allocates a general LingPipe object counter to use for the word counts.
	            counter.set(word,sample.topicWordCount(topic,word));
	    //  We create a list of the keys, here word indexes, ordered in descending order of their counts. 
	        List<Integer> topWords = counter.keysOrderedByCountList();
	    /* We bound the number of words by the maximum given and make sure not to
	     * overflow the model’s size bounds.  
	     */ 
	        for (int rank = 0; rank < maxWords && rank < topWords.size();++rank) {
	            int wordId = topWords.get(rank);
	            String word = mSymbolTable.idToSymbol(wordId);
	    // Topic keywords are kept in the topicArray, the variable to return
	            topicArray[topic][rank]=word;
	        }
	    }
	    return topicArray;
	}

	/* report topics by z-value of words, minCount is the number of counts required for consideration
	 */
	public String[][] reportTopicsByZ(GibbsSample sample, int maxWords, int minCount) {
		/* topicArray is used to store the keywords for each topic,
		 * basically two dimension array determined by number of topics and 
		 * number of words(maxWords) to describe the topic.   
		 */
		String[][] topicArray=new String[sample.numTopics()][maxWords];
	    int numTokens = sample.numTokens();
	    for (int topic = 0; topic < sample.numTopics(); ++topic) {
	        int topicCount = sample.topicCount(topic);
	        ObjectToDoubleMap<Integer> wordToZ = new ObjectToDoubleMap<Integer>();
	        for (int word = 0; word < sample.numWords(); ++word) {
	            int topicWordCount = sample.topicWordCount(topic,word);
	            if (topicWordCount < minCount) continue;
	            int wordCount = sample.wordCount(word);
	            double z = binomialZ(topicWordCount,topicCount,wordCount,numTokens);
	            wordToZ.set(word, z);
	        }
	    //  We create a list of the keys, here word indexes, ordered in descending order of z-value. 
	        List<Integer> topWords = wordToZ.keysOrderedByValueList();
	    
	    // report topics by z-value and topic keywords are kept in topicArray.
	        for (int rank = 0; rank < maxWords && rank < topWords.size();++rank) {
	            int wordId = topWords.get(rank);
	            String word = mSymbolTable.idToSymbol(wordId);
	            topicArray[topic][rank]=word;  
	        }
	    }
	    return topicArray;
	}

/* provides a report on the documents, docsTopic is two dimension array by the number 
 * of documents and topics for each document, used to store topic assignment. 
 */
	public int[][] reportDocs(GibbsSample sample, int maxTopics) {
		int[][] docsTopic=new int[sample.numDocuments()][maxTopics];
		// This method enumerates all the documents in the corpus, reporting on each one in turn.
	    for (int doc = 0; doc < sample.numDocuments(); ++doc) {
	        ObjectToCounterMap<Integer> counter = new ObjectToCounterMap<Integer>();
	        for (int topic = 0; topic < sample.numTopics();++topic)
	         /* we use a LingPipe object counter mapping topic identifiers to the number of 
		      * times a word in the current document was assigned to that topic.*/
	            counter.set(topic,sample.documentTopicCount(doc,topic));
	        /* We just enumerate over the tokens in a document printing the topic 
	         * to which they were assigned.
	         */
	        List<Integer> topTopics = counter.keysOrderedByCountList();
	        for (int rank = 0;rank < topTopics.size() && rank < maxTopics;++rank) {
	            int topic = topTopics.get(rank);
	         	docsTopic[doc][rank]=topic;
	        }
	    }
	    return docsTopic;
	}

	static double binomialZ(double wordCountInDoc, 
	                        double wordsInDoc,
	                        double wordCountinCorpus, 
	                        double wordsInCorpus) {
	/* Report the z-score of the word in the document versus in the corpus.
	 *  The z-score is then the actual value minus the expected value scaled
	 * by the expected deviation. 
	 */ 
	    double pCorpus = wordCountinCorpus / wordsInCorpus;
	    double var = wordsInCorpus * pCorpus * (1 - pCorpus);
	    double dev = Math.sqrt(var);
	    double expected = wordsInDoc * pCorpus;
	    return (wordCountInDoc - expected) / dev;
	}


}
