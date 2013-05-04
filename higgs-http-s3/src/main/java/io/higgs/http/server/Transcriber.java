package io.higgs.http.server;

import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

/**
 * Essentially a request re-writer
 * On receiving a request it modifies the request path based on the rules given
 *
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class Transcriber {
    //sort transcriptions by creation time ensuring FIFO
    Set<Transcription> transcriptions = new TreeSet<>(new Comparator<Transcription>() {
        public int compare(final Transcription o1, final Transcription o2) {
            if (o1.getCreatedAt() < o2.getCreatedAt()) {
                return -1;
            }
            if (o1.getCreatedAt() > o2.getCreatedAt()) {
                return 1;
            }
            return 0;
        }
    });

    public void transcribe(HttpRequest request) {
        //apply any transcription
        for (Transcription transcription : transcriptions) {
            if (transcription.matches(request.getUri())) {
                if (transcription.isReplaceWholeRequest()) {
                    request.setUri(transcription.getReplacementPath());
                } else {
                    String newPath;
                    if (transcription.isReplaceFirstOccurrence()) {
                        newPath = transcription.replaceFirstMatch(request.getUri());
                    } else {
                        newPath = transcription.replaceAllMatches(request.getUri());
                    }
                    request.setUri(newPath);
                }
                break;
            }
        }
    }

    /**
     * Adds a transcription to be applied to matching requests
     *
     * @param transcription
     */
    public void addTranscription(Transcription transcription) {
        transcriptions.add(transcription);
    }

}
