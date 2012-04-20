/*
 * Define Document class, which includes document id, document subject,
 * document content and document topics.
 * 
 * Zicong Zhou, March 2011
 * 
 */


package edu.zicong.topics;

import java.util.ArrayList;

public class Document {
	// id of the document, represented by the id of root message in the thread
	private Long docID;
	// subject of the document, represented by the subject of root message in the thread
	private String docSubject;
	/* content of the document, represented by several(defined by weight) duplicates subject
	 * and bodies in all the messages in the thread
	 */
	private CharSequence docContent;
	/* topics of the document, using data structure of ArrayList. Each String ArrayList 
	 * is the keywords in the topic, and one document can have several topics. 
	 */
	private ArrayList<ArrayList<String>> docTopics;
	
	public Document(){
		docID=new Long(0);
		docSubject=new String();
		docContent=new String();
		docTopics=new ArrayList<ArrayList<String>>();
	}
	
	public Long getDocIDs(){
		return docID;
	}
	
	public String getDocSubject(){
		return docSubject;
	}
	
	public CharSequence getDocContent(){
		return docContent;
	}
	
	public ArrayList<ArrayList<String>> getDocTopics(){
		return docTopics;
	}
	
	public void setDocIds(Long ids){
		docID=ids;
	}
	
	public void setDocSubject(String str){
		docSubject=str;
	}

	public void setDocContent(String str){
		docContent=str;
	}
	
	public void addTopics(ArrayList<String> topic){
		docTopics.add(topic);
	}
}
