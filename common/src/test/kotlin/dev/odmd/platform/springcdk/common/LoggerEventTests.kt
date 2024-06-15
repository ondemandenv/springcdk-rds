package dev.odmd.platform.springcdk.common

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.event.Level

class LoggerEventTests {
    private class TestAppender(
        val events: MutableList<ILoggingEvent> = mutableListOf()
    ) : AppenderBase<ILoggingEvent>() {
        override fun append(eventObject: ILoggingEvent) {
            events.add(eventObject)
        }
    }

    private val testAppender = TestAppender()
    private val logger = getLogger<LoggerEventTests>()

    @BeforeEach
    fun beforeEach() {
        (logger as ch.qos.logback.classic.Logger).addAppender(testAppender)
        testAppender.start()
    }

    @AfterEach
    fun afterEach() {
        testAppender.stop()
        (logger as ch.qos.logback.classic.Logger).detachAppender(testAppender)
    }

    @Test
    fun `event(name, data) logs info message with data in MDC context at info level`() {
        val eventName = "name"
        val eventData = mapOf("foo" to "bar")
        logger.event(eventName = eventName, eventData = eventData)

        assertThat(testAppender.events, hasSize(1))

        val event = testAppender.events.first()
        assertThat(event.level.levelStr, equalTo(Level.INFO.name))
        assertThat(event.message, equalTo(eventName))
        assertThat(event.mdcPropertyMap, equalTo(eventData))
    }

    @Test
    fun `logging at different levels logs events with expected levels`() {
        val logDataOne = Triple("one", mapOf("one" to "one"), Level.INFO)
        val logDataTwo = Triple("one", mapOf("two" to "two"), Level.WARN)

        logger.event(
            eventName = logDataOne.first,
            eventData = logDataOne.second,
            logLevel = logDataOne.third
        )

        logger.event(
            eventName = logDataTwo.first,
            eventData = logDataTwo.second,
            logLevel = logDataTwo.third
        )

        assertThat(testAppender.events, hasSize(2))

        val eventOne = testAppender.events.first()
        assertThat(eventOne.message, equalTo(logDataOne.first))
        assertThat(eventOne.mdcPropertyMap, equalTo(logDataOne.second))
        assertThat(eventOne.level.levelStr, equalTo(logDataOne.third.name))

        val eventTwo = testAppender.events[1]
        assertThat(eventTwo.message, equalTo(logDataTwo.first))
        assertThat(eventTwo.mdcPropertyMap, equalTo(logDataTwo.second))
        assertThat(eventTwo.level.levelStr, equalTo(logDataTwo.third.name))
    }

    @Test
    fun `event(name, data, level) logs info message with data in MDC context at specified level`() {
        val eventName = "name"
        val eventData = mapOf("foo" to "bar")
        val level = Level.WARN
        logger.event(eventName = eventName, eventData = eventData, logLevel = level)

        assertThat(testAppender.events, hasSize(1))

        val event = testAppender.events.first()
        assertThat(event.level.levelStr, equalTo(level.name))
        assertThat(event.message, equalTo(eventName))
        assertThat(event.mdcPropertyMap, equalTo(eventData))
    }

    @Test
    fun `event(name, data, level) logs adds all pairs to context`() {
        val eventName = "name"
        val eventData = mapOf("foo" to "bar", "fuz" to "buz")
        logger.event(eventName = eventName, eventData = eventData)

        assertThat(testAppender.events, hasSize(1))

        val event = testAppender.events.first()
        assertThat(event.message, equalTo(eventName))
        assertThat(event.mdcPropertyMap, equalTo(eventData))
    }

    @Test
    fun `logging at disabled level does nothing`() {
        (logger as ch.qos.logback.classic.Logger).level = ch.qos.logback.classic.Level.ERROR

        logger.event("test", mapOf(), logLevel = Level.TRACE)

        assertThat(testAppender.events, hasSize(0))
    }
}
