package com.random;

/*
 Evernote API sample code, structured as a simple command line
 application that demonstrates several API calls.

 To compile (Unix):
 javac -classpath ../../target/evernote-api-*.jar EDAMDemo.java

 To run:
 java -classpath ../../target/evernote-api-*.jar EDAMDemo

 Full documentation of the Evernote API can be found at 
 http://dev.evernote.com/documentation/cloud/
 */

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.evernote.auth.EvernoteAuth;
import com.evernote.auth.EvernoteService;
import com.evernote.clients.ClientFactory;
import com.evernote.clients.NoteStoreClient;
import com.evernote.clients.UserStoreClient;
import com.evernote.edam.error.EDAMErrorCode;
import com.evernote.edam.error.EDAMSystemException;
import com.evernote.edam.error.EDAMUserException;
import com.evernote.edam.notestore.NoteFilter;
import com.evernote.edam.notestore.NoteList;
import com.evernote.edam.type.Note;
import com.evernote.edam.type.NoteSortOrder;
import com.evernote.thrift.transport.TTransportException;

public class TaskNotes {

	/***************************************************************************
	 * You must change the following values before running this sample code *
	 ***************************************************************************/

	// Real applications authenticate with Evernote using OAuth, but for the
	// purpose of exploring the API, you can get a developer token that allows
	// you to access your own Evernote account. To get a developer token, visit
	// https://sandbox.evernote.com/api/DeveloperToken.action
	private static final String AUTH_TOKEN = "your developer token";

	/***************************************************************************
	 * You shouldn't need to change anything below here to run sample code *
	 ***************************************************************************/

	private UserStoreClient userStore;
	private NoteStoreClient noteStore;

	/**
	 * Console entry point.
	 */
	public static void main(String args[]) throws Exception {
		String token = System.getenv("AUTH_TOKEN");
		if (token == null) {
			token = AUTH_TOKEN;
		}
		if ("your developer token".equals(token)) {
			System.err.println("Please fill in your developer token");
			System.err
					.println("To get a developer token, go to https://sandbox.evernote.com/api/DeveloperToken.action");
			return;
		}

		TaskNotes demo = new TaskNotes(token);
		try {
			demo.searchNotes(Integer.valueOf(args[0]));
		} catch (EDAMUserException e) {
			// These are the most common error types that you'll need to
			// handle
			// EDAMUserException is thrown when an API call fails because a
			// paramter was invalid.
			if (e.getErrorCode() == EDAMErrorCode.AUTH_EXPIRED) {
				System.err.println("Your authentication token is expired!");
			} else if (e.getErrorCode() == EDAMErrorCode.INVALID_AUTH) {
				System.err.println("Your authentication token is invalid!");
			} else if (e.getErrorCode() == EDAMErrorCode.QUOTA_REACHED) {
				System.err.println("Your authentication token is invalid!");
			} else {
				System.err.println("Error: " + e.getErrorCode().toString()
						+ " parameter: " + e.getParameter());
			}
		} catch (EDAMSystemException e) {
			System.err.println("System error: " + e.getErrorCode().toString());
		} catch (TTransportException t) {
			System.err.println("Networking error: " + t.getMessage());
		}
	}

	/**
	 * Intialize UserStore and NoteStore clients. During this step, we
	 * authenticate with the Evernote web service. All of this code is
	 * boilerplate - you can copy it straight into your application.
	 */
	public TaskNotes(String token) throws Exception {
		// Set up the UserStore client and check that we can speak to the server
		EvernoteAuth evernoteAuth = new EvernoteAuth(
				EvernoteService.PRODUCTION, token);
		ClientFactory factory = new ClientFactory(evernoteAuth);
		userStore = factory.createUserStoreClient();

		boolean versionOk = userStore.checkVersion("Evernote EDAMDemo (Java)",
				com.evernote.edam.userstore.Constants.EDAM_VERSION_MAJOR,
				com.evernote.edam.userstore.Constants.EDAM_VERSION_MINOR);
		if (!versionOk) {
			System.err.println("Incompatible Evernote client protocol version");
			System.exit(1);
		}

		// Set up the NoteStore client
		noteStore = factory.createNoteStoreClient();
	}

	/**
	 * Search a user's notes and display the results.
	 */
	private void searchNotes(int monthBack) throws Exception {
		// Searches are formatted according to the Evernote search grammar.
		// Learn more at
		// http://dev.evernote.com/documentation/cloud/chapters/Searching_notes.php

		// In this example, we search for notes that have the term "EDAMDemo" in
		// the title.
		// This should return the sample note that we created in this demo app.
		String query = "intitle:Дела" + " " + getMonthRestrictions(monthBack);

		// To search for notes with a specific tag, we could do something like
		// this:
		// String query = "tag:tagname";

		// To search for all notes with the word "elephant" anywhere in them:
		// String query = "elephant";

		NoteFilter filter = new NoteFilter();
		filter.setWords(query);
		filter.setOrder(NoteSortOrder.UPDATED.getValue());
		filter.setAscending(false);

		// Find the first 50 notes matching the search
		System.out.println("Searching for notes matching query: " + query);
		NoteList notes = noteStore.findNotes(filter, 0, 500);
		System.out
				.println("Found " + notes.getTotalNotes() + " matching notes");

		Iterator<Note> iter = notes.getNotesIterator();
		int taskCount = 0;
		Pattern p = Pattern.compile("<div>\\d+\\s?\\.(.*)\\n");
		while (iter.hasNext()) {
			Note note = iter.next();
			// System.out.println("Note: " + note.getTitle());

			// Note objects returned by findNotes() only contain note attributes
			// such as title, GUID, creation date, update date, etc. The note
			// content
			// and binary resource data are omitted, although resource metadata
			// is included.
			// To get the note content and/or binary resources, call getNote()
			// using the note's GUID.
			Note fullNote = noteStore.getNote(note.getGuid(), true, true,
					false, false);
			String noteText = fullNote.getContent();
			Matcher m = p.matcher(noteText);
			while (m.find()) {
				//System.out.println("\"" + m.group(0) + "\"");
				taskCount++;
			}
		}
		System.out.println("Found " + taskCount + " tasks");
	}

	private String getMonthRestrictions(int monthBack) {
		Calendar timeFrom = Calendar.getInstance();
		timeFrom.set(Calendar.DAY_OF_MONTH, 1);
		timeFrom.add(Calendar.MONTH, -monthBack);

		Calendar timeTo = (Calendar) timeFrom.clone();
		timeTo.add(Calendar.MONTH, 1);

		DateFormat format = new SimpleDateFormat("yyyyMMdd");
		return "created:" + format.format(timeFrom.getTime()) + " -created:"
				+ format.format(timeTo.getTime());
	}

}
