package de.knurl.domain.calc

import kotlin.test.Test
import kotlin.test.assertEquals

class CalibrationTest {
    @Test fun easyGoesUpAboutTenPercentOnAPlateStep() {
        assertEquals(67.5, Calibration.next(60.0, Feel.EASY, 2.5))
        assertEquals(22.0, Calibration.next(20.0, Feel.EASY, 2.0))
    }

    @Test fun hardGoesDownAtLeastOneStep() {
        assertEquals(52.5, Calibration.next(60.0, Feel.HARD, 2.5))
        assertEquals(7.5, Calibration.next(10.0, Feel.HARD, 2.5))
        assertEquals(0.0, Calibration.next(2.5, Feel.HARD, 2.5))
    }

    @Test fun rightOrBodyweightStays() {
        assertEquals(60.0, Calibration.next(60.0, Feel.RIGHT, 2.5))
        assertEquals(0.0, Calibration.next(0.0, Feel.EASY, 2.5))
    }
}
