package com.fillta.higgs.http.server.resource;

import java.util.*;

/**
 * An abstraction for a media type. Instances are immutable.
 * Based on <a href="http://docs.oracle.com/javaee/6/api/javax/ws/rs/core/MediaType.html">Oracle Jersey MediaType</a>
 *
 * @see <a href="http://docs.oracle.com/javaee/6/api/javax/ws/rs/core/package-summary.html">Oracle Jersey docs</a>
 * @see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.7">HTTP/1.1 section 3.7</a>
 */
public class MediaType {

	private String type;
	private String subtype;
	private Map<String, String> parameters;

	/**
	 * Empty immutable map used for all instances without parameters
	 */
	private static final Map<String, String> emptyMap = Collections.emptyMap();

	/**
	 * The value of a type or subtype wildcard: "*"
	 */
	public static final String MEDIA_TYPE_WILDCARD = "*";

	// Common media type constants
	/**
	 * "*&#47;*"
	 */
	public final static String WILDCARD = "*/*";
	/**
	 * "*&#47;*"
	 */
	public final static MediaType WILDCARD_TYPE = new MediaType();

	/**
	 * "application/xml"
	 */
	public final static String APPLICATION_XML = "application/xml";
	/**
	 * "application/xml"
	 */
	public final static MediaType APPLICATION_XML_TYPE = new MediaType("application", "xml");

	/**
	 * "application/atom+xml"
	 */
	public final static String APPLICATION_ATOM_XML = "application/atom+xml";
	/**
	 * "application/atom+xml"
	 */
	public final static MediaType APPLICATION_ATOM_XML_TYPE = new MediaType("application", "atom+xml");

	/**
	 * "application/xhtml+xml"
	 */
	public final static String APPLICATION_XHTML_XML = "application/xhtml+xml";
	/**
	 * "application/xhtml+xml"
	 */
	public final static MediaType APPLICATION_XHTML_XML_TYPE = new MediaType("application", "xhtml+xml");

	/**
	 * "application/svg+xml"
	 */
	public final static String APPLICATION_SVG_XML = "application/svg+xml";
	/**
	 * "application/svg+xml"
	 */
	public final static MediaType APPLICATION_SVG_XML_TYPE = new MediaType("application", "svg+xml");

	/**
	 * "application/json"
	 */
	public final static String APPLICATION_JSON = "application/json";
	/**
	 * "application/json"
	 */
	public final static MediaType APPLICATION_JSON_TYPE = new MediaType("application", "json");

	/**
	 * "application/x-www-form-urlencoded"
	 */
	public final static String APPLICATION_FORM_URLENCODED = "application/x-www-form-urlencoded";
	/**
	 * "application/x-www-form-urlencoded"
	 */
	public final static MediaType APPLICATION_FORM_URLENCODED_TYPE = new MediaType("application", "x-www-form-urlencoded");

	/**
	 * "multipart/form-data"
	 */
	public final static String MULTIPART_FORM_DATA = "multipart/form-data";
	/**
	 * "multipart/form-data"
	 */
	public final static MediaType MULTIPART_FORM_DATA_TYPE = new MediaType("multipart", "form-data");

	/**
	 * "application/octet-stream"
	 */
	public final static String APPLICATION_OCTET_STREAM = "application/octet-stream";
	/**
	 * "application/octet-stream"
	 */
	public final static MediaType APPLICATION_OCTET_STREAM_TYPE = new MediaType("application", "octet-stream");

	/**
	 * "text/plain"
	 */
	public final static String TEXT_PLAIN = "text/plain";
	/**
	 * "text/plain"
	 */
	public final static MediaType TEXT_PLAIN_TYPE = new MediaType("text", "plain");

	/**
	 * "text/xml"
	 */
	public final static String TEXT_XML = "text/xml";
	/**
	 * "text/xml"
	 */
	public final static MediaType TEXT_XML_TYPE = new MediaType("text", "xml");

	/**
	 * "text/html"
	 */
	public final static String TEXT_HTML = "text/html";
	/**
	 * "text/html"
	 */
	public final static MediaType TEXT_HTML_TYPE = new MediaType("text", "html");

	/**
	 * Creates a new instance of MediaType with the supplied type, subtype and
	 * parameters.
	 *
	 * @param type       the primary type, null is equivalent to
	 *                   {@link #MEDIA_TYPE_WILDCARD}.
	 * @param subtype    the subtype, null is equivalent to
	 *                   {@link #MEDIA_TYPE_WILDCARD}.
	 * @param parameters a map of media type parameters, null is the same as an
	 *                   empty map.
	 */
	public MediaType(String type, String subtype, Map<String, String> parameters) {
		this.type = type == null ? MEDIA_TYPE_WILDCARD : type;
		this.subtype = subtype == null ? MEDIA_TYPE_WILDCARD : subtype;
		if (parameters == null) {
			this.parameters = emptyMap;
		} else {
			Map<String, String> map = new TreeMap<String, String>(new Comparator<String>() {
				public int compare(String o1, String o2) {
					return o1.compareToIgnoreCase(o2);
				}
			});
			for (Map.Entry<String, String> e : parameters.entrySet()) {
				map.put(e.getKey().toLowerCase(), e.getValue());
			}
			this.parameters = Collections.unmodifiableMap(map);
		}
	}

	/**
	 * Creates a new instance of MediaType with the supplied type and subtype.
	 *
	 * @param type    the primary type, null is equivalent to
	 *                {@link #MEDIA_TYPE_WILDCARD}
	 * @param subtype the subtype, null is equivalent to
	 *                {@link #MEDIA_TYPE_WILDCARD}
	 */
	public MediaType(String type, String subtype) {
		this(type, subtype, emptyMap);
	}

	/**
	 * Creates a new instance of MediaType, both type and subtype are wildcards.
	 * Consider using the constant {@link #WILDCARD_TYPE} instead.
	 */
	public MediaType() {
		this(MEDIA_TYPE_WILDCARD, MEDIA_TYPE_WILDCARD);
	}

	/**
	 * Getter for primary type.
	 *
	 * @return value of primary type.
	 */
	public String getType() {
		return this.type;
	}

	/**
	 * Checks if the primary type is a wildcard.
	 *
	 * @return true if the primary type is a wildcard
	 */
	public boolean isWildcardType() {
		return this.getType().equals(MEDIA_TYPE_WILDCARD);
	}

	/**
	 * Getter for subtype.
	 *
	 * @return value of subtype.
	 */
	public String getSubtype() {
		return this.subtype;
	}

	/**
	 * Checks if the subtype is a wildcard
	 *
	 * @return true if the subtype is a wildcard
	 */
	public boolean isWildcardSubtype() {
		return this.getSubtype().equals(MEDIA_TYPE_WILDCARD);
	}

	/**
	 * Getter for a read-only parameter map. Keys are case-insensitive.
	 *
	 * @return an immutable map of parameters.
	 */
	public Map<String, String> getParameters() {
		return parameters;
	}

	/**
	 * Check if this media type is compatible with another media type. E.g.
	 * image/* is compatible with image/jpeg, image/png, etc. Media type
	 * parameters are ignored. The function is commutative.
	 *
	 * @param other the media type to compare with
	 * @return true if the types are compatible, false otherwise.
	 */
	public boolean isCompatible(MediaType other) {
		if (other == null)
			return false;
		if (type.equals(MEDIA_TYPE_WILDCARD) || other.type.equals(MEDIA_TYPE_WILDCARD))
			return true;
		else if (type.equalsIgnoreCase(other.type) && (subtype.equals(MEDIA_TYPE_WILDCARD) || other.subtype.equals(MEDIA_TYPE_WILDCARD)))
			return true;
		else
			return this.type.equalsIgnoreCase(other.type)
					&& this.subtype.equalsIgnoreCase(other.subtype);
	}

	/**
	 * Compares obj to this media type to see if they are the same by comparing
	 * type, subtype and parameters. Note that the case-sensitivity of parameter
	 * values is dependent on the semantics of the parameter name, see
	 * {@link <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.7">HTTP/1.1</a>}.
	 * This method assumes that values are case-sensitive.
	 *
	 * @param obj the object to compare to
	 * @return true if the two media types are the same, false otherwise.
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (!(obj instanceof MediaType))
			return false;
		MediaType other = (MediaType) obj;
		return (this.type.equalsIgnoreCase(other.type)
				&& this.subtype.equalsIgnoreCase(other.subtype)
				&& this.parameters.equals(other.parameters));
	}

	/**
	 * Generate a hashcode from the type, subtype and parameters.
	 *
	 * @return a hashcode
	 */
	@Override
	public int hashCode() {
		return (this.type.toLowerCase() + this.subtype.toLowerCase()).hashCode() + this.parameters.hashCode();
	}


	/**
	 * Creates a new instance of MediaType by parsing the supplied string.
	 *
	 * @param mtype the media type string
	 * @return the newly created MediaType
	 * @throws IllegalArgumentException if the supplied string cannot be parsed
	 *                                  or is null
	 */
	public static List<MediaType> valueOf(String mtype) {
		ArrayList<MediaType> types = new ArrayList<>();
		if (mtype == null)
			return types;
		//multiple media types are separated by commas
		Set<String> individualTypes = parseDelimitedType(mtype.length(), mtype, ",");
		for (String m : individualTypes) {
			//type and subtype is everything up to the first semi colon
			int idx = m.indexOf(";");
			MediaType type = new MediaType();
			String typeSubType = m;
			if (idx != -1) {
				typeSubType = m.substring(0, idx);
				String strParams = m.substring(idx + 1);
				//multiple media parameters are separated by semi-colons
				Set<String> params = parseDelimitedType(strParams.length(), strParams, ";");
				if (params.size() > 0) {
					type.parameters = new HashMap<>();
				}
				for (String p : params) {
					int eqIdx = p.indexOf("=");
					if (eqIdx != -1) {
						String name = p.substring(0, eqIdx);
						String value = p.substring(eqIdx + 1);
						if (value.startsWith("\"") && value.endsWith("\"")) {
							value = value.substring(1, value.length() - 1);
						}
						//add param and remove spurious whitespaces
						type.parameters.put(name.trim(), value.trim());
					}
				}
			}
			String[] parts = typeSubType.split("/");
			if (parts.length > 1) {
				type.type = parts[0];
				type.subtype = parts[1];
				//a media type is only valid if it has both type and subtype specified
				types.add(type);
			}
		}
		return types;
	}

	private static Set<String> parseDelimitedType(int length, String str, String separator) {
		HashSet<String> tmp = new HashSet<>();
		//parameter values can be quoted. if they are can contain commas,semi-colons and slashes
		//text/xhtml;q="a;bc,123/abc",text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8
		int start = 0, end = 0;
		String tmpChunk = "", chunk = "";
		while (end < length) {
			end = str.indexOf(separator);
			if (end == -1) {
				//just one media type or at the end
				if (!str.isEmpty()) {
					if (tmpChunk.isEmpty()) {
						tmp.add(str);
					} else {
						tmp.add(tmpChunk + str);
					}
				}
				end = length;
			} else {
				chunk = str.substring(start, end);
				str = str.substring(end);
				//if a quote occurred in the last chunk then the comma is part of a quoted parameter
				//go find the closing quote then get the comma after it BUT
				//if there has also only been ONE quote in the tmpChunk so far we need to keep going until
				// we get the closing quote
				if (chunk.lastIndexOf('"') != -1 || countOccurrences(tmpChunk, '"') == 1) {
					//+1 to remove the comma in the chunk
					str = str.substring(1);
					tmpChunk += chunk + separator;
					continue;
				} else {
					if (tmpChunk.endsWith(separator))
						tmpChunk = tmpChunk.substring(0, tmpChunk.length() - 1);
					if (chunk.endsWith(separator))
						chunk = chunk.substring(0, chunk.length() - 1);
					if (!tmpChunk.isEmpty())
						tmp.add(tmpChunk);
					if (!chunk.isEmpty())
						tmp.add(chunk);
					//reset
					chunk = "";
					tmpChunk = "";
					//+1 to remove the last comma seen
					str = str.substring(1);
				}
			}
		}
		return tmp;
	}

	public static int countOccurrences(String haystack, char needle) {
		int count = 0;
		for (int i = 0; i < haystack.length(); i++) {
			if (haystack.charAt(i) == needle) {
				count++;
			}
		}
		return count;
	}

	/**
	 * Convert the media type to a string suitable for use as the value of a
	 * corresponding HTTP header.
	 *
	 * @return a stringified media type
	 */
	@Override
	public String toString() {
		//see http://tools.ietf.org/html/rfc2046#page-29
		//and http://tools.ietf.org/html/rfc2046#page-18 (WARNING TO IMPLEMENTERS) bit
		//Content-Type: Message/Partial; number=2; total=3; id="oc=jpbe0M2Yt4s@thumper.bellcore.com"
		StringBuilder b = new StringBuilder();
		b.append(type).append("/").append(subtype);
		if (parameters.size() > 0) {
			b.append(";");
		}
		int added = 0;
		for (String name : parameters.keySet()) {
			String attribute = parameters.get(name);
			//ietf spec says " This is not always necessary, but never hurts."  so we always quote attribute values
			//results in ' name="attribute"'
			b.append(' ').append(name).append('=').append('"').append(attribute).append('"');
			//if more than one parameter is available we need to add a semicolon to all except the last
			if (parameters.size() > 1 && ++added < parameters.size()) {
				b.append(';');
			}
		}
		return b.toString();
	}
}
