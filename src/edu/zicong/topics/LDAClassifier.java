package edu.zicong.topics;

import java.io.*;
import java.util.*;

import com.aliasi.cluster.LatentDirichletAllocation;
import com.aliasi.symbol.*;
import com.aliasi.tokenizer.*;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;



public class LDAClassifier extends TopicClassifier{
	
	int burnin;
	int sampleLag;
	int numSamples;
	Random random;
	int topicsPerDoc;
	int wordsPerTopic;


	public LDAClassifier(String dataCollection) {
		setParameters();
		readDocsFromDir("/media/netdisk/zzhou/playstation/201006/");
	}
	public void readMovieDocs(String dirPath) {
		dirPath=new String("/media/netdisk/zzhou/data/sentiment/movie_review/txt_sentoken/");
		File folder = new File(dirPath+"pos");
		File[] listOfFiles = folder.listFiles();

		for (int i = 0; i < listOfFiles.length; i++) {
			String fileName = dirPath + "pos/" + listOfFiles[i].getName();
			Long threadIds = new Long(i);
			String docContent = new String();
			String docSubject = new String();
			try {
				String readPerLine = new String();
				File inFile = new File(fileName);
				BufferedReader bufferReader = new BufferedReader(
						new InputStreamReader(new FileInputStream(inFile)));
				while ((readPerLine = bufferReader.readLine()) != null) {
					docContent = docContent + "\n" + readPerLine;
				}
				bufferReader.close();
				Document currentDoc = new Document();
				currentDoc.setDocIds(threadIds);
				currentDoc.setDocSubject(docSubject.toLowerCase());
				currentDoc.setDocContent(docContent.toLowerCase());
				docsArrays.add(currentDoc);
			} catch (Exception ex) {
				System.out.println("Cannot read the file!");
				ex.printStackTrace();
			}
		}
		
		folder = new File(dirPath+"neg");
		listOfFiles = folder.listFiles();

		for (int i = 0; i < listOfFiles.length; i++) {
			String fileName = dirPath + "neg/" + listOfFiles[i].getName();
			Long threadIds = new Long(i+1000);
			String docContent = new String();
			String docSubject = new String();
			try {
				String readPerLine = new String();
				File inFile = new File(fileName);
				BufferedReader bufferReader = new BufferedReader(
						new InputStreamReader(new FileInputStream(inFile)));
				while ((readPerLine = bufferReader.readLine()) != null) {
					docContent = docContent + "\n" + readPerLine;
				}
				bufferReader.close();
				Document currentDoc = new Document();
				currentDoc.setDocIds(threadIds);
				currentDoc.setDocSubject(docSubject.toLowerCase());
				currentDoc.setDocContent(docContent.toLowerCase());
				docsArrays.add(currentDoc);
			} catch (Exception ex) {
				System.out.println("Cannot read the file!");
				ex.printStackTrace();
			}
		}
		
		System.out.println("Total files read from disk:" + docsArrays.size());

	}

	// set parameters for LDA
	public void setParameters() {

		// burnin - The number of samples to take and throw away during the
		// burnin period.
		burnin = 1000;
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
		minCount = 10;
		titleBodyWeight=2;

		// number of words to represent the topic
		wordsPerTopic = 10;
		// number of topics assigned to each document
		topicsPerDoc = 1;
		// maximum number of topics allowed for one iteration of LDA
		// maxNumOfTopics = 20;
	}


	public void assignTopics(int numOfClusters) {

		// get a stemming tokenizer factory to tokenization
		MyTokenizer myTokenizer = new MyTokenizer();
		TokenizerFactory tokFactory = myTokenizer.getStemTokenizer();

		// build a array of character sequences that holds content of each
		// document
		CharSequence[] text = new String[docsArrays.size()];
		for (int i = 0; i < docsArrays.size(); i++) {
			text[i] = docsArrays.get(i).getDocContent();
		}

		// create a symbol table which we will use to provide integer indexes to
		// each of the tokens.
		SymbolTable symTab = new MapSymbolTable();
		int[][] docWords = LatentDirichletAllocation.tokenizeDocuments(text,
				tokFactory, symTab, minCount);
		System.out.println("Total number of documents:"+ docWords.length);
		System.out.println("Total number of tokens:"+symTab.numSymbols());


		LatentDirichletAllocater handler = new LatentDirichletAllocater(symTab);

		short numTopics = (short) numOfClusters;
		double docTopicPrior = 1.0;
		double topicWordPrior = 1.0;
		// call the LDA inference method, which is named after the algorithmic
		// approach, Gibbs sampling
		LatentDirichletAllocation.GibbsSample sample = LatentDirichletAllocation
				.gibbsSampler(docWords, numTopics, docTopicPrior,
						topicWordPrior, burnin, sampleLag, numSamples, random,
						handler);
		handler.reportParameters(sample);
		/*
		 * Words assignments to each topic are obtained in two-dimension array
		 * topicWords We keep wordsPerTopic for each topic and use minCount as
		 * filter for tokens
		 */
		String[][] topicWords = handler.reportTopicsByZ(sample, wordsPerTopic,
				minCount);

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
			for (int j = 0; j < topicWords[i].length; j++) 
				topicWords[i][j] = stemToWord.get(topicWords[i][j]);
			ArrayList<String> thisTopic = new ArrayList<String>(
					Arrays.asList(topicWords[docTopics[i][0]]));
			docsArrays.get(i).addTopics(thisTopic);
		}
		
	}
	public void reportResult(String outFile){
		
	}
	public void run(String outFile) {
		setStemCountMap();
		pickWordML();
		int numOfClusters=2;
		assignTopics(numOfClusters);
		// Report the hierarchy tree structure
		reportResult(outFile);
	}

	public static void main(String[] args) {

		// DocumentClassifier example = new DocumentClassifier(args[0]);

		String dataDate = new String("201006");
		LDAClassifier docClassifier = new LDAClassifier(dataDate);
		long startTime = System.currentTimeMillis();
		docClassifier.run("/media/netdisk/zzhou/playstation/201006net.dat");
		long endTime = System.currentTimeMillis();

		System.out.println("That took " + (endTime - startTime) / 60000
				+ " mins");
	}
}