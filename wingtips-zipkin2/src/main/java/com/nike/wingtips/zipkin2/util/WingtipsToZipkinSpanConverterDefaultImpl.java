package com.nike.wingtips.zipkin2.util;

import com.nike.wingtips.Span;
import com.nike.wingtips.Span.SpanPurpose;

import com.nike.wingtips.TraceAndSpanIdGenerator;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

import zipkin2.Endpoint;

/**
 * Default implementation of {@link WingtipsToZipkinSpanConverter} that knows how to convert a Wingtips span to a
 * Zipkin span.
 *
 * @author Nic Munroe
 */
public class WingtipsToZipkinSpanConverterDefaultImpl implements WingtipsToZipkinSpanConverter {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final String LOWER_HEX_DICTIONARY = "1234567890abcdef";

    @Override
    public zipkin2.Span convertWingtipsSpanToZipkinSpan(Span wingtipsSpan, Endpoint zipkinEndpoint) {
        long durationMicros = TimeUnit.NANOSECONDS.toMicros(wingtipsSpan.getDurationNanos());

        return zipkin2.Span
            .newBuilder()
            .id(sanitizeIdIfNecessary(wingtipsSpan.getSpanId(), false))
            .name(wingtipsSpan.getSpanName())
            .parentId(sanitizeIdIfNecessary(wingtipsSpan.getParentSpanId(), false))
            .traceId(sanitizeIdIfNecessary(wingtipsSpan.getTraceId(), true))
            .timestamp(wingtipsSpan.getSpanStartTimeEpochMicros())
            .duration(durationMicros)
            .localEndpoint(zipkinEndpoint)
            .kind(determineZipkinKind(wingtipsSpan))
            .build();
    }

    @SuppressWarnings("WeakerAccess")
    protected zipkin2.Span.Kind determineZipkinKind(Span wingtipsSpan) {
        SpanPurpose wtsp = wingtipsSpan.getSpanPurpose();

        // Clunky if checks necessary to avoid code coverage gaps with a switch statement
        //      due to unreachable default case. :(
        if (SpanPurpose.SERVER == wtsp) {
            return zipkin2.Span.Kind.SERVER;
        }
        else if (SpanPurpose.CLIENT == wtsp) {
            return zipkin2.Span.Kind.CLIENT;
        }
        else if (SpanPurpose.LOCAL_ONLY == wtsp || SpanPurpose.UNKNOWN == wtsp) {
            // No Zipkin Kind associated with these SpanPurposes.
            return null;
        }
        else {
            // This case should technically be impossible, but in case it happens we'll log a warning and default to
            //      no Zipkin kind.
            logger.warn("Unhandled SpanPurpose type: {}", String.valueOf(wtsp));
            return null;
        }
    }

    protected String sanitizeIdIfNecessary(final String originalId, final boolean allow128Bit) {
        if (originalId == null) {
            return originalId;
        }

        if (isLowerHex(originalId) && isAllowedNumChars(originalId, allow128Bit)) {
            return originalId;
        }

        // If the origId can be parsed as a long, then its sanitized ID is the lower-hex representation of that long.
        final Long originalIdAsRawLong = attemptToConvertToLong(originalId);
        if (originalIdAsRawLong != null) {
            return TraceAndSpanIdGenerator.longToUnsignedLowerHexString(originalIdAsRawLong);
        }

        //  Do a SHA256 hash of the original ID to get a valid sanitized lower-hex ID that can be
        //      converted to a long, but only take the number of characters we're allowed to take. Truncation
        //      of a SHA digest like this is specifically allowed by the SHA algorithm - see Section 7
        //      ("TRUNCATION OF A MESSAGE DIGEST") here:
        //      https://csrc.nist.gov/csrc/media/publications/fips/180/4/final/documents/fips180-4-draft-aug2014.pdf
        final int allowedNumChars = allow128Bit ? 32 : 16;
        return DigestUtils.sha256Hex(originalId).toLowerCase().substring(0, allowedNumChars - 1);
    }

    private boolean isLowerHex(final String id) {
        for (final char c : id.toCharArray()) {
            if (!LOWER_HEX_DICTIONARY.contains(String.valueOf(c))) {
                return false;
            }
        }
        return true;
    }

    private boolean isAllowedNumChars(final String id, final boolean allow128Bit) {
        if (allow128Bit) {
            return id.length() <= 16 || id.length() == 32;
        } else {
            return id.length() <= 16;
        }
    }

    private Long attemptToConvertToLong(final String id) {
        try {
            return Long.valueOf(id);
        } catch (final NumberFormatException nfe) {
            return null;
        }
    }
}
