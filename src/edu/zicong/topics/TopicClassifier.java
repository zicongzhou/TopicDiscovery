package edu.zicong.topics;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.aliasi.tokenizer.PorterStemmerTokenizerFactory;
import com.aliasi.tokenizer.Tokenizer;
import com.aliasi.tokenizer.TokenizerFactory;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;

public class TopicClassifier {
	protected List<Document> docsArrays;
	protected Map<String, HashMap<String, Integer>> wordsMapHist;
	protected Map<String, String> stemToWord;
	protected int titleBodyWeight;
	protected int numOfWords;
	protected int minCount;

	public TopicClassifier() {
		docsArrays = new ArrayList<Document>();
		wordsMapHist = new HashMap<String, HashMap<String, Integer>>();
		stemToWord = new HashMap<String, String>();
		titleBodyWeight=1;
		minCount=1;
	}

	public TopicClassifier(int weight,int count){
		docsArrays = new ArrayList<Document>();
		wordsMapHist = new HashMap<String, HashMap<String, Integer>>();
		stemToWord = new HashMap<String, String>();
		titleBodyWeight=weight;
		minCount=count;
	}
	public void readDocsFromDir(String docsDirectory) {
		File folder = new File(docsDirectory);
		File[] listOfFiles = folder.listFiles();

		for (int i = 0; i < listOfFiles.length; i++) {
			String fileName = docsDirectory + listOfFiles[i].getName();
			Long threadIds = Long.parseLong(listOfFiles[i].getName()
					.replace(".txt", "").trim());
			String docContent = new String();
			String docSubject = new String();
			try {
				String readPerLine = new String();
				File inFile = new File(fileName);
				BufferedReader bufferReader = new BufferedReader(
						new InputStreamReader(new FileInputStream(inFile)));
				while ((readPerLine = bufferReader.readLine()) != null) {
					String[] stringArrays = readPerLine.split(":");
					if (stringArrays[0].equals("Subject")) {
						docSubject = readPerLine.replace("Subject:", "");
						for (int k = 0; k < titleBodyWeight; k++)
							docContent = docContent + " " + docSubject;
					} else if (stringArrays[0].equals("Body"))
						docContent = docContent + " "
								+ readPerLine.replace("Body:", "");
					else
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
		System.out.println("Total files read from disk:" + listOfFiles.length);
		System.out.println("Total number of documents read: "
				+ docsArrays.size());
	}

	public void readDocsFromDB(String dataCollection) {
		try {
			Mongo m = new Mongo("starsky.ee.ucla.edu", 27017);
			DB db = m.getDB("playstation");
			DBCollection coll = db.getCollection(dataCollection);
			// Set<String> colls = db.getCollectionNames();
			// for (String s : colls) {
			// System.out.println(s);
			// }
			DBCursor cursor = coll.find();
			System.out.println(coll.getCount());
			while (cursor.hasNext()) {
				String threadId = new String();
				String docSubject = new String();
				String docContent = new String();
				DBObject thisObject = cursor.next();
				threadId = thisObject.get("thread_id").toString();
				docSubject = thisObject.get("subject").toString().trim();
				docContent = thisObject.get("body").toString().trim();
				for (int k = 0; k < titleBodyWeight; k++)
					docContent = docSubject + " " + docContent;
				Document currentDoc = new Document();
				currentDoc.setDocIds(Long.parseLong(threadId.trim()));
				currentDoc.setDocSubject(docSubject.toLowerCase());
				currentDoc.setDocContent(docContent.toLowerCase());
				docsArrays.add(currentDoc);
			}
			System.out.println("Items in database: " + coll.getCount());
			System.out.println("Total number of documents read: "
					+ docsArrays.size());
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public void selDocsFromDir(String selThreads,String docsDirectory) {
		List<File> listOfFiles = new ArrayList<File>();
		try {
			String readPerLine = new String();
			File inFile = new File(selThreads);
			BufferedReader bufferReader = new BufferedReader(
					new InputStreamReader(new FileInputStream(inFile)));
			while ((readPerLine = bufferReader.readLine()) != null) {
				listOfFiles.add(new File(readPerLine.trim() + ".txt"));

			}
		} catch (IOException ex) {
			System.out.println("Cannot read the file!");
			ex.printStackTrace();
		}

		for (int i = 0; i < listOfFiles.size(); i++) {
			String fileName = docsDirectory + listOfFiles.get(i).getName();
			Long threadIds = Long.parseLong(listOfFiles.get(i).getName()
					.replace(".txt", "").trim());
			String docContent = new String();
			String docSubject = new String();
			try {
				String readPerLine = new String();
				File inFile = new File(fileName);
				BufferedReader bufferReader = new BufferedReader(
						new InputStreamReader(new FileInputStream(inFile)));
				while ((readPerLine = bufferReader.readLine()) != null) {
					String[] stringArrays = readPerLine.split(":");
					if (stringArrays[0].equals("Subject")) {
						docSubject = readPerLine.replaceFirst("Subject:", "");
						for (int k = 0; k < titleBodyWeight; k++)
							docContent = docSubject + " " + docContent;
					} else if (stringArrays[0].equals("Body"))
						docContent = docContent + " "
								+ readPerLine.replaceFirst("Body:", "");
					else
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

		System.out.println("Total files read from disk:" + listOfFiles.size());
		System.out.println("Total number of documents read: "
				+ docsArrays.size());
	}

	public void readDocsFromMothering(String fileName) {
		try {
			String readPerLine = new String();
			File inFile = new File(fileName);
			BufferedReader bufferReader = new BufferedReader(
					new InputStreamReader(new FileInputStream(inFile)));

			Long threadId = new Long(-1);
			String title = new String();
			String text = new String();
			while ((readPerLine = bufferReader.readLine()) != null) {
				String[] strArrays = readPerLine.split(":");
				if (strArrays[0].equals("ThreadId")) {
					threadId = Long.parseLong(readPerLine.replaceFirst(
							"ThreadId:", ""));
				} else if (strArrays[0].equals("Subject")) {
					title = readPerLine.replaceFirst("Subject:", "");
				} else if (strArrays[0].equals("Body")) {
					text = this.cleanURL(readPerLine.replaceFirst("Body:", ""));
					Document doc = new Document();
					doc.setDocIds(threadId);
					String content = text;
					for (int k = 0; k < titleBodyWeight; k++)
						content = title + " " + content;
					doc.setDocContent(content);
					doc.setDocSubject(title);
					docsArrays.add(doc);
				}
			}
			bufferReader.close();
		} catch (Exception ex) {
			System.out.println("Cannot read the file!");
			ex.printStackTrace();
		}
		System.out.println("Total files read from disk:" + docsArrays.size());
	}
	
	/*
     * build wordsMap Histogram between stemmed token and original token with
     * counts included an example of wordsMap: <"communit",<"community",4>>;
     * <"communit",<"communities",2>> <"communit",<"community",4>> means
     * community has been stemmed to communit for 4 times. Later we will recover
     * the stemmed tokens based on the counts in wordsMap histogram.
     */

	public void setStemCountMap() {
		MyTokenizer myTokenizer = new MyTokenizer();
		// get tokenizer without stemming and perform tokenization for all
		// documents
		TokenizerFactory tokFactory = myTokenizer.getTokenizer();
		for (int i = 0; i < docsArrays.size(); i++) {
			char[] cs = docsArrays.get(i).getDocContent().toString()
					.toCharArray();
			Tokenizer tokenizer = tokFactory.tokenizer(cs, 0, cs.length);
			// for each token in the sentence, get its stemmed token and build
			// wordsMap histogram
			for (String token : tokenizer.tokenize()) {
				String stemToken = PorterStemmerTokenizerFactory.stem(token);
				HashMap<String, Integer> tokensHist = new HashMap<String, Integer>();
				// if stemmed token already exists in the wordsMapHist
				if (wordsMapHist.containsKey(stemToken)) {
					tokensHist = wordsMapHist.get(stemToken);
					Integer value = 0;
					// if token already exists in the map which stemmed token
					// mapped to
					if (tokensHist.containsKey(token)) {
						value = tokensHist.get(token);
					}
					// update counts in the map which stemmed token mapped to
					tokensHist.put(token, value + 1);
				} else {
					tokensHist.put(token, 1);
				}
				// update counts in wordsMapHist with key value of stemmed token
				wordsMapHist.put(stemToken, tokensHist);
			}
		}
	}

	public void pickWordML() {
		for (String word : wordsMapHist.keySet()) {
			HashMap<String, Integer> wordsHist = wordsMapHist.get(word);
			int maxCount = 0;
			String maxString = new String();
			for (String str : wordsHist.keySet())
				if (maxCount < wordsHist.get(str)) {
					maxString = str;
					maxCount = wordsHist.get(str);
				}
			if (stemToWord.containsKey(word))
				System.out.println("Unexpected error, please check word: "
						+ word);
			else
				stemToWord.put(word, maxString);
		}
	}

	public String cleanURL(String content) {
		Pattern urlTag = Pattern.compile("http\\://[\\S]+");
		Matcher matcher = urlTag.matcher(content);
		while (matcher.find()) {
			content = content.replace(matcher.group(), "");
		}
		return content;
	}

	public List<Document> getDocsArrays() {
		return docsArrays;
	}

	public Map<String, HashMap<String, Integer>> getWordsMapHist() {
		return wordsMapHist;
	}

	public Map<String, String> getStemToWord() {
		return stemToWord;
	}
}
