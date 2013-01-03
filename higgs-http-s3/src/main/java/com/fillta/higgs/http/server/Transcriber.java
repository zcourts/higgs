package com.fillta.higgs.http.server;

import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * Essentially a re-writer
 * On receiving a request it modifies the request path based on the rules given
 *
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class Transcriber extends DefaultResourceFilter {
	//sort transcriptions by creation time ensuring FIFO
	Set<Transcription> transcriptions = new TreeSet(new Comparator<Transcription>() {
		public int compare(final Transcription o1, final Transcription o2) {
			if (o1.getCreatedAt() < o2.getCreatedAt())
				return -1;
			if (o1.getCreatedAt() > o2.getCreatedAt())
				return 1;
			return 0;
		}
	});

	public Transcriber(HttpServer<?> server) {
		super(server);
	}

	public Endpoint getEndpoint(HttpRequest request) {
		//apply any transcription
		for (Transcription transcription : transcriptions) {
			//check starts with premise
			if (transcription.hasStartsWith()) {
				if (request.getUri().startsWith(transcription.getStartsWith())) {
					//should we replace the entire request path?
					if (transcription.startsWithReplaceWholeRequest()) {
						request.setUri(transcription.startsWithReplacePath());
					} else {
						String uri = request.getUri();
						//just replace the start
						uri = transcription.startsWithReplacePath() +
								uri.substring(transcription.getStartsWith().length() - 1);
						request.setUri(uri);
					}
					break;
				}
			}
			if (transcription.isMatchable()) {
				if (transcription.matches(request.getUri())) {
					if (transcription.matchesReplaceWholeRequest()) {
						request.setUri(transcription.matchesReplacePath());
					} else {
						String newPath = transcription.replaceMatches(request.getUri());
						request.setUri(newPath);
					}
					break;
				}
			}
		}
		return super.getEndpoint(request);
	}

	/**
	 * Adds a transcription to be applied to matching requests
	 *
	 * @param transcription
	 */
	public void addTranscription(Transcription transcription) {
		transcriptions.add(transcription);
	}

	/**
	 * Convenience method which creates a {@link Transcription} with starts with criteria.
	 * Essentially, if an HTTP request's path starts with the given string, replace it with
	 * the provided alternative
	 *
	 * @param startsWith        the string to check if the request path begins with
	 * @param replaceWith       the string to replace it with if it begins with the startsWith param
	 * @param replaceEntirePath if true then the entire request path is replace, otherwise only the
	 *                          startWith portion is replaced from the start of the string
	 */
	public void addTranscription(String startsWith, String replaceWith, boolean replaceEntirePath) {
		Transcription transcription = new Transcription();
		transcription.setStartsWith(startsWith);
		transcription.startsWithReplacePath(replaceWith);
		transcription.startsWithReplaceWholeRequest(replaceEntirePath);
		addTranscription(transcription);
	}

	/**
	 * Convenience method which creates a {@link Transcription} with a {@link Pattern} criteria.
	 * if an HTTP request's path matches the given regex, replace all occurrences with
	 * the provided alternative
	 *
	 * @param regex             the regex to use for search
	 * @param replaceWith       the string to replace it with if it begins with the startsWith param
	 * @param replaceEntirePath if true then the entire request path is replace, otherwise only the
	 *                          startWith portion is replaced from the start of the string
	 */
	public void addTranscriptionPattern(String regex, String replaceWith, boolean replaceEntirePath) {
		Transcription transcription = new Transcription();
		transcription.setMatches(Pattern.compile(regex));
		transcription.matchesReplacePath(replaceWith);
		transcription.matchesReplaceWholeRequest(replaceEntirePath);
		addTranscription(transcription);
	}

	/**
	 * A Transcription defines a set of actions that can be performed on a request given
	 * one or more of the predefined premises are true.
	 * For e.g. if startsWith is defined, then if a request's path starts with the given string
	 * the request's path can be completely changed or the start of the path can be replaced.
	 */
	public static class Transcription {
		private String startsWith;
		//the path to replace startWith with
		private String startsWithReplacePath;
		private boolean startsWithWholeRequest;
		private String endsWith;
		private String contains;
		private Pattern matches;
		private final long createdAt = System.nanoTime();
		private boolean matchesReplaceWholeRequest;
		private String matchesReplacePath;

		public Transcription() {
		}

		public Transcription(String startsWith, String endsWith, String contains, Pattern matches) {
			this.startsWith = startsWith;
			this.endsWith = endsWith;
			this.contains = contains;
			this.matches = matches;
		}

		public long getCreatedAt() {
			return createdAt;
		}

		public void startsWithReplacePath(String path) {
			startsWithReplacePath = path;
		}

		public void startsWithReplaceWholeRequest(boolean replace) {
			startsWithWholeRequest = replace;
		}

		public String startsWithReplacePath() {
			return startsWithReplacePath;
		}

		public boolean startsWithReplaceWholeRequest() {
			return startsWithWholeRequest;
		}

		public boolean hasStartsWith() {
			return startsWith != null;
		}

		//ends with
		public boolean hasEndsWith() {
			return endsWith != null;
		}

		public boolean hasContains() {
			return contains != null;
		}

		public String getStartsWith() {
			return startsWith;
		}

		public void setStartsWith(final String startsWith) {
			this.startsWith = startsWith;
		}

		public String getEndsWith() {
			return endsWith;
		}

		public void setEndsWith(final String endsWith) {
			this.endsWith = endsWith;
		}

		public String getContains() {
			return contains;
		}

		public void setContains(final String contains) {
			this.contains = contains;
		}


		public boolean isMatchable() {
			return matches != null;
		}

		public Pattern getMatches() {
			return matches;
		}

		public void setMatches(final Pattern matches) {
			this.matches = matches;
		}

		public boolean equals(final Object o) {
			if (this == o) return true;
			if (!(o instanceof Transcription)) return false;
			final Transcription that = (Transcription) o;
			if (contains != null ? !contains.equals(that.contains) : that.contains != null) return false;
			if (endsWith != null ? !endsWith.equals(that.endsWith) : that.endsWith != null) return false;
			if (matches != null ? !matches.equals(that.matches) : that.matches != null) return false;
			if (startsWith != null ? !startsWith.equals(that.startsWith) : that.startsWith != null) return false;
			return true;
		}

		public int hashCode() {
			int result = startsWith != null ? startsWith.hashCode() : 0;
			result = 31 * result + (endsWith != null ? endsWith.hashCode() : 0);
			result = 31 * result + (contains != null ? contains.hashCode() : 0);
			result = 31 * result + (matches != null ? matches.hashCode() : 0);
			return result;
		}

		public boolean matches(final String uri) {
			return matches.matcher(uri).matches();
		}

		public boolean matchesReplaceWholeRequest() {
			return matchesReplaceWholeRequest;
		}

		public String matchesReplacePath() {
			return matchesReplacePath;
		}

		public String replaceMatches(String uri) {
			return matches.matcher(uri).replaceAll(matchesReplacePath());
		}

		public void matchesReplaceWholeRequest(final boolean replace) {
			matchesReplaceWholeRequest = replace;
		}

		public void matchesReplacePath(final String replaceWith) {
			matchesReplacePath = replaceWith;
		}
	}
}
