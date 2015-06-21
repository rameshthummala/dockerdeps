package org.docker.hackathon.util;

import java.io.*;
import java.util.*;
import java.text.MessageFormat;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Date;

import org.eclipse.egit.github.core.*;
import org.eclipse.egit.github.core.client.*;
import org.eclipse.egit.github.core.service.*;
import org.eclipse.egit.github.core.util.*;

import java.io.IOException;

public class GitRepository {
    GitHubClient client ;

    public GitRepository() {
	try {
		//Basic authentication
		client = new GitHubClient();
		client.setCredentials("YOURUSER", "YOURCRED");
	} catch(Exception e) {
	    System.out.println("Exception while trying to connect to github. Exiting.");
	    e.printStackTrace();
	    System.exit(1);    
	}
    }

    private void listRepositories() throws IOException {
	final String user = "rameshthummala";
	final String format = "{0}) {1}- created on {2}";
	int count = 1;
	RepositoryService service = new RepositoryService();
	for (Repository repo : service.getRepositories(user))
		System.out.println(MessageFormat.format(format, count++, repo.getName(), repo.getCreatedAt()));
    }

    private void listCommits() throws IOException {
	final int size = 25;
		final RepositoryId repo = new RepositoryId("rameshthummala", "dockerdeps");
		final String message = "   {0} by {1} on {2}";
		final CommitService service = new CommitService();
		int pages = 1;
		for (Collection<RepositoryCommit> commits : service.pageCommits(repo,
				size)) {
			System.out.println("Commit Page " + pages++);
			for (RepositoryCommit commit : commits) {
				String sha = commit.getSha().substring(0, 7);
				String author = commit.getCommit().getAuthor().getName();
				Date date = commit.getCommit().getAuthor().getDate();
				System.out.println(MessageFormat.format(message, sha, author,
						date));
			}
		}
    }

    public static void main(String[] args) {
	try {
	    GitRepository myRepo = new GitRepository();
	    myRepo.listRepositories();
	    myRepo.listCommits();
	} catch(Exception e) {
	    System.out.println("Exception while trying to connect to github. Exiting.");
	    e.printStackTrace();
	    System.exit(1);    
	}
    }
}
