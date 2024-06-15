package dev.odmd.platform.springcdk.common

import java.math.BigDecimal


val BigDecimal.isZero
    /**
     * Checking for zero can be done using either [BigDecimal.compareTo] or [BigDecimal.signum].
     * This uses signum since compareTo calls signum internally.
     */
    get() = signum() == 0
