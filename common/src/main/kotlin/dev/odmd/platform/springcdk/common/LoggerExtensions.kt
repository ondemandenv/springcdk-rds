package dev.odmd.platform.springcdk.common

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.slf4j.event.Level

/**
 * Logs a named event with provided eventData as MDC properties.
 *
 * Example:
 *      logger.event("upload.started", mapOf(
 *          "userId" to user.id,
 *          "file" to filePath
 *      ))
 *
 * @param eventName Name of the structured log event.
 *
 * @param eventData Map of key/value pairs which will be set as explicit fields in the resulting log message.
 *
 * @param logLevel The level that should be used when logging the message.
 */
fun Logger.event(
    eventName: String?,
    eventData: Map<String, Any?>,
    logLevel: Level = Level.INFO
) {
    if (!this.isEnabled(logLevel)) {
        return
    }

    return eventData.asMDCContext {
        log(message = eventName, level = logLevel)
    }
}

/**
 * Populate [MDC] context map using the receiver, scoped to the [block] argument.
 *
 * Useful when you want multiple log statements to have the same properties. See [Logger.event] for an example.
 *
 * @return The value returned by [block].
 */
fun <T> Map<String, Any?>.asMDCContext(block: () -> T): T {
    val oldContextMap = MDC.getCopyOfContextMap() ?: emptyMap()

    MDC.setContextMap(oldContextMap + this.mapValues { it.value.toString() })

    try {
        return block()
    } finally {
        MDC.setContextMap(oldContextMap)
    }
}

inline fun <reified T> getLogger(): Logger = LoggerFactory.getLogger(T::class.java)

fun Logger.isEnabled(level: Level) =
    when (level) {
        Level.TRACE -> isTraceEnabled
        Level.DEBUG -> isDebugEnabled
        Level.INFO -> isInfoEnabled
        Level.WARN -> isWarnEnabled
        Level.ERROR -> isErrorEnabled
    }

fun Logger.log(message: String?, level: Level) {
    val method: (String?) -> Unit = when (level) {
        Level.TRACE -> ::trace
        Level.DEBUG -> ::debug
        Level.INFO -> ::info
        Level.WARN -> ::warn
        Level.ERROR -> ::error
    }
    method(message)
}
