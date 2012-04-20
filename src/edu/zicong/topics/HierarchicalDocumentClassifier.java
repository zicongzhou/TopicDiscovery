package edu.zicong.topics;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.aliasi.cluster.LatentDirichletAllocation;
import com.aliasi.symbol.*;
import com.aliasi.tokenizer.*;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;

public class HierarchicalDocumentClassifier extends TopicClassifier{

	LinkedList<ArrayList<Integer>> LDAProcQueue;
	int burnin;
	int sampleLag;
	int numSamples;
	Random random;
	int topicsPerDoc;
	int wordsPerTopic;

	int maxNumOfThreadPerTopic;
	int maxNumOfLevels;
	int maxNumOfTopics;

	public HierarchicalDocumentClassifier(String dataCollection) {
		setParameters();
		readDocsFromDir(dataCollection);
		// setDocsFromFiles(subBodyWeight,dataCollection);
		// docsSelFromMothering(subBodyWeight, dataCollection);
	//	docsFromMothering(subBodyWeight, dataCollection);
		setLDAProcQueue();
	}
	
	public LinkedList<ArrayList<Integer>> getLDAProcQueue() {
		return LDAProcQueue;
	}

	// set parameters for LDA
	public void setParameters() {

		// burnin - The number of samples to take and throw away during the
		// burnin period.
		burnin = 500;
		// sampleLag - The interval between samples after burnin.
		sampleLag = 1;
		// numSamples - Number of Gibbs samples to return.
		numSamples = 1024;
		// random - The random number generator to use for this sampling
		// process.
		random = new Random(43L);

		/*
		 * minCount will specify how many instances of a token must be seen in
		 * the entire set of documents for it to be used in the model. Here we
		 * set the threshold to 2, so tokens that only appear once in the corpus
		 * will be pruned out.
		 */
		minCount = 2;
		titleBodyWeight = 2;

		// number of words to represent the topic
		wordsPerTopic = 5;
		// number of topics assigned to each document
		topicsPerDoc = 1;
		// maximum number of topics allowed for one iteration of LDA
		// maxNumOfTopics = 20;
		maxNumOfTopics = 20;
		// stopping criterion: maximum number of threads allowed without another
		// iteration of LDA
		// maxNumOfThreadPerTopic = 20;
		maxNumOfThreadPerTopic = 2;

		maxNumOfLevels = 3;
	}

	/*
	 * build wordsMap Histogram between stemmed token and original token with
	 * counts included an example of wordsMap: <"communit",<"community",4>>;
	 * <"communit",<"communities",2>> <"communit",<"community",4>> means
	 * community has been stemmed to communit for 4 times. Later we will recover
	 * the stemmed tokens based on the counts in wordsMap histogram.
	 */


	/*
	 * build Queue of ArrayLists for LDA to process. Each ArrayList in the queue
	 * represent documents that needs to be classified. Initially we set put all
	 * the documents in Queue.
	 */
	public void setLDAProcQueue() {
		LDAProcQueue = new LinkedList<ArrayList<Integer>>();
		ArrayList<Integer> docsList = new ArrayList<Integer>();
		// all the documents need to be classified first
		for (int i = 0; i < docsArrays.size(); i++) {
			docsList.add(i);
		}
		LDAProcQueue.add(docsList);
	}

	/*
	 * Given ArrayList of documents, determine the number of clusters in the
	 * documents. We solve this model selection problem by exploring the
	 * consequences of varying numTopics For all runs of the algorithm, we set
	 * topicWordPrior(alpha), docTopicPrior(beta) be to 1.0 and 50.0/numTopics;
	 * The number of clusters is determined by finding minimum value of
	 * cross-entropy
	 * 
	 * Reference: Finding scientific topics, T. Grifﬁths and M. Steyver, PNAS,
	 * 2004
	 */

	public int findNumOfClusters(ArrayList<Integer> docsList) {

		// get a stemming tokenizer factory to tokenization
		MyTokenizer myTokenizer = new MyTokenizer();
		TokenizerFactory tokFactory = myTokenizer.getStemTokenizer();

		// build a array of character sequences that holds content of each
		// document
		CharSequence[] text = new String[docsList.size()];
		for (int i = 0; i < docsList.size(); i++) {
			text[i] = docsArrays.get(docsList.get(i)).getDocContent();
		}

		// create a symbol table which we will use to provide integer indexes to
		// each of the tokens.
		SymbolTable symTab = new MapSymbolTable();
		/*
		 * The variable minCount will specify how many instances of a token must
		 * be seen in the entire set of documents for it to be used in the
		 * model. We create a two dimensional array of document-words
		 * identifiers.
		 */
		int[][] docWords = LatentDirichletAllocation.tokenizeDocuments(text,
				tokFactory, symTab, minCount);

		LatentDirichletAllocater handler = new LatentDirichletAllocater(symTab);

		/*
		 * Explore the number of clusters by running series of LDA with same
		 * alpha and beta We restrict the largest possible number of clusters
		 * for practical purposes.
		 */
		Double preEntropy = Double.MAX_VALUE;
		short numOfClusters = (short) maxNumOfTopics;
		for (int k = 1; k <= maxNumOfTopics; k++) {
			short numTopics = (short) k;
//			double docTopicPrior = 100.0 / numTopics;
			double docTopicPrior = 50.0 / numTopics;
			double topicWordPrior = 1.0;
//			double topicWordPrior = 1.0;
			random = new Random(103L);
			// call the LDA inference method, which is named after the
			// algorithmic approach, Gibbs sampling
			LatentDirichletAllocation.GibbsSample sample = LatentDirichletAllocation
					.gibbsSampler(docWords, numTopics, docTopicPrior,
							topicWordPrior, burnin, sampleLag, numSamples,
							random, handler);
			double curEntropy = handler.reportEntropy(sample);
			// find out the first local minimum obtained
			if (preEntropy <= curEntropy) {
				numTopics--;
				numOfClusters = numTopics;
				break;
			} else {
				preEntropy = curEntropy;
			}
		}
			return numOfClusters;
		
	}
	
	/*
	 * Given ArrayList of documents and the number of clusters, we use LDA to
	 * find the keywords to represent each topic and determine the topic for
	 * each document.
	 */
	public void assignTopics(int numOfClusters, ArrayList<Integer> docsList) {

		// get a stemming tokenizer factory to tokenization
		MyTokenizer myTokenizer = new MyTokenizer();
		TokenizerFactory tokFactory = myTokenizer.getStemTokenizer();

		// build a array of character sequences that holds content of each
		// document
		CharSequence[] text = new String[docsList.size()];
		for (int i = 0; i < docsList.size(); i++) {
			text[i] = docsArrays.get(docsList.get(i)).getDocContent();
		}

		// create a symbol table which we will use to provide integer indexes to
		// each of the tokens.
		SymbolTable symTab = new MapSymbolTable();
		/*
		 * The variable minCount will specify how many instances of a token must
		 * be seen in the entire set of documents for it to be used in the
		 * model. We create a two dimensional array of document-words
		 * identifiers.
		 */
		int[][] docWords = LatentDirichletAllocation.tokenizeDocuments(text,
				tokFactory, symTab, minCount);

		LatentDirichletAllocater handler = new LatentDirichletAllocater(symTab);

		random = new Random(43L);

		short numTopics = (short) numOfClusters;
		double docTopicPrior = 50.0 / numTopics;	
		double topicWordPrior = 1.0;
		// call the LDA inference method, which is named after the algorithmic
		// approach, Gibbs sampling
		LatentDirichletAllocation.GibbsSample sample = LatentDirichletAllocation
				.gibbsSampler(docWords, numTopics, docTopicPrior,
						topicWordPrior, burnin, sampleLag, numSamples, random,
						handler);
		/*
		 * Words assignments to each topic are obtained in two-dimension array
		 * topicWords We keep wordsPerTopic for each topic and use minCount as
		 * filter for tokens
		 */
		String[][] topicWords = handler.reportTopicsByZ(sample, wordsPerTopic,
				minCount);
		handler.reportEntropy(sample);
		/*
		 * Topic assignments to each document are obtained in two-dimension
		 * array docTopics We keep topicsPerDoc for each document
		 */
		int[][] docTopics = handler.reportDocs(sample, topicsPerDoc);

		/*
		 * Update the topicWords array to make sure each token is readable We
		 * find the best match for each stemmed token based on wordsMap
		 * histogram
		 */
		for (int i = 0; i < topicWords.length; i++) {
			for (int j = 0; j < topicWords[i].length; j++) {
				String maxString = stemToWord.get(topicWords[i][j]);
				topicWords[i][j] = maxString;
			}
		}

		// add topic words for each document in the docsList,picked topic with
		// largest probability
		for (int i = 0; i < docsList.size(); i++) {
			ArrayList<String> thisTopic = new ArrayList<String>(
					Arrays.asList(topicWords[docTopics[i][0]]));
			docsArrays.get(docsList.get(i)).addTopics(thisTopic);
		}
		
		// create new docLists by the topic and initialize it
		ArrayList<ArrayList<Integer>> docsListByTopic = new ArrayList<ArrayList<Integer>>();
		for (int i = 0; i < topicWords.length; i++) {
			ArrayList<Integer> emptyList = new ArrayList<Integer>();
			docsListByTopic.add(emptyList);
		}
		// add documents to different lists by topics
		for (int i = 0; i < docsList.size(); i++) {
			docsListByTopic.get(docTopics[i][0]).add(docsList.get(i));
		}
		// add docsList into LDA processing queue if the number of threads
		// inside is greater than 20.
		for (int i = 0; i < docsListByTopic.size(); i++){
			if (docsListByTopic.get(i).size() > maxNumOfThreadPerTopic
					&& docsArrays.get(docsListByTopic.get(i).get(0))
							.getDocTopics().size() < maxNumOfLevels) {
//				System.out.println("adding lists to LDA Queue!");
				LDAProcQueue.add(docsListByTopic.get(i));
			}
		}
	}

	/*
	 * Find topic hierarchy structure for all the documents. Build hashtable
	 * using the topics structure in document as key and the arraylist of
	 * subjects in each thread as value Report the hierarchy tree structure by
	 * the path from root to leaves
	 */
	public Hashtable<ArrayList<ArrayList<String>>, ArrayList<Document>> findHierarchy(
			String outFile) {
		Hashtable<ArrayList<ArrayList<String>>, ArrayList<Document>> structureTable = new Hashtable<ArrayList<ArrayList<String>>, ArrayList<Document>>();
		// build the structure table to record the topic hierarchy structure
		for (int i = 0; i < docsArrays.size(); i++) {
			ArrayList<ArrayList<String>> thisTopics = docsArrays.get(i)
					.getDocTopics();
			ArrayList<Document> threadLists;
			if (structureTable.containsKey(thisTopics))
				threadLists = structureTable.get(thisTopics);
			else
				threadLists = new ArrayList<Document>();
			// add the subjects into the structure table with topics as key
			threadLists.add(docsArrays.get(i));
			structureTable.put(thisTopics, threadLists);
		}
	
		try {
			FileOutputStream fstream = new FileOutputStream(outFile);
			BufferedWriter fout = new BufferedWriter(new OutputStreamWriter(
					fstream, "UTF-8"));
			fout.write("[\"all\",null,0,0],\n");
			for (ArrayList<ArrayList<String>> topicStructure : structureTable
					.keySet()) {
				fout.write("[\"");
				if (topicStructure.size() < 2) {
					for (int j = 0; j < wordsPerTopic - 1; j++)
						fout.write(topicStructure.get(0).get(j) + " ");
					fout.write(topicStructure.get(0).get(wordsPerTopic - 1)
							+ "\",");
					fout.write("\"all\","
							+ structureTable.get(topicStructure).size()
							+ ",0],\n");
				} else {

					for (int j = 0; j < wordsPerTopic - 1; j++)
						fout.write(topicStructure.get(0).get(j) + " ");
					fout.write(topicStructure.get(0).get(wordsPerTopic - 1)
							+ "\",");
					fout.write("\"all\",0,0],\n");

					for (int i = 1; i < topicStructure.size() - 1; i++) {
						fout.write("[\"");
						for (int j = 0; j < wordsPerTopic - 1; j++)
							fout.write(topicStructure.get(i).get(j) + " ");
						fout.write(topicStructure.get(i).get(wordsPerTopic - 1)
								+ "\",");
						fout.write("\"");
						for (int j = 0; j < wordsPerTopic - 1; j++)
							fout.write(topicStructure.get(i - 1).get(j) + " ");
						fout.write(topicStructure.get(i - 1).get(
								wordsPerTopic - 1)
								+ "\",0,0],\n");
					}
					fout.write("[\"");
					for (int j = 0; j < wordsPerTopic - 1; j++)
						fout.write(topicStructure
								.get(topicStructure.size() - 1).get(j) + " ");
					fout.write(topicStructure.get(topicStructure.size() - 1)
							.get(wordsPerTopic - 1) + "\",");
					fout.write("\"");

					for (int j = 0; j < wordsPerTopic - 1; j++)
						fout.write(topicStructure
								.get(topicStructure.size() - 2).get(j) + " ");
					fout.write(topicStructure.get(topicStructure.size() - 2)
							.get(wordsPerTopic - 1) + "\",");
					fout.write(structureTable.get(topicStructure).size()
							+ ",0],\n");
				}

			}
			fout.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// return the hierarchy structure if necessary.
		return structureTable;
	}

	public void printClassResult() {
		try {
			FileOutputStream fstream = new FileOutputStream(
					"/media/netdisk/zzhou/data/playstationeu/xml/board/LDAResult.dat");
			BufferedWriter fout = new BufferedWriter(new OutputStreamWriter(
					fstream, "UTF-8"));
			Map<ArrayList<ArrayList<String>>, Integer> topicMap = new HashMap<ArrayList<ArrayList<String>>, Integer>();
			int tCounter = 1;
			for (int i = 0; i < docsArrays.size(); i++) {
				ArrayList<ArrayList<String>> thisTopics = docsArrays.get(i)
						.getDocTopics();
				if (!topicMap.containsKey(thisTopics)) {
					topicMap.put(thisTopics, tCounter++);
				}
				fout.write(topicMap.get(thisTopics) + "\t"
						+ docsArrays.get(i).getDocIDs() + "\n");
			}
			System.out.println(tCounter);
			fout.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void printThreadsResult() {
		try {
			FileOutputStream fstream = new FileOutputStream(
					"/media/netdisk/zzhou/temp/LDAResult.dat");
			BufferedWriter fout = new BufferedWriter(new OutputStreamWriter(
					fstream, "UTF-8"));
			
			for (int i = 0; i < docsArrays.size(); i++) {
				ArrayList<String> docTopics = docsArrays.get(i).getDocTopics().get(0);
				fout.write(docsArrays.get(i).getDocIDs()+"\t");
				for(int j=0;j<docTopics.size();j++)
					fout.write(docTopics.get(j)+" ");
				fout.write("\n");
			}
			fout.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void run(String outFile) {
		// when the LDA processing queue is empty, we stop the algorithm
		setStemCountMap();
		pickWordML();
		while (LDAProcQueue.size() != 0) {
			System.out.println("Number of lists in the queue:"
					+ LDAProcQueue.size());
			// get the first docsList in the queue
			ArrayList<Integer> docsList = LDAProcQueue.remove();
			// find the number of clusters included in the docsList first
			int numOfClusters = findNumOfClusters(docsList);
			System.out.println("Clusters detected:" + numOfClusters);
			// if there is more than one clusters, process the LDA.
			if (numOfClusters > 1)
				assignTopics(numOfClusters, docsList);
		}
		// // Report the hierarchy tree structure
		// System.out
		// .println("*************************displaying result**************************************");
		findHierarchy(outFile);
		printClassResult();
	}


	public static void main(String[] args) {

		// DocumentClassifier example = new DocumentClassifier(args[0]);

		String dataDate= new String("201009");
		HierarchicalDocumentClassifier docClassifier = new HierarchicalDocumentClassifier(dataDate);
//		DocumentClassifier docClassifier = new DocumentClassifier(
//		"/media/netdisk/zzhou/vaccination/ThreadTable.txt");
//		long startTime = System.currentTimeMillis();
////	    docClassifier.run("/media/netdisk/zzhou/temp.dat");
//		for (int i = 20; i <= 20; i++) {
//			DocumentClassifier docClassifier = new DocumentClassifier(
//					"/media/netdisk/zzhou/vaccination/ThreadTable.txt");
//			docClassifier.LDASequences("/media/netdisk/zzhou/temp", i);
//		}
//		long endTime = System.currentTimeMillis();

//		System.out.println("That took " + (endTime - startTime) / 60000
//				+ " mins");
	}
}