package com.lukekim.hcbridge

import java.time.LocalDate
import java.util.Locale
import kotlin.math.roundToLong

/**
 * 하루+ 웹 앱으로 넘길 값들을 URL로 만드는 순수 함수 모음입니다.
 * (Android API에 의존하지 않아 단위 테스트로 검증합니다.)
 */
object HealthPayload {

    const val APP_URL = "https://lukekim777.github.io/luke-routine-app/plus/"

    /**
     * 값이 있는 항목만 쿼리 파라미터로 붙입니다.
     * 예) https://lukekim777.github.io/luke-routine-app/plus/?hc_steps=8432&hc_sleep=7.5&hc_sbp=128&hc_dbp=82&hc_date=2026-11-28
     */
    fun buildUrl(
        steps: Long?,
        sleepHours: Double?,
        systolic: Int?,
        diastolic: Int?,
        date: LocalDate?
    ): String {
        val params = mutableListOf<String>()
        if (steps != null && steps > 0) params.add("hc_steps=${steps}")
        if (sleepHours != null && sleepHours > 0) {
            params.add("hc_sleep=" + String.format(Locale.US, "%.1f", sleepHours))
        }
        if (systolic != null && systolic > 0) params.add("hc_sbp=$systolic")
        if (diastolic != null && diastolic > 0) params.add("hc_dbp=$diastolic")
        if (date != null) params.add("hc_date=$date")

        return if (params.isEmpty()) APP_URL else APP_URL + "?" + params.joinToString("&")
    }

    fun hasAnyValue(steps: Long?, sleepHours: Double?, systolic: Int?, diastolic: Int?): Boolean =
        (steps != null && steps > 0) ||
            (sleepHours != null && sleepHours > 0) ||
            (systolic != null && systolic > 0) ||
            (diastolic != null && diastolic > 0)

    /** 수면 시간(분)을 시간 단위 소수 1자리로 반올림합니다. 예) 450분 → 7.5 */
    fun roundMinutesToHours(minutes: Long): Double {
        if (minutes <= 0L) return 0.0
        return (minutes / 6.0).roundToLong() / 10.0
    }
}
