package com.lukekim.hcbridge

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HealthPayloadTest {

    @Test
    fun `전체 값이 있으면 네 항목과 날짜가 모두 붙는다`() {
        val url = HealthPayload.buildUrl(
            steps = 8432L,
            sleepHours = 7.5,
            systolic = 128,
            diastolic = 82,
            date = LocalDate.of(2026, 11, 28)
        )
        assertEquals(
            "https://lukekim777.github.io/luke-routine-app/plus/" +
                "?hc_steps=8432&hc_sleep=7.5&hc_sbp=128&hc_dbp=82&hc_date=2026-11-28",
            url
        )
    }

    @Test
    fun `없는 값은 파라미터에서 빠진다`() {
        val url = HealthPayload.buildUrl(
            steps = 5210L,
            sleepHours = null,
            systolic = null,
            diastolic = null,
            date = LocalDate.of(2026, 11, 28)
        )
        assertEquals(
            "https://lukekim777.github.io/luke-routine-app/plus/?hc_steps=5210&hc_date=2026-11-28",
            url
        )
    }

    @Test
    fun `혈압만 있어도 주소가 만들어진다`() {
        val url = HealthPayload.buildUrl(
            steps = null,
            sleepHours = null,
            systolic = 135,
            diastolic = 88,
            date = null
        )
        assertEquals(
            "https://lukekim777.github.io/luke-routine-app/plus/?hc_sbp=135&hc_dbp=88",
            url
        )
    }

    @Test
    fun `값이 하나도 없으면 앱 주소만 남는다`() {
        assertEquals(
            HealthPayload.APP_URL,
            HealthPayload.buildUrl(null, null, null, null, null)
        )
        assertFalse(HealthPayload.hasAnyValue(null, null, null, null))
        assertFalse(HealthPayload.hasAnyValue(0L, 0.0, 0, 0))
        assertTrue(HealthPayload.hasAnyValue(1L, null, null, null))
    }

    @Test
    fun `수면 분을 시간 소수 1자리로 반올림한다`() {
        assertEquals(7.5, HealthPayload.roundMinutesToHours(450L), 0.0001)
        assertEquals(7.4, HealthPayload.roundMinutesToHours(445L), 0.0001)
        assertEquals(7.5, HealthPayload.roundMinutesToHours(448L), 0.0001)
        assertEquals(0.5, HealthPayload.roundMinutesToHours(30L), 0.0001)
        assertEquals(8.0, HealthPayload.roundMinutesToHours(480L), 0.0001)
        assertEquals(0.0, HealthPayload.roundMinutesToHours(0L), 0.0001)
    }
}
