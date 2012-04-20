package edu.zicong.topics;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import com.aliasi.classify.BaseClassifier;
import com.aliasi.classify.Classification;
import com.aliasi.util.AbstractExternalizable;

import com.mongodb.Mongo;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.DBCursor;

public class ThreadParser {
	private Map<Long, Message> threadTable;

	public ThreadParser() {
		threadTable = new HashMap<Long, Message>();
	}

	public Map<Long, Message> getThreads() {
		return threadTable;
	}

	public void appendTags(String file1, String file2) {
		try {
			String readPerLine = new String();
			File inFile = new File(file1);
			BufferedReader bufferReader = new BufferedReader(
					new InputStreamReader(new FileInputStream(inFile), "UTF-8"));
			FileOutputStream fstream = new FileOutputStream(file2);
			BufferedWriter fout = new BufferedWriter(new OutputStreamWriter(
					fstream, "UTF-8"));
			fout.write("<messages>\n");
			while ((readPerLine = bufferReader.readLine()) != null) {
				fout.write(readPerLine);
				fout.write("\n");
			}
			fout.write("</messages>\n");
			fout.close();
			bufferReader.close();
		} catch (Exception ex) {
			System.out.println("Cannot read/write the file!");
			ex.printStackTrace();
		}
	}

	public void mergeThreads(List<Message> megLists) {
		Set<Long> messageSet = new TreeSet<Long>();
		for (int i = 0; i < megLists.size(); i++) {
			if (!messageSet.contains(megLists.get(i).getMessageId())) {
				Message currThreads = new Message();
				if (threadTable.containsKey(megLists.get(i).getThreadId())) 
					currThreads = threadTable.get(megLists.get(i).getThreadId());
				
				// If the new message is the root message, update thread information
				if (megLists.get(i).getThreadId().equals(megLists.get(i).getMessageId())) {
					currThreads.setSubject(megLists.get(i).getSubject());
					currThreads.setRoot();
					currThreads.setMessageId(megLists.get(i).getMessageId());
				}
				// Just check if the board information in consistent
//				if (currThreads.getBoardId() > 0
//						&& !currThreads.getBoardId().equals(
//								megLists.get(i).getBoardId()))
//					System.out.println("Error in board informaton: " + currThreads.getBoardId() + "vs"
//							+ megLists.get(i).getBoardId());

				// update aggregate body for messages in the same thread
				currThreads.setBoardId(megLists.get(i).getBoardId());
				currThreads.setThreadId(megLists.get(i).getThreadId());
				currThreads.setBody(megLists.get(i).getBody()
						+ currThreads.getBody());

				threadTable.put(megLists.get(i).getThreadId(), currThreads);
			}
			messageSet.add(megLists.get(i).getMessageId());
		}

	}

	public void langClassifier(File modelFile) {
		BaseClassifier<CharSequence> classifier;

		try {
			// @SuppressWarnings("unchecked")
			classifier = (BaseClassifier<CharSequence>) AbstractExternalizable
					.readObject(modelFile);
			// update language information
			int enCounter = 0;
			for (Long threadId : threadTable.keySet()) {
				Message currMessage = threadTable.get(threadId);
				Classification classification = classifier.classify(currMessage.getBody());
				currMessage.setLanguage(classification.bestCategory().toString());
				if (classification.bestCategory().toString().equals("en"))
					enCounter++;
			}
			System.out.println("Total number of Threads extracted: "
					+ threadTable.size());
			System.out.println("Total number of English Threads extracted: " + enCounter);
		} catch (Exception ex) {
			ex.printStackTrace();
			System.out.println("Error! Cannot read the model file!");
		}

	}

	public void writeToDB(String collName) {
		try {
			int enCounter=0;
			Mongo m = new Mongo("starsky.ee.ucla.edu",27017);
			DB db = m.getDB("playstation");
			DBCollection coll = db.getCollection(collName);
			for (Long threadId : threadTable.keySet()) {
				Message curMeg = threadTable.get(threadId);
				if (curMeg.getLanguage().equals("en")) {
					enCounter++;
					BasicDBObject doc = new BasicDBObject();
			        doc.put("thread_id", threadId);
			        doc.put("subject", curMeg.getSubject());
			        doc.put("body", curMeg.getBody());
			        coll.insert(doc);

				}
			}
			System.out.println("Total number of threads saved to database: " + enCounter);
		} catch (Exception e) {
			System.out.println("Error! Cannot save to the database!");
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		try {
			SAXParserFactory spf = SAXParserFactory.newInstance();
			SAXParser saxParser = spf.newSAXParser();
			String logFile = new String("/media/netdisk/zzhou/playstation/201009.log");
//			String xmlFile = "/media/netdisk/zzhou/data/playstationeu/xml/201009.xml";
			String xmlFile = logFile.replace("log", "xml");
			ThreadParser threadProcessor = new ThreadParser();
			threadProcessor.appendTags(logFile, xmlFile);

			XMLSAXParser XMLParser = new XMLSAXParser();
			List<Message> megList = XMLParser.getMessageList();

			saxParser.parse(new File(xmlFile), XMLParser);
			threadProcessor.mergeThreads(megList);
			String langModelFile=new String("/media/netdisk/zzhou/mycode/load/langid-leipzig.classifier");
			threadProcessor.langClassifier(new File(langModelFile));
			String dataDate=logFile.split("/")[logFile.split("/").length-1].replace(".log", "");
			threadProcessor.writeToDB(dataDate);
			} catch (Exception e) {
				System.out.println("ERROR! Cannot parse the XML file!");
				e.printStackTrace();
		}
	}

}
