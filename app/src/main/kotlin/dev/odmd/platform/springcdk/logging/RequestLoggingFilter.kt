package dev.odmd.platform.springcdk.logging

import dev.odmd.platform.springcdk.common.asMDCContext
import dev.odmd.platform.springcdk.common.event
import dev.odmd.platform.springcdk.common.logging.DatadogJsonLayout
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.slf4j.event.Level
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.web.servlet.HandlerMapping
import javax.servlet.Filter
import javax.servlet.FilterChain
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

@Configuration
class RequestLoggingFilterConfiguration {
    @Bean
    fun requestLoggingFilter(): FilterRegistrationBean<RequestLoggingFilter> {
        val filter = RequestLoggingFilter()
        val filterRegistration = FilterRegistrationBean(filter)
        filterRegistration.order = Ordered.LOWEST_PRECEDENCE

        return filterRegistration
    }
}

@OptIn(ExperimentalTime::class)
class RequestLoggingFilter(private val logger: Logger = LoggerFactory.getLogger(RequestLoggingFilter::class.java)) :
    Filter {
    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        if (request !is HttpServletRequest) {
            chain.doFilter(request, response)
            return
        }

        val host = request.remoteHost
        val path = request.servletPath
        val qs = request.queryString?.let { "?$it" } ?: ""
        val method = request.method
        request
            .getHeader(DatadogJsonLayout.LZ_CORRELATION_ID_HEADER)
            ?.also { MDC.put(DatadogJsonLayout.LZ_CORRELATION_ID_HEADER, it) }

        val result = mapOf(
            "method" to method,
            "path" to path,
            "pathTemplate" to request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE),
            "queryString" to qs,
            "host" to host
        ).asMDCContext {
            logger.info("request.started")

            val (result, duration) = measureTimedValue {
                runCatching {
                    chain.doFilter(request, response)
                }.map {
                    response
                }
            }

            logTimedResult(result, duration)

            result
        }

        MDC.remove(DatadogJsonLayout.LZ_CORRELATION_ID_HEADER)

        result.getOrThrow()
    }

    private fun logTimedResult(result: Result<ServletResponse>, duration: Duration) {
        val baseEventData = mapOf<String, Any?>(
            "duration.ms" to duration.inWholeMilliseconds,
            "success" to result.isSuccess
        )

        val resultEventData = result.fold(
            onSuccess = { response ->
                if (response is HttpServletResponse) {
                    val code = response.status
                    mapOf("responseCode" to code)
                } else {
                    emptyMap()
                }
            },
            onFailure = { e ->
                mapOf(
                    "errorClassName" to e.javaClass.name,
                    "errorMessage" to (e.message ?: "")
                )
            }
        )

        logger.event(
            eventName = "request.processed",
            eventData = baseEventData + resultEventData,
            logLevel = if (result.isSuccess) {
                Level.INFO
            } else {
                Level.WARN
            }
        )
    }
}
