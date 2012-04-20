package edu.zicong.topics;

import java.io.*;
import java.util.*;

import com.aliasi.cluster.LatentDirichletAllocation;
import com.aliasi.symbol.*;
import com.aliasi.tokenizer.*;



public class BiogramLDAClassifier extends TopicClassifier{
	
	int burnin;
	int sampleLag;
	int numSamples;
	Random random;
	int topicsPerDoc;
	int wordsPerTopic;

	SymbolTable newSymTab;
	int[] wordCorpCount;

	public BiogramLDAClassifier(String dataCollection) {
		minCount = 2;
		titleBodyWeight = 1;
		setParameters();
		readDocsFromMothering(dataCollection);
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
		TokenizerFactory tokFactory = myTokenizer.getTokenizer();

		// build a array of character sequences that holds content of each
		// document
		CharSequence[] newText = new String[docsArrays.size()];

		for (int k = 0; k < docsArrays.size(); k++) {
			String thisDoc = (String) docsArrays.get(k).getDocContent();
			CharSequence[] text = thisDoc.split("^|[|,|.|:|;|!|?|]|$");
			SymbolTable symTab = new MapSymbolTable();
			int[][] docWords = LatentDirichletAllocation.tokenizeDocuments(text,
				tokFactory, symTab, 1);
//			System.out.println(thisDoc);
			newText[k]="";
			for (int i = 0; i < docWords.length; i++) {
				int length=docWords[i].length;
				if(length>1){
					newText[k] = newText[k]+ symTab.idToSymbol(docWords[i][0]);
					for(int j=1;j<length-1;j++)
						newText[k]= newText[k]+symTab.idToSymbol(docWords[i][j]).toUpperCase()+" "+symTab.idToSymbol(docWords[i][j]);
					newText[k] = newText[k]+ symTab.idToSymbol(docWords[i][length-1]).toUpperCase()+". ";
				}
			}
//			System.out.println(newText[k]);
		}
		// create a symbol table which we will use to provide integer indexes to
		// each of the tokens.
		newSymTab = new MapSymbolTable();
		
		String regex = "[\\x2D\\p{L}\\p{N}]{2,}";
        TokenizerFactory factory= new RegExTokenizerFactory(regex);
		int[][] newDocWords = LatentDirichletAllocation.tokenizeDocuments(newText,
				factory, newSymTab, minCount);
		
		wordCorpCount=new int[newSymTab.numSymbols()];
	
		for (int i = 0; i < newDocWords.length; i++) 
			for (int k = 0; k < newDocWords[i].length; k++)
					wordCorpCount[newDocWords[i][k]]++;
		
		System.out.println("Total number of documents:"+ newDocWords.length);
		System.out.println("Total number of tokens:"+newSymTab.numSymbols());


		LatentDirichletAllocater handler = new LatentDirichletAllocater(newSymTab);

		short numTopics = (short) numOfClusters;
		double docTopicPrior = 1.0;
		double topicWordPrior = 1.0;
		// call the LDA inference method, which is named after the algorithmic
		// approach, Gibbs sampling
		LatentDirichletAllocation.GibbsSample sample = LatentDirichletAllocation
				.gibbsSampler(newDocWords, numTopics, docTopicPrior,
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
			System.out.println("Topic "+i);
			for (int j = 0; j < topicWords[i].length; j++) 
				System.out.print(topicWords[i][j]+" ");
			System.out.println();
		}
		
	}
	public void reportResult(String outFile){
		
	}
	
	
	public void outWordStat(String outFile) {
		try {
			FileOutputStream fstream = new FileOutputStream(outFile);
			BufferedWriter fout = new BufferedWriter(new OutputStreamWriter(
					fstream, "UTF-8"));
			for(int i=0;i<newSymTab.numSymbols();i++){
				String word=newSymTab.idToSymbol(i);
				fout.write(wordCorpCount[i]+"\t"+i+"\t"+word+"\n");
			}
			fout.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void run(String outFile) {
//		setStemCountMap();
//		pickWordML();
		int numOfClusters=10;
		assignTopics(numOfClusters);
		outWordStat(outFile);
	}

	public static void main(String[] args) {

		// DocumentClassifier example = new DocumentClassifier(args[0]);

		BiogramLDAClassifier ex = new BiogramLDAClassifier("/media/netdisk/zzhou/vaccination/ThreadTable.txt");
		long startTime = System.currentTimeMillis();
		ex.run("/media/netdisk/zzhou/temp/vax_biogram.dat");
		long endTime = System.currentTimeMillis();

		System.out.println("That took " + (endTime - startTime) / 60000
				+ " mins");
	}
}