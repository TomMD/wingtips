package com.nike.wingtips.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nike.internal.util.StringUtils;
import com.nike.wingtips.Span;
import com.nike.wingtips.Span.SpanPurpose;

public class SpanParser {

	private static final Logger logger = LoggerFactory.getLogger(SpanParser.class);
	
	/** The name of the trace ID field when serializing/deserializing to/from JSON (see {@link #toJSON()} and {@link #fromJSON(String)}). Corresponds to {@link #getTraceId()}. */
    public static final String TRACE_ID_FIELD = "traceId";
    /** The name of the parent span ID field when serializing/deserializing to/from JSON (see {@link #toJSON()} and {@link #fromJSON(String)}). Corresponds to {@link #getParentSpanId()}. */
    public static final String PARENT_SPAN_ID_FIELD = "parentSpanId";
    /** The name of the span ID field when serializing/deserializing to/from JSON (see {@link #toJSON()} and {@link #fromJSON(String)}). Corresponds to {@link #getSpanId()}. */
    public static final String SPAN_ID_FIELD = "spanId";
    /** The name of the span name field when serializing/deserializing to/from JSON (see {@link #toJSON()} and {@link #fromJSON(String)}). Corresponds to {@link #getSpanName()}. */
    public static final String SPAN_NAME_FIELD = "spanName";
    /** The name of the sampleable field when serializing/deserializing to/from JSON (see {@link #toJSON()} and {@link #fromJSON(String)}). Corresponds to {@link #isSampleable()}. */
    public static final String SAMPLEABLE_FIELD = "sampleable";
    /** The name of the user ID field when serializing/deserializing to/from JSON (see {@link #toJSON()} and {@link #fromJSON(String)}). Corresponds to {@link #getUserId()}. */
    public static final String USER_ID_FIELD = "userId";
    /** The name of the span purpose field when serializing to JSON (see {@link #toJSON()}. Corresponds to {@link #getSpanPurpose()}. */
    public static final String SPAN_PURPOSE_FIELD = "spanPurpose";
    /** The name of the start-time-in-epoch-micros field when serializing/deserializing to/from JSON (see {@link #toJSON()} and {@link #fromJSON(String)}). Corresponds to {@link #getSpanStartTimeNanos()}. */
    public static final String START_TIME_EPOCH_MICROS_FIELD = "startTimeEpochMicros";
    /** The name of the duration-in-nanoseconds field when serializing to JSON (see {@link #toJSON()}. Corresponds to {@link #getDurationNanos()}. */
    public static final String DURATION_NANOS_FIELD = "durationNanos";
    /** The name of the span tags field when serializing to JSON (see {@link #toJSON()}. Corresponds to {@link #getTags()}. */
    public static final String TAGS_FIELD = "tags";
    
	public static Span parseKeyValueFormat(String input) {
		return null;
	}
	
	public static String convertSpanToKeyValueFormat(Span span) {
		 StringBuilder builder = new StringBuilder();

        builder.append(TRACE_ID_FIELD).append("=").append(span.getTraceId());
        builder.append(",").append(PARENT_SPAN_ID_FIELD).append("=").append(span.getParentSpanId());
        builder.append(",").append(SPAN_ID_FIELD).append("=").append(span.getSpanId());
        builder.append(",").append(SPAN_NAME_FIELD).append("=").append(span.getSpanName());
        builder.append(",").append(SAMPLEABLE_FIELD).append("=").append(span.isSampleable());
        builder.append(",").append(USER_ID_FIELD).append("=").append(span.getUserId());
        builder.append(",").append(SPAN_PURPOSE_FIELD).append("=").append(span.getSpanPurpose().name());
        builder.append(",").append(START_TIME_EPOCH_MICROS_FIELD).append("=").append(span.getSpanStartTimeEpochMicros());
        if (span.isCompleted()) {
            builder.append(",").append(DURATION_NANOS_FIELD).append("=").append(span.getDurationNanos());
        }
        if (span.getTags() != null && !span.getTags().isEmpty()) 
        		builder.append(",").append(TAGS_FIELD).append("=").append(convertMapToPipeDelimitedString(span.getTags()));
        
        return builder.toString();
	}
	
	/**
     * Calculates and returns the JSON representation of this span instance. We build this manually ourselves to avoid pulling in an extra dependency
     * (e.g. Jackson) just for building a simple JSON string.
     */
	public static String convertSpanToJSON(Span span) {
		StringBuilder builder = new StringBuilder();

        builder.append("{\"").append(TRACE_ID_FIELD).append("\":\"").append(span.getTraceId());
        builder.append("\",\"").append(PARENT_SPAN_ID_FIELD).append("\":\"").append(span.getParentSpanId());
        builder.append("\",\"").append(SPAN_ID_FIELD).append("\":\"").append(span.getSpanId());
        builder.append("\",\"").append(SPAN_NAME_FIELD).append("\":\"").append(span.getSpanName());
        builder.append("\",\"").append(SAMPLEABLE_FIELD).append("\":\"").append(span.isSampleable());
        builder.append("\",\"").append(USER_ID_FIELD).append("\":\"").append(span.getUserId());
        builder.append("\",\"").append(SPAN_PURPOSE_FIELD).append("\":\"").append(span.getSpanPurpose().name());
        builder.append("\",\"").append(START_TIME_EPOCH_MICROS_FIELD).append("\":\"").append(span.getSpanStartTimeEpochMicros());
        if (span.isCompleted()) {
            builder.append("\",\"").append(DURATION_NANOS_FIELD).append("\":\"").append(span.getDurationNanos());
        }
        if(!span.getTags().isEmpty()) {
        		builder.append("\",\"").append(TAGS_FIELD).append("\":\"").append(convertMapToPipeDelimitedString(span.getTags()));
        }
        builder.append("\"}");

        return builder.toString();
	}
    
    /**
     * @return The {@link Span} represented by the given key/value string, or null if a proper span could not be deserialized from the given string.
     *          <b>WARNING:</b> This method assumes the string you're trying to deserialize originally came from
     *          {@link #toKeyValueString()}. This assumption allows it to be as fast as possible, not worry about syntactically-correct-but-annoying-to-deal-with whitespace,
     *          not have to use a third party utility, etc.
     */
    public static Span fromKeyValueString(String keyValueStr) {
        try {
            // Create a map of keys to values.
            Map<String, String> map = new HashMap<>();

            // Split on the commas that separate the key/value pairs.
            String[] fieldPairs = keyValueStr.split(",");
            for (String fieldPair : fieldPairs) {
                // Split again on the equals character that separate the field's key from its value.
                String[] keyVal = fieldPair.split("=");
                map.put(keyVal[0], keyVal[1]);
            }

            return fromKeyValueMap(map);
        } catch (Exception e) {
            logger.error("Error extracting Span from key/value string. Defaulting to null. bad_span_key_value_string={}", keyValueStr, e);
            return null;
        }
    }

    /**
     * @return The {@link Span} represented by the given JSON string, or null if a proper span could not be deserialized from the given string.
     *          <b>WARNING:</b> This method assumes the JSON you're trying to deserialize originally came from {@link #toJSON()}.
     *          This assumption allows it to be as fast as possible, not have to check for malformed JSON, not worry about syntactically-correct-but-annoying-to-deal-with whitespace,
     *          not have to use a third party utility like Jackson, etc.
     */
    public static Span fromJSON(String json) {
        try {
            // Create a map of JSON field keys to values.
            Map<String, String> map = new HashMap<>();

            // Strip off the {" and "} at the beginning/end.
            String innerJsonCore = json.substring(2, json.length() - 2);
            // Split on the doublequotes-comma-doublequotes that separate the fields.
            String[] fieldPairs = innerJsonCore.split("\",\"");
            for (String fieldPair : fieldPairs) {
                // Split again on the doublequotes-colon-doublequotes that separate the field's key from its value. At this point all double-quotes have been stripped off
                // and we can just map the key to the value.
                String[] keyVal = fieldPair.split("\":\"");
                map.put(keyVal[0], keyVal[1]);
            }

            return fromKeyValueMap(map);
        } catch (Exception e) {
            logger.error("Error extracting Span from JSON. Defaulting to null. bad_span_json={}", json, e);
            return null;
        }
    }

    private static Span fromKeyValueMap(Map<String, String> map) {
        // Use the map to get the field values for the span.
        String traceId = nullSafeGetString(map, TRACE_ID_FIELD);
        String spanId = nullSafeGetString(map, SPAN_ID_FIELD);
        String parentSpanId = nullSafeGetString(map, PARENT_SPAN_ID_FIELD);
        String spanName = nullSafeGetString(map, SPAN_NAME_FIELD);
        Boolean sampleable = nullSafeGetBoolean(map, SAMPLEABLE_FIELD);
        if (sampleable == null)
            throw new IllegalStateException("Unable to parse " + SAMPLEABLE_FIELD + " from JSON");
        String userId = nullSafeGetString(map, USER_ID_FIELD);
        Long startTimeEpochMicros = nullSafeGetLong(map, START_TIME_EPOCH_MICROS_FIELD);
        if (startTimeEpochMicros == null)
            throw new IllegalStateException("Unable to parse " + START_TIME_EPOCH_MICROS_FIELD + " from JSON");
        Long durationNanos = nullSafeGetLong(map, DURATION_NANOS_FIELD);
        SpanPurpose spanPurpose = nullSafeGetSpanPurpose(map, SPAN_PURPOSE_FIELD);
        Map<String,String> tags = parsePipeDelimitedString(nullSafeGetString(map, TAGS_FIELD));
        return new Span(traceId, parentSpanId, spanId, spanName, sampleable, userId, spanPurpose, startTimeEpochMicros, null, durationNanos, tags);
    }
    
    /**
     * Takes a map and stores the {@code key} and {@code value} in a specific format that won't be split apart by the other 
     * parsers in this {@code class}. 
     * <pre>
     * Given map [{key1,value1},{key2,value2}] the resulting string will be:
     * "key1":"value1"|"key2":"value2"
     * </pre>
     * @param map
     * @return
     * @todo Could have a ConcurrentModificationException if tags are added while iterating through
     */
    static String convertMapToPipeDelimitedString(Map<String, String> map) {
    		StringBuilder builder = new StringBuilder();
		if(map != null && map.size() > 0) {
			Iterator<Map.Entry<String, String>> it = map.entrySet().iterator();
		    while (it.hasNext()) {
		        Map.Entry<String,String> pair = (Map.Entry<String,String>)it.next();
		        builder.append("'").append(pair.getKey()).append("':'").append(pair.getValue()).append("'");
		        if(it.hasNext())
		        		builder.append(">");
		    }
		} 

		return builder.toString();
    }
    
    static Map<String,String> parsePipeDelimitedString(String pipeDelimitedString) {
    		if(StringUtils.isEmpty(pipeDelimitedString))
    			return Collections.emptyMap();
    		return parseDelimitedPairs(pipeDelimitedString, "'>'", "':'");
    }
    
    private static Map<String,String> parseDelimitedPairs(String toBeParsed, String pairDelimiter, String keyValueDelimiter) {
    		Map<String, String> map = new HashMap<>();

		//Split the pairs apart based on the first pattern
        String[] fieldPairs = toBeParsed.split(pairDelimiter);
        for (String fieldPair : fieldPairs) {
            // Split again on the pattern that separate the field's key from its value. 
            String[] keyVal = fieldPair.split(keyValueDelimiter);
            map.put(keyVal[0], keyVal[1]);
        }

        return map;	
    }
    
    private static String nullSafeGetString(Map<String, String> map, String key) {
        String value = map.get(key);
        if (value == null || value.equals("null"))
            return null;

        return value;
    }

    private static Long nullSafeGetLong(Map<String, String> map, String key) {
        String value = nullSafeGetString(map, key);
        if (value == null)
            return null;

        return Long.parseLong(value);
    }

    private static Boolean nullSafeGetBoolean(Map<String, String> map, String key) {
        String value = nullSafeGetString(map, key);
        if (value == null)
            return null;

        return Boolean.parseBoolean(value);
    }

    private static SpanPurpose nullSafeGetSpanPurpose(Map<String, String> map, String key) {
        String value = nullSafeGetString(map, key);
        if (value == null)
            return null;

        try {
            return SpanPurpose.valueOf(value);
        }
        catch(Exception ex) {
            logger.warn("Unable to parse \"{}\" to a SpanPurpose enum. Received exception: {}", value, ex.toString());
            return null;
        }
    }
}
