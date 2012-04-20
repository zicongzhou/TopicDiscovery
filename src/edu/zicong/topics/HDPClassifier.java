package edu.zicong.topics;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;

import com.aliasi.cluster.LatentDirichletAllocation;
import com.aliasi.symbol.MapSymbolTable;
import com.aliasi.symbol.SymbolTable;
import com.aliasi.tokenizer.TokenizerFactory;

public class HDPClassifier extends TopicClassifier {

	public HDPClassifier(String dataCollection) {
		readDocsFromDir(dataCollection);
	}

	public void getHDPReady(String outFile, String vFile) {

		MyTokenizer myTokenizer = new MyTokenizer();
		TokenizerFactory tokFactory = myTokenizer.getStemTokenizer();
		CharSequence[] text = new String[docsArrays.size()];
		for (int i = 0; i < docsArrays.size(); i++) {
			text[i] = docsArrays.get(i).getDocContent();
		}
		SymbolTable symTab = new MapSymbolTable();
		int[][] docWords = LatentDirichletAllocation.tokenizeDocuments(text,
				tokFactory, symTab, minCount);

		try {
			FileOutputStream fstream = new FileOutputStream(outFile);
			BufferedWriter fout = new BufferedWriter(new OutputStreamWriter(
					fstream));
			System.out.println("Total number of documents:" + docWords.length);
			for (int i = 0; i < docWords.length; i++) {
				Map<Integer, Integer> tokenCount = new HashMap<Integer, Integer>();
				for (int j = 0; j < docWords[i].length; j++) {
					int count = tokenCount.containsKey(docWords[i][j]) ? tokenCount
							.get(docWords[i][j]) : 0;
					tokenCount.put(docWords[i][j], count + 1);
				}
				Integer totalCount = tokenCount.size();
				if (totalCount > 0) {
					fout.write(totalCount.toString());
					for (Integer token : tokenCount.keySet())
						fout.write(" " + token + ":" + tokenCount.get(token));
					fout.write("\n");
				} else
					System.out
							.println("Empty document after filtering, skipping document "
									+ docsArrays.get(i).getDocIDs());
			}
			fout.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			FileOutputStream fstream = new FileOutputStream(vFile);
			BufferedWriter fout = new BufferedWriter(new OutputStreamWriter(
					fstream, "UTF-8"));
			for (int i = 0; i < symTab.numSymbols(); i++) {
				String word = symTab.idToSymbol(i);
				fout.write(i + "\t" + stemToWord.get(word) + "\n");
			}
			fout.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void run(String outFile, String vFile) {
		setStemCountMap();
		pickWordML();
		getHDPReady(outFile, vFile);
	}

	public static void main(String[] args) {
		HDPClassifier ex = new HDPClassifier("/media/netdisk/zzhou/playstation/201006/");
		ex.run("/home/zzhou/Downloads/hdp/sony.txt",
				"/home/zzhou/Downloads/hdp/sony_vocab.txt");
	}

}
