package dev.odmd.platform.springcdk.common

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

internal class ExpirationFormatTest {
    @CsvSource(
        "2022, 11, 1122",
        "2022, 1,  0122",
        "2000, 11, 1100",
        "2001, 2,  0201",
        "25,   11, 1125",
        "25,   2,  0225"
    )
    @ParameterizedTest
    fun `returns last two digits of year and zero-padded month`(
        year: Int,
        month: Int,
        expectedString: String
    ) {
        assertThat(
            ExpirationFormat.twoDigitMonthAndYear(
                year = year,
                month = month
            ),
            equalTo(expectedString)
        )
    }
}
