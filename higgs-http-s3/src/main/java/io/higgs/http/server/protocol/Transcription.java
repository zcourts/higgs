package io.higgs.http.server.protocol;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */

import io.higgs.core.Sortable;

import java.util.regex.Pattern;

/**
 * A Transcription defines a set of actions that can be performed on a request given
 * a predefined premise is true.
 */
public class Transcription implements Sortable<Transcription> {
    private Pattern pattern;
    private final long createdAt = System.nanoTime();
    private boolean replaceWholeRequest;
    private String replacementPath;
    private boolean replaceFirstOccurrence;
    private int priority;

    /**
     * Convenience method which creates a {@link Transcription} with a {@link java.util.regex.Pattern} criteria.
     * if an HTTP request's path pattern the given regex, replace all occurrences with
     * the provided alternative
     *
     * @param regex               the regex to use for search
     * @param replaceWith         the string to replace it with if it begins with the startsWith param
     * @param replaceEntirePath   if true then the entire request path is replace, otherwise only the
     *                            startWith portion is replaced from the start of the string
     * @param firstOccurrenceOnly if true only the first occurrence of the pattern is replaced/re-written
     */
    public Transcription(Pattern regex, String replaceWith, boolean replaceEntirePath,
                         boolean firstOccurrenceOnly) {
        if (regex == null) {
            throw new NullPointerException("A regex is required");
        }
        if (replaceWith == null) {
            throw new NullPointerException("A replacement string is required");
        }
        //setPattern(Pattern.compile(regex));
        this.pattern = regex;
        setReplacementPath(replaceWith);
        setReplaceWholeRequest(replaceEntirePath);
        setReplaceFirstOccurrence(firstOccurrenceOnly);
    }

    /**
     * Rewrites/replaced a request based on the provided options
     *
     * @param regex             a regex to match
     * @param replaceWith       a replacement string to be used to replace matches
     * @param replaceEntirePath if true the request's entire path is replaced with the replacement string
     */
    public Transcription(String regex, String replaceWith, boolean replaceEntirePath) {
        this(Pattern.compile(regex), replaceWith, replaceEntirePath, false);
    }

    /**
     * Replaces/re-writes the entire request path with the given replacement
     *
     * @param regex       the regex to match
     * @param replaceWith the string to replace with
     */
    public Transcription(String regex, String replaceWith) {
        this(Pattern.compile(regex), replaceWith, true, false);
    }


    public Transcription(Pattern pattern, String replaceWith) {
        this(pattern, replaceWith, true, false);
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public Pattern getPattern() {
        return pattern;
    }

    public void setPattern(final Pattern pattern) {
        if (pattern == null) {
            throw new NullPointerException("You must provide a transcription pattern");
        }
        this.pattern = pattern;
    }

    public boolean matches(final String uri) {
        return pattern.matcher(uri).matches();
    }

    public boolean isReplaceWholeRequest() {
        return replaceWholeRequest;
    }

    public void setReplaceWholeRequest(final boolean replace) {
        replaceWholeRequest = replace;
    }

    public String getReplacementPath() {
        return replacementPath;
    }

    public void setReplacementPath(final String replaceWith) {
        replacementPath = replaceWith;
    }

    public String replaceAllMatches(String uri) {
        return pattern.matcher(uri).replaceAll(getReplacementPath());
    }

    public String replaceFirstMatch(String uri) {
        return pattern.matcher(uri).replaceFirst(getReplacementPath());
    }

    public boolean isReplaceFirstOccurrence() {
        return replaceFirstOccurrence;
    }

    public void setReplaceFirstOccurrence(final boolean firstOccurrenceOnly) {
        this.replaceFirstOccurrence = firstOccurrenceOnly;
    }

    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Transcription)) {
            return false;
        }
        final Transcription that = (Transcription) o;
        if (createdAt != that.createdAt) {
            return false;
        }
        if (replaceFirstOccurrence != that.replaceFirstOccurrence) {
            return false;
        }
        if (replaceWholeRequest != that.replaceWholeRequest) {
            return false;
        }
        if (pattern != null ? !pattern.equals(that.pattern) : that.pattern != null) {
            return false;
        }
        return !(replacementPath != null ? !replacementPath.equals(that.replacementPath) :
                that.replacementPath != null);
    }

    public int hashCode() {
        int result = pattern != null ? pattern.hashCode() : 0;
        result = 31 * result + (int) (createdAt ^ (createdAt >>> 32));
        result = 31 * result + (replaceWholeRequest ? 1 : 0);
        result = 31 * result + (replacementPath != null ? replacementPath.hashCode() : 0);
        result = 31 * result + (replaceFirstOccurrence ? 1 : 0);
        return result;
    }

    @Override
    public int priority() {
        return priority;
    }

    @Override
    public int compareTo(Transcription that) {
        return that.priority() - this.priority();
    }
}
