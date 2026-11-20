package de.knurl.feature.common

import de.knurl.data.WeightUnit
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class FormattingTest {
    private val de = Locale.GERMAN
    private val en = Locale.ENGLISH

    @Test fun kgUsesTheLocaleSeparator() {
        assertEquals("102,5", Formatting.kg(102.5, de))
        assertEquals("102.5", Formatting.kg(102.5, en))
        assertEquals("100", Formatting.kg(100.0, de))
    }

    @Test fun tonnageSwitchesToTonnes() {
        assertEquals("850 kg", Formatting.tonnage(850.0, WeightUnit.KG, en))
        assertEquals("18,4 t", Formatting.tonnage(18_400.0, WeightUnit.KG, de))
        assertEquals("2,205 lb", Formatting.tonnage(1000.0, WeightUnit.LB, en))
    }

    @Test fun poundsAreConvertedForDisplayOnly() {
        assertEquals("100 kg", Formatting.weight(100.0, WeightUnit.KG, en))
        assertEquals("220.5 lb", Formatting.weight(100.0, WeightUnit.LB, en))
        assertEquals(100.0, Formatting.fromUnit(Formatting.toUnit(100.0, WeightUnit.LB), WeightUnit.LB), 1e-9)
    }

    @Test fun durations() {
        assertEquals("1:24", Formatting.duration(84))
        assertEquals("1:02:03", Formatting.duration(3723))
    }

    @Test fun bodyweightSetsShowRepsOnly() = assertEquals("12", Formatting.set(0.0, 12, WeightUnit.KG, en))
}
