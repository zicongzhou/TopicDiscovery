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

public class Preprocesser {
	private static final ArrayList<String> infoPattern;
	private static final ArrayList<String> xmlPattern;
	static {
		xmlPattern = new ArrayList<String>();
		xmlPattern.add("&gt");
		xmlPattern.add("&lt");
		xmlPattern.add("&nbsp");
		xmlPattern.add("&amp");
		xmlPattern.add("&quot");
		xmlPattern.add("&apos");
		infoPattern = new ArrayList<String>();
		infoPattern.add("<BLOCKQUOTE>[\\s\\S]*?</BLOCKQUOTE>");
		infoPattern
				.add("<!--\\[if gte mso [\\d]+\\]>[\\s\\S]*?<!\\[endif\\]-->");
		infoPattern.add("<[\\s\\S]*?>");
		infoPattern
				.add("Message Edited by [\\S]+? on\\s*\\d\\d-\\d\\d-\\d\\d\\d\\d\\s*\\d\\d:\\d\\d [PA]M");
		infoPattern.add("[pP]hoto by[\\s]+[\\S]+");
		infoPattern.add("http://[\\S]+");
		infoPattern.add("www.[\\S]+");
	};

	public void cleanData(String docsDirectory) {
		File folder = new File(docsDirectory);
		File[] listOfFiles = null;
		listOfFiles = folder.listFiles();
		System.out.println("total number of files:"+listOfFiles.length);
		for (int i = 0; i < listOfFiles.length; i++) {
			String fileName = docsDirectory + "/" + listOfFiles[i].getName();
			System.out.println(listOfFiles[i].toString());
			Long threadIds = Long.parseLong(listOfFiles[i].getName()
					.replace(".txt", "").replace("MotheringThread", "").trim());
			System.out.println(threadIds);
			String thisText = new String();
			String thisTitle = new String();
			try {
				String readPerLine = new String();
				File inFile = new File(fileName);
				BufferedReader bufferReader = new BufferedReader(
						new InputStreamReader(new FileInputStream(inFile)));
				
				boolean textFlag = false;
				boolean threadFlag = false;
				while ((readPerLine = bufferReader.readLine()) != null) {
					
					if (readPerLine.trim().equals("THREAD TITLE:")) {
						threadFlag=true;
					}
					else if(readPerLine.trim().equals("TEXT:")){
						textFlag=true;
					}
					else if(readPerLine.trim().equals("META DATA:") || readPerLine.trim().equals("POSTS:") || readPerLine.trim().equals("END OF POSTS ON THIS PAGE")){
						textFlag=false;
						threadFlag=false;
					}
					else{
						if(textFlag)
							thisText=thisText+readPerLine;
						if(threadFlag)
							thisTitle=thisTitle+readPerLine;
					}

				}
				bufferReader.close();
		//		System.out.println(thisText);
				System.out.println("************************************");
		//		System.out.println(thisTitle);
			} catch (Exception ex) {
				System.out.println("Cannot read the file!");
				ex.printStackTrace();
			}
			try {
				File outFile=new File("/media/netdisk/zzhou/threads/"+threadIds.toString()+".txt");
				FileOutputStream fstream = new FileOutputStream(outFile);
				BufferedWriter fout = new BufferedWriter(new OutputStreamWriter(
						fstream, "UTF-8"));
				fout.write("Subject:"+thisTitle+"\n");
				fout.write("Body:"+thisText+"\n");
				fout.close();
			}
			catch (Exception ex) {
				System.out.println("Cannot read the file!");
				ex.printStackTrace();
			}
			
		}
	}

	public static void main(String[] args) {
		Preprocesser test=new Preprocesser();
		test.cleanData("/media/netdisk/zzhou/mothering");

	}
}
