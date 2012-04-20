package edu.zicong.topics;

import java.util.ArrayList;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class XMLSAXParser extends DefaultHandler {
	private List<Message> messageLists;

	public XMLSAXParser() {
		messageLists = new ArrayList<Message>();
	}

	private Message currMessage;

	private int subjectCounter;

	private boolean startUid;

	private boolean startBody;

	private boolean endBody;

	private boolean startSubject;

	private boolean endSubject;

	private List<String> uidArrays;

	private String subjectStr;

	private String bodyStr;

	public void startDocument() throws SAXException {
		currMessage = new Message();
		subjectCounter = 0;
		startUid = false;
		startBody = false;
		endBody = false;
		startSubject = false;
		endSubject = false;
		uidArrays = new ArrayList<String>();
		subjectStr = new String();
		bodyStr = new String();
	}

	public void endDocument() throws SAXException {

		for (int i = 0; i < messageLists.size(); i++) {
			messageLists.get(i).cleanSubject();
			messageLists.get(i).cleanBody();
		}
		System.out.println("Total number of Messages extracted from XML: "
				+ messageLists.size());
	}

	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {
		if (qName.equals("uid")) {
			startUid = true;
		} else if (qName.equals("subject")) {
			if (subjectCounter == 0) {
				startSubject = true;
			}
		} else if (qName.equals("body")) {
			startBody = true;
		}
	}

	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		if (qName.equals("subject")) {
			if (subjectCounter == 0) {
				endSubject = true;
			}
			subjectCounter = subjectCounter + 1;
		} else if (qName.equals("body")) {
			endBody = true;
		} else if (qName.equals("message")) {
			// meaning this message is the root of thread
			if (uidArrays.size() == 6) {
				currMessage
						.setAuthorId(Long.parseLong(uidArrays.get(0).trim()));
				currMessage.setMessageId(Long
						.parseLong(uidArrays.get(1).trim()));
				currMessage.setBoardId(Long.parseLong(uidArrays.get(2).trim()));
				currMessage
						.setThreadId(Long.parseLong(uidArrays.get(3).trim()));
				if (!uidArrays.get(3).equals(uidArrays.get(4))) {
					System.out
							.println("Found inconsistent message, please double check!");
					System.out.println(uidArrays.size()
							+ "**************************");
					for (int i = 0; i < uidArrays.size(); i++)
						System.out.println(Long.parseLong(uidArrays.get(i)
								.trim()));
				}
				// meaning this message is not the root of thread
			} else if (uidArrays.size() == 7) {
				currMessage
						.setAuthorId(Long.parseLong(uidArrays.get(0).trim()));
				currMessage.setMessageId(Long
						.parseLong(uidArrays.get(1).trim()));
				currMessage.setBoardId(Long.parseLong(uidArrays.get(2).trim()));
				currMessage
						.setThreadId(Long.parseLong(uidArrays.get(4).trim()));
				if (!uidArrays.get(4).equals(uidArrays.get(5))) {
					System.out
							.println("Found inconsistent message, please double check!");
					System.out.println(uidArrays.size()
							+ "**************************");
					for (int i = 0; i < uidArrays.size(); i++)
						System.out.println(Long.parseLong(uidArrays.get(i)
								.trim()));
				}
			} else {
				System.out.println("Something unexpected!");
			}
			messageLists.add(currMessage);
			subjectCounter = 0;
			currMessage = new Message();
			subjectStr = new String();
			bodyStr = new String();
			uidArrays = new ArrayList<String>();
		}
	}

	public void characters(char ch[], int start, int length)
			throws SAXException {
		if (startUid) {
			uidArrays.add(new String(ch, start, length));
			startUid = false;
		}

		if (startBody) {
			bodyStr = bodyStr + new String(ch, start, length);
			if (endBody) {
				currMessage.setBody(bodyStr);
				startBody = false;
				endBody = false;
			}
		}
		if (startSubject) {
			subjectStr = subjectStr + new String(ch, start, length);
			if (endSubject) {
				currMessage.setSubject(subjectStr);
				startSubject = false;
				endSubject = false;
			}
		}

	}

	public List<Message> getMessageList() {
		return messageLists;
	}
}
