package dev.odmd.platform.springcdk.domain.converters

import dev.odmd.platform.springcdk.common.CurrencyAmount
import java.math.BigDecimal
import javax.persistence.AttributeConverter
import javax.persistence.Converter

@Converter(autoApply = true)
class CurrencyAmountJpaConverter : AttributeConverter<CurrencyAmount, BigDecimal> {
    override fun convertToDatabaseColumn(amount: CurrencyAmount?): BigDecimal? {
        return amount?.amount
    }

    override fun convertToEntityAttribute(amount: BigDecimal?): CurrencyAmount? {
        return if (amount != null) {
            CurrencyAmount(amount)
        } else null
    }
}
