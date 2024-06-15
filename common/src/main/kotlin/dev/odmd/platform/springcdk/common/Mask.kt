package dev.odmd.platform.springcdk.common

import kotlin.math.ceil

enum class MaskingMode {
    ASTERISK,
    LAST_FOUR,
    FIRST_FOUR
}

fun withAsterisks(input: String): String {
    val lengthToAsterisk = ceil(input.length / 2.0).toInt()
    return input.replaceRange(0, lengthToAsterisk, "*".repeat(lengthToAsterisk))
}

fun takeLastFour(input: String): String = input.takeLast(4)

fun Any.mask(mode: MaskingMode): String = toString().let{
    when (mode) {
        MaskingMode.ASTERISK -> withAsterisks(it)
        MaskingMode.LAST_FOUR -> takeLastFour(it)
        MaskingMode.FIRST_FOUR -> it.take(4)
    }
}