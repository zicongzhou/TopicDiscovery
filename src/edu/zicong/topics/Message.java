package edu.zicong.topics;

import java.util.*;
import java.util.regex.*;

class Message {

	private Long messageId;
	private Long threadId;
	private Long boardId;
	private Long authorId;
	private Long parentId;
	private Boolean root;
	private String subject;
	private String body;
	private String language;
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

	public Message() {
		messageId = new Long(-1);
		threadId = new Long(-1);
		boardId = new Long(-1);
		authorId = new Long(-1);
		parentId = new Long(-1);
		root = false;
		body = new String();
		subject = new String();
		language = "unknown";
	}

	public Long getMessageId() {
		return messageId;
	}

	public Long getThreadId() {
		return threadId;
	}

	public Long getBoardId() {
		return boardId;
	}

	public Long getAuthorId() {
		return authorId;
	}

	public Long getParentId() {
		return parentId;
	}

	public Boolean getRoot() {
		return root;
	}

	public String getSubject() {
		return subject;
	}

	public String getBody() {
		return body;
	}

	public String getLanguage() {
		return language;
	}

	public void setMessageId(Long id) {
		messageId = id;
	}

	public void setThreadId(Long id) {
		threadId = id;
	}

	public void setBoardId(Long id) {
		boardId = id;
	}

	public void setAuthorId(Long id) {
		authorId = id;
	}

	public void setParentId(Long id) {
		parentId = id;
	}

	public void setRoot() {
		root = true;
	}

	public void setSubject(String str) {
		subject = str;
	}

	public void setBody(String str) {
		body = str;
	}

	public void setLanguage(String str) {
		language = str;
	}

	public void cleanSubject() {
		for (int i = 0; i < xmlPattern.size(); i++) {
			subject = subject.replace(xmlPattern.get(i), " ");
		}
	}

	public void cleanBody() {
		for (int i = 0; i < xmlPattern.size(); i++) {
			body = body.replace(xmlPattern.get(i), " ");
		}
		for (int i = 0; i < infoPattern.size(); i++) {
			Pattern p = Pattern.compile(infoPattern.get(i));
			Matcher m = p.matcher(body);
			body = m.replaceAll(" ");
		}
	}
}