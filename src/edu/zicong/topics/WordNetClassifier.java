package edu.zicong.topics;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.aliasi.cluster.LatentDirichletAllocation;
import com.aliasi.symbol.MapSymbolTable;
import com.aliasi.symbol.SymbolTable;
import com.aliasi.tokenizer.TokenizerFactory;

class Edge {
	int start;
	int end;

	Edge(int s, int e) {
		start = s;
		end = e;
	}

	public boolean equals(Object edge) {
		return (((Edge) edge).start == this.start)
				&& (((Edge) edge).end == this.end);
	}

	String edgeString() {
		return String.valueOf(start) + " " + String.valueOf(end);
	}

	public int hashCode() {
		return 101 * start + end;
	}

}

class WordStat {
	int corpusId;
	int count;
	double zScore;
	double tfIdf;
	boolean retain;
	public WordStat(int id, int c, double s1, double s2) {
		corpusId = id;
		count = c;
		zScore = s1;
		tfIdf = s2;
	}
	public void remove(int ct, double z, double tfidf){
		 if (count>=ct && zScore>=z && tfIdf>=tfidf)
			 retain=true;
		 else
			 retain=false;
	}
}

public class WordNetClassifier extends TopicClassifier {

	// words statistics matrix
	WordStat[][] wordDocStat;
	// map between id and word
	SymbolTable symTab;
	// unigram, how many times words appear in the corpus
	int[] wordCorpCount;
	// how many document contain the words
	int[] wordDocCount;
	Map<Edge, Integer> edgeWeight;
	int countCutOff;
	double zCutOff;
	double tfIdfCutOff;

	public WordNetClassifier(String dataCollection) {
		super(2, 1);
		edgeWeight = new HashMap<Edge, Integer>();
		readDocsFromMothering(dataCollection);
	}
	
	public void setCutOff(int c, double z, double t){
		countCutOff=c;
		zCutOff=z;
		tfIdfCutOff=t;
	}

	public void findWordsStat() {
		MyTokenizer myTokenizer = new MyTokenizer();
		TokenizerFactory tokFactory = myTokenizer.getStemTokenizer();
		// TokenizerFactory tokFactory = myTokenizer.getTokenizer();
		CharSequence[] text = new String[docsArrays.size()];
		for (int i = 0; i < docsArrays.size(); i++)
			text[i] = docsArrays.get(i).getDocContent();
		symTab = new MapSymbolTable();
		int[][] docWords = LatentDirichletAllocation.tokenizeDocuments(text,
				tokFactory, symTab, minCount);

		System.out.println("total number of stemmed tokens: "+ symTab.numSymbols());

		wordCorpCount = new int[symTab.numSymbols()];
		wordDocStat = new WordStat[docWords.length][];
		wordDocCount = new int[symTab.numSymbols()];
		List<HashMap<Integer, Integer>> wordDoc = new ArrayList<HashMap<Integer, Integer>>();

		for (int i = 0; i < docWords.length; i++) {
			HashMap<Integer, Integer> thisDoc = new HashMap<Integer, Integer>();
			for (int j = 0; j < docWords[i].length; j++) {
				wordCorpCount[docWords[i][j]]++;
				numOfWords++;
				int count = thisDoc.containsKey(docWords[i][j]) ? thisDoc
						.get(docWords[i][j]) : 0;
				thisDoc.put(docWords[i][j], count + 1);
			}
			for (Integer wordId : thisDoc.keySet())
				wordDocCount[wordId]++;
			wordDoc.add(i, thisDoc);
		}
		// compute the zScore and tfIdf etc for the words in the documents and filter by these statistics
		
		for (int i = 0; i < docWords.length; i++) {
				HashMap<Integer,Integer> thisDoc=wordDoc.get(i);
				WordStat[] docStat = new WordStat[thisDoc.size()];
				int wordCounter = 0;
				for (Integer wordId : thisDoc.keySet()) {
					double zScore = binomialZ(wordDoc.get(i).get(wordId),
							docWords[i].length, wordCorpCount[wordId],
							numOfWords);
					double tfIdf = tfIdfScore(thisDoc.get(wordId),
							wordDocCount[wordId], docsArrays.size());
					WordStat wStat = new WordStat(wordId, thisDoc.get(
							wordId), zScore, tfIdf);
					// use count, zScore and tfIdf score to filter the words
					wStat.remove(countCutOff, zCutOff, tfIdfCutOff);
					docStat[wordCounter++] = wStat;
				}
				wordDocStat[i] = docStat;
			}	

	}

	static double binomialZ(double wordCountInDoc, double wordsInDoc,
			double wordCountinCorpus, double wordsInCorpus) {
		/*
		 * Report the z-score of the word in the document versus in the corpus.
		 * The z-score is then the actual value minus the expected value scaled
		 * by the expected deviation.
		 */
		double pCorpus = wordCountinCorpus / wordsInCorpus;
		double var = wordsInCorpus * pCorpus * (1 - pCorpus);
		double dev = Math.sqrt(var);
		double expected = wordsInDoc * pCorpus;
		return (wordCountInDoc - expected) / dev;
	}

	static double tfIdfScore(double wordCountInDoc, double docsCountInCorpus,
			double docsInCorpus) {
		double idf = Math.log(docsInCorpus / docsCountInCorpus);
		return wordCountInDoc * idf;
	}


	public void buildNets() {

		for (int k = 0; k < wordDocStat.length; k++) {
			for (int i = 0; i < wordDocStat[k].length; i++)
				for (int j = i + 1; j < wordDocStat[k].length; j++) {
					WordStat n1=wordDocStat[k][i];
					WordStat n2=wordDocStat[k][j];
					// check if we still want to keep n1 and n2 in our wordnet
					if(n1.retain && n2.retain){
						Edge newEdge = n1.corpusId > n2.corpusId ? new Edge(n2.corpusId,n1.corpusId) : new Edge(n1.corpusId,n2.corpusId);
						if (n1.corpusId == n2.corpusId)
							System.out.println("unexpected ERROR, please check bug!");
						int count = edgeWeight.containsKey(newEdge) ? edgeWeight
								.get(newEdge) : 0;
						edgeWeight.put(newEdge, count + 1);
					}
				}
		}
		System.out.println("Total number of edges between wordnet:"
				+ edgeWeight.size());
	}

	public void printNets(String outFile) {
		try {
			FileOutputStream fstream = new FileOutputStream(outFile);
			BufferedWriter fout = new BufferedWriter(new OutputStreamWriter(
					fstream, "UTF-8"));
			for (Edge eachEdge : edgeWeight.keySet()) {
				double r1 = edgeWeight.get(eachEdge) * 1.0
						/ wordDocCount[eachEdge.start];
				double r2 = edgeWeight.get(eachEdge) * 1.0
						/ wordDocCount[eachEdge.end];
				// define the weight of edge as pointwise mutual information
				double mInfo = edgeWeight.get(eachEdge) * 1.0 * docsArrays.size()
						/ wordDocCount[eachEdge.start]
						/ wordDocCount[eachEdge.end];
				// define the weight of edge as the product of conditional probability
				double probProd=r1*r2;
				// fout.write(eachEdge.start + "\t" + eachEdge.end + "\t"+
				// String.format("%.6f",prodProd)+ "\n");
				fout.write(eachEdge.start + "\t" + eachEdge.end + "\t"
						+ String.format("%.6f", probProd) + "\n");
				if (r1 > 1.0 || r2 > 1.0)
					System.out.println("Unexpected error, doule check!"+ r1 + ":" + r2);
			}
			fout.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void printVocab(String vobFile) {
		try {
			FileOutputStream fstream = new FileOutputStream(vobFile);
			BufferedWriter fout = new BufferedWriter(new OutputStreamWriter(
					fstream, "UTF-8"));
			for (int i = 0; i < symTab.numSymbols(); i++) {
				String word = symTab.idToSymbol(i);
				fout.write(wordCorpCount[i]+"\t"+i+"\t"+stemToWord.get(word) + "\n");
			}
			fout.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void run(String outFile, String vobFile) {
		// build stem to words mapping. For example wordsMap: <"communit",<"community",4>>;
	   // <"communit",<"communities",2>> <"communit",<"community",4>> means
	   //  community has been stemmed to communit for 4 times.
		setStemCountMap();
		// pick the words based on the wordsMap: in the previous example, 
		// pick stem communit mapped to community instead of communities b/c of maximum likelihood 
		pickWordML();
		// compute all the statistics of words in each document
		findWordsStat();
		// build the words network
		buildNets();
		// print the wordsnet to the file, format: wordId1 wordId2 weight
		printNets(outFile);
		// print the Vocabulary to the file, format: count wordId word
		printVocab(vobFile);
	}

	public static void main(String[] args) {
		String threadFile;
		String networkFile;
		String wordsFile;
		if(args.length==3){
			threadFile=args[0];
			networkFile=args[1];
			wordsFile=args[2];
		}
		else{
			threadFile="/media/netdisk/zzhou/vaccination/ThreadTable.txt";
			networkFile="/media/netdisk/zzhou/temp/net.dat";
			wordsFile="/media/netdisk/zzhou/temp/vob_net.dat";
		}
		WordNetClassifier ex = new WordNetClassifier(threadFile);
		ex.setCutOff(1,-1.0,10.0);
		ex.run(networkFile,wordsFile);
	}

}
