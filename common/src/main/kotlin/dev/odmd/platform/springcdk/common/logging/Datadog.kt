package dev.odmd.platform.springcdk.common.logging

import io.opentelemetry.api.trace.Span

/**
 * Converts opentelemetry traceId to Datadog format traceId to support log correlation.
 * Copied from [here](https://docs.datadoghq.com/tracing/connect_logs_and_traces/opentelemetry/)
 */
fun datadogTraceId(): String {
    val traceIdValue = Span.current().spanContext.traceId
    val traceIdHexString = traceIdValue.substring(traceIdValue.length - 16)
    val datadogTraceId = java.lang.Long.parseUnsignedLong(traceIdHexString, 16)
    val datadogTraceIdString = java.lang.Long.toUnsignedString(datadogTraceId)

    return datadogTraceIdString
}

/**
 * Converts opentelemetry spanId to Datadog format spanId to support log correlation.
 * Copied from [here](https://docs.datadoghq.com/tracing/connect_logs_and_traces/opentelemetry/)
 */
fun datadogSpanId(): String {
    val spanIdValue = Span.current().spanContext.spanId
    val spanIdHexString = spanIdValue.substring(spanIdValue.length - 16)
    val datadogSpanId = java.lang.Long.parseUnsignedLong(spanIdHexString, 16)
    val datadogSpanIdString = java.lang.Long.toUnsignedString(datadogSpanId)

    return datadogSpanIdString
}
