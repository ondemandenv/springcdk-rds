package dev.odmd.platform.springcdk.common.logging

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.contrib.json.classic.JsonLayout
import org.slf4j.MDC

class DatadogJsonLayout : JsonLayout() {
    companion object{
        const val LZ_CORRELATION_ID_HEADER = "odmd-correlationId"
    }
    override fun addCustomDataToJsonMap(map: MutableMap<String, Any>?, event: ILoggingEvent?) {
        map?.put("dd.trace_id", datadogTraceId())
        map?.put("dd.span_id", datadogSpanId())
        MDC.get(LZ_CORRELATION_ID_HEADER)?.apply {
            map?.put(
                "Properties",
                mapOf(LZ_CORRELATION_ID_HEADER to this)
            )
        }
    }
}
