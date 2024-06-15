package dev.odmd.platform.springcdk.common.logging

import ch.qos.logback.classic.pattern.ClassicConverter
import ch.qos.logback.classic.spi.ILoggingEvent

class DatadogSpanIdConverter : ClassicConverter() {
    override fun convert(event: ILoggingEvent?): String {
        return datadogSpanId()
    }
}
