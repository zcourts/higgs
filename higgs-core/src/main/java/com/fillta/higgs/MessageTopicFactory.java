package com.fillta.higgs;

/**
 * Given an incoming message a topic factory can extract the message's topic.
 * If used in a multi-threaded Queueing strategy this factory should use new instances of its
 * extractor as opposed to trying to use a single instance to extract all topics
 *
 * @author Courtney Robinson <courtney@crlog.info>
 */
public interface MessageTopicFactory<T, IM> {
	public T extract(IM msg);
}
